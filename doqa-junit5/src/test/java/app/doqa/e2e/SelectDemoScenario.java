package app.doqa.e2e;

import app.doqa.annotations.DoqaId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Mode-0 (selective run) E2E fixtures: the fake backend puts {@code E2E-SEL-1} and
 * {@code E2E-P-42} in the run. {@code DoqaSelectFilter} must deselect {@code E2E-SEL-2} at
 * discovery; the placeholder template {@code E2E-P-{v}} is included as a whole (regex match) and
 * the non-selected invocation ({@code E2E-P-43}) is dropped at report time.
 * Runs only inside the nested launcher (name avoids surefire patterns).
 */
public class SelectDemoScenario {

    public static int executed;

    @Test
    @DoqaId("E2E-SEL-1")
    void selectedTest() {
        executed++;
    }

    @Test
    @DoqaId("E2E-SEL-2")
    void deselectedTest() {
        executed++;
    }

    @ParameterizedTest
    @ValueSource(ints = {42, 43})
    @DoqaId("E2E-P-{v}")
    void placeholderSelect(int v) {
        executed++;
    }
}
