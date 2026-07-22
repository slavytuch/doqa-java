package app.doqa.core;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@code {param}} placeholder substitution in annotation values (externalId,
 * displayName, title, description, namespace, classname, labels, tags, link fields). Values come
 * from the invocation parameters captured by the framework extension (plus runtime
 * {@code Doqa.addParameter}); unknown placeholders are left verbatim.
 *
 * <p>Internal adapter API.
 */
public final class Placeholders {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^{}]*}");
    /** Template regexes are matched per discovery descriptor - compile each template once. */
    private static final ConcurrentMap<String, Pattern> TEMPLATE_CACHE = new ConcurrentHashMap<>();

    private Placeholders() {
    }

    /** Named parameters of the test: invocation args first, then runtime-added (first wins). */
    public static Map<String, String> paramsOf(RuntimeContext ctx) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Object[] pair : ctx.invocationParameters) {
            out.putIfAbsent(String.valueOf(pair[0]), String.valueOf(pair[1]));
        }
        for (Object[] pair : ctx.parameters) {
            out.putIfAbsent(String.valueOf(pair[0]), String.valueOf(pair[1]));
        }
        return out;
    }

    public static String resolve(String value, Map<String, String> params) {
        if (value == null || params.isEmpty() || value.indexOf('{') < 0) {
            return value;
        }
        String out = value;
        for (Map.Entry<String, String> e : params.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    public static boolean hasPlaceholder(String value) {
        return value != null && PLACEHOLDER.matcher(value).find();
    }

    /**
     * Turns a parameterized externalId template ({@code login_{browser}}) into a regex matching
     * any substituted id ({@code login_chrome}) - used by the mode-0 discovery filter. Literal
     * segments are quoted; each placeholder becomes {@code .*}.
     */
    public static Pattern templateToRegex(String template) {
        return TEMPLATE_CACHE.computeIfAbsent(template, t -> {
            String[] parts = t.split("\\{[^{}]*}", -1);
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    regex.append(".*");
                }
                if (!parts[i].isEmpty()) {
                    regex.append(Pattern.quote(parts[i]));
                }
            }
            return Pattern.compile(regex.toString());
        });
    }

    /** Stringification of an invocation argument (arrays deep-printed). */
    public static String stringify(Object arg) {
        if (arg == null) {
            return "null";
        }
        Class<?> c = arg.getClass();
        if (!c.isArray()) {
            return String.valueOf(arg);
        }
        if (arg instanceof Object[]) {
            return Arrays.deepToString((Object[]) arg);
        }
        int length = Array.getLength(arg);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(Array.get(arg, i));
        }
        return sb.append(']').toString();
    }

    /** Resolve placeholders across a list of strings (returns a new list). */
    public static List<String> resolveAll(List<String> values, Map<String, String> params) {
        return values.stream().map(v -> resolve(v, params)).collect(Collectors.toList());
    }
}
