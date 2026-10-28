package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Applies the plugin to an in-memory project and checks what it registers and how the extension drives it. */
class JbakeSitePluginTest {

    private Project project;
    private JbakeSiteExtension site;

    @BeforeEach
    void applyPlugin() {
        project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("jbake-site");
        site = project.getExtensions().getByType(JbakeSiteExtension.class);
    }

    @Test
    void registersTheExtensionWithTheSiteDefaults() {
        assertTrue(project.getPlugins().hasPlugin(JbakeSitePlugin.class));

        assertEquals(projectFile("src/jbake"), site.getSource().get().getAsFile());
        assertEquals(projectFile("build/jbake"), site.getDestination().get().getAsFile());
        assertEquals(projectFile("src/case-redirects"), site.getCaseRedirects().get().getAsFile());
        assertFalse(site.getLogbackConfiguration().isPresent(), "unset: the bundled configuration");
        assertEquals(projectFile("src/scss"), site.getScss().getSource().get().getAsFile());
        assertEquals("css", site.getScss().getPublishedDir().get());
        assertEquals(List.of(), site.getScss().getRetiredCss().get());
        assertEquals("the site's CSS is compiled from the SCSS in src/scss; JBake would publish this copy, and it would go stale.",
                site.getScss().getHandEditedCssExplanation().get(), "a project-relative path: the same task input on every machine");
        assertEquals(JbakeClasspath.DEFAULT_EXCLUDED_GROUPS, site.getClasspath().getExcludedGroups().get());
        assertEquals(JbakeClasspath.DEFAULT_SUBSTITUTIONS, site.getClasspath().getSubstitutions().get());
    }

    @Test
    void registersCompileSassWithoutAGroup() {
        CompileSassTask task = (CompileSassTask) project.getTasks().getByName(JbakeSitePlugin.COMPILE_SASS_TASK_NAME);

        assertNull(task.getGroup(), "an implementation detail of bake, hidden from `gradlew tasks` (T133)");
        assertEquals(projectFile("src/scss"), task.getSource().get().getAsFile());
        assertEquals(projectFile("build/generated/sass/css"), task.getDestinationDirectory().get().getAsFile());
    }

    @Test
    void registersBakeInTheJbakeGroupWithWhatJbakeNeeds() {
        BakeTask bake = (BakeTask) project.getTasks().getByName(JbakeSitePlugin.BAKE_TASK_NAME);

        assertEquals(JbakeSitePlugin.JBAKE_GROUP, bake.getGroup());
        assertEquals("Bakes the site from src/jbake into build/jbake with JBake.", bake.getDescription());
        assertEquals(projectFile("src/jbake"), bake.getSource().get().getAsFile());
        assertEquals(projectFile("build/jbake"), bake.getDestination().get().getAsFile());
        assertEquals(projectFile("src/case-redirects"), bake.getCaseRedirects().get().getAsFile());
        assertEquals(Set.of(projectFile("build/generated/sass/css")), bake.getThemeCss().getFiles(), "compileSass's output");
        assertEquals("css", bake.getThemeCssPublishedDir().get());
        assertEquals(NewJdkJvmArgs.forCurrentJvm(), bake.getJvmArguments().get());
        assertEquals(projectFile("build/tmp/jffi"), bake.getJffiDirectory().get().getAsFile());
        assertFalse(bake.getLogbackConfiguration().isPresent());
        assertEquals(Set.of(JbakeSitePlugin.CHECK_NO_HAND_EDITED_CSS_TASK_NAME, JbakeSitePlugin.COMPILE_SASS_TASK_NAME),
                dependenciesOf(bake), "the guard explicitly, compileSass through the CSS input");
    }

