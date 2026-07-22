package app.doqa.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A single link. Repeatable - put several {@code @DoqaLink} on the same element, or group them in
 * {@link DoqaLinks}; both forms are read. {@code type} is a {@link app.doqa.client.LinkType} wire
 * value (related|defect|requirement|blocked_by|repository).
 */
@Documented
@Repeatable(DoqaLinks.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DoqaLink {
    String url();

    String type() default "";

    String title() default "";

    String description() default "";
}
