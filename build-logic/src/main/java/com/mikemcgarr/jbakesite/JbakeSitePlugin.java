package com.mikemcgarr.jbakesite;

import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * The {@code jbake-site} plugin: how the site is built, as Java classes with tests, applied by the root
 * {@code build.gradle} and configured through the {@link JbakeSiteExtension} ({@code jbakeSite { ... }}).
 *
 * <p>The build logic moves in from {@code build.gradle} one slice at a time (docs/06-GRADLE-PLUGIN.md, M10); this
 * class registers each slice's tasks with the names the site has used since 2014. T123 created it; T124 added the
 * theme CSS: {@code compileSass} ({@link CompileSassTask}) and {@code checkNoHandEditedCss}
 * ({@link CheckNoHandEditedCssTask}); T125 the {@code jbake} configuration ({@link JbakeClasspath}) and
 * {@code bake} ({@link BakeTask}).
 */
public final class JbakeSitePlugin implements Plugin<Project> {

    /** The name of the extension, {@code jbakeSite}, as it reads in {@code build.gradle}. */
    public static final String EXTENSION_NAME = "jbakeSite";

    /** The task group of the tasks that generate and preview the site: {@code bake}, and {@code bakePreview} with T126. */
    public static final String JBAKE_GROUP = "jbake";

    /** The task that bakes the site. */
    public static final String BAKE_TASK_NAME = "bake";

    /** The task that compiles the SCSS entries. No group: an implementation detail of the bake and the preview. */
    public static final String COMPILE_SASS_TASK_NAME = "compileSass";

    /** The task that fails the build if hand-written CSS is back under the assets. */
    public static final String CHECK_NO_HAND_EDITED_CSS_TASK_NAME = "checkNoHandEditedCss";

    /** Where {@code compileSass} writes, under the build directory; the published directory is appended. */
    private static final String COMPILED_CSS_DIRECTORY = "generated/sass";

    /** Where jffi extracts its native library while JBake runs, under the build directory (T024). */
    private static final String JFFI_DIRECTORY = "tmp/jffi";

    /** JBake's assets folder under its source directory, copied to the site root as it is. */
    private static final String ASSETS_FOLDER = "assets";

    @Override
    public void apply(Project project) {
        JbakeSiteExtension site = project.getExtensions().create(EXTENSION_NAME, JbakeSiteExtension.class);
        ProjectLayout layout = project.getLayout();
        TaskContainer tasks = project.getTasks();
        setDefaults(site, layout);

        Configuration jbake = JbakeClasspath.create(project.getConfigurations(), site.getClasspath());
        TaskProvider<CompileSassTask> compileSass = registerThemeCss(site, tasks, layout);
        configureJbakeTasks(site, jbake, tasks, layout, project.getProviders());
        registerBake(site, compileSass, tasks, layout);
    }

    private static void setDefaults(JbakeSiteExtension site, ProjectLayout layout) {
        Directory projectDirectory = layout.getProjectDirectory();
        site.getSource().convention(projectDirectory.dir("src/jbake"));
        site.getDestination().convention(layout.getBuildDirectory().dir("jbake"));
        site.getCaseRedirects().convention(projectDirectory.dir("src/case-redirects"));

        ScssExtension scss = site.getScss();
        scss.getSource().convention(projectDirectory.dir("src/scss"));
        scss.getPublishedDir().convention("css");
        scss.getRetiredCss().convention(List.of());
        scss.getHandEditedCssExplanation().convention(scss.getSource().map(source ->
                "the site's CSS is compiled from the SCSS in " + ProjectPaths.relativize(projectDirectory, source.getAsFile())
                        + "; JBake would publish this copy, and it would go stale."));

        JbakeClasspathExtension classpath = site.getClasspath();
        classpath.getExcludedGroups().convention(JbakeClasspath.DEFAULT_EXCLUDED_GROUPS);
        classpath.getSubstitutions().convention(JbakeClasspath.DEFAULT_SUBSTITUTIONS);
    }

    private static TaskProvider<CompileSassTask> registerThemeCss(JbakeSiteExtension site, TaskContainer tasks, ProjectLayout layout) {
        ScssExtension scss = site.getScss();

        Provider<Directory> compiledCss = layout.getBuildDirectory()
                .dir(scss.getPublishedDir().map(dir -> COMPILED_CSS_DIRECTORY + "/" + dir));
        TaskProvider<CompileSassTask> compileSass = tasks.register(COMPILE_SASS_TASK_NAME, CompileSassTask.class, task -> {
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
        return compileSass;
    }

    /** What every JBake run shares ({@link JbakeTask}): the source, the classpath, the JVM flags, logging, jffi. */
    private static void configureJbakeTasks(JbakeSiteExtension site, Configuration jbake, TaskContainer tasks,
            ProjectLayout layout, ProviderFactory providers) {
        tasks.withType(JbakeTask.class).configureEach(task -> {
            task.getSource().set(site.getSource());
            task.getClasspath().from(jbake);
            task.getJvmArguments().convention(providers.provider(NewJdkJvmArgs::forCurrentJvm));
            task.getLogbackConfiguration().set(site.getLogbackConfiguration());
            task.getJffiDirectory().set(layout.getBuildDirectory().dir(JFFI_DIRECTORY));
            // The theme CSS is checked first (T043): the guard is cheap and its message is clearer than a stale CSS
            task.dependsOn(CHECK_NO_HAND_EDITED_CSS_TASK_NAME);
        });
    }

    private static void registerBake(JbakeSiteExtension site, TaskProvider<CompileSassTask> compileSass,
            TaskContainer tasks, ProjectLayout layout) {
        tasks.register(BAKE_TASK_NAME, BakeTask.class, task -> {
            task.setGroup(JBAKE_GROUP);
            task.setDescription("Bakes the site from " + ProjectPaths.relativize(layout.getProjectDirectory(), site.getSource().get().getAsFile())
                    + " into " + ProjectPaths.relativize(layout.getProjectDirectory(), site.getDestination().get().getAsFile()) + " with JBake.");
            task.getDestination().set(site.getDestination());
            task.getThemeCss().from(compileSass.flatMap(CompileSassTask::getDestinationDirectory));
            task.getThemeCssPublishedDir().set(site.getScss().getPublishedDir());
            task.getCaseRedirects().set(site.getCaseRedirects());
        });
    }
}
