package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Copying a tree into the bake output, and the one file walk the tools list files with. */
class FileTreesTest {

    @TempDir
    Path from;

    @TempDir
    Path to;

    @Test
    void copiesEveryFileKeepingRelativePathsAndReplacing() throws IOException {
        Files.createDirectories(from.resolve("css"));
        Files.writeString(from.resolve("css/clean-blog.css"), "new");
        Files.writeString(from.resolve("index.html"), "home");
        Files.createDirectories(to.resolve("css"));
        Files.writeString(to.resolve("css/clean-blog.css"), "old");
        Files.writeString(to.resolve("about.html"), "kept");

        FileTrees.copy(from, to);

        assertEquals("new", Files.readString(to.resolve("css/clean-blog.css")));
        assertEquals("home", Files.readString(to.resolve("index.html")));
        assertEquals("kept", Files.readString(to.resolve("about.html")), "nothing is deleted from the destination");
    }

    @Test
    void listsTheFilesUnderADirectoryOrDirectlyInItSortedAndNoneForAMissingOne() throws IOException {
        Files.createDirectories(from.resolve("drafts"));
        Files.writeString(from.resolve("b.md"), "");
        Files.writeString(from.resolve("a.md"), "");
        Files.writeString(from.resolve("drafts/c.md"), "");

        assertEquals(List.of(from.resolve("a.md"), from.resolve("b.md"), from.resolve("drafts/c.md")), FileTrees.regularFilesUnder(from));
        assertEquals(List.of(from.resolve("a.md"), from.resolve("b.md")), FileTrees.regularFilesIn(from), "not the subdirectory's");
        assertEquals(List.of(), FileTrees.regularFilesUnder(from.resolve("missing")));
        assertEquals(List.of(), FileTrees.regularFilesIn(from.resolve("missing")));
    }

    @Test
    void anEmptySourceCopiesNothing() throws IOException {
        FileTrees.copy(from, to);

        try (var files = Files.list(to)) {
            assertTrue(files.findAny().isEmpty());
        }
    }
}
