package com.mikemcgarr.jbakesite;

import java.nio.file.Path;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;

/**
 * The {@code jbake-site} plugin: how the site is built, as Java classes with tests, applied by the root
 * {@code build.gradle} and configured through the {@link JbakeSiteExtension} ({@code jbakeSite { ... }}).
 *
 * <p>The build logic moves in from {@code build.gradle} one slice at a time (docs/06-GRADLE-PLUGIN.md, M10); this
 * class registers each slice's tasks with the names the site has used since 2014. T123 created it; T124 added the
 * theme CSS: {@code compileSass} ({@link CompileSassTask}) and {@code checkNoHandEditedCss}
 * ({@link CheckNoHandEditedCssTask}).
 */
public final class JbakeSitePlugin implements Plugin<Project> {

    /** The name of the extension, {@code jbakeSite}, as it reads in {@code build.gradle}. */
    public static final String EXTENSION_NAME = "jbakeSite";

    /** The task that compiles the SCSS entries. No group: an implementation detail of the bake and the preview. */
    public static final String COMPILE_SASS_TASK_NAME = "compileSass";

    /** The task that fails the build if hand-written CSS is back under the assets. */
    public static final String CHECK_NO_HAND_EDITED_CSS_TASK_NAME = "checkNoHandEditedCss";

    /** Where {@code compileSass} writes, under the build directory; the published directory is appended. */
    private static final String COMPILED_CSS_DIRECTORY = "generated/sass";

    /** JBake's assets folder under its source directory, copied to the site root as it is. */
    private static final String ASSETS_FOLDER = "assets";

    @Override
    public void apply(Project project) {
        JbakeSiteExtension site = project.getExtensions().create(EXTENSION_NAME, JbakeSiteExtension.class);
        setDefaults(site, project.getLayout());
        registerThemeCss(site, project.getTasks(), project.getLayout());
    }

    private static void setDefaults(JbakeSiteExtension site, ProjectLayout layout) {
        Directory projectDirectory = layout.getProjectDirectory();
        site.getSource().convention(projectDirectory.dir("src/jbake"));

        ScssExtension scss = site.getScss();
        scss.getSource().convention(projectDirectory.dir("src/scss"));
        scss.getPublishedDir().convention("css");
        scss.getRetiredCss().convention(List.of());
        scss.getHandEditedCssExplanation().convention(scss.getSource().map(source ->
                "the site's CSS is compiled from the SCSS in " + relativeToProject(projectDirectory, source)
                        + "; JBake would publish this copy, and it would go stale."));
    }

    /**
     * A directory as a project-relative path when it is under the project, so that a task input built from it is
     * the same on every machine; the absolute path otherwise.
     */
    private static String relativeToProject(Directory projectDirectory, Directory directory) {
        Path project = projectDirectory.getAsFile().toPath();
        Path path = directory.getAsFile().toPath();
        return path.startsWith(project) ? project.relativize(path).toString() : path.toString();
    }

    private static void registerThemeCss(JbakeSiteExtension site, TaskContainer tasks, ProjectLayout layout) {
        ScssExtension scss = site.getScss();

        Provider<Directory> compiledCss = layout.getBuildDirectory()
                .dir(scss.getPublishedDir().map(dir -> COMPILED_CSS_DIRECTORY + "/" + dir));
        tasks.register(COMPILE_SASS_TASK_NAME, CompileSassTask.class, task -> {
            task.setDescription("Compiles the SCSS entries into CSS with Dart Sass (run by bake and bakePreview).");
            task.getSource().set(scss.getSource());
            task.getDestinationDirectory().set(compiledCss);
        });

        Provider<Directory> assetsCss = site.getSource()
                .dir(scss.getPublishedDir().map(dir -> ASSETS_FOLDER + "/" + dir));
        tasks.register(CHECK_NO_HAND_EDITED_CSS_TASK_NAME, CheckNoHandEditedCssTask.class, task -> {
            task.setDescription("Fails if a hand-written copy of the generated CSS is back under the assets (T043).");
            task.getScssSource().set(scss.getSource());
            task.getAssetsCss().from(assetsCss.map(dir -> dir.getAsFileTree().matching(tree -> tree.include("*.css"))));
            task.getRetiredCss().set(scss.getRetiredCss());
            task.getExplanation().set(scss.getHandEditedCssExplanation());
        });
    }
}
