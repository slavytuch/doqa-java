package app.doqa.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared payload helpers: contract-shaped compaction and list serialization. */
final class Payloads {

    private Payloads() {
    }

    /**
     * Drop {@code null} and empty (String / Collection / Map) values; keep falsy scalars
     * (0, false) - the wire form the API contract expects.
     */
    static Map<String, Object> compact(Map<String, Object> in) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : in.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                continue;
            }
            if (v instanceof String && ((String) v).isEmpty()) {
                continue;
            }
            if (v instanceof Collection && ((Collection<?>) v).isEmpty()) {
                continue;
            }
            if (v instanceof Map && ((Map<?, ?>) v).isEmpty()) {
                continue;
            }
            out.put(e.getKey(), v);
        }
        return out;
    }

    /** Put {@code key} only when {@code value} is non-null (contract omits absent keys). */
    static void putIfPresent(Map<String, Object> body, String key, Object value) {
        if (value != null) {
            body.put(key, value);
        }
    }

    /** Serialize a list of models to a list of payload maps (never null). */
    static List<Object> payloads(Collection<? extends Model> models) {
        List<Object> out = new ArrayList<>();
        if (models != null) {
            for (Model m : models) {
                if (m != null) {
                    out.add(m.toPayload());
                }
            }
        }
        return out;
    }
}
