package app.doqa.core;

/**
 * One attachment as collected at runtime - either a path to a local file or in-memory content.
 * Uploaded by the active sink at result-build time (API multipart or file-sink copy).
 *
 * <p>Internal adapter API - user code attaches via {@code Doqa.addAttachments} /
 * {@code Doqa.addAttachment}.
 */
public final class AttachmentRef {

    public final String path;         // non-null for file-based attachments
    public final String name;         // non-null for in-memory attachments
    public final byte[] content;      // non-null for in-memory attachments
    public final String contentType;  // nullable; inferred from the name when absent
    /**
     * Media id memoized after the first successful upload - class-fixture attachments are
     * merged into every test of the class and must not be re-uploaded per test.
     */
    volatile String uploadedId;

    private AttachmentRef(String path, String name, byte[] content, String contentType) {
        this.path = path;
        this.name = name;
        this.content = content;
        this.contentType = contentType;
    }

    public static AttachmentRef ofPath(String path) {
        return new AttachmentRef(path, null, null, null);
    }

    public static AttachmentRef ofBytes(String name, byte[] content, String contentType) {
        return new AttachmentRef(null, name == null ? "attachment" : name,
                content == null ? new byte[0] : content, contentType);
    }

    @Override
    public String toString() {
        return path != null ? path : name;
    }
}
