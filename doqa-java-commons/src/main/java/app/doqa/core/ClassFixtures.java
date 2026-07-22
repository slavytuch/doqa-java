package app.doqa.core;

import app.doqa.client.StepResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of class-level fixtures ({@code @BeforeAll} / {@code @AfterAll}) captured by the
 * framework extension, keyed by the exact declaring class FQCN. A test resolves its fixtures by
 * walking the enclosing-class chain (outer to inner), so a nested class's {@code @BeforeAll} is
 * attributed only to that class's tests - the enclosing class's own tests never inherit it.
 *
 * <p>Attachment semantics per sink: {@code @BeforeAll} results are prepended to every test's
 * {@code setup_results} (they always run before the test); {@code @AfterAll} runs after the
 * results are built, so it is appended at the flush (API batch / per-class streaming) or emitted
 * as a shared Allure container (files).
 *
 * <p>State is reset between test plans (see {@link #reset()}) so a surefire rerun re-running
 * {@code @BeforeAll} does not accumulate duplicate fixtures.
 *
 * <p>Internal adapter API.
 */
public final class ClassFixtures {

    static final class Entry {
        // Concurrent lists: different nested classes' @BeforeAll may run on parallel threads while
        // report() reads them - avoids ConcurrentModificationException and lost nodes.
        final List<StepNode> before = new CopyOnWriteArrayList<>();
        final List<StepNode> after = new CopyOnWriteArrayList<>();
    }

    private static final ConcurrentMap<String, Entry> BY_CLASS = new ConcurrentHashMap<>();

    private ClassFixtures() {
    }

    public static void recordBefore(String classKey, StepNode node) {
        entry(classKey).before.add(node);
    }

    public static void recordAfter(String classKey, StepNode node) {
        entry(classKey).after.add(node);
    }

    /** @BeforeAll of the whole enclosing chain, outermost first (execution order). */
    public static List<StepResult> beforeResults(String testClassFqcn,
                                                 ResultBuilder.AttachmentUploader uploader) {
        List<StepNode> nodes = new ArrayList<>();
        for (String key : chainKeys(testClassFqcn)) {
            Entry e = BY_CLASS.get(key);
            if (e != null) {
                nodes.addAll(e.before);
            }
        }
        return toResults(nodes, uploader);
    }

    /** @AfterAll of the whole enclosing chain, innermost first (execution order). */
    public static List<StepResult> afterResults(String testClassFqcn,
                                                ResultBuilder.AttachmentUploader uploader) {
        List<StepNode> nodes = new ArrayList<>();
        List<String> chain = chainKeys(testClassFqcn);
        for (int i = chain.size() - 1; i >= 0; i--) {
            Entry e = BY_CLASS.get(chain.get(i));
            if (e != null) {
                nodes.addAll(e.after);
            }
        }
        return toResults(nodes, uploader);
    }

    public static void reset() {
        BY_CLASS.clear();
    }

    /**
     * Enclosing-class chain keys for a test-class FQCN, outermost first:
     * {@code pkg.Outer$Inner$Deep} =&gt; [{@code pkg.Outer}, {@code pkg.Outer$Inner},
     * {@code pkg.Outer$Inner$Deep}]. Nesting is marked by {@code $}.
     */
    private static List<String> chainKeys(String fqcn) {
        List<String> keys = new ArrayList<>();
        if (fqcn == null) {
            return keys;
        }
        for (int i = 0; i < fqcn.length(); i++) {
            if (fqcn.charAt(i) == '$') {
                keys.add(fqcn.substring(0, i));
            }
        }
        keys.add(fqcn);
        return keys;
    }

    private static Entry entry(String classKey) {
        return BY_CLASS.computeIfAbsent(classKey, k -> new Entry());
    }

    private static List<StepResult> toResults(List<StepNode> nodes,
                                              ResultBuilder.AttachmentUploader uploader) {
        List<StepResult> out = new ArrayList<>();
        for (StepNode n : nodes) {
            out.add(ResultBuilder.toResultStep(n, uploader));
        }
        return out;
    }
}
