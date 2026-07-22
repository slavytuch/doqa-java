package app.doqa.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * HTTP transport seam - adapters and tests can substitute a fake (no network). The default
 * implementation ({@link HttpClientTransport}) is built from {@link DoqaConfig}
 * (cert-validation / proxy / timeout).
 */
public interface Transport {

    Response send(Request request) throws IOException;

    /**
     * An outbound request. Exactly one of {@code jsonBody} / {@code bodyStream} is non-null for
     * POSTs. {@code idempotent} drives the retry policy in {@link ApiClient}: only idempotent
     * requests may be replayed after the server might already have processed them.
     */
    final class Request {
        public final String method;
        public final String url;          // full URL incl. query string
        public final String jsonBody;     // application/json body (nullable)
        /** Streamed body (e.g. multipart) - a fresh stream per call so retries can re-send. */
        public final Supplier<InputStream> bodyStream;
        /** Exact body length in bytes when {@code bodyStream} != null. */
        public final long contentLength;
        public final String contentType;
        public final boolean idempotent;

        private Request(String method, String url, String jsonBody,
                        Supplier<InputStream> bodyStream, long contentLength,
                        String contentType, boolean idempotent) {
            this.method = method;
            this.url = url;
            this.jsonBody = jsonBody;
            this.bodyStream = bodyStream;
            this.contentLength = contentLength;
            this.contentType = contentType;
            this.idempotent = idempotent;
        }

        public static Request get(String url) {
            return new Request("GET", url, null, null, 0, null, true);
        }

        public static Request postJson(String url, String jsonBody) {
            return postJson(url, jsonBody, false);
        }

        /** {@code idempotent=true} only when the server deduplicates the write (e.g. external_key). */
        public static Request postJson(String url, String jsonBody, boolean idempotent) {
            return new Request("POST", url, jsonBody, null, 0, "application/json", idempotent);
        }

        public static Request postStream(String url, Supplier<InputStream> bodyStream,
                                         long contentLength, String contentType) {
            return new Request("POST", url, null, bodyStream, contentLength, contentType, false);
        }
    }

    /** A response. */
    final class Response {
        public final int status;
        public final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = body == null ? "" : body;
        }
    }
}
