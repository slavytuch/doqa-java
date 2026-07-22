package app.doqa.e2e;

import app.doqa.annotations.DoqaId;
import org.junit.jupiter.api.Test;

/**
 * Ordering E2E fixture (class "B") - see {@link OrderScenarioA}.
 */
public class OrderScenarioB {

    @Test
    @DoqaId("E2E-ORD-B1")
    void b1() {
        OrderLog.EXECUTED.add("E2E-ORD-B1");
    }

    @Test
    @DoqaId("E2E-ORD-B2")
    void b2() {
        OrderLog.EXECUTED.add("E2E-ORD-B2");
    }
}
