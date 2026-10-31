package com.mikemcgarr.jbakesite.review;

import com.mikemcgarr.jbakesite.internal.FrontMatter;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reviews the pages a change touched (task T117 of docs/05-REVIEW-FLOW.md). It lists every file that differs from the merge base with the base branch (committed or not, plus untracked
 * files under {@code src/jbake}), maps each to the page it renders, and checks the source and the rendered page:
 *
 * <pre>
 *   Errors (fail):     E1 Markdown link syntax     E2 brackets left in the rendered text
 *                      E3 local links and images    E4 front matter          E5 image rules
 *   Warnings (listed): W2 a page's date didn't move W3 tag spelling          W4 alt text
 *                      W6 a published URL changes   W7 an existing post has no summary
 * </pre>
 *
 * <p>External links (W1) are task T119. Spelling (W5) is deferred (T120). A finding that is right for the site but
 * wrong for one line is suppressed by an HTML comment on the line before it ({@link Suppressions}). It never changes
 * a file. {@link Report} prints what it found; {@link ReviewResult#failed} says whether the task fails;
 * {@link ReviewRunner} is how the rest of the plugin runs it.
 */
final class Review {

    private final SiteLayout site;
    private final Path bakedSite;
    private final String siteHost;
    private final Git git;
    private final Clock clock;
    private final PrintStream out;

    /**
     * Creates the review.
     *
     * @param site where the content, the assets, the templates and the baseline of published URLs are
     * @param bakedSite the baked site, {@code build/jbake}
     * @param siteHost the site's published address, so links to it are checked as local ones
     * @param git the repository
     * @param clock today, for the date fixes
     * @param out where the clean-tree message goes
     */
    Review(SiteLayout site, Path bakedSite, String siteHost, Git git, Clock clock, PrintStream out) {
        this.site = site;
        this.bakedSite = bakedSite;
        this.siteHost = siteHost;
        this.git = git;
        this.clock = clock;
        this.out = out;
    }

    /**
     * The sentence for a clean tree, printed in place of a report.
     *
     * @param base the branch the changes were looked for against
     * @return the sentence
     */
    String nothingToReview(String base) {
        return "Nothing to review: no changes under " + site.display(site.source()) + " or " + site.display(site.caseRedirects())
                + " since the merge base with " + base + ".";
    }

    /**
     * Runs the review.
     *
     * @param options what to review
     * @return what was found
     * @throws IOException if a file can't be read
     * @throws InterruptedException if interrupted while asking git
     * @throws ToolFailure (exit 2) when the review can't run: no content, no bake, no git, no such base
     */
    ReviewResult run(ReviewOptions options) throws IOException, InterruptedException {
        if (options.online()) {
            out.println("Note: external links aren't checked yet (task T119); -Ponline is accepted and ignored.");
        }
        if (!Files.isDirectory(site.content())) {
            throw ToolFailure.couldNotRun("Run this from the repository root (no " + site.display(site.content()) + " here).");
        }
        if (!Files.isRegularFile(bakedSite.resolve("index.html"))) {
            throw ToolFailure.couldNotRun("No baked site in " + site.display(bakedSite) + ". Run ./gradlew bake first (./gradlew reviewChanges does).");
        }
        Set<String> publishedUrls = Set.of();
        if (Files.exists(site.publishedUrls())) {
            publishedUrls = new HashSet<>(Files.readAllLines(site.publishedUrls(), StandardCharsets.UTF_8));
        }

        Changes changes = new Changes(site, git, options.base().strip());
        List<Change> changed = options.all() ? changes.everything() : changes.changed();
        if (changed.isEmpty()) {
            out.println(nothingToReview(changes.base()));
            return ReviewResult.nothing();
        }

        Findings findings = new Findings();
        Pages pages = new Pages(site, bakedSite);
        ImageChecks imageChecks = new ImageChecks(site, findings);
        MarkdownChecks markdown = new MarkdownChecks(site, changes, imageChecks, findings, clock, options.all());
        PageChecks pageChecks = new PageChecks(site, bakedSite, siteHost, publishedUrls, pages, findings);

        Map<String, Change> toCheck = new LinkedHashMap<>();   // site-relative page -> the change that made it
        boolean templatesChanged = false;
        List<Path> images = new ArrayList<>();
        for (Change c : changed) {
            if (c.path().startsWith(site.content()) && c.path().toString().endsWith(".md")) {
                if (!c.exists()) {
                    continue;
                }
                FrontMatter fm = markdown.check(c);
                if (fm != null) {
                    toCheck.put(pages.pageFor(c.path(), fm), c);
                }
            } else if (c.path().startsWith(site.templates()) && c.exists()) {
                templatesChanged = true;
            } else if (c.path().startsWith(site.images()) && c.exists()) {
                images.add(c.path());
            }
            if ((c.status() == 'D' || c.status() == 'R') && c.removed().startsWith(site.assets())) {
                for (String page : pages.referencing(c.removed())) {
                    toCheck.putIfAbsent(page, null);   // still points at the old name: E3 says so
                }
            }
        }
        new RemovalChecks(site, changes, findings).check(changed);
        for (Path image : images) {
            imageChecks.checkImage(image);
            for (String page : pages.referencing(image)) {
                toCheck.putIfAbsent(page, null);
            }
        }
        if (templatesChanged) {
            for (String page : pages.samplePages()) {
                toCheck.putIfAbsent(page, null);
            }
        }
        for (Map.Entry<String, Change> e : toCheck.entrySet()) {
            pageChecks.checkPage(e.getKey(), e.getValue());
        }
        return new ReviewResult(findings.sorted(), findings.suppressed(), new ArrayList<>(pageChecks.reviewedPages()),
                changed.size(), options.all(), false);
    }
}
