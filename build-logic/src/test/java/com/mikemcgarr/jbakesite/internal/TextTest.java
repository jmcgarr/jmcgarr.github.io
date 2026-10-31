package com.mikemcgarr.jbakesite.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The one reading policy for the author's files, and the one line-ending normalisation. */
class TextTest {

    @TempDir
    Path dir;

    @Test
    void readsTheAuthorsFileLenientlyKeepingItsLineEndings() throws IOException {
        Path file = dir.resolve("post.md");
        Files.write(file, new byte[] {'t', 'i', 't', 'l', 'e', '=', 'c', 'a', 'f', (byte) 0xE9, '\r', '\n', 'x'});   // Latin-1 é

        assertEquals("title=caf\uFFFD\r\nx", Text.read(file), "a malformed byte is reported as U+FFFD, not thrown");
    }

    @Test
    void makesEveryLineEndingLfAndSplitsLinesLikeSplitDoes() {
        assertEquals("a\nb\nc\n", Text.lf("a\r\nb\rc\n"));
        String lfOnly = "a\nb";
        assertSame(lfOnly, Text.lf(lfOnly), "nothing to do: the same string");
        assertArrayEquals(new String[] {"a", "b", "c", ""}, Text.lines("a\r\nb\rc\n"), "a trailing newline gives a trailing empty line");
        assertArrayEquals(new String[] {""}, Text.lines(""));
    }
}
