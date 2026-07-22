package app.doqa.client;

/** Allowed {@code Link.type} values (contract: related|defect|requirement|blocked_by|repository). */
public enum LinkType {
    RELATED("related"),
    DEFECT("defect"),
    REQUIREMENT("requirement"),
    BLOCKED_BY("blocked_by"),
    REPOSITORY("repository");

    private final String wire;

    LinkType(String wire) {
        this.wire = wire;
    }

    /** Contract wire value (snake_case). */
    public String wire() {
        return wire;
    }

    /** Parse a wire / enum name into a {@link LinkType}; null on unknown/blank. */
    public static LinkType from(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase().replace("-", "_");
        for (LinkType t : values()) {
            if (t.wire.equals(v) || t.name().toLowerCase().equals(v)) {
                return t;
            }
        }
        return null;
    }
}
