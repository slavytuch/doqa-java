package app.doqa.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A definition step (upsert). Recursive via {@code steps}; {@code kind} in {@link StepKind}. */
public final class Step implements Model {

    private final String title;
    private final String description;
    private final String kind;   // wire value; nullable
    private final List<Step> steps = new ArrayList<>();

    public Step(String title, String description, String kind, List<Step> steps) {
        this.title = title;
        this.description = description;
        this.kind = kind;
        if (steps != null) {
            this.steps.addAll(steps);
        }
    }

    public Step(String title, String description, StepKind kind, List<Step> steps) {
        this(title, description, kind == null ? null : kind.wire(), steps);
    }

    public static Step of(String title) {
        return new Step(title, null, (String) null, null);
    }

    public Step addStep(Step child) {
        if (child != null) {
            steps.add(child);
        }
        return this;
    }

    public String title() { return title; }
    public String description() { return description; }
    public String kind() { return kind; }
    public List<Step> steps() { return Collections.unmodifiableList(steps); }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("description", description);
        m.put("kind", kind);
        m.put("steps", Payloads.payloads(steps));
        return Payloads.compact(m);
    }
}
