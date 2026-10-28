package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

/** Applies the plugin to an in-memory project and checks what it registers. */
class JbakeSitePluginTest {

    @Test
    void appliesAndRegistersTheExtension() {
        Project project = ProjectBuilder.builder().build();

        project.getPluginManager().apply("jbake-site");

        assertTrue(project.getPlugins().hasPlugin(JbakeSitePlugin.class));
        assertNotNull(project.getExtensions().findByType(JbakeSiteExtension.class));
        assertNotNull(project.getExtensions().findByName(JbakeSitePlugin.EXTENSION_NAME));
    }
}
