package app.doqa.client;

import java.util.Map;

/** A model element that serializes to a contract payload map (snake_case keys). */
public interface Model {
    Map<String, Object> toPayload();
}
