package app.doqa.e2e;

import app.doqa.annotations.DoqaId;
import org.junit.jupiter.api.Test;

/**
 * Ordering E2E fixture (class "A"): explicit externalIds so the plan orderers can position
 * the methods. Runs only inside the nested launcher (name avoids surefire patterns).
 */
public class OrderScenarioA {

    @Test
    @DoqaId("E2E-ORD-A1")
    void a1() {
        OrderLog.EXECUTED.add("E2E-ORD-A1");
    }

    @Test
    @DoqaId("E2E-ORD-A2")
    void a2() {
        OrderLog.EXECUTED.add("E2E-ORD-A2");
    }
}
