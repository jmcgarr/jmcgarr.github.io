package com.mikemcgarr.jbakesite.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ProcessRunner;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.TestRepositories;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The package's entry point: the outcome and the summary line, on a clean tree and on a broken page. */
class ReviewRunnerTest {

    private static final Clock TODAY = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path root;

    private SiteLayout site;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private ReviewRunner runner;

    @BeforeEach
    void aCommittedSiteWithABake() throws IOException, InterruptedException {
        site = SiteLayout.standard(root);
        Path baked = root.resolve("build/jbake");
        Files.createDirectories(site.content());
        Files.createDirectories(baked);
        Files.writeString(site.content().resolve("about.md"), "title=About\ndate=2026-01-01\ntype=page\nstatus=published\n~~~~~~\n\nAbout me.\n");
        Files.writeString(baked.resolve("index.html"), "<html><body>Home</body></html>\n");
        Files.writeString(baked.resolve("about.html"), "<html><body><p>About me.</p></body></html>\n");
        Files.writeString(root.resolve(".gitignore"), "build/\n");
        TestRepositories.init(root);
        TestRepositories.commitAll(root, "the site");
        PrintStream printer = new PrintStream(out, true, StandardCharsets.UTF_8);
        Git git = new Git(new ProcessRunner(root, printer, printer));
        runner = new ReviewRunner(site, baked, "https://www.example.test", git, TODAY, printer, Map.of());
    }

    @Test
    void aCleanTreePassesWithTheCleanTreeSentenceAsItsSummary() throws IOException, InterruptedException {
        ReviewOutcome outcome = runner.run("main");

        assertTrue(outcome.passed());
        assertEquals("Nothing to review: no changes under src/jbake or src/case-redirects since the merge base with main.", outcome.summary());
        assertEquals(outcome.summary() + "\n", out.toString(StandardCharsets.UTF_8), "printed once, by the review");
    }

    @Test
    void aBrokenPageFailsWithTheReportsSummaryLineAfterTheReportIsPrinted() throws IOException, InterruptedException {
        Path about = site.content().resolve("about.md");
        Files.writeString(about, Files.readString(about) + "Worked at [Acme] https://acme.example/ for years.\n");

        ReviewOutcome outcome = runner.run("main");

        assertFalse(outcome.passed());
        assertEquals("Reviewed 1 page (about.html) from 1 changed file. 1 error, 1 warning. External links not checked (T119).", outcome.summary(),
                "E1 for the link, W2 for the page's date");
        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.startsWith("ERROR E1 src/jbake/content/about.md:8\n"), printed);
        assertTrue(printed.endsWith(outcome.summary() + "\n"), printed);
    }

    @Test
    void strictCountsWarnings() throws IOException, InterruptedException {
        Path about = site.content().resolve("about.md");
        Files.writeString(about, Files.readString(about) + "A harmless edit.\n");

        assertTrue(runner.run("main").passed(), "W2 alone passes");
        assertFalse(runner.run(new ReviewOptions("main", false, true, false)).passed(), "unless -Pstrict");
    }
}
