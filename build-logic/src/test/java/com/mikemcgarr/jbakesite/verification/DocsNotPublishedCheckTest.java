package com.mikemcgarr.jbakesite.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The Rule 4 guard: a docs folder, Markdown outside vendor/, the marker word in a text file, and the messages. */
class DocsNotPublishedCheckTest {

    @TempDir
    Path root;

    private SiteLayout site;
    private Path baked;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @BeforeEach
    void aCleanBake() throws IOException {
        site = SiteLayout.standard(root);
        baked = root.resolve("build/jbake");
        write("index.html", "<html><body>Home</body></html>\n");
        write("vendor/fontawesome-free/README.md", "# Font Awesome\n");
        write("img/photo.png", "\u0089PNG\r\n\u001a\n\u0000\u0000REVIVAL");   // a binary file with the marker's bytes
    }

    @Test
    void passesACleanBakeAndSkipsVendorReadmesAndBinaryFiles() throws IOException {
        check().check(baked);

        assertEquals("OK: no docs/ content in build/jbake\n", printed());
    }

    @Test
    void failsOnADocsFolderMarkdownAndTheMarkerListingEachWithTheRuleFourSentence() throws IOException {
        write("docs/00-REVIVAL.md", "# The REVIVAL plan\n");
        write("notes.md", "scratch\n");
        write("about.html", "<p>See the REVIVAL plan.</p>\n");
        write("vendor/lib/CHANGES.html", "<p>REVIVAL of the library.</p>\n");   // vendor/ is skipped for Markdown only

        ToolFailure e = assertThrows(ToolFailure.class, () -> check().check(baked));

        assertEquals(ToolFailure.FAILED, e.exitCode());
        assertEquals("docs/ content must never be published. See AGENTS.md (Rule 4).", e.getMessage());
        assertEquals("FAIL: 'build/jbake/docs' exists.\n"
                + "FAIL: Markdown files found in the site:\n  build/jbake/docs/00-REVIVAL.md\n  build/jbake/notes.md\n"
                + "FAIL: planning-doc marker 'REVIVAL' found in:\n  build/jbake/about.html\n  build/jbake/docs/00-REVIVAL.md\n"
                + "  build/jbake/vendor/lib/CHANGES.html\n", printed());
    }

    @Test
    void theMarkerSearchIsByteWiseAndIgnoresBinaryFiles() throws IOException {
        write("text.txt", "the REVIVAL plan\n");
        assertTrue(DocsNotPublishedCheck.containsMarker(baked.resolve("text.txt")));
        assertFalse(DocsNotPublishedCheck.containsMarker(baked.resolve("img/photo.png")), "a NUL byte makes it binary, as grep -I");
        assertFalse(DocsNotPublishedCheck.containsMarker(baked.resolve("index.html")));
    }

    @Test
    void cannotRunWithoutASite() {
        ToolFailure e = assertThrows(ToolFailure.class, () -> check().check(root.resolve("build/site")));

        assertEquals(ToolFailure.COULD_NOT_RUN, e.exitCode());
        assertEquals("ERROR: site directory 'build/site' not found. Run ./gradlew bake first.", e.getMessage());
    }

    private DocsNotPublishedCheck check() {
        return new DocsNotPublishedCheck(site, new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    private void write(String path, String text) throws IOException {
        Path file = baked.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.ISO_8859_1);
    }

    private String printed() {
        return out.toString(StandardCharsets.UTF_8);
    }
}
