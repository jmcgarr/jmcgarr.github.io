package com.mikemcgarr.jbakesite.verification;

import com.mikemcgarr.jbakesite.SiteToolTask;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import java.io.IOException;
import java.nio.file.Path;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * {@code checkDocsNotPublished}: nothing from {@code docs/} (planning and internal docs) is in the baked site
 * ({@link DocsNotPublishedCheck}; AGENTS.md Rule 4; T004, T141, {@code scripts/check-docs-not-published.sh} until
 * T143). CI runs it on {@code build/site}, the staged copy without drafts ({@code -PsiteDir=build/site}); by default
 * it checks the bake it depends on.
 *
 * <p>The task has no outputs, so it runs on every build.
 */
public abstract class CheckDocsNotPublishedTask extends SiteToolTask {

    /**
     * The baked site to check: the bake's destination, or {@code -PsiteDir=<dir>}. {@code @Internal}, not an input:
     * the task has no outputs, so it runs every time anyway (the same choice as {@code CheckTagRedirectsTask}).
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
        runTool(() -> new DocsNotPublishedCheck(site, System.out).check(bakedSite));
    }
}
