package app.doqa.junit5;

import app.doqa.core.AdapterRuntime;
import app.doqa.core.Attribution;
import app.doqa.core.DoqaSession;
import app.doqa.core.Placeholders;
import app.doqa.core.TestRef;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;

/**
 * Mode-0 selective execution: at discovery, keep only the test methods whose resolved
 * {@code externalId} is in the run's selected list ({@code GET /test-runs/{id}/autotests}).
 * Auto-registered via
 * {@code META-INF/services/org.junit.platform.launcher.PostDiscoveryFilter} (LauncherConfig enables
 * post-discovery-filter auto-registration by default).
 *
 * <p>Runs before the listener, so it lazily triggers {@link DoqaSession#getOrInit()} (which, for
 * mode 0, fetches the selective list). For any other mode / disabled session, everything is
 * included (no filtering). Filtering is applied at the <em>method</em> descriptor granularity: a
 * {@code @ParameterizedTest} template is included/excluded as a whole (its externalId collapses to
 * method level), a {@code @TestFactory} container is always included (its dynamic tests do not
 * exist at discovery - report-time gating filters them precisely), and class containers are always
 * included so their children filter individually. Any resolution error includes the descriptor -
 * reporting machinery must never break the user's discovery.
 */
public class DoqaSelectFilter implements PostDiscoveryFilter {

    static {
        AdapterRuntime.configure("junit5", "junit-platform");
    }

    @Override
    public FilterResult apply(TestDescriptor descriptor) {
        try {
            return applySafely(descriptor);
        } catch (Throwable t) {
            // e.g. NoClassDefFoundError raised while introspecting a broken test class
            return FilterResult.included("DoQA selection failed to resolve, keeping: " + t);
        }
    }

    private static FilterResult applySafely(TestDescriptor descriptor) {
        // Only mode-0 selection filters at discovery. Gating on the resolved config avoids
        // forcing session init in other modes: in mode 2 that would create a run on the server
        // merely because an IDE discovered the tests.
        if (!DoqaSession.discoverySelectionActive()) {
            return FilterResult.included("DoQA mode-0 selection not active");
        }
        DoqaSession session;
        try {
            session = DoqaSession.getOrInit();
        } catch (RuntimeException e) {
            return FilterResult.included("DoQA session init failed, no filtering");
        }
        if (!session.enabled || session.runContext == null
                || session.runContext.selectedExternalIds() == null) {
            // disabled, files sink (no run) or non-selective mode: nothing to filter.
            return FilterResult.included("DoQA mode-0 selection not active");
        }

        Optional<TestSource> sourceOpt = descriptor.getSource();
        if (!sourceOpt.isPresent() || !(sourceOpt.get() instanceof MethodSource)) {
            // class / engine containers and non-method sources are kept; children filter themselves.
            return FilterResult.included("not a test method");
        }
        if (TestRefs.isFactoryContainer(descriptor.getUniqueId())) {
            // dynamic tests are born at execution time with their own display names - their ids
            // can never match the factory's discovery-time id; keep the container and let
            // report-time gating (runContext.allows) filter the individual dynamic tests.
            return FilterResult.included("dynamic container; ids re-checked at report time");
        }
        MethodSource ms = (MethodSource) sourceOpt.get();
        TestRef ref = TestRefs.fromDescriptor(descriptor, ms);

        String externalId = Attribution.resolve(ref).externalId;

        // A parameterized externalId template ("login_{browser}") cannot be compared literally at
        // discovery; include the whole template if any selected id matches its wildcard form;
        // the exact per-invocation ids are re-checked at report time.
        if (Placeholders.hasPlaceholder(externalId)) {
            Pattern p = Placeholders.templateToRegex(externalId);
            for (String selected : session.runContext.selectedExternalIds()) {
                if (p.matcher(selected).matches()) {
                    return FilterResult.included("template matches selected: " + externalId);
                }
            }
            return FilterResult.excluded("no selected id matches template: " + externalId);
        }

        if (session.runContext.allows(externalId)) {
            return FilterResult.included("selected: " + externalId);
        }
        return FilterResult.excluded("deselected (not in run's autotest list): " + externalId);
    }
}
