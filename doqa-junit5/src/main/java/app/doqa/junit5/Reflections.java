package app.doqa.junit5;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * Shared reflective helpers to resolve a test class / method from source coordinates. Lookups are
 * cached (the same method resolves on every discovery/execution event and per parameterized
 * invocation) and NEVER throw: broken classpaths ({@code NoClassDefFoundError} from a method
 * signature referencing an absent class) degrade to {@code null} - reporting machinery must not
 * break the user's discovery.
 */
final class Reflections {

    private static final ConcurrentMap<String, Optional<Class<?>>> CLASSES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Optional<Method>> METHODS = new ConcurrentHashMap<>();

    private Reflections() {
    }

    static Class<?> loadClass(String fqcn) {
        if (fqcn == null) {
            return null;
        }
        return CLASSES.computeIfAbsent(fqcn, key -> {
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = Reflections.class.getClassLoader();
                }
                return Optional.of(Class.forName(key, false, cl));
            } catch (Throwable t) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    static Method findMethod(Class<?> clazz, String methodName, String paramTypes) {
        if (clazz == null || methodName == null) {
            return null;
        }
        String key = clazz.getName() + "#" + methodName + "(" + (paramTypes == null ? "" : paramTypes) + ")";
        return METHODS.computeIfAbsent(key,
                k -> Optional.ofNullable(resolveMethod(clazz, methodName, paramTypes))).orElse(null);
    }

    private static Method resolveMethod(Class<?> clazz, String methodName, String paramTypes) {
        try {
            Method exact = ReflectionSupport
                    .findMethod(clazz, methodName, paramTypes == null ? "" : paramTypes.trim())
                    .orElse(null);
            if (exact != null) {
                return exact;
            }
        } catch (RuntimeException ignored) {
            // unloadable parameter types: fall back to the by-name scan below
        }
        try {
            // by-name fallback disambiguated by parameter count: annotations read off the wrong
            // overload would mis-attribute the test; ambiguity resolves to null (the signature
            // hash stays stable on the string coordinates alone)
            int expected = parameterCount(paramTypes);
            Method byName = null;
            boolean ambiguous = false;
            for (Method m : clazz.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) {
                    continue;
                }
                if (expected >= 0 && m.getParameterCount() != expected) {
                    continue;
                }
                ambiguous = byName != null;
                byName = m;
            }
            return ambiguous ? null : byName;
        } catch (Throwable t) {
            // e.g. NoClassDefFoundError from another method's signature
            return null;
        }
    }

    private static int parameterCount(String paramTypes) {
        if (paramTypes == null) {
            return -1;
        }
        String trimmed = paramTypes.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return (int) (trimmed.chars().filter(c -> c == ',').count() + 1);
    }

    /** Clear caches at plan boundaries - long-lived JVMs may reload test classes. */
    static void reset() {
        CLASSES.clear();
        METHODS.clear();
    }
}
