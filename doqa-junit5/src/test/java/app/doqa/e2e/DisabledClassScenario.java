package app.doqa.e2e;

import app.doqa.annotations.DoqaId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Skipped-container E2E fixture: {@code @Disabled} on the class fires one container skip event
 * and no per-test events - the listener must expand it into a skip per descendant test.
 * Runs only inside the nested launcher (name avoids surefire patterns).
 */
@Disabled("maintenance window")
public class DisabledClassScenario {

    @Test
    @DoqaId("E2E-DIS-1")
    void first() {
    }

    @Test
    @DoqaId("E2E-DIS-2")
    void second() {
    }
}
