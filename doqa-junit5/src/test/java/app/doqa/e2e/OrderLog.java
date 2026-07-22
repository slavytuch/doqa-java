package app.doqa.e2e;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Ordering E2E fixture - shared execution log for {@link OrderScenarioA}/{@link OrderScenarioB}. */
public final class OrderLog {

    public static final List<String> EXECUTED = new CopyOnWriteArrayList<>();

    private OrderLog() {
    }
}
