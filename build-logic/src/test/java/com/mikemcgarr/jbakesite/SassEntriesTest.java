package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The entry convention: which files in src/scss are compiled, and to what name. */
class SassEntriesTest {

    @TempDir
    Path scss;

    @Test
    void entriesAreTheTopLevelScssFilesNotNamedLikePartials() throws IOException {
        for (String name : List.of("code.scss", "_variables.scss", "clean-blog.scss", "_mixins.scss", "README.md")) {
            Files.writeString(scss.resolve(name), "");
        }
        Files.createDirectories(scss.resolve("vendor"));
        Files.writeString(scss.resolve("vendor/bootstrap.scss"), "");

        assertEquals(List.of(scss.resolve("clean-blog.scss"), scss.resolve("code.scss")), SassEntries.in(scss));
    }

    @Test
    void aMissingDirectoryHasNoEntries() throws IOException {
        assertEquals(List.of(), SassEntries.in(scss.resolve("nowhere")));
    }

    @Test
    void isEntryNeedsANameBeforeTheSuffix() {
        assertTrue(SassEntries.isEntry("clean-blog.scss"));
        assertFalse(SassEntries.isEntry("_variables.scss"));
        assertFalse(SassEntries.isEntry(".scss"));
        assertFalse(SassEntries.isEntry("clean-blog.css"));
        assertFalse(SassEntries.isEntry("clean-blog.sass"));
    }

    @Test
    void cssNameReplacesTheSuffix() {
        assertEquals("clean-blog.css", SassEntries.cssName(Path.of("src/scss/clean-blog.scss")));
        assertEquals("code.css", SassEntries.cssName(Path.of("code.scss")));
    }
}
