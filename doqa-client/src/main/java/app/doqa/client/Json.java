package app.doqa.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny dependency-free JSON codec (keeps client-core offline-buildable, no Gson/Jackson).
 *
 * <p><b>Writer</b> supports {@link Map}, {@link List}, {@link String}, {@link Number},
 * {@link Boolean} and {@code null}; maps are emitted in iteration order (use
 * {@link LinkedHashMap} to keep the wire keys in a stable order).
 * <b>Reader</b> parses an arbitrary JSON document into {@link Map}/{@link List}/
 * {@link String}/{@link Double}/{@link Long}/{@link Boolean}/{@code null} - enough to read the
 * small response envelopes ({@code map}, {@code runId}, {@code mediaFileId}, {@code autotests[]}).
 */
public final class Json {

    private Json() {
    }

    // --------------------------------------------------------------------- writer
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map) {
            writeObject(sb, (Map<?, ?>) value);
        } else if (value instanceof List) {
            writeArray(sb, (List<?>) value);
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Double || value instanceof Float) {
            // NaN/Infinity are not valid JSON literals - one such value must not
            // invalidate the whole payload.
            double d = ((Number) value).doubleValue();
            sb.append(Double.isFinite(d) ? value.toString() : "null");
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value.toString());
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(':');
            writeValue(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<?> list) {
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeValue(sb, item);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // --------------------------------------------------------------------- reader
    /** Parse a JSON document; returns Map/List/String/Double/Long/Boolean/null. */
    public static Object parse(String text) {
        return new Parser(text == null ? "" : text).parseDocument();
    }

    /** Convenience: parse and cast to an object (empty map on non-object / parse failure). */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        try {
            Object o = parse(text);
            if (o instanceof Map) {
                return (Map<String, Object>) o;
            }
        } catch (RuntimeException ignored) {
            // fall through
        }
        return new LinkedHashMap<>();
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
        }

        Object parseDocument() {
            skipWs();
            if (i >= s.length()) {
                return null;
            }
            Object v = parseValue();
            skipWs();
            return v;
        }

        private Object parseValue() {
            skipWs();
            if (i >= s.length()) {
                throw new IllegalStateException("unexpected end of JSON");
            }
            char c = s.charAt(i);
            switch (c) {
                case '{': return parseObj();
                case '[': return parseArr();
                case '"': return parseStr();
                case 't': case 'f': return parseBool();
                case 'n': expect("null"); return null;
                default:  return parseNum();
            }
        }

        private Map<String, Object> parseObj() {
            Map<String, Object> map = new LinkedHashMap<>();
            i++; // {
            skipWs();
            if (peek() == '}') { i++; return map; }
            while (true) {
                skipWs();
                String key = parseStr();
                skipWs();
                if (peek() != ':') {
                    throw new IllegalStateException("expected ':' at " + i);
                }
                i++;
                map.put(key, parseValue());
                skipWs();
                char c = peek();
                if (c == ',') { i++; continue; }
                if (c == '}') { i++; break; }
                throw new IllegalStateException("expected ',' or '}' at " + i);
            }
            return map;
        }

        private List<Object> parseArr() {
            List<Object> list = new ArrayList<>();
            i++; // [
            skipWs();
            if (peek() == ']') { i++; return list; }
            while (true) {
                list.add(parseValue());
                skipWs();
                char c = peek();
                if (c == ',') { i++; continue; }
                if (c == ']') { i++; break; }
                throw new IllegalStateException("expected ',' or ']' at " + i);
            }
            return list;
        }

        private String parseStr() {
            if (peek() != '"') {
                throw new IllegalStateException("expected string at " + i);
            }
            i++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"':  sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                            break;
                        default: sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalStateException("unterminated string");
        }

        private Boolean parseBool() {
            if (peek() == 't') { expect("true"); return Boolean.TRUE; }
            expect("false");
            return Boolean.FALSE;
        }

        private Object parseNum() {
            int start = i;
            while (i < s.length() && "-+.eE0123456789".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            String num = s.substring(start, i);
            if (num.indexOf('.') >= 0 || num.indexOf('e') >= 0 || num.indexOf('E') >= 0) {
                return Double.parseDouble(num);
            }
            try {
                return Long.parseLong(num);
            } catch (NumberFormatException e) {
                return Double.parseDouble(num);
            }
        }

        private void expect(String literal) {
            if (!s.regionMatches(i, literal, 0, literal.length())) {
                throw new IllegalStateException("expected '" + literal + "' at " + i);
            }
            i += literal.length();
        }

        private char peek() {
            return i < s.length() ? s.charAt(i) : '\0';
        }

        private void skipWs() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }
    }
}
