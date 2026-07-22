package app.doqa.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Registry linking a test's {@code uniqueId} to its {@link RuntimeContext}, plus a thread-local
 * "current" pointer read by {@code Doqa}, the AspectJ step aspect and the framework extension.
 *
 * <p>The framework listener {@link #open}s a context when a test starts and {@link #remove}s it
 * when it finishes; the extension {@link #bind}s the thread-local on the actual test thread,
 * which may differ from the listener's thread. The map keyed by uniqueId is the source of truth.
 * A removed context is marked closed - late writers (a straggling async thread, a foreign-thread
 * listener) see {@code null} instead of silently writing into an already-reported context.
 *
 * <p>Internal adapter API.
 */
public final class DoqaContexts {

    private static final ConcurrentMap<String, RuntimeContext> BY_UID = new ConcurrentHashMap<>();
    private static final ThreadLocal<RuntimeContext> CURRENT = new ThreadLocal<>();

    private DoqaContexts() {
    }

    public static RuntimeContext open(String uniqueId) {
        RuntimeContext ctx = new RuntimeContext(uniqueId);
        BY_UID.put(uniqueId, ctx);
        CURRENT.set(ctx);
        return ctx;
    }

    /** Re-bind the thread-local to the context for {@code uniqueId} (returns it, or null). */
    public static RuntimeContext bind(String uniqueId) {
        RuntimeContext ctx = BY_UID.get(uniqueId);
        if (ctx != null && !ctx.closed) {
            CURRENT.set(ctx);
            return ctx;
        }
        return null;
    }

    public static RuntimeContext current() {
        RuntimeContext ctx = CURRENT.get();
        if (ctx == null) {
            return null;
        }
        // Stale pointer left behind when the context was removed from another thread: a pool
        // thread must not keep writing into (or retaining) an already-reported context.
        if (ctx.closed) {
            CURRENT.remove();
            return null;
        }
        return ctx;
    }

    public static RuntimeContext remove(String uniqueId) {
        RuntimeContext ctx = BY_UID.remove(uniqueId);
        if (ctx != null) {
            ctx.closed = true;
        }
        if (CURRENT.get() == ctx) {
            CURRENT.remove();
        }
        return ctx;
    }

    /**
     * Bind a transient (non-registered) context to this thread - class fixtures and
     * {@code Doqa.runWith} context transfer. Returns the previously bound context so the caller
     * can {@link #restore} it in a finally block.
     */
    public static RuntimeContext push(RuntimeContext ctx) {
        RuntimeContext previous = CURRENT.get();
        CURRENT.set(ctx);
        return previous;
    }

    public static void restore(RuntimeContext previous) {
        if (previous != null) {
            CURRENT.set(previous);
        } else {
            CURRENT.remove();
        }
    }
}
