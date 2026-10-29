package com.mikemcgarr.jbakesite.verification;

import com.mikemcgarr.jbakesite.FileTrees;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares the HTML pages in a baked site against the inventory of the live site's URLs, so a change can't silently
 * remove a published URL (AGENTS.md Rule 1; revival task T006, {@code scripts/check-urls.sh} until T143):
 *
 * <pre>
 *   missing = in the baseline, not in the build, not an expected removal  -&gt; FAIL
 *   removed = in the baseline, not in the build, listed as an expected removal
 *   new     = in the build, not in the baseline                           -&gt; information only
 * </pre>
 *
 * <p>On a case-insensitive filesystem (a Mac) pages whose names differ only by case, {@code tags/DevOps.html} and
 * {@code tags/devops.html}, overwrite each other during the bake, so one of each pair can never exist locally. A
 * missing URL that matches a built page case-insensitively is reported as a warning there, not a failure; CI runs on
 * Linux, where the check stays strict, and {@code -Pstrict} asks for the strict check anywhere. The messages are the
 * shell script's, line for line; what the script wrote to standard error is the {@link ToolFailure}'s message.
 */
final class UrlCheck {

    /** The marker of a case-insensitive filesystem: the shell script looked for the upper-case twin of the home page. */
    private static final String UPPER_CASE_INDEX = "INDEX.HTML";

    private final SiteLayout site;
    private final Path baseline;
    private final Path expectedRemovals;
    private final boolean strict;
    private final PrintStream out;

    /**
     * Creates the check.
     *
     * @param site the site's layout, for the paths in the messages
     * @param baseline the baseline of published URLs, one site-relative path per line
     * @param expectedRemovals the URLs removed on purpose, one per line, {@code #} comments and blank lines ignored;
     *        may not exist
     * @param strict whether to report case collisions as missing even on a case-insensitive filesystem
     * @param out where the lists and the OK line go
     */
    UrlCheck(SiteLayout site, Path baseline, Path expectedRemovals, boolean strict, PrintStream out) {
        this.site = site;
        this.baseline = baseline;
        this.expectedRemovals = expectedRemovals;
        this.strict = strict;
        this.out = out;
    }

    /**
     * Checks a baked site.
     *
     * @param bakedSite the site, {@code build/jbake} or CI's staged {@code build/site}
     * @throws IOException if a file can't be read
     * @throws ToolFailure exit 2 without a site or a baseline; exit 1 with the Rule 1 sentence when a published URL
     *         is missing, after the missing URLs are listed
     */
    void check(Path bakedSite) throws IOException {
        String shownSite = site.display(bakedSite);
        if (!Files.isDirectory(bakedSite)) {
            throw ToolFailure.couldNotRun("ERROR: site directory '" + shownSite + "' not found. Run ./gradlew bake first.");
        }
        if (!Files.isRegularFile(baseline)) {
            throw ToolFailure.couldNotRun("ERROR: baseline '" + site.display(baseline) + "' not found.");
        }
        List<String> baselineLines = Files.readAllLines(baseline, StandardCharsets.UTF_8);
        Set<String> allowed = expectedRemovals(expectedRemovals);
        boolean forgiveCaseCollisions = !strict && Files.exists(bakedSite.resolve(UPPER_CASE_INDEX));
        Comparison comparison = compare(baselineLines, builtPages(bakedSite), allowed, forgiveCaseCollisions);

        list("Expected removals", "  - ", comparison.removed());
        list("New URLs", "  + ", comparison.added());
        if (!comparison.caseCollisions().isEmpty()) {
            out.println("WARNING: case-insensitive filesystem; these differ only by case from a built page and can't be verified here (CI checks them):");
            comparison.caseCollisions().forEach(url -> out.println("  ~ " + url));
        }
        if (!comparison.missing().isEmpty()) {
            list("FAIL: published URLs missing from the build", "  ! ", comparison.missing());
            throw ToolFailure.failed("Restore them, add redirect stubs, or (with approval) list them in "
                    + site.display(expectedRemovals) + ".");
        }
        out.println("OK: all " + baselineLines.size() + " baseline URLs accounted for in " + shownSite);
    }

    private void list(String heading, String bullet, Collection<String> urls) {
        if (urls.isEmpty()) {
            return;
        }
        out.println(heading + " (" + urls.size() + "):");
        urls.forEach(url -> out.println(bullet + url));
    }

    /**
     * The HTML pages of a baked site, as site-relative paths with {@code /} separators.
     *
     * @param bakedSite the site
     * @return the pages, sorted
     * @throws IOException if the site can't be read
     */
    static Set<String> builtPages(Path bakedSite) throws IOException {
        Set<String> pages = new TreeSet<>();
        for (Path file : FileTrees.regularFilesUnder(bakedSite)) {
            if (file.getFileName().toString().endsWith(".html")) {
                pages.add(SiteLayout.relativePath(bakedSite, file));
            }
        }
        return pages;
    }

    /**
     * The URLs listed as removed on purpose: every line of the file that isn't a {@code #} comment or blank.
     *
     * @param file the expected-removals file; a missing file means none
     * @return the URLs
     * @throws IOException if the file can't be read
     */
    static Set<String> expectedRemovals(Path file) throws IOException {
        Set<String> allowed = new TreeSet<>();
        if (Files.isRegularFile(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.startsWith("#") && !line.isBlank()) {
                    allowed.add(line);
                }
            }
        }
        return allowed;
    }

    /**
     * The comparison itself, on the three lists.
     *
     * @param baseline the published URLs
     * @param built the pages in the build
     * @param allowed the URLs removed on purpose
     * @param forgiveCaseCollisions whether a missing URL that matches a built page case-insensitively is a collision
     *        (a case-insensitive filesystem) rather than missing
     * @return what is removed, new, colliding and missing, each sorted
     */
    static Comparison compare(Collection<String> baseline, Set<String> built, Set<String> allowed, boolean forgiveCaseCollisions) {
        Set<String> gone = new TreeSet<>(baseline);
        gone.removeAll(built);
        Set<String> removed = new TreeSet<>(gone);
        removed.retainAll(allowed);
        Set<String> added = new TreeSet<>(built);
        added.removeAll(baseline);
        List<String> collisions = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String url : gone) {
            if (allowed.contains(url)) {
                continue;
            }
            if (forgiveCaseCollisions && built.stream().anyMatch(page -> page.equalsIgnoreCase(url))) {
                collisions.add(url);
            } else {
                missing.add(url);
            }
        }
        return new Comparison(List.copyOf(removed), List.copyOf(added), List.copyOf(collisions), List.copyOf(missing));
    }

    /**
     * What the comparison found, each list sorted.
     *
     * @param removed baseline URLs not in the build that are listed as expected removals
     * @param added pages in the build that the baseline doesn't have
     * @param caseCollisions baseline URLs not in the build that match a built page case-insensitively, on a
     *        case-insensitive filesystem
     * @param missing baseline URLs not in the build and not accounted for: the failure
     */
    record Comparison(List<String> removed, List<String> added, List<String> caseCollisions, List<String> missing) {
    }
}
