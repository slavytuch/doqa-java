package app.doqa.junit5;

import app.doqa.client.RunContext;
import app.doqa.core.Attribution;
import app.doqa.core.DoqaSession;
import app.doqa.core.Placeholders;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Nested;

/**
 * Shared plan-position resolution for {@link DoqaPlanMethodOrderer} / {@link DoqaPlanClassOrderer}.
 * Reuses the same {@link Attribution#resolve} externalId attribution as {@link DoqaSelectFilter},
 * so ordering and mode-0 selection agree on identities. Resolved method ids are cached - the
 * class orderer scans every method of every class and the method orderer resolves them again.
 *
 * <p>Ordering never changes the set of tests: ids outside the plan get
 * {@link Integer#MAX_VALUE} and stay at the tail of a stable sort; any resolution failure degrades
 * to "no reordering" rather than breaking the build.
 */
final class PlanOrdering {

    /** Method -> resolved externalId (default display name); reset at plan boundaries. */
    private static final ConcurrentMap<Method, Optional<String>> RESOLVED = new ConcurrentHashMap<>();

    private PlanOrdering() {
    }

    /**
     * The active ORDERED plan (api sink, selective mode 0 with a server-ordered list) or
     * {@code null} - in which case the orderers are strict no-ops (default order preserved).
     * Gated exactly like {@link DoqaSelectFilter}: outside mode 0 the orderers must not force
     * session init - Jupiter applies them at DISCOVERY, and in mode 2 that would create a run on
     * the server merely because an IDE discovered the tests.
     */
    static RunContext activePlan() {
        if (!DoqaSession.discoverySelectionActive()) {
            return null;
        }
        DoqaSession session;
        try {
            session = DoqaSession.getOrInit();
        } catch (RuntimeException e) {
            return null;
        }
        if (!session.enabled || session.runContext == null
                || session.runContext.selectedOrder() == null
                || session.runContext.selectedOrder().isEmpty()) {
            return null;
        }
        return session.runContext;
    }

    /**
     * Plan position of one test method (0-based); not in the plan =&gt; {@link Integer#MAX_VALUE}.
     * A placeholder externalId template ({@code login_{browser}}) takes the position of the first
     * selected id matching its wildcard form, mirroring {@link DoqaSelectFilter}'s template match.
     */
    static int methodIndex(RunContext plan, Method method, String displayName) {
        try {
            String externalId = displayName != null
                    ? Attribution.resolve(TestRefs.fromMethod(method, displayName)).externalId
                    : RESOLVED.computeIfAbsent(method, m -> {
                        try {
                            return Optional.ofNullable(
                                    Attribution.resolve(TestRefs.fromMethod(m, null)).externalId);
                        } catch (RuntimeException e) {
                            return Optional.empty();
                        }
                    }).orElse(null);
            if (externalId == null) {
                return Integer.MAX_VALUE;
            }
            if (Placeholders.hasPlaceholder(externalId)) {
                Pattern pattern = Placeholders.templateToRegex(externalId);
                List<String> order = plan.selectedOrder();
                for (int i = 0; i < order.size(); i++) {
                    if (pattern.matcher(order.get(i)).matches()) {
                        return i;
                    }
                }
                return Integer.MAX_VALUE;
            }
            return plan.orderIndex(externalId);
        } catch (RuntimeException e) {
            return Integer.MAX_VALUE; // unresolvable -> stable tail
        }
    }

    /**
     * Min plan position across the class's methods - declared, inherited and {@code @Nested} (a
     * top-level class whose planned tests live only in nested classes must still sort by them):
     * inter-class interleaving is impossible in Jupiter (classes run as blocks), so a class sorts
     * by its best (minimal) method position.
     */
    static int classIndex(RunContext plan, Class<?> testClass) {
        int best = Integer.MAX_VALUE;
        try {
            for (Class<?> c = testClass; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method method : declaredMethods(c)) {
                    if (method.isSynthetic()) {
                        continue;
                    }
                    best = Math.min(best, methodIndex(plan, method, null));
                    if (best == 0) {
                        return 0;
                    }
                }
            }
            for (Class<?> nested : testClass.getDeclaredClasses()) {
                if (nested.isAnnotationPresent(Nested.class)) {
                    best = Math.min(best, classIndex(plan, nested));
                    if (best == 0) {
                        return 0;
                    }
                }
            }
        } catch (Throwable t) {
            // broken classpath while introspecting: keep whatever position is known
        }
        return best;
    }

    private static Method[] declaredMethods(Class<?> c) {
        try {
            return c.getDeclaredMethods();
        } catch (Throwable t) {
            // NoClassDefFoundError from a method signature referencing an absent class
            return new Method[0];
        }
    }

    static void reset() {
        RESOLVED.clear();
    }
}
