package app.doqa.e2e;

import app.doqa.Doqa;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Dynamic-test E2E fixture: the factory container must survive mode-0 discovery filtering (its
 * dynamic tests do not exist at discovery), and each dynamic test pins its stable id at runtime
 * via {@code Doqa.addExternalId} - report-time gating then filters precisely.
 * Runs only inside the nested launcher (name avoids surefire patterns).
 */
public class FactoryScenario {

    public static int executed;

    @TestFactory
    List<DynamicTest> dynamicChecks() {
        return List.of(
                DynamicTest.dynamicTest("[1] first check", () -> {
                    Doqa.addExternalId("E2E-DYN-1");
                    executed++;
                }),
                DynamicTest.dynamicTest("[2] second check", () -> {
                    Doqa.addExternalId("E2E-DYN-2");
                    executed++;
                }));
    }
}
