package com.mikemcgarr.jbakesite.verification;

import com.mikemcgarr.jbakesite.SiteToolTask;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import java.io.IOException;
import java.nio.file.Path;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

/**
 * {@code checkUrls}: every URL in the baseline of published URLs ({@code jbakeSite.publishedUrls}) is still a page in
 * the baked site, or is listed in the expected removals ({@code jbakeSite.expectedRemovals}); the Rule 1 gate
 * ({@link UrlCheck}; T006, T141, {@code scripts/check-urls.sh} until T143). CI runs it on {@code build/site}, the
 * staged copy without drafts ({@code -PsiteDir=build/site}); by default it checks the bake it depends on. On macOS
 * the pages that differ from a built page only by case are warned about, not failed, unless {@code -Pstrict} is
 * given; Linux, where CI builds, is strict by nature.
 *
 * <p>The task has no outputs, so it runs on every build.
 */
public abstract class CheckUrlsTask extends SiteToolTask {

    /**
     * The URLs removed on purpose ({@code jbakeSite.expectedRemovals}).
     *
     * @return the file
     */
    @Internal
    public abstract RegularFileProperty getExpectedRemovals();

    /**
     * {@code -Pstrict}: report a case collision as a missing URL even on a case-insensitive filesystem.
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getStrict();

    /**
     * The baked site to check: the bake's destination, or {@code -PsiteDir=<dir>}. {@code @Internal}, not an input:
     * the task has no outputs, so it runs every time anyway, and fingerprinting the 500 baked files would only cost
     * time (the same choice as {@code CheckTagRedirectsTask}).
     *
     * @return the directory
     */
    @Internal
    public abstract DirectoryProperty getSite();

    /**
     * Checks the site.
     *
     * @throws IOException if a file can't be read
     * @throws InterruptedException never; declared by the tool contract
     */
    @TaskAction
    public void check() throws IOException, InterruptedException {
        SiteLayout site = siteLayout();
        Path bakedSite = getSite().get().getAsFile().toPath();
        Path expectedRemovals = getExpectedRemovals().get().getAsFile().toPath();
        runTool(() -> new UrlCheck(site, site.publishedUrls(), expectedRemovals, flag(getStrict(), "strict"), System.out)
                .check(bakedSite));
    }
}
