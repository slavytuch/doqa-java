package app.doqa.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An executed step (results). Recursive via {@code steps}; {@code outcome} in {@link Outcome}. */
public final class StepResult implements Model {

    private final String title;
    private final String outcome;   // wire value; nullable
    private final Long durationMs;
    private final String message;
    private final List<Attachment> attachments = new ArrayList<>();
    private final List<StepResult> steps = new ArrayList<>();
    // Optional epoch-ms timestamps: the file (Allure) sink needs start/stop so the parser can
    // recompute durationMs; the API sink tolerates the extra keys (backend reads known keys only).
    private Long startedOn;
    private Long completedOn;

    public StepResult(String title, String outcome, Long durationMs, String message,
                      List<Attachment> attachments, List<StepResult> steps) {
        this.title = title;
        this.outcome = outcome;
        this.durationMs = durationMs;
        this.message = message;
        if (attachments != null) {
            this.attachments.addAll(attachments);
        }
        if (steps != null) {
            this.steps.addAll(steps);
        }
    }

    public StepResult(String title, Outcome outcome, Long durationMs, String message,
                      List<Attachment> attachments, List<StepResult> steps) {
        this(title, outcome == null ? null : outcome.wire(), durationMs, message, attachments, steps);
    }

    public StepResult startedOn(Long epochMs) { this.startedOn = epochMs; return this; }
    public StepResult completedOn(Long epochMs) { this.completedOn = epochMs; return this; }

    public String title() { return title; }
    public String outcome() { return outcome; }
    public Long durationMs() { return durationMs; }
    public String message() { return message; }
    public List<Attachment> attachments() { return Collections.unmodifiableList(attachments); }
    public List<StepResult> steps() { return Collections.unmodifiableList(steps); }
    public Long startedOn() { return startedOn; }
    public Long completedOn() { return completedOn; }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("outcome", outcome);
        m.put("duration_ms", durationMs);
        m.put("started_on", startedOn);
        m.put("completed_on", completedOn);
        m.put("message", message);
        m.put("attachments", Payloads.payloads(attachments));
        m.put("steps", Payloads.payloads(steps));
        return Payloads.compact(m);
    }
}
