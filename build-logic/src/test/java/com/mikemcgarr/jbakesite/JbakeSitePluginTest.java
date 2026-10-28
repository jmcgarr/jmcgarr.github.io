package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Applies the plugin to an in-memory project and checks what it registers and how the extension drives it. */
class JbakeSitePluginTest {

    private Project project;

    @BeforeEach
    void applyPlugin() {
        project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("jbake-site");
    }

    @Test
    void registersTheExtensionWithTheSiteDefaults() {
        assertTrue(project.getPlugins().hasPlugin(JbakeSitePlugin.class));
        JbakeSiteExtension site = project.getExtensions().getByType(JbakeSiteExtension.class);

        assertEquals(projectFile("src/jbake"), site.getSource().get().getAsFile());
        assertEquals(projectFile("src/scss"), site.getScss().getSource().get().getAsFile());
        assertEquals("css", site.getScss().getPublishedDir().get());
        assertEquals(List.of(), site.getScss().getRetiredCss().get());
        assertTrue(site.getScss().getHandEditedCssExplanation().get().contains(projectFile("src/scss").getPath()));
    }

    @Test
    void registersCompileSassWithoutAGroup() {
        CompileSassTask task = (CompileSassTask) project.getTasks().getByName(JbakeSitePlugin.COMPILE_SASS_TASK_NAME);

        assertNull(task.getGroup(), "an implementation detail of bake, hidden from `gradlew tasks` (T133)");
        assertEquals(projectFile("src/scss"), task.getSource().get().getAsFile());
        assertEquals(projectFile("build/generated/sass/css"), task.getDestinationDirectory().get().getAsFile());
    }

    @Test
    void registersTheHandEditedCssGuard() {
        Task task = project.getTasks().getByName(JbakeSitePlugin.CHECK_NO_HAND_EDITED_CSS_TASK_NAME);

        assertNotNull(task);
        assertTrue(task instanceof CheckNoHandEditedCssTask);
    }

    @Test
    void theExtensionIsReadLazily() {
        JbakeSiteExtension site = project.getExtensions().getByType(JbakeSiteExtension.class);
        site.getSource().set(projectFile("site"));
        site.scss(scss -> {
            scss.getSource().set(projectFile("styles"));
            scss.getPublishedDir().set("assets/styles");
            scss.getRetiredCss().set(List.of("old.css"));
        });

        CompileSassTask compile = (CompileSassTask) project.getTasks().getByName(JbakeSitePlugin.COMPILE_SASS_TASK_NAME);
        CheckNoHandEditedCssTask guard =
                (CheckNoHandEditedCssTask) project.getTasks().getByName(JbakeSitePlugin.CHECK_NO_HAND_EDITED_CSS_TASK_NAME);

        assertEquals(projectFile("styles"), compile.getSource().get().getAsFile());
        assertEquals(projectFile("build/generated/sass/assets/styles"), compile.getDestinationDirectory().get().getAsFile());
        assertEquals(projectFile("styles"), guard.getScssSource().get().getAsFile());
        assertEquals(List.of("old.css"), guard.getRetiredCss().get());
        assertTrue(guard.getExplanation().get().contains(projectFile("styles").getPath()));
    }

    private File projectFile(String path) {
        return new File(project.getProjectDir(), path);
    }
}
