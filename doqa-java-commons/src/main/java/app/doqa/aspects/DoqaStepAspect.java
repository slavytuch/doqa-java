package app.doqa.aspects;

import app.doqa.Doqa;
import app.doqa.annotations.Step;
import app.doqa.core.Limits;
import app.doqa.core.Placeholders;
import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;

/**
 * AspectJ aspect that records every invocation of a {@link Step @Step}-annotated method as a DoQA
 * step (nested, with status + duration) in the current test's step tree - the Allure-style
 * declarative steps. {@code {param}} placeholders in the title resolve from the method arguments
 * (by name with {@code -parameters}, and by index {@code {0}}..{@code {n}}). No-op when there is
 * no active test context.
 *
 * <p>Woven at runtime via load-time weaving ({@code -javaagent:aspectjweaver.jar} + the bundled
 * {@code META-INF/aop.xml}) or at build time via the {@code aspectj-maven-plugin}. For projects
 * that do not enable AspectJ, use the explicit {@code Doqa.step("title", () -> {...})} instead.
 */
@Aspect
public class DoqaStepAspect {

    @Around("execution(@app.doqa.annotations.Step * *(..)) && @annotation(step)")
    public Object aroundStep(ProceedingJoinPoint pjp, Step step) throws Throwable {
        String title = step.value() == null || step.value().isEmpty()
                ? pjp.getSignature().getName()
                : resolveTitle(step.value(), pjp);
        String description = step.description() == null || step.description().isEmpty()
                ? null
                : step.description();
        return Doqa.step(title, description, (Doqa.ThrowingSupplier<Object>) pjp::proceed);
    }

    private static String resolveTitle(String template, ProceedingJoinPoint pjp) {
        if (template.indexOf('{') < 0) {
            return template;
        }
        Object[] args = pjp.getArgs();
        Map<String, String> params = new LinkedHashMap<>();
        if (pjp.getSignature() instanceof CodeSignature) {
            String[] names = ((CodeSignature) pjp.getSignature()).getParameterNames();
            if (names != null) {
                for (int i = 0; i < names.length && i < args.length; i++) {
                    params.putIfAbsent(names[i],
                            Limits.truncate(Placeholders.stringify(args[i]), Limits.maxParameterLength()));
                }
            }
        }
        for (int i = 0; i < args.length; i++) {
            params.putIfAbsent(String.valueOf(i),
                    Limits.truncate(Placeholders.stringify(args[i]), Limits.maxParameterLength()));
        }
        return Placeholders.resolve(template, params);
    }
}
