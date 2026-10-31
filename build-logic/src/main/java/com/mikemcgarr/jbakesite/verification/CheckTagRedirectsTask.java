package com.mikemcgarr.jbakesite.verification;

import com.mikemcgarr.jbakesite.SiteToolTask;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.writing.TagRedirects;
import java.io.IOException;
import java.nio.file.Path;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * {@code checkTagRedirects}: every old tag URL in the baked site is a tag page or a redirect stub whose target is
 * a real tag page with the right canonical, and every tag page shows its tag as the posts write it
 * ({@link TagRedirects#check}; T039, T055, T057). CI runs it on {@code build/site}, the staged copy without
 * drafts ({@code -PsiteDir=build/site}); by default it checks the bake it depends on. On macOS the two case-only
 * stubs are "not checkable here" and the task still passes; Linux, where CI builds, checks them too.
 *
 * <p>The task has no outputs, so it runs on every build.
 */
public abstract class CheckTagRedirectsTask extends SiteToolTask {

    /**
     * Tags merged or renamed on purpose, old to new as written in the posts ({@code jbakeSite.tagRenames}).
     *
     * @return the renames
     */
    @Input
    public abstract MapProperty<String, String> getTagRenames();

    /**
     * The baked site to check: the bake's destination, or {@code -PsiteDir=<dir>}. {@code @Internal}, not an input:
     * the task has no outputs, so it runs every time anyway, and fingerprinting the 500 baked files would only cost
     * time (the same choice as {@code ReviewChangesTask}).
     *
     * @return the directory
     */
    @Internal
    public abstract DirectoryProperty getSite();

    /**
     * Checks the site.
     *
     * @throws IOException if a page can't be read
     * @throws InterruptedException never; declared by the tool contract
     */
    @TaskAction
    public void check() throws IOException, InterruptedException {
        SiteLayout site = siteLayout();
        Path bakedSite = getSite().get().getAsFile().toPath();
        runTool(() -> new TagRedirects(site, siteHost(site), getTagRenames().get(), System.out).check(bakedSite));
    }
}
