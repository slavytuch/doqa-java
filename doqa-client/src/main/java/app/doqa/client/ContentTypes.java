package app.doqa.client;

/**
 * Content-type by file extension - shared by the API sink (multipart {@code file} part) and the
 * file sink (Allure attachment {@code type}); the DoQA parser maps it to the attachment kind.
 */
final class ContentTypes {

    private ContentTypes() {
    }

    static String of(String filename) {
        String f = filename == null ? "" : filename.toLowerCase();
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".gif")) return "image/gif";
        if (f.endsWith(".mp4")) return "video/mp4";
        if (f.endsWith(".webm")) return "video/webm";
        if (f.endsWith(".txt") || f.endsWith(".log")) return "text/plain";
        if (f.endsWith(".html")) return "text/html";
        if (f.endsWith(".json")) return "application/json";
        if (f.endsWith(".xml")) return "application/xml";
        if (f.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }
}
