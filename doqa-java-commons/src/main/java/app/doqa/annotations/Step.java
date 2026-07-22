package app.doqa.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a DoQA step (Allure-style). When the AspectJ {@code DoqaStepAspect} is
 * woven (LTW {@code -javaagent:aspectjweaver.jar} or the aspectj-maven-plugin), every invocation
 * of the annotated method is recorded as a step (nested, with status + duration) in the current
 * test's step tree. For no-AspectJ setups use the explicit {@code Doqa.step("title", () -> {...})}.
 *
 * <p>The title supports {@code {param}} placeholders resolved from the method's arguments - by
 * name (host compiled with {@code -parameters}) and by index ({@code {0}}, {@code {1}}, ...).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Step {
    /** Step title; empty =&gt; the method name is used. */
    String value() default "";

    /** Optional step description (travels to the autotest definition step). */
    String description() default "";
}
