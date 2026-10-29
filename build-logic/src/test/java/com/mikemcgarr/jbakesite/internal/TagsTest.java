package com.mikemcgarr.jbakesite.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The tags= line, and the spellings in use across the posts (T055). */
class TagsTest {

    @TempDir
    Path dir;

    @Test
    void splitsAndStripsAndDropsEmptyTags() {
        assertEquals(List.of("management", "leadership", "unit testing"), Tags.split(" management, leadership ,, unit testing,"));
        assertEquals(List.of(), Tags.split(""));
    }

    @Test
    void collectsEverySpellingOfATagAcrossFilesWhateverTheLineEndings() throws IOException {
        Path a = Files.writeString(dir.resolve("a.md"), "title=A\ntags=DevOps, java\nstatus=published\n~~~~~~\ntags=notheader\n");
        Path b = Files.writeString(dir.resolve("b.md"), "title=B\r\ntags=devops, Java\r\n~~~~~~\r\n");

        Map<String, TreeSet<String>> spellings = Tags.spellingsIn(List.of(a, b));

        assertEquals(new TreeSet<>(List.of("DevOps", "devops")), spellings.get("devops"));
        assertEquals(new TreeSet<>(List.of("Java", "java")), spellings.get("java"));
        assertEquals(2, spellings.size(), "nothing after the separator counts");
    }
}
