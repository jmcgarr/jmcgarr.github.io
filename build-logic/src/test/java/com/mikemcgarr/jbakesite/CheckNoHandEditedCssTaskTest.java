package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The guard, as the plugin wires it, run against a project directory on disk. */
class CheckNoHandEditedCssTaskTest {

    private static final String EXPLANATION = "the site's CSS is generated from src/scss now (T043, T134, T135): "
            + "/css/clean-blog.css and /css/code.css are compiled from the entry of the same name, "
            + "and extra.css and fonts.css are part of clean-blog.css.";

    @TempDir
    Path projectDir;

    private Path assetsCss;
    private CheckNoHandEditedCssTask guard;

    @BeforeEach
    void setUpTheSite() throws IOException {
        Path scss = projectDir.resolve("src/scss");
        Files.createDirectories(scss);
        Files.writeString(scss.resolve("clean-blog.scss"), "");
        Files.writeString(scss.resolve("code.scss"), "");
        Files.writeString(scss.resolve("_variables.scss"), "");
        // The real path: Gradle canonicalizes the project directory, and macOS's temp dir is behind a symlink
        assetsCss = projectDir.toRealPath().resolve("src/jbake/assets/css");

        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();
        project.getPluginManager().apply("jbake-site");
        project.getExtensions().getByType(JbakeSiteExtension.class).scss(it -> {
            it.getRetiredCss().set(List.of("clean-blog.min.css", "extra.css", "fonts.css"));
            it.getHandEditedCssExplanation().set(EXPLANATION);
        });
        guard = (CheckNoHandEditedCssTask) project.getTasks().getByName(JbakeSitePlugin.CHECK_NO_HAND_EDITED_CSS_TASK_NAME);
    }

    @Test
    void passesWhenTheAssetsHaveNoCssFolder() {
        assertDoesNotThrow(guard::check);
    }

    @Test
    void passesForCssThatIsNotGenerated() throws IOException {
        Files.createDirectories(assetsCss);
        Files.writeString(assetsCss.resolve("print.css"), "");
        Files.writeString(assetsCss.resolve("clean-blog.css.txt"), "");

        assertDoesNotThrow(guard::check);
    }

    @Test
    void failsForAnEntrysCssWithTheT043Message() throws IOException {
        Files.createDirectories(assetsCss);
        Files.writeString(assetsCss.resolve("clean-blog.css"), "x{}");

        GradleException failure = assertThrows(GradleException.class, guard::check);

        assertEquals(assetsCss.resolve("clean-blog.css") + ": " + EXPLANATION
                + " Move any change into the SCSS, then delete this file.", failure.getMessage());
    }

    @Test
    void failsForARetiredStylesheetAndListsEveryOffender() throws IOException {
        Files.createDirectories(assetsCss);
        Files.writeString(assetsCss.resolve("fonts.css"), "");
        Files.writeString(assetsCss.resolve("code.css"), "");

        GradleException failure = assertThrows(GradleException.class, guard::check);

        assertEquals(assetsCss.resolve("code.css") + ", " + assetsCss.resolve("fonts.css") + ": " + EXPLANATION
                + " Move any change into the SCSS, then delete this file.", failure.getMessage());
    }
}
