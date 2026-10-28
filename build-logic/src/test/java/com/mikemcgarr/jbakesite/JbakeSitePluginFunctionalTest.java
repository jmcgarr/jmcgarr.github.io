package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Gradle TestKit: the plugin applied by id to a small temporary build, run with the configuration cache on, as the
 * site runs it. Covers what {@link CompileSassTask} documents: the CSS, the Sass-error failure, up-to-date checks
 * and the cache, no stale output. The fixture site and the full set of functional tests are T129.
 */
class JbakeSitePluginFunctionalTest {

    @TempDir
    Path projectDir;

    private Path scss;

    @BeforeEach
    void writeTheBuild() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'site'\n");
        Files.writeString(projectDir.resolve("build.gradle"), "plugins { id 'jbake-site' }\n");
        scss = projectDir.resolve("src/scss");
        Files.createDirectories(scss);
        Files.writeString(scss.resolve("_variables.scss"), "$brand: #0085a1;\n");
        Files.writeString(scss.resolve("site.scss"), "@use 'variables';\na { color: variables.$brand; }\n");
    }

    @Test
    void compilesTheEntriesAndRunsTheGuard() throws IOException {
        BuildResult result = runner().build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileSass").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkNoHandEditedCss").getOutcome());
        assertEquals("a {\n  color: #0085a1;\n}", Files.readString(compiledCss("site.css")));
        assertTrue(Files.notExists(compiledCss("_variables.css")));
    }

    @Test
    void aSassErrorFailsTheBuildWithDartSassMessage() throws IOException {
        Files.writeString(scss.resolve("_variables.scss"), "$brand: #0085a1;\n$oops: ;\n");

        BuildResult result = runner().buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":compileSass").getOutcome());
        assertTrue(result.getOutput().contains("Error: Expected expression."), result.getOutput());
        assertTrue(result.getOutput().contains("_variables.scss 2:8"), result.getOutput());
        assertTrue(Files.notExists(compiledCss("site.css")), "nothing is written when an entry fails");
    }

    @Test
    void aSecondRunIsUpToDateAndReusesTheConfigurationCache() {
        runner().build();

        BuildResult second = runner().build();

        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":compileSass").getOutcome());
        assertTrue(second.getOutput().contains("Reusing configuration cache."), second.getOutput());
    }

    @Test
    void aRenamedEntryLeavesNoStaleCss() throws IOException {
        runner().build();
        Files.move(scss.resolve("site.scss"), scss.resolve("theme.scss"));

        BuildResult result = runner().build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileSass").getOutcome());
        assertTrue(Files.exists(compiledCss("theme.css")));
        assertTrue(Files.notExists(compiledCss("site.css")), "the destination holds exactly the entries' CSS");
    }

    private GradleRunner runner() {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("compileSass", "checkNoHandEditedCss", "--configuration-cache");
    }

    private Path compiledCss(String name) {
        return projectDir.resolve("build/generated/sass/css").resolve(name);
    }
}
