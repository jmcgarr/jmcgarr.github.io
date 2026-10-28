package com.mikemcgarr.jbakesite.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The Rule 1 gate: the comparison, the baseline and expected-removals files, and the shell script's messages. */
class UrlCheckTest {

    @TempDir
    Path root;

    private SiteLayout site;
    private Path baked;
    private Path removals;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @BeforeEach
    void aBakeAndABaseline() throws IOException {
        site = SiteLayout.standard(root);
        baked = root.resolve("build/jbake");
        removals = root.resolve("docs/baseline/expected-removals.txt");
        Files.createDirectories(site.publishedUrls().getParent());
        Files.writeString(site.publishedUrls(), "index.html\nabout.html\nblog/a-post.html\ntags/DevOps.html\ntags/devops.html\n");
        for (String page : List.of("index.html", "about.html", "blog/a-post.html", "tags/devops.html")) {
            page(page);
        }
        Files.writeString(baked.resolve("feed.xml"), "<rss/>\n");
    }

    @Test
    void comparesTheThreeListsSorted() {
        Set<String> built = new TreeSet<>(Set.of("index.html", "blog/new.html", "tags/devops.html", "blog/a-post.html"));
        List<String> baseline = List.of("index.html", "about.html", "blog/a-post.html", "tags/DevOps.html", "tags/devops.html", "old.html");

        UrlCheck.Comparison strict = UrlCheck.compare(baseline, built, Set.of("old.html"), false);
        assertEquals(List.of("old.html"), strict.removed());
        assertEquals(List.of("blog/new.html"), strict.added());
        assertEquals(List.of(), strict.caseCollisions());
        assertEquals(List.of("about.html", "tags/DevOps.html"), strict.missing());

        UrlCheck.Comparison forgiving = UrlCheck.compare(baseline, built, Set.of("old.html"), true);
        assertEquals(List.of("tags/DevOps.html"), forgiving.caseCollisions(), "same file as tags/devops.html on a Mac");
        assertEquals(List.of("about.html"), forgiving.missing());
    }

    @Test
    void readsTheExpectedRemovalsSkippingCommentsAndBlankLinesAndAcceptsAMissingFile() throws IOException {
        assertEquals(Set.of(), UrlCheck.expectedRemovals(removals), "no file: nothing is expected to be gone");

        Files.writeString(removals, "# Published URLs removed on purpose.\n\n# 2026-10-04, T009:\nblog/drafts/x-draft.html\n   \n#   css/extra.css\nold.html\n");
        assertEquals(Set.of("blog/drafts/x-draft.html", "old.html"), UrlCheck.expectedRemovals(removals));
    }

    @Test
    void listsTheHtmlPagesOfABakeAsSitePaths() throws IOException {
        assertEquals(Set.of("index.html", "about.html", "blog/a-post.html", "tags/devops.html"), UrlCheck.builtPages(baked),
                "feed.xml is not a page");
    }

    @Test
    void passesWhenEveryBaselineUrlIsBuiltOrAnExpectedRemovalAndReportsTheNewOnes() throws IOException {
        Files.writeString(removals, "# approved\ntags/DevOps.html\n");
        page("blog/new.html");

        check(true).check(baked);

        assertEquals("Expected removals (1):\n  - tags/DevOps.html\nNew URLs (1):\n  + blog/new.html\n"
                + "OK: all 5 baseline URLs accounted for in build/jbake\n", printed());
    }

    @Test
    void failsNamingTheMissingUrlsWithTheRuleOneSentence() throws IOException {
        Files.delete(baked.resolve("about.html"));

        ToolFailure e = assertThrows(ToolFailure.class, () -> check(true).check(baked));

        assertEquals(ToolFailure.FAILED, e.exitCode());
        assertEquals("Restore them, add redirect stubs, or (with approval) list them in docs/baseline/expected-removals.txt.", e.getMessage());
        assertEquals("FAIL: published URLs missing from the build (2):\n  ! about.html\n  ! tags/DevOps.html\n", printed(),
                "strict: the case twin is missing too");
    }

    @Test
    void forgivesACaseCollisionOnlyOnACaseInsensitiveFilesystemAndWithoutStrict() throws IOException {
        // The shell script's marker of a case-insensitive filesystem: the home page's upper-case twin exists
        boolean caseInsensitive = Files.exists(baked.resolve("INDEX.HTML"));

        if (caseInsensitive) {
            check(false).check(baked);
            assertEquals("WARNING: case-insensitive filesystem; these differ only by case from a built page and can't be verified here"
                    + " (CI checks them):\n  ~ tags/DevOps.html\nOK: all 5 baseline URLs accounted for in build/jbake\n", printed());
        } else {
            ToolFailure e = assertThrows(ToolFailure.class, () -> check(false).check(baked));
            assertEquals("FAIL: published URLs missing from the build (1):\n  ! tags/DevOps.html\n", printed(), e.getMessage());
        }
    }

    @Test
    void cannotRunWithoutASiteOrABaseline() throws IOException {
        ToolFailure noSite = assertThrows(ToolFailure.class, () -> check(false).check(root.resolve("build/site")));
        assertEquals(ToolFailure.COULD_NOT_RUN, noSite.exitCode());
        assertEquals("ERROR: site directory 'build/site' not found. Run ./gradlew bake first.", noSite.getMessage());

        Files.delete(site.publishedUrls());
        ToolFailure noBaseline = assertThrows(ToolFailure.class, () -> check(false).check(baked));
        assertEquals(ToolFailure.COULD_NOT_RUN, noBaseline.exitCode());
        assertEquals("ERROR: baseline 'docs/baseline/urls.txt' not found.", noBaseline.getMessage());
        assertEquals("", printed());
    }

    private UrlCheck check(boolean strict) {
        return new UrlCheck(site, site.publishedUrls(), removals, strict, new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    private void page(String path) throws IOException {
        Path file = baked.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "<html></html>\n");
    }

    private String printed() {
        return out.toString(StandardCharsets.UTF_8);
    }
}
