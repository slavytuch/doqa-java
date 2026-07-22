package app.doqa.core;

import app.doqa.client.DoqaConfig;

/**
 * Payload-size guard rails: truncation with an explicit marker, and the active limits (from the
 * session config, or the client-core defaults when no session is up yet).
 *
 * <p>Internal adapter API.
 */
public final class Limits {

    private Limits() {
    }

    /** Truncate to {@code max} chars, appending a marker with the dropped size. */
    public static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "\n… truncated (" + (s.length() - max) + " chars)";
    }

    /** Active limit for stringified parameter values. */
    public static int maxParameterLength() {
        DoqaConfig config = DoqaSession.currentConfig();
        return config != null ? config.maxParameterLength() : DoqaConfig.DEFAULT_MAX_PARAMETER_LENGTH;
    }
}
