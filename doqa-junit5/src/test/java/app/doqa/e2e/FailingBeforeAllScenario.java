package app.doqa.e2e;

import app.doqa.annotations.DoqaId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Container-failure E2E fixture: {@code @BeforeAll} throws, so no test ever starts - the listener
 * must synthesize a result for every descendant ("results are not lost").
 * Runs only inside the nested launcher (name avoids surefire patterns).
 */
public class FailingBeforeAllScenario {

    @BeforeAll
    static void boom() {
        throw new IllegalStateException("infra down");
    }

    @Test
    @DoqaId("E2E-BA-1")
    void first() {
    }

    @Test
    @DoqaId("E2E-BA-2")
    void second() {
    }
}
