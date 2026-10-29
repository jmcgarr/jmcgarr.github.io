package com.mikemcgarr.jbakesite.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The report's text, as the program printed it, and its GitHub Actions output. */
class ReportTest {

    private static final Finding E1 = new Finding(true, "E1", "src/jbake/content/about.md", 14,
            "Link text \"[Booz Allen Hamilton]\" is followed by \"https://www.boozallen.com/\" with no \"(\": it renders as literal brackets.",
            "[Booz Allen Hamilton](https://www.boozallen.com/)");
    private static final Finding W2 = new Finding(false, "W2", "src/jbake/content/about.md", 2,
            "This page changed but date=2026-01-01 did not. The sitemap reports it as lastmod. Today is 2026-10-28.", "date=2026-10-28");
    private static final Finding W6 = new Finding(false, "W6", "src/jbake/assets/img/old.png", 0, "Deleting this file removes the published URL /img/old.png.", null);

    @TempDir
    Path dir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final PrintStream printer = new PrintStream(out, true, StandardCharsets.UTF_8);

    @Test
    void printsEachFindingWrappedAt110ColumnsThenTheSummary() throws IOException {
        ReviewResult result = new ReviewResult(List.of(E1, W2, W6), 0, List.of("about.html"), 1, false, false);

        new Report(printer, Map.of()).print(result);

        assertEquals("""
                ERROR E1 src/jbake/content/about.md:14
                  Link text "[Booz Allen Hamilton]" is followed by "https://www.boozallen.com/" with no "(": it renders as\s
                  literal brackets.
                  Fix: [Booz Allen Hamilton](https://www.boozallen.com/)

                WARN  W2 src/jbake/content/about.md:2
                  This page changed but date=2026-01-01 did not. The sitemap reports it as lastmod. Today is 2026-10-28.
                  Fix: date=2026-10-28

                WARN  W6 src/jbake/assets/img/old.png
                  Deleting this file removes the published URL /img/old.png.

                Reviewed 1 page (about.html) from 1 changed file. 1 error, 2 warnings. External links not checked (T119).
                """, out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void theSummaryCountsPluralsListsAtMostSixPagesAndTheSuppressions() throws IOException {
        List<String> pages = List.of("a.html", "b.html", "c.html", "d.html", "e.html", "f.html", "g.html");

        new Report(printer, Map.of()).print(new ReviewResult(List.of(), 2, pages, 3, false, false));
        new Report(printer, Map.of()).print(new ReviewResult(List.of(), 0, List.of(), 0, true, false));
        new Report(printer, Map.of()).print(ReviewResult.nothing());

        assertEquals("Reviewed 7 pages (a.html, b.html, c.html, d.html, e.html, f.html, …) from 3 changed files. 0 errors, 0 warnings, 2 suppressed."
                + " External links not checked (T119).\n"
                + "Reviewed no pages. 0 errors, 0 warnings. External links not checked (T119).\n", out.toString(StandardCharsets.UTF_8),
                "-Pall has no changed-file count; nothing to review prints nothing more");
    }

    @Test
    void inGitHubActionsItAnnotatesAndAppendsTheStepSummary() throws IOException {
        Path summary = dir.resolve("summary.md");
        Files.writeString(summary, "earlier\n");
        ReviewResult result = new ReviewResult(List.of(E1, W6), 0, List.of("about.html"), 1, false, false);

        new Report(printer, Map.of("GITHUB_ACTIONS", "true", "GITHUB_STEP_SUMMARY", summary.toString())).print(result);

        String printed = out.toString(StandardCharsets.UTF_8);
        String text = Report.text(result);
        assertEquals(text
                + "::error file=src/jbake/content/about.md,line=14,title=E1::Link text \"[Booz Allen Hamilton]\" is followed by"
                + " \"https://www.boozallen.com/\" with no \"(\": it renders as literal brackets. Fix: [Booz Allen Hamilton](https://www.boozallen.com/)\n"
                + "::warning file=src/jbake/assets/img/old.png,title=W6::Deleting this file removes the published URL /img/old.png.\n", printed);
        assertEquals("earlier\n### Review of the changed pages\n\n```\n" + text + "```\n", Files.readString(summary));
    }

    @Test
    void outsideGitHubActionsThereAreNoAnnotations() throws IOException {
        new Report(printer, Map.of("GITHUB_ACTIONS", "false")).print(new ReviewResult(List.of(E1), 0, List.of("about.html"), 1, false, false));

        assertFalse(out.toString(StandardCharsets.UTF_8).contains("::error"));
    }

    @Test
    void annotationsEscapePercentAndNewlines() {
        Finding f = new Finding(true, "E4", "x.md", 0, "100%\nsure", null);

        assertEquals(List.of("::error file=x.md,title=E4::100%25%0Asure"), Report.annotations(new ReviewResult(List.of(f), 0, List.of(), 1, false, false)));
    }
}
