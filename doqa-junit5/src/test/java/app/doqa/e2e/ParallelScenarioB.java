package app.doqa.e2e;

import app.doqa.annotations.DoqaId;
import org.junit.jupiter.api.Test;

/**
 * Parallel-execution E2E fixture - see {@link ParallelScenarioA}.
 */
public class ParallelScenarioB {

    @Test
    @DoqaId("E2E-PAR-B1")
    void b1() throws Exception {
        ParallelScenarioA.recordSteps("E2E-PAR-B1");
    }

    @Test
    @DoqaId("E2E-PAR-B2")
    void b2() throws Exception {
        ParallelScenarioA.recordSteps("E2E-PAR-B2");
    }
}
