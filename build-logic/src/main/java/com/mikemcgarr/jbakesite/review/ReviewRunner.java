package com.mikemcgarr.jbakesite.review;

import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;

/**
 * The review package's entry point: runs the {@link Review}, prints its {@link Report}, and answers with a
 * {@link ReviewOutcome}. {@code reviewChanges} ({@link ReviewChangesTask}) and {@code openPr} (T121, which runs the
 * review in-process before it pushes anything) both go through it, so the review exists once and the rest of the
 * package stays private.
 */
public final class ReviewRunner {

    private final Review review;
    private final Report report;

    /**
     * Creates the runner.
     *
     * @param site where the content, the assets, the templates and the baseline of published URLs are
     * @param bakedSite the baked site, {@code build/jbake}
     * @param siteHost the site's published address, so links to it are checked as local ones
     * @param git the repository
     * @param clock today, for the date fixes
     * @param out where the report goes
     * @param env the environment, read for {@code GITHUB_ACTIONS} and {@code GITHUB_STEP_SUMMARY} (annotations and the
     *        step summary in CI); empty for a local command
     */
    public ReviewRunner(SiteLayout site, Path bakedSite, String siteHost, Git git, Clock clock, PrintStream out,
            Map<String, String> env) {
        this.review = new Review(site, bakedSite, siteHost, git, clock, out);
        this.report = new Report(out, env);
    }

    /**
     * Reviews the pages changed since the merge base with a branch, the way {@code ./gradlew reviewChanges} does
     * without options, and prints the report.
     *
     * @param base the branch to diff against, {@code main} for a pull request
     * @return whether the review passed, and its summary line
     * @throws IOException if a file can't be read
     * @throws InterruptedException if interrupted while asking git
     * @throws ToolFailure (exit 2) when the review can't run: no content, no bake, no git, no such base
     */
    public ReviewOutcome run(String base) throws IOException, InterruptedException {
        return run(new ReviewOptions(base, false, false, false));
    }

    /**
     * Reviews with every option and prints the report.
     *
     * @param options what to review
     * @return whether the review passed ({@code -Pstrict} counts warnings), and its summary line
     * @throws IOException if a file can't be read
     * @throws InterruptedException if interrupted while asking git
     * @throws ToolFailure (exit 2) when the review can't run
     */
    ReviewOutcome run(ReviewOptions options) throws IOException, InterruptedException {
        ReviewResult result = review.run(options);
        report.print(result);
        String summary = result.nothingToReview() ? review.nothingToReview(options.base().strip()) : Report.summary(result);
        return new ReviewOutcome(!result.failed(options.strict()), summary);
    }
}
