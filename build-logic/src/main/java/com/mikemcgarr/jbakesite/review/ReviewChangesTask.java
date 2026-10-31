package com.mikemcgarr.jbakesite.review;

import com.mikemcgarr.jbakesite.SiteToolTask;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ProcessRunner;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * {@code reviewChanges}: reviews the pages this branch changed, after a bake (T117, docs/05-REVIEW-FLOW.md).
 * {@code check} runs it.
 *
 * <pre>
 *   ./gradlew reviewChanges [-Pbase=main] [-Pall] [-Pstrict]
 * </pre>
 *
 * <p>{@code -Pbase} is the branch to diff against (default main); {@code -Pall} reviews every page; {@code -Pstrict}
 * fails on warnings too. Errors fail the task with one line that says so; a review that couldn't run (no bake, no
 * such base) fails with the reason. In GitHub Actions the findings also become annotations and a step summary
 * ({@link Report}). The task has no outputs, so it runs on every build, and its options are {@code @Internal} like
 * the site: there is nothing an input could keep up to date.
 */
public abstract class ReviewChangesTask extends SiteToolTask {

    /**
     * The branch to diff against ({@code -Pbase}).
     *
     * @return the ref
     */
    @Internal
    public abstract Property<String> getBase();

    /**
     * {@code -Pall}: review every page.
     *
     * @return the value
     */
    @Internal
    public abstract Property<String> getAll();

    /**
     * {@code -Pstrict}: fail on warnings too.
     *
     * @return the value
     */
    @Internal
    public abstract Property<String> getStrict();

    /**
     * {@code -Ponline}: accepted and ignored until T119.
     *
     * @return the value
     */
    @Internal
    public abstract Property<String> getOnline();

    /**
     * The baked site to review, {@code jbakeSite.destination}. {@code @Internal}, not an input: the task has no
     * outputs, so it runs every time anyway, and fingerprinting the 500 baked files would only cost time (the same
     * choice as {@code CheckTagRedirectsTask}).
     *
     * @return the directory
     */
    @Internal
    public abstract DirectoryProperty getSite();

    /**
     * Gradle's providers, for the environment.
     *
     * @return the factory
     */
    @Inject
    protected abstract ProviderFactory getProviders();

    /**
     * Runs the review.
     *
     * @throws IOException if a file can't be read
     * @throws InterruptedException if interrupted while asking git
     */
    @TaskAction
    public void review() throws IOException, InterruptedException {
        SiteLayout site = siteLayout();
        Path bakedSite = getSite().get().getAsFile().toPath();
        Map<String, String> env = new HashMap<>();   // the variables that are set; Report asks with get(name)
        for (String name : new String[] {"GITHUB_ACTIONS", "GITHUB_STEP_SUMMARY"}) {
            String value = getProviders().environmentVariable(name).getOrNull();
            if (value != null) {
                env.put(name, value);
            }
        }
        Git git = new Git(new ProcessRunner(site.root(), System.out, System.err));
        runTool(() -> {
            ReviewOptions options = new ReviewOptions(getBase().getOrElse("main"), flag(getAll(), "all"),
                    flag(getStrict(), "strict"), flag(getOnline(), "online"));
            ReviewRunner review = new ReviewRunner(site, bakedSite, siteHost(site), git, Clock.systemDefaultZone(), System.out, env);
            if (!review.run(options).passed()) {
                throw new GradleException("The review found errors (listed above). Fix them, or suppress one with"
                        + " <!-- review: ignore E2, why --> on the line before it.");
            }
        });
    }
}
