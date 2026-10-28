package com.mikemcgarr.jbakesite;

import com.mikemcgarr.jbakesite.writing.NewPostTask;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.services.BuildServiceRegistry;
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
 * {@code bake} ({@link BakeTask}); T126 {@code bakePreview} ({@link BakePreviewTask}) with its SCSS watcher
 * ({@link SassPreviewWatcher}); T127 (with the T130 decision) the four single-file programs of {@code scripts/} as
 * task classes, one commit each: so far the {@code writing} group's {@code newPost} ({@link NewPostTask}).
 *
 * <p>The plugin applies Gradle's {@code base} plugin for {@code clean} and the {@code check} lifecycle task.
 */
public final class JbakeSitePlugin implements Plugin<Project> {

    /** The name of the extension, {@code jbakeSite}, as it reads in {@code build.gradle}. */
    public static final String EXTENSION_NAME = "jbakeSite";

    /** The task group of the tasks that generate and preview the site: {@code bake} and {@code bakePreview}. */
    public static final String JBAKE_GROUP = "jbake";

    /** The task that bakes the site. */
    public static final String BAKE_TASK_NAME = "bake";

    /** The task that bakes, serves and rebuilds on save. */
    public static final String BAKE_PREVIEW_TASK_NAME = "bakePreview";

    /** The task group of the author's commands: {@code newPost}, {@code reviewChanges}, {@code prepareImage}, {@code tagRedirects}. */
    public static final String WRITING_GROUP = "writing";

    /** The task that starts a draft post on a new branch. */
    public static final String NEW_POST_TASK_NAME = "newPost";

    /** The task that compiles the SCSS entries. No group: an implementation detail of the bake and the preview. */
    public static final String COMPILE_SASS_TASK_NAME = "compileSass";

    /** The task that fails the build if hand-written CSS is back under the assets. */
    public static final String CHECK_NO_HAND_EDITED_CSS_TASK_NAME = "checkNoHandEditedCss";

    /** The SCSS watcher's name in Gradle's shared build services, qualified with the project path. */
    static final String SCSS_WATCHER_SERVICE_NAME = "sassPreviewWatcher";

    /** Where {@code compileSass} writes, under the build directory; the published directory is appended. */
    private static final String COMPILED_CSS_DIRECTORY = "generated/sass";

    /** Where jffi extracts its native library while JBake runs, under the build directory (T024). */
    private static final String JFFI_DIRECTORY = "tmp/jffi";

    /** JBake's assets folder under its source directory, copied to the site root as it is. */
    private static final String ASSETS_FOLDER = "assets";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(BasePlugin.class);
        JbakeSiteExtension site = project.getExtensions().create(EXTENSION_NAME, JbakeSiteExtension.class);
        ProjectLayout layout = project.getLayout();
        TaskContainer tasks = project.getTasks();
        ProviderFactory providers = project.getProviders();
        setDefaults(site, layout);

