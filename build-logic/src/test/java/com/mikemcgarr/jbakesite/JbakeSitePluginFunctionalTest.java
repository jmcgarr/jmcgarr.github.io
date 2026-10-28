package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A smoke test with Gradle TestKit: the plugin applied by id to a small temporary build, run with the configuration
 * cache on, as the site runs it. The fixture site and the full set of functional tests are T129.
 */
class JbakeSitePluginFunctionalTest {

    @TempDir
    Path projectDir;

    @Test
    void compilesTheEntriesAndRunsTheGuard() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'site'\n");
        Files.writeString(projectDir.resolve("build.gradle"), "plugins { id 'jbake-site' }\n");
        Path scss = projectDir.resolve("src/scss");
        Files.createDirectories(scss);
        Files.writeString(scss.resolve("_variables.scss"), "$brand: #0085a1;\n");
        Files.writeString(scss.resolve("site.scss"), "@use 'variables';\na { color: variables.$brand; }\n");

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("compileSass", "checkNoHandEditedCss", "--configuration-cache", "--quiet")
                .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileSass").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkNoHandEditedCss").getOutcome());
        assertEquals("a {\n  color: #0085a1;\n}", Files.readString(projectDir.resolve("build/generated/sass/css/site.css")));
        assertTrue(Files.notExists(projectDir.resolve("build/generated/sass/css/_variables.css")));
    }
}
