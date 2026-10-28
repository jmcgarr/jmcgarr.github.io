package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.SiteToolTask;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ProcessRunner;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.review.ReviewRunner;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

/**
 * {@code openPr}: reviews the branch's changes and, if they pass, pushes the branch and opens a ready pull request
 * against {@code main} (T121, docs/05-REVIEW-FLOW.md). Depends on {@code bake}, which the review reads. The
 * {@code -P} options are the task's properties, wired from the Gradle properties of the same names, and
 * {@link OpenPr} does the work:
 *
 * <pre>
 * ./gradlew openPr [-Ptitle="..."] [-Pmessage="..."] [-Pcloses=3,16] [-Prefs=14] [-PdryRun]
 * </pre>
 *
 * <p>Untracked: the task commits, pushes and talks to GitHub, so Gradle has nothing to keep up to date; it runs every
 * time it is asked.
 */
@UntrackedTask(because = "it commits, pushes and opens a pull request; there is nothing to be up to date")
public abstract class OpenPrTask extends SiteToolTask {

    /**
     * The pull request's title ({@code -Ptitle}); default the subject of the branch's first commit.
     *
     * @return the title
     */
    @Input
    @Optional
    public abstract Property<String> getTitle();

    /**
     * The commit message for the uncommitted work ({@code -Pmessage}); without it, the work is listed and the task stops.
     *
     * @return the message
     */
    @Input
    @Optional
    public abstract Property<String> getMessage();

    /**
     * Issue numbers the pull request closes ({@code -Pcloses=3,16}), one {@code Closes #N} line each.
     *
     * @return the numbers
     */
    @Input
    @Optional
    public abstract Property<String> getCloses();

    /**
     * Issue numbers the pull request contributes to ({@code -Prefs=14}), one {@code Refs #N} line each.
     *
     * @return the numbers
     */
    @Input
    @Optional
    public abstract Property<String> getRefs();

    /**
     * {@code -PdryRun}: everything but the commit, the push and the pull request.
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getDryRun();

    /**
     * The trailer lines of a commit the task makes ({@code jbakeSite.commitTrailers}).
     *
     * @return the lines
     */
    @Input
    public abstract ListProperty<String> getCommitTrailers();

    /**
     * The baked site the review reads, {@code jbakeSite.destination}. {@code @Internal}: the task is untracked.
     *
     * @return the directory
     */
    @Internal
    public abstract DirectoryProperty getSite();

    /**
     * Opens the pull request.
     *
     * @throws IOException if a file can't be read or written
     * @throws InterruptedException if interrupted while waiting for git or gh
     */
    @TaskAction
    public void open() throws IOException, InterruptedException {
        SiteLayout site = siteLayout();
        Path bakedSite = getSite().get().getAsFile().toPath();
        ProcessRunner runner = new ProcessRunner(site.root(), System.out, System.err);
        Git git = new Git(runner);
        runTool(() -> {
            OpenPrOptions options = new OpenPrOptions(getTitle().getOrNull(), getMessage().getOrNull(), getCloses().getOrNull(),
                    getRefs().getOrNull(), flag(getDryRun(), "dryRun"));
            // A local command: no GitHub Actions annotations, so the environment isn't read
            ReviewRunner review = new ReviewRunner(site, bakedSite, siteHost(site), git, Clock.systemDefaultZone(), System.out, Map.of());
            new OpenPr(site, git, runner, review::run, getCommitTrailers().get(), System.out).open(options);
        });
    }
}
