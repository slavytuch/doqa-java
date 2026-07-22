package app.doqa.core;

import java.lang.reflect.Method;

/**
 * Immutable, framework-light description of a single test resolved from the framework's test
 * identifier (+ reflection). Kept free of framework types so {@link Attribution} /
 * {@link SignatureHash} stay unit-testable without launching an engine.
 *
 * <p>{@code parameterized} means "invocations of this element collapse to one method-level
 * identity" (a parameterized/templated test) - each framework adapter derives it from its own
 * phase-specific signal; keep those derivations in ONE factory per adapter so discovery,
 * ordering and reporting agree on identities.
 *
 * <p>Internal adapter API.
 */
public final class TestRef {

    public final String fqcn;
    public final String methodName;
    public final String methodParamTypes;
    public final String displayName;
    public final boolean parameterized;
    public final Class<?> testClass;   // nullable
    public final Method testMethod;    // nullable

    public TestRef(String fqcn, String methodName, String methodParamTypes, String displayName,
                   boolean parameterized, Class<?> testClass, Method testMethod) {
        this.fqcn = fqcn;
        this.methodName = methodName;
        this.methodParamTypes = methodParamTypes;
        this.displayName = displayName;
        this.parameterized = parameterized;
        this.testClass = testClass;
        this.testMethod = testMethod;
    }

    /** {@code <fqcn>.<method>} - the default autotest name path. */
    public String fullName() {
        if (fqcn == null || fqcn.isEmpty()) {
            return methodName == null ? displayName : methodName;
        }
        if (methodName == null || methodName.isEmpty()) {
            return fqcn;
        }
        return fqcn + "." + methodName;
    }

    /** {@code <fqcn>#<method>} - identity of the declaring method (duplicate-id detection). */
    public String methodKey() {
        return fqcn + "#" + methodName;
    }

    /** Package part of the FQCN (default namespace), or null. */
    public String packageName() {
        if (fqcn == null) {
            return null;
        }
        int dot = fqcn.lastIndexOf('.');
        return dot > 0 ? fqcn.substring(0, dot) : null;
    }

    /** Simple class name (default classname), or null. */
    public String simpleClassName() {
        if (fqcn == null) {
            return null;
        }
        int dot = fqcn.lastIndexOf('.');
        return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
    }
}
