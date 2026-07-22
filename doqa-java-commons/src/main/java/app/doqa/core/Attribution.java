package app.doqa.core;

import app.doqa.annotations.DoqaCaseIds;
import app.doqa.annotations.DoqaId;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the DoQA {@code externalId} for one test (priority top-down):
 * <ol>
 *   <li>{@code @DoqaId} on method (wins) or class &rarr; used verbatim. NB: a class-level
 *       {@code @DoqaId} applies to EVERY test of the class - sensible only for single-test
 *       classes; {@link DoqaSession} warns when several methods collapse into one id.</li>
 *   <li>id-in-title {@code [DOQA-123]} / {@code @DOQA:123} in the display name.</li>
 *   <li>native reuse - Allure {@code @AllureId} (read reflectively) &rarr; {@code ALLURE-<n>}.</li>
 *   <li>fallback - deterministic {@link SignatureHash} ({@code <framework>:<sha1>}).</li>
 * </ol>
 * {@code @DoqaCaseIds} (method then class) is captured independently.
 *
 * <p>Internal adapter API.
 */
public final class Attribution {

    public static final Pattern ID_IN_TITLE = Pattern.compile("(?:\\[|@)DOQA[-:](\\d+)\\]?");
    private static final String ALLURE_ID_ANNOTATION = "io.qameta.allure.AllureId";

    private Attribution() {
    }

    public enum Source { EXPLICIT_EXTERNAL_ID, ID_IN_TITLE, NATIVE_ALLURE, SIGNATURE_HASH }

    public static final class Result {
        public final String externalId;
        public final Source source;
        public final long[] caseIds;     // nullable
        public final String allureId;    // nullable

        Result(String externalId, Source source, long[] caseIds, String allureId) {
            this.externalId = externalId;
            this.source = source;
            this.caseIds = caseIds;
            this.allureId = allureId;
        }
    }

    public static Result resolve(TestRef ref) {
        String allureId = readAllureId(ref.testMethod);
        if (allureId == null) {
            allureId = readAllureId(ref.testClass);
        }

        long[] caseIds = readCaseIds(ref.testMethod);
        if (caseIds == null) {
            caseIds = readCaseIds(ref.testClass);
        }

        String explicit = readExternalId(ref.testMethod);
        if (explicit == null) {
            explicit = readExternalId(ref.testClass);
        }
        if (explicit != null && !explicit.trim().isEmpty()) {
            return new Result(explicit.trim(), Source.EXPLICIT_EXTERNAL_ID, caseIds, allureId);
        }

        String fromTitle = extractIdInTitle(ref.displayName);
        if (fromTitle != null) {
            return new Result(fromTitle, Source.ID_IN_TITLE, caseIds, allureId);
        }

        if (allureId != null && !allureId.trim().isEmpty()) {
            return new Result("ALLURE-" + allureId.trim(), Source.NATIVE_ALLURE, caseIds, allureId);
        }

        String signature = SignatureHash.stableSignature(
                ref.fqcn, ref.methodName, ref.methodParamTypes, ref.displayName, ref.parameterized);
        return new Result(SignatureHash.fallbackExternalId(signature),
                Source.SIGNATURE_HASH, caseIds, allureId);
    }

    public static String extractIdInTitle(String displayName) {
        if (displayName == null) {
            return null;
        }
        Matcher m = ID_IN_TITLE.matcher(displayName);
        return m.find() ? "DOQA-" + m.group(1) : null;
    }

    private static String readExternalId(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        DoqaId a = element.getAnnotation(DoqaId.class);
        return a == null ? null : a.value();
    }

    private static long[] readCaseIds(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        DoqaCaseIds a = element.getAnnotation(DoqaCaseIds.class);
        if (a == null || a.value().length == 0) {
            return null;
        }
        return a.value();
    }

    private static String readAllureId(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        for (Annotation annotation : element.getAnnotations()) {
            if (ALLURE_ID_ANNOTATION.equals(annotation.annotationType().getName())) {
                try {
                    Method valueMethod = annotation.annotationType().getMethod("value");
                    Object value = valueMethod.invoke(annotation);
                    return value == null ? null : String.valueOf(value);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
