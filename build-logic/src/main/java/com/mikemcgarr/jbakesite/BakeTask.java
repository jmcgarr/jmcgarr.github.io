package com.mikemcgarr.jbakesite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * {@code bake}: renders the site with JBake ({@code jbake <source> <destination> -b}), drafts included as
 * {@code <name>-draft.html} (CI leaves them out of the deploy), then puts two things into the output that JBake
 * must not get to replace: the compiled theme CSS ({@code compileSass}, T043) and, on a case-sensitive filesystem
 * only, the case-redirect stubs ({@link CaseRedirects}, T055). The JBake Gradle plugin's task name, kept since 2014
 * (T096); T125 moved the task here from {@code build.gradle}.
 *
 * <p>The task is up to date when its inputs (the source, the stubs, the compiled CSS, JBake's classpath and JVM
 * flags) and its output directory haven't changed since the last bake, so a second {@code ./gradlew bake} does
 * nothing; and because the preview writes into the same directory, with the local host and the live-reload
 * script, the bake after a preview runs again. It is <em>not</em> cacheable: JBake stamps {@code feed.xml} with
 * the time of the bake, so the output isn't a function of the inputs, and a cached copy would be a stale one.
 */
public abstract class BakeTask extends JbakeTask {

    /**
     * Where the site is written; what CI deploys, minus the drafts.
     *
     * @return the directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getDestination();

    /**
     * The compiled theme CSS, {@code compileSass}'s output, copied into the site after JBake.
     *
     * @return the directory of {@code .css} files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getThemeCss();

    /**
     * Where the theme CSS goes, relative to the site root ({@link ScssExtension#getPublishedDir()}).
     *
     * @return the directory, such as {@code css}
     */
    @Input
    public abstract Property<String> getThemeCssPublishedDir();

    /**
     * The case-redirect stubs, copied into the site after JBake when the filesystem is case-sensitive. A missing
     * or empty directory means none.
     *
     * @return the directory
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getCaseRedirects();

    /**
     * Bakes the site.
     *
     * @throws IOException if a file can't be copied into the site
     */
    @TaskAction
    public void bake() throws IOException {
        Path destination = getDestination().get().getAsFile().toPath();
        runJbake(destination, List.of("-b"), Map.of());

        // After JBake, so nothing JBake copies can replace it
        for (var cssDirectory : getThemeCss().getFiles()) {
            FileTrees.copy(cssDirectory.toPath(), destination.resolve(getThemeCssPublishedDir().get()));
        }

        Path stubs = getCaseRedirects().get().getAsFile().toPath();
        if (CaseRedirects.copy(stubs, destination) == CaseRedirects.Outcome.SKIPPED) {
            getLogger().lifecycle("Case-insensitive filesystem: left out the redirect stubs in {} (they would replace "
                    + "the real pages with the same name in another case). CI adds them.", projectRelative(stubs.toFile()));
        }
    }
}
