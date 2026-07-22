package app.doqa.e2e;

import app.doqa.Doqa;
import app.doqa.annotations.DoqaId;
import org.junit.jupiter.api.Test;

/**
 * Parallel-execution E2E fixture (with {@link ParallelScenarioB}): every test records steps named
 * after its own id - the e2e asserts steps never leak between concurrently running tests.
 * Runs only inside the nested launcher (name avoids surefire patterns).
 */
public class ParallelScenarioA {

    @Test
    @DoqaId("E2E-PAR-A1")
    void a1() throws Exception {
        recordSteps("E2E-PAR-A1");
    }

    @Test
    @DoqaId("E2E-PAR-A2")
    void a2() throws Exception {
        recordSteps("E2E-PAR-A2");
    }

    static void recordSteps(String id) throws Exception {
        Doqa.step("step-1 of " + id, () -> Thread.sleep(20));
        Doqa.step("step-2 of " + id, () -> Thread.sleep(20));
    }
}
