package app.doqa;

/**
 * The {@code key:value} label convention and its well-known keys. DoQA labels are flat strings;
 * encoding structured facets as {@code key:value} keeps them filterable and portable across
 * adapters - the Allure migration bridge emits exactly these keys ({@code severity:critical},
 * {@code owner:jane}, {@code epic:checkout}).
 *
 * <pre>{@code
 * @DoqaLabels({"severity:critical", "owner:jane"})
 * // or at runtime:
 * Doqa.addLabel(Labels.SEVERITY, "critical");
 * }</pre>
 */
public final class Labels {

    public static final String SEVERITY = "severity";
    public static final String OWNER = "owner";
    public static final String EPIC = "epic";
    public static final String FEATURE = "feature";
    public static final String STORY = "story";
    public static final String COMPONENT = "component";

    private Labels() {
    }

    /** {@code key:value} form of one label. */
    public static String of(String key, String value) {
        return key + ":" + value;
    }
}
