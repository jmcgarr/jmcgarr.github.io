package com.mikemcgarr.jbakesite.verification;

import com.mikemcgarr.jbakesite.SiteToolTask;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.writing.MastheadVariants;
import java.io.IOException;
import org.gradle.api.tasks.TaskAction;

/**
 * {@code checkMastheadVariants}: every header image in use has its 960 and 1440 px variants (T071), or phones get a
 * blank header. CI runs it on every build; the review's E5 does the same for the pages a change touched. The check
 * that was {@code masthead-variants.py --check}, then {@code java scripts/PrepareImage.java --check-variants}
 * (T140); in the plugin since T127 (listed under T141 in docs/08-SCRIPTS-MIGRATION.md, moved forward because CI's
 * step went with the program). It reads the content, the templates and the masthead directory, all under
 * {@code jbakeSite.source}.
 *
 * <p>The task has no outputs, so it runs on every build.
 */
public abstract class CheckMastheadVariantsTask extends SiteToolTask {

    /**
     * Checks the mastheads.
     *
     * @throws IOException if a file can't be read
     * @throws InterruptedException never; declared by the tool contract
     */
    @TaskAction
    public void check() throws IOException, InterruptedException {
        SiteLayout site = siteLayout();
        runTool(() -> new MastheadVariants(site, System.out).check());
    }
}
