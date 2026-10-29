package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.SiteToolTask;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import java.io.IOException;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

/**
 * {@code tagRedirects}: writes any missing or outdated redirect stub for an old tag URL into the source tree, and
 * never deletes one ({@link TagRedirects#write()}; T039, T055, T139; a task since T127). Run only on an
 * owner-approved tag rename or merge (AGENTS.md Rule 1): add the pair to {@code RENAMES}, change the posts, then
 * {@code ./gradlew tagRedirects}. On a clean tree it writes nothing: every committed stub regenerates byte for byte.
 */
@UntrackedTask(because = "it writes redirect stubs into the source tree; there is nothing to be up to date")
public abstract class TagRedirectsTask extends SiteToolTask {

    /**
     * Tags merged or renamed on purpose, old to new as written in the posts ({@code jbakeSite.tagRenames}).
     *
     * @return the renames
     */
    @Input
    public abstract MapProperty<String, String> getTagRenames();

    /**
     * Writes the stubs.
     *
     * @throws IOException if a file can't be read or written
     * @throws InterruptedException never; declared by the tool contract
     */
    @TaskAction
    public void write() throws IOException, InterruptedException {
        SiteLayout site = siteLayout();
        runTool(() -> new TagRedirects(site, siteHost(site), getTagRenames().get(), System.out).write());
    }
}
