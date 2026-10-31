package com.mikemcgarr.jbakesite.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The header of a content file: {@code key=value} lines up to the {@code ~~~~~~} separator (AGENTS.md Rule 3), each
 * with its line number, as {@code ReviewChanges.java} read it. The review checks it (E4), the tag tools read
 * {@code tags=} and {@code status=} from it, and {@code newPost} writes one. The first value of a repeated key wins.
 *
 * @param values the keys and their raw values (not stripped), in file order
 * @param lines each key's 1-based line number
 * @param bodyStart the 0-based index of the first body line (the line after the separator)
 * @param separated whether the separator line was found; without it JBake renders nothing
 */
public record FrontMatter(Map<String, String> values, Map<String, Integer> lines, int bodyStart, boolean separated) {

    /** The line that ends the header. */
    public static final String SEPARATOR = "~~~~~~";

    /**
     * Reads the header of a file's text; line endings of any kind are accepted ({@link Text#lines}).
     *
     * @param text the whole file
     * @return the header
     */
    public static FrontMatter parse(String text) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, Integer> lines = new HashMap<>();
        String[] all = Text.lines(text);
        for (int i = 0; i < all.length; i++) {
            String line = all[i].strip();
            if (line.startsWith(SEPARATOR)) {
                return new FrontMatter(values, lines, i + 1, true);
            }
            int eq = line.indexOf('=');
            if (eq > 0) {
                String key = line.substring(0, eq).strip();
                values.putIfAbsent(key, line.substring(eq + 1));
                lines.putIfAbsent(key, i + 1);
            }
        }
        return new FrontMatter(values, lines, all.length, false);
    }

    /**
     * A value, stripped; "" when the key is absent.
     *
     * @param key the key, such as {@code date}
     * @return the value
     */
    public String get(String key) {
        return values.getOrDefault(key, "").strip();
    }

    /**
     * Whether the file is a draft ({@code status=draft}): rendered for preview, never published.
     *
     * @return true for a draft
     */
    public boolean isDraft() {
        return get("status").equals("draft");
    }

    /**
     * Whether the file is published ({@code status=published}).
     *
     * @return true for a published page or post
     */
    public boolean isPublished() {
        return get("status").equals("published");
    }
}
