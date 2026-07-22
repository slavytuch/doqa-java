package app.doqa.core;

import app.doqa.client.Link;
import app.doqa.client.LinkType;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * Migration bridge for teams coming from Allure: the common Allure annotations are read
 * REFLECTIVELY (no compile dependency, mirrors the {@code @AllureId} handling in
 * {@link Attribution}) and mapped onto the DoQA model, so an Allure-annotated suite lands in the
 * catalog with its labels and links intact before anyone rewrites it to {@code @Doqa*}:
 * <ul>
 *   <li>{@code @Epic}/{@code @Feature}/{@code @Story}/{@code @Owner}/{@code @Severity} &rarr;
 *       {@code key:value} labels ({@code epic:...}, {@code severity:critical}, ...);</li>
 *   <li>{@code @Description} &rarr; description (only when no {@code @DoqaDescription});</li>
 *   <li>{@code @Link} (and URL-valued {@code @Issue}/{@code @TmsLink}) &rarr; typed links
 *       (issue&rarr;defect, tms&rarr;requirement).</li>
 * </ul>
 * Native {@code @Doqa*} values always win; Allure only fills the gaps.
 */
final class AllureCompat {

    private static final String PKG = "io.qameta.allure.";

    private AllureCompat() {
    }

    /** Merge Allure-annotation data of {@code el} into {@code meta} (additive, Doqa-first). */
    static void fill(Meta meta, AnnotatedElement el) {
        if (el == null) {
            return;
        }
        for (Annotation annotation : el.getAnnotations()) {
            String name = annotation.annotationType().getName();
            if (!name.startsWith(PKG)) {
                continue;
            }
            switch (name.substring(PKG.length())) {
                case "Epic":     addLabel(meta, "epic", value(annotation)); break;
                case "Feature":  addLabel(meta, "feature", value(annotation)); break;
                case "Story":    addLabel(meta, "story", value(annotation)); break;
                case "Owner":    addLabel(meta, "owner", value(annotation)); break;
                case "Severity": addLabel(meta, "severity", lower(value(annotation))); break;
                case "Description":
                    if (meta.description == null) {
                        meta.description = value(annotation);
                    }
                    break;
                case "Link":     addLink(meta, member(annotation, "url"),
                        member(annotation, "type"), member(annotation, "name")); break;
                case "Links":    each(meta, annotation); break;
                case "Issue":    addUrlLink(meta, value(annotation), LinkType.DEFECT); break;
                case "TmsLink":  addUrlLink(meta, value(annotation), LinkType.REQUIREMENT); break;
                default:
                    break;
            }
        }
    }

    private static void each(Meta meta, Annotation container) {
        try {
            Object value = container.annotationType().getMethod("value").invoke(container);
            if (value instanceof Annotation[]) {
                for (Annotation nested : (Annotation[]) value) {
                    addLink(meta, member(nested, "url"), member(nested, "type"), member(nested, "name"));
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // absent/incompatible Allure version: skip silently, native @Doqa* still applies
        }
    }

    private static void addLabel(Meta meta, String key, String value) {
        if (value != null && !value.isEmpty()) {
            meta.labels.add(key + ":" + value);
        }
    }

    private static void addLink(Meta meta, String url, String type, String title) {
        if (url == null || url.isEmpty()) {
            return;
        }
        LinkType mapped = LinkType.from(type);
        meta.links.add(new Link(url, mapped == null ? null : mapped.wire(),
                title == null || title.isEmpty() ? null : title, null));
    }

    /** {@code @Issue}/{@code @TmsLink} carry an id, not a URL - only literal URLs map cleanly. */
    private static void addUrlLink(Meta meta, String value, LinkType type) {
        if (value != null && (value.startsWith("http://") || value.startsWith("https://"))) {
            meta.links.add(new Link(value, type, null, null));
        }
    }

    private static String value(Annotation annotation) {
        return member(annotation, "value");
    }

    private static String member(Annotation annotation, String name) {
        try {
            Method m = annotation.annotationType().getMethod(name);
            Object v = m.invoke(annotation);
            return v == null ? null : String.valueOf(v);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String lower(String s) {
        return s == null ? null : s.toLowerCase();
    }
}
