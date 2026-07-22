package app.doqa.client;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Builds the framing of a {@code multipart/form-data} body for {@code POST /attachments} - form
 * fields {@code token} / {@code space_id} plus the binary {@code file} part. The file content
 * itself is NOT buffered here: callers stream it between {@link #preamble} and {@link #epilogue}
 * so arbitrarily large attachments never occupy heap.
 */
final class Multipart {

    private Multipart() {
    }

    /** Everything before the file bytes: auth fields + the {@code file} part headers. */
    static byte[] preamble(String boundary, String filename, String contentType,
                           String token, String spaceId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeField(out, boundary, "token", token == null ? "" : token);
        writeField(out, boundary, "space_id", spaceId == null ? "" : spaceId);
        writeUtf8(out, "--" + boundary + "\r\n");
        writeUtf8(out, "Content-Disposition: form-data; name=\"file\"; filename=\""
                + sanitizeFilename(filename) + "\"\r\n");
        writeUtf8(out, "Content-Type: "
                + (contentType == null || contentType.isEmpty()
                        ? "application/octet-stream" : contentType)
                + "\r\n\r\n");
        return out.toByteArray();
    }

    /** Everything after the file bytes: part terminator + closing boundary. */
    static byte[] epilogue(String boundary) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeUtf8(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    /**
     * Strip CR/LF (header-injection) and escape backslashes/quotes so a crafted file name cannot
     * break out of the {@code filename="..."} parameter or inject extra header lines.
     */
    private static String sanitizeFilename(String filename) {
        String name = filename == null ? "" : filename;
        name = name.replace("\r", "").replace("\n", "");
        name = name.replace("\\", "\\\\").replace("\"", "\\\"");
        return name;
    }

    private static void writeField(ByteArrayOutputStream out, String boundary, String name, String value) {
        writeUtf8(out, "--" + boundary + "\r\n");
        writeUtf8(out, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        writeUtf8(out, value + "\r\n");
    }

    private static void writeUtf8(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.write(b, 0, b.length);
    }
}
