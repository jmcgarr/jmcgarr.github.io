package com.mikemcgarr.jbakesite.review;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Just enough HTML reading for the page checks: where a tag ends, its name and attributes, and entities. */
final class HtmlTags {

    private static final Pattern ATTR = Pattern.compile("([a-zA-Z_:][-\\w:.]*)\\s*(?:=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+)))?");

    private HtmlTags() {
    }

    /** A start or end tag. */
    record Tag(String name, Map<String, String> attrs, boolean closing, boolean selfClosing) {
    }

    /** The index of the {@code >} that ends the tag opening at {@code lt}, skipping quoted attribute values. */
    static int endOfTag(String html, int lt) {
        int i = lt + 1;
        char quote = 0;
        while (i < html.length()) {
            char ch = html.charAt(i);
            if (quote != 0) {
                if (ch == quote) {
                    quote = 0;
                }
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (ch == '>') {
                return i;
            }
            i++;
        }
        return html.length() - 1;
    }

    /** The tag between {@code <} and {@code >}, or null for a doctype, a processing instruction or junk. */
    static Tag parseTag(String inside) {
        if (inside.isEmpty() || inside.startsWith("!") || inside.startsWith("?")) {
            return null;
        }
        boolean closing = inside.startsWith("/");
        String body = closing ? inside.substring(1) : inside;
        boolean selfClosing = body.endsWith("/");
        if (selfClosing) {
            body = body.substring(0, body.length() - 1);
        }
        Matcher m = ATTR.matcher(body);
        if (!m.find() || m.start() != 0 && !body.substring(0, m.start()).isBlank()) {
            return null;
        }
        String name = m.group(1).toLowerCase(Locale.ROOT);
        Map<String, String> attrs = new LinkedHashMap<>();
        while (m.find()) {
            String value = m.group(2) != null ? m.group(2) : m.group(3) != null ? m.group(3) : m.group(4);
            attrs.put(m.group(1).toLowerCase(Locale.ROOT), value == null ? null : decode(value));
        }
        return new Tag(name, attrs, closing, selfClosing);
    }

    /** The named entities that matter for brackets and URLs; numeric ones are dropped. */
    static String decode(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&nbsp;", " ").replaceAll("&#(\\d+);", "\u0001$1\u0002").replace("&amp;", "&")
                .replaceAll("\u0001(\\d+)\u0002", "")   // numeric entities don't matter for brackets or URLs
                .replace("\u0001", "").replace("\u0002", "");
    }

    /** How many times {@code ch} occurs in {@code s} between {@code from} and {@code to}. */
    static int count(String s, int from, int to, char ch) {
        int n = 0;
        for (int i = from; i < to && i < s.length(); i++) {
            if (s.charAt(i) == ch) {
                n++;
            }
        }
        return n;
    }
}
