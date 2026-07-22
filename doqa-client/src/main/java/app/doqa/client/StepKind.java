package app.doqa.client;

/** Allowed definition {@code Step.kind} values: {@code step | before | after}. */
public enum StepKind {
    STEP("step"),
    BEFORE("before"),
    AFTER("after");

    private final String wire;

    StepKind(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }
}