    @Test
    void registersBakePreviewInTheJbakeGroupWithTheScssWatcher() {
        BakePreviewTask preview = (BakePreviewTask) project.getTasks().getByName(JbakeSitePlugin.BAKE_PREVIEW_TASK_NAME);

        assertEquals(JbakeSitePlugin.JBAKE_GROUP, preview.getGroup());
        assertEquals("Bakes, serves http://localhost:8080, rebuilds on save, and reloads the open page (Ctrl+C to stop).",
                preview.getDescription());
        assertEquals(projectFile("src/jbake"), preview.getSource().get().getAsFile());
        assertEquals(projectFile("build/jbake"), preview.getDestination().get().getAsFile());
        assertEquals(Set.of(projectFile("build/generated/sass/css")), preview.getThemeCss().getFiles());
        assertEquals("css", preview.getThemeCssPublishedDir().get());
        assertEquals(NewJdkJvmArgs.forCurrentJvm(), preview.getJvmArguments().get());
        assertEquals(Set.of(JbakeSitePlugin.CHECK_NO_HAND_EDITED_CSS_TASK_NAME, JbakeSitePlugin.COMPILE_SASS_TASK_NAME),
                dependenciesOf(preview));
        assertTrue(preview.getOutputs().getFiles().isEmpty(), "no outputs: the preview runs every time");

        SassPreviewWatcher watcher = preview.getScssWatcher().get();
        assertEquals(projectFile("src/scss"), watcher.getParameters().getSourceDir().get().getAsFile());
        assertEquals(projectFile("build/jbake/css"), watcher.getParameters().getTargetDir().get().getAsFile());
        assertEquals("css", watcher.getParameters().getPublishedDir().get());
        assertEquals("src/scss", watcher.getParameters().getSourceName().get(), "as the log lines name it");
    }

    @Test
    void theExtensionIsReadLazily() {
        site.getSource().set(projectFile("site"));
        site.getDestination().set(projectFile("out"));
        site.getCaseRedirects().set(projectFile("stubs"));
        site.getLogbackConfiguration().set(projectFile("logging.xml"));
        site.scss(scss -> {
            scss.getSource().set(projectFile("styles"));
            scss.getPublishedDir().set("assets/styles");
            scss.getRetiredCss().set(List.of("old.css"));
        });

        CompileSassTask compile = (CompileSassTask) project.getTasks().getByName(JbakeSitePlugin.COMPILE_SASS_TASK_NAME);
        CheckNoHandEditedCssTask guard =
                (CheckNoHandEditedCssTask) project.getTasks().getByName(JbakeSitePlugin.CHECK_NO_HAND_EDITED_CSS_TASK_NAME);
        BakeTask bake = (BakeTask) project.getTasks().getByName(JbakeSitePlugin.BAKE_TASK_NAME);

        assertEquals(projectFile("styles"), compile.getSource().get().getAsFile());
        assertEquals(projectFile("build/generated/sass/assets/styles"), compile.getDestinationDirectory().get().getAsFile());
        assertEquals(projectFile("styles"), guard.getScssSource().get().getAsFile());
        assertEquals(List.of("old.css"), guard.getRetiredCss().get());
        assertTrue(guard.getExplanation().get().contains("in styles;"), guard.getExplanation().get());
        assertEquals(projectFile("site"), bake.getSource().get().getAsFile());
        assertEquals(projectFile("out"), bake.getDestination().get().getAsFile());
        assertEquals(projectFile("stubs"), bake.getCaseRedirects().get().getAsFile());
        assertEquals(projectFile("logging.xml"), bake.getLogbackConfiguration().get().getAsFile());
        assertEquals("assets/styles", bake.getThemeCssPublishedDir().get());

        BakePreviewTask preview = (BakePreviewTask) project.getTasks().getByName(JbakeSitePlugin.BAKE_PREVIEW_TASK_NAME);
        assertEquals(projectFile("out"), preview.getDestination().get().getAsFile());
        assertEquals(projectFile("out/assets/styles"), preview.getScssWatcher().get().getParameters().getTargetDir().get().getAsFile());
        assertEquals("styles", preview.getScssWatcher().get().getParameters().getSourceName().get());
    }

    private Set<String> dependenciesOf(Task task) {
        return task.getTaskDependencies().getDependencies(task).stream().map(Task::getName).collect(Collectors.toSet());
    }

    private File projectFile(String path) {
        return new File(project.getProjectDir(), path);
    }
}