        Configuration jbake = JbakeClasspath.create(project.getConfigurations(), site.getClasspath());
        TaskProvider<CompileSassTask> compileSass = registerThemeCss(site, tasks, layout);
        configureJbakeTasks(site, jbake, compileSass, tasks, layout, providers);
        registerBake(site, tasks);
        registerBakePreview(site, tasks, layout, project.getPath(), project.getGradle().getSharedServices());
        configureSiteToolTasks(site, tasks);
        registerWritingTasks(tasks, providers);
    }

    private static void setDefaults(JbakeSiteExtension site, ProjectLayout layout) {
        Directory projectDirectory = layout.getProjectDirectory();
        site.getSource().convention(projectDirectory.dir("src/jbake"));
        site.getDestination().convention(layout.getBuildDirectory().dir("jbake"));
        site.getCaseRedirects().convention(projectDirectory.dir("src/case-redirects"));
        site.getPublishedUrls().convention(projectDirectory.file("docs/baseline/urls.txt"));

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
    private static void configureJbakeTasks(JbakeSiteExtension site, Configuration jbake,
            TaskProvider<CompileSassTask> compileSass, TaskContainer tasks, ProjectLayout layout, ProviderFactory providers) {
        tasks.withType(JbakeTask.class).configureEach(task -> {
            task.getSource().set(site.getSource());
            task.getClasspath().from(jbake);
            task.getJvmArguments().convention(providers.provider(NewJdkJvmArgs::forCurrentJvm));
            task.getLogbackConfiguration().set(site.getLogbackConfiguration());
            task.getJffiDirectory().set(layout.getBuildDirectory().dir(JFFI_DIRECTORY));
            // The theme CSS (T043): checked first (the guard is cheap and its message is clearer than a stale CSS),
            // compiled (the dependency on compileSass comes with its output), and copied into the site by the task
            task.dependsOn(CHECK_NO_HAND_EDITED_CSS_TASK_NAME);
            task.getThemeCss().from(compileSass.flatMap(CompileSassTask::getDestinationDirectory));
            task.getThemeCssPublishedDir().set(site.getScss().getPublishedDir());
        });
    }

    private static void registerBake(JbakeSiteExtension site, TaskContainer tasks) {
        tasks.register(BAKE_TASK_NAME, BakeTask.class, task -> {
            task.setGroup(JBAKE_GROUP);
            task.setDescription("Bakes the site from src/jbake into build/jbake with JBake.");
            task.getDestination().set(site.getDestination());
            task.getCaseRedirects().set(site.getCaseRedirects());
        });
    }

    private static void registerBakePreview(JbakeSiteExtension site, TaskContainer tasks, ProjectLayout layout,
            String projectPath, BuildServiceRegistry services) {
        ScssExtension scss = site.getScss();
        // One watcher per project the plugin is applied to: the service name carries the project path, so two site
        // projects in one build would each get their own, with their own directories
        String serviceName = SCSS_WATCHER_SERVICE_NAME + " (" + projectPath + ")";
        Provider<SassPreviewWatcher> watcher = services.registerIfAbsent(serviceName, SassPreviewWatcher.class, spec -> {
            spec.getParameters().getSourceDir().set(scss.getSource());
            spec.getParameters().getTargetDir().set(site.getDestination().dir(scss.getPublishedDir()));
            spec.getParameters().getPublishedDir().set(scss.getPublishedDir());
            spec.getParameters().getSourceName().set(scss.getSource()
                    .map(source -> ProjectPaths.relativize(layout.getProjectDirectory(), source.getAsFile())));
        });
        tasks.register(BAKE_PREVIEW_TASK_NAME, BakePreviewTask.class, task -> {
            task.setGroup(JBAKE_GROUP);
            task.setDescription("Bakes, serves http://localhost:8080, rebuilds on save, and reloads the open page (Ctrl+C to stop).");
            task.getDestination().set(site.getDestination());
            task.getScssWatcher().set(watcher);
            task.usesService(watcher);
        });
    }

    /** What every writing and verification task shares ({@link SiteToolTask}): where the site's sources are. */
    private static void configureSiteToolTasks(JbakeSiteExtension site, TaskContainer tasks) {
        tasks.withType(SiteToolTask.class).configureEach(task -> {
            task.getSource().set(site.getSource());
            task.getCaseRedirects().set(site.getCaseRedirects());
            task.getPublishedUrls().set(site.getPublishedUrls());
        });
    }

    /**
     * The author's commands. Each {@code -P} option is read as a Gradle property, lazily, so the configuration cache
     * (T114) holds the provider and the task reads the value when it runs.
     */
    private static void registerWritingTasks(TaskContainer tasks, ProviderFactory providers) {
        tasks.register(NEW_POST_TASK_NAME, NewPostTask.class, task -> {
            task.setGroup(WRITING_GROUP);
            task.setDescription("Starts a draft post on a new post/<slug> branch: ./gradlew newPost -Ptitle=\"My Post Title\"");
            task.getTitle().set(providers.gradleProperty("title"));
            task.getTags().set(providers.gradleProperty("tags"));
            task.getSummary().set(providers.gradleProperty("summary"));
            task.getSlug().set(providers.gradleProperty("slug"));
            task.getBranch().set(providers.gradleProperty("branch"));
            task.getPr().set(providers.gradleProperty("pr"));
            task.getFormat().set(providers.gradleProperty("format"));
        });
    }
}
