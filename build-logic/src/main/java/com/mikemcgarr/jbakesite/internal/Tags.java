package com.mikemcgarr.jbakesite.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * The {@code tags=} line: a comma-separated list, each tag stripped. Tags that differ only by case make separate tag
 * pages that each list only some posts (T055), so {@code newPost} and the review (W3) both compare a new tag with
 * the spellings in use; this is that comparison's data, which the two programs built separately until T127.
 */
public final class Tags {

    private Tags() {
    }

    /**
     * Splits a {@code tags=} value into its tags.
     *
     * @param value the value, such as {@code "management, leadership"}
     * @return the tags, stripped, without empty ones
     */
    public static List<String> split(String value) {
        List<String> tags = new ArrayList<>();
        for (String tag : value.split(",")) {
            if (!tag.isBlank()) {
                tags.add(tag.strip());
            }
        }
        return tags;
    }

    /**
     * Every spelling of every tag in the given content files, keyed by the tag in lower case.
     *
     * @param files content files whose headers are read; line endings of any kind are accepted
     * @return lower-cased tag to the spellings in use, each set sorted
     * @throws IOException if a file can't be read
     */
    public static Map<String, TreeSet<String>> spellingsIn(Collection<Path> files) throws IOException {
        Map<String, TreeSet<String>> spellings = new HashMap<>();
        for (Path file : files) {
            for (String tag : split(FrontMatter.parse(Text.read(file)).get("tags"))) {
                spellings.computeIfAbsent(tag.toLowerCase(Locale.ROOT), k -> new TreeSet<>()).add(tag);
            }
        }
        return spellings;
    }
}
