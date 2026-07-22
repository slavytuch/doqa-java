package app.doqa.client;

/** Allowed result outcomes: {@code passed | failed | skipped | broken}. */
public enum Outcome {
    PASSED("passed"),
    FAILED("failed"),
    SKIPPED("skipped"),
    BROKEN("broken");

    private final String wire;

    Outcome(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }
}
