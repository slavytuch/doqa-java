package app.doqa.client;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A result parameter - serialized as {@code {name, value}} and carried in a LIST (never a map),
 * per the contract note (the server camelCases keys on receipt).
 */
public final class Parameter implements Model {

    private final String name;
    private final Object value;

    public Parameter(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String name() { return name; }
    public Object value() { return value; }

    @Override
    public Map<String, Object> toPayload() {
        // Intentionally NOT compacted: value may legitimately be "", 0 or null.
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
}
