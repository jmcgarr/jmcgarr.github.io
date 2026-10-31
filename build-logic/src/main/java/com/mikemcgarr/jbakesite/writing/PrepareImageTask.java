package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.SiteToolTask;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ProcessRunner;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

/**
 * {@code prepareImage}: makes a hand-added image fit the image rules, and a masthead's variants (T140; in the plugin
 * since T127, before that an {@code Exec} of {@code scripts/PrepareImage.java}). The {@code -P} options are the
 * task's properties: a flag is on when given alone or as {@code true}, off as {@code false}, and anything else is a
 * usage error ({@link SiteToolTask#flag}, the rule every task's flags follow); {@code -Pimage} takes a
 * comma-separated list, {@code -Pwidth} a number. {@link PrepareImage} does
 * the work:
 *
 * <pre>
 * ./gradlew prepareImage -Pimage=src/jbake/assets/img/masthead/photo.png[,other.jpg] [-PdryRun] [-Ppng|-Pjpeg] [-Pwidth=1000] [-PkeepSource]
 * ./gradlew prepareImage -Pimage=src/jbake/assets/img/masthead/photo.jpg -Pvariants    the missing 960/1440 variants alone
 * </pre>
 *
 * <p>Untracked: the task rewrites files in the source tree, so Gradle has nothing to keep up to date.
 */
@UntrackedTask(because = "it rewrites images and their references in the source tree; there is nothing to be up to date")
public abstract class PrepareImageTask extends SiteToolTask {

    /**
     * The image(s) to prepare, comma-separated, relative to the project ({@code -Pimage}); required.
     *
     * @return the paths
     */
    @Input
    @Optional
    public abstract Property<String> getImage();

    /**
     * {@code -PdryRun}: say what would happen, change nothing.
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getDryRun();

    /**
     * {@code -Ppng}: keep a PNG as PNG.
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getPng();

    /**
     * {@code -Pjpeg}: turn a PNG into a JPEG even if it looks like a graphic.
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getJpeg();

    /**
     * {@code -Pwidth=N}: cap the width at N px.
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getWidth();

    /**
     * {@code -PkeepSource}: don't remove a new PNG after writing its JPEG.
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getKeepSource();

    /**
     * {@code -Pvariants}: make only the named mastheads' missing 960/1440 variants, never overwriting one.
     *
     * @return the value
     */
    @Input
    @Optional
    public abstract Property<String> getVariants();

    /**
     * Prepares the images.
     *
     * @throws IOException if a file can't be read or written
     * @throws InterruptedException if interrupted while asking git
     */
    @TaskAction
    public void prepare() throws IOException, InterruptedException {
        System.setProperty("java.awt.headless", "true");
        ImageIO.setUseCache(false);
        SiteLayout site = siteLayout();
        List<String> images = images();
        Git git = new Git(new ProcessRunner(site.root(), System.out, System.err));
        runTool(() -> {
            PrepareImageOptions options = new PrepareImageOptions(flag(getDryRun(), "dryRun"), flag(getPng(), "png"),
                    flag(getJpeg(), "jpeg"), flag(getKeepSource(), "keepSource"), width());
            PrepareImage tool = new PrepareImage(site, git, System.out);
            if (flag(getVariants(), "variants")) {
                tool.makeMissingVariants(images, options);
            } else {
                tool.prepare(images, options);
            }
        });
    }

    private List<String> images() {
        List<String> images = new ArrayList<>();
        for (String path : getImage().getOrElse("").split(",")) {
            if (!path.strip().isEmpty()) {
                images.add(path.strip());
            }
        }
        return images;
    }

    private Integer width() {
        if (!getWidth().isPresent()) {
            return null;
        }
        try {
            return Integer.parseInt(getWidth().get().strip());
        } catch (NumberFormatException e) {
            throw ToolFailure.failed("-Pwidth needs a number, not '" + getWidth().get() + "'");
        }
    }
}
