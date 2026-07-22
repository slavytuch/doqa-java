package app.doqa.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 * Default {@link Transport} backed by {@code java.net.http.HttpClient} (JDK 11+). Honours
 * {@link DoqaConfig#certValidation()} (an all-trusting {@link SSLContext} when disabled) and
 * {@link DoqaConfig#proxy()} ({@code host:port}). Per-request connect/read timeout applied.
 */
public final class HttpClientTransport implements Transport {

    private final HttpClient client;
    private final Duration timeout;

    public HttpClientTransport(DoqaConfig config, Duration timeout) {
        this.timeout = timeout;
        // HTTP/1.1 pinned: on plain-http URLs the JDK default (HTTP_2) sends an h2c-upgrade
        // preamble (Connection: Upgrade + Upgrade: h2c), which some HTTP servers mishandle by
        // dropping the request body. The upgrade buys nothing for short one-shot POSTs.
        HttpClient.Builder b = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout);
        if (!config.certValidation()) {
            b.sslContext(trustAllContext());
        }
        String proxy = config.proxy();
        if (proxy != null && !proxy.trim().isEmpty()) {
            b.proxy(ProxySelector.of(parseProxy(proxy.trim())));
        }
        this.client = b.build();
    }

    @Override
    public Response send(Request request) throws IOException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(request.url))
                .timeout(timeout);
        if ("GET".equals(request.method)) {
            b.GET();
        } else if (request.bodyStream != null) {
            // fromPublisher pins the exact Content-Length so the server never sees a chunked
            // multipart (some backends reject chunked uploads).
            b.header("Content-Type", request.contentType)
                    .POST(HttpRequest.BodyPublishers.fromPublisher(
                            HttpRequest.BodyPublishers.ofInputStream(request.bodyStream),
                            request.contentLength));
        } else {
            b.header("Content-Type", request.contentType == null
                            ? "application/json" : request.contentType)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            request.jsonBody == null ? "" : request.jsonBody));
        }
        try {
            HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(resp.statusCode(), resp.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("request interrupted", e);
        }
    }

    private static InetSocketAddress parseProxy(String proxy) {
        String hostPort = proxy;
        int scheme = hostPort.indexOf("://");
        if (scheme >= 0) {
            hostPort = hostPort.substring(scheme + 3);
        }
        String host = hostPort;
        String portPart = null;
        if (hostPort.startsWith("[")) {
            // bracketed IPv6 literal, e.g. [::1]:3128
            int close = hostPort.indexOf(']');
            if (close > 0) {
                host = hostPort.substring(1, close);
                if (hostPort.startsWith(":", close + 1)) {
                    portPart = hostPort.substring(close + 2);
                }
            }
        } else {
            int colon = hostPort.lastIndexOf(':');
            // a single colon separates host:port; several colons mean a bare IPv6 literal
            if (colon >= 0 && hostPort.indexOf(':') == colon) {
                host = hostPort.substring(0, colon);
                portPart = hostPort.substring(colon + 1);
            }
        }
        int port = 8080;
        if (portPart != null) {
            try {
                port = Integer.parseInt(portPart.trim());
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }
        return new InetSocketAddress(host, port);
    }

    private static SSLContext trustAllContext() {
        try {
            // An EXTENDED trust manager with empty checks: the JDK performs endpoint (hostname)
            // identification itself only when wrapping a non-extended manager, so a plain
            // X509TrustManager would still reject self-signed certs issued for another host -
            // the typical on-prem case certValidation=false exists for.
            TrustManager[] trustAll = {new X509ExtendedTrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] c, String a) { }
                @Override public void checkServerTrusted(X509Certificate[] c, String a) { }
                @Override public void checkClientTrusted(X509Certificate[] c, String a, Socket s) { }
                @Override public void checkServerTrusted(X509Certificate[] c, String a, Socket s) { }
                @Override public void checkClientTrusted(X509Certificate[] c, String a, SSLEngine e) { }
                @Override public void checkServerTrusted(X509Certificate[] c, String a, SSLEngine e) { }
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }};
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("failed to build trust-all SSLContext", e);
        }
    }
}
