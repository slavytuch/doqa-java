package app.doqa.client;

import java.util.LinkedHashMap;
import java.util.Map;

/** A previously-uploaded attachment referenced by its media file id ({@code media_file_id}). */
public final class Attachment implements Model {

    private final String mediaFileId;

    public Attachment(String mediaFileId) {
        this.mediaFileId = mediaFileId;
    }

    public String mediaFileId() {
        return mediaFileId;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("media_file_id", mediaFileId);
        return m;
    }
}
