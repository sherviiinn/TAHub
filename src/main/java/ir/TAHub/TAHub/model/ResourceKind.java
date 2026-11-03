package ir.TAHub.TAHub.model;

import java.net.URI;
import java.util.Locale;

/**
 * Guesses what a link points to from its address, so the page can show a matching icon
 * (CSS class "resource-<kind>"). It is only a guess for the look; nothing depends on it.
 */
public final class ResourceKind {

    private ResourceKind() {
    }

    public static String of(String url) {
        String host = "";
        String path = "";
        try {
            URI uri = URI.create(url);
            host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return "link";
        }
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }

        if (is(host, "youtube.com") || is(host, "youtu.be")) {
            return "youtube";
        }
        if (is(host, "aparat.com")) {
            return "aparat";
        }
        if (is(host, "drive.google.com") || is(host, "docs.google.com")) {
            return "drive";
        }
        if (is(host, "t.me") || is(host, "telegram.me")) {
            return "telegram";
        }
        if (is(host, "github.com")) {
            return "github";
        }
        if (path.endsWith(".pdf")) {
            return "pdf";
        }
        return "link";
    }

    /** The host itself or one of its subdomains (m.youtube.com counts as youtube.com). */
    private static boolean is(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }
}
