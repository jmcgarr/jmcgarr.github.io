package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.SiteToolTask;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ProcessRunner;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import java.io.IOException;
import java.time.Clock;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

/**
 * {@code newPost}: starts a draft post on a new {@code post/<slug>} branch (T060, T061; in the plugin since T127,
 * before that an {@code Exec} of {@code scripts/NewPost.java} in {@code build.gradle}). The {@code -P} options are
 * the task's properties, wired from the Gradle properties of the same names, and {@link NewPost} does the work:
 *
 * <pre>
 * ./gradlew newPost -Ptitle="My Post Title" [-Ptags="a, b"] [-Psummary="..."] [-Pslug=my-post] [-Pbranch=false] [-Ppr]
 * </pre>
 *
 * <p>Untracked: the task writes into the source tree and switches branches, so Gradle has nothing to keep up to
 * date; it runs every time it is asked.
 */
@UntrackedTask(because = "it creates a draft in the source tree and a git branch; there is nothing to be up to date")
public abstract class NewPostTask extends SiteToolTask {

    /**
     * The post's title ({@code -Ptitle}); required.
     *
     * @return the title
     */
    @Input
    @Optional
    public abstract Property<String> getTitle();

    /**
     * Comma-separated tags ({@code -Ptags}).
     *
     * @return the tags
     */
    @Input
    @Optional
    public abstract Property<String> getTags();

    /**
     * The teaser under the title on the home page ({@code -Psummary}).
     *
     * @return the summary
     */
    @Input
    @Optional
    public abstract Property<String> getSummary();

    /**
     * The file and URL name, instead of one made from the title ({@code -Pslug}).
     *
     * @return the slug
     */
    @Input
    @Optional
    public abstract Property<String> getSlug();

    /**
     * {@code -Pbranch=false} stays on the current branch.
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getBranch();

    /**
     * {@code -Ppr}: also commit, push and open a draft pull request (T061).
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getPr();

    /**
     * {@code -Pformat}: passed on only so that a {@code -Pformat=asciidoc} from habit gets the program's clear
     * refusal (T102).
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getFormat();

    /**
     * Creates the draft.
     *
     * @throws IOException if a file can't be written
     * @throws InterruptedException if interrupted while waiting for git or gh
     */
    @TaskAction
    public void create() throws IOException, InterruptedException {
        SiteLayout site = siteLayout();
        ProcessRunner runner = new ProcessRunner(site.root(), System.out, System.err);
        NewPostOptions options = new NewPostOptions(getTitle().getOrNull(), getTags().getOrNull(), getSummary().getOrNull(),
                getSlug().getOrNull(), getBranch().getOrNull(), getPr().getOrNull(), getFormat().getOrNull());
        runTool(() -> new NewPost(site, siteHost(site), new Git(runner), runner, Clock.systemDefaultZone(), System.out)
                .create(options));
    }
}
