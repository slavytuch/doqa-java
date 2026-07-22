package app.doqa.client;

import java.util.LinkedHashMap;
import java.util.Map;

/** A link (upsert def link or result link). {@code type} in {@link LinkType}. */
public final class Link implements Model {

    private final String url;
    private final String type;   // wire value; nullable
    private final String title;
    private final String description;

    public Link(String url, String type, String title, String description) {
        this.url = url;
        this.type = type;
        this.title = title;
        this.description = description;
    }

    public Link(String url, LinkType type, String title, String description) {
        this(url, type == null ? null : type.wire(), title, description);
    }

    public String url() { return url; }
    public String type() { return type; }
    public String title() { return title; }
    public String description() { return description; }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("url", url);
        m.put("type", type);
        m.put("title", title);
        m.put("description", description);
        return Payloads.compact(m);
    }
}
