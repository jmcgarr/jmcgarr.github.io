package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.internal.TestRepositories;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Gradle TestKit: the tasks that moved in from {@code scripts/} in T127, on the fixture site with the configuration
 * cache on: {@code reviewChanges} (clean tree, then a broken page), {@code checkTagRedirects}, {@code tagRedirects}
 * and {@code checkMastheadVariants}. The fixture is copied into a temporary directory and made a git repository with
 * one commit on {@code main}, as the review needs.
 */
class SiteToolsFunctionalTest {

    private static final Path FIXTURE = Path.of(Objects.requireNonNull(System.getProperty("fixtures.dir"),
            "fixtures.dir is set by build-logic/build.gradle; run the tests through Gradle"), "site");

    @TempDir
    Path projectDir;

    @BeforeEach
    void copyTheFixtureSiteIntoARepository() throws IOException, InterruptedException {
        try (Stream<Path> paths = Files.walk(FIXTURE)) {
            for (Path path : paths.toList()) {
                Path target = projectDir.resolve(FIXTURE.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Files.writeString(projectDir.resolve(".gitignore"), "build/\n.gradle/\n");
        TestRepositories.init(projectDir);
        TestRepositories.commitAll(projectDir, "the fixture site");
    }

    @Test
    void reviewChangesFindsNothingOnACleanTreeAndFailsOnABrokenPage() throws IOException {
        BuildResult clean = runner("reviewChanges").build();

        assertEquals(TaskOutcome.SUCCESS, clean.task(":bake").getOutcome(), "the review depends on the bake");
        assertEquals(TaskOutcome.SUCCESS, clean.task(":reviewChanges").getOutcome());
        assertTrue(clean.getOutput().contains("Nothing to review: no changes under src/jbake or src/case-redirects since the merge base with main."),
                clean.getOutput());

        Path about = projectDir.resolve("src/jbake/content/about.md");
        Files.writeString(about, Files.readString(about) + "\nWorked at [Acme] https://acme.example/ for years.\n");
        BuildResult broken = runner("reviewChanges").buildAndFail();

        assertEquals(TaskOutcome.FAILED, broken.task(":reviewChanges").getOutcome());
        String output = broken.getOutput();
        assertTrue(output.contains("ERROR E1 src/jbake/content/about.md:9\n"), output);
        assertTrue(output.contains("ERROR E2 src/jbake/content/about.md:9\n"), "the baked page shows the brackets: " + output);
        assertTrue(output.contains("Reviewed 1 page (about.html) from 1 changed file. 2 errors, 1 warning. External links not checked (T119)."), "W2: the page changed but its date did not. " + output);
        assertTrue(output.contains("The review found errors (listed above). Fix them, or suppress one with <!-- review: ignore E2, why --> on the line before it."), output);

        BuildResult again = runner("reviewChanges").buildAndFail();
        assertTrue(again.getOutput().contains("Reusing configuration cache."), again.getOutput());
        assertEquals(TaskOutcome.FAILED, again.task(":reviewChanges").getOutcome(), "no outputs: the review runs every time");
    }

    @Test
    void checkTagRedirectsPassesOnTheBakeAndTagRedirectsWritesNothingOnACleanTree() throws IOException, InterruptedException {
        BuildResult check = runner("checkTagRedirects").build();

        assertEquals(TaskOutcome.SUCCESS, check.task(":bake").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, check.task(":checkTagRedirects").getOutcome());
        String output = check.getOutput();
        assertTrue(output.contains("2 tag pages checked for the tag name they show: 2 as written in the posts\n"), output);
        assertTrue(output.contains("3 old tag URLs (2 baseline, 1 hyphenated since T039): 2 real tag pages, 1 redirect stubs, 0 not checkable here\n"), output);
        assertTrue(output.contains("OK: every old tag URL in build/jbake is a tag page or a stub whose target is a tag page\n"), output);

        BuildResult write = runner("tagRedirects").build();
        assertEquals(TaskOutcome.SUCCESS, write.task(":tagRedirects").getOutcome());
        assertTrue(write.getOutput().contains("1 tag redirect stubs (0 written, 1 already up to date)\n"), write.getOutput());
        assertEquals("", TestRepositories.git(projectDir, "status", "--porcelain", "--", "src"), "the committed stub regenerates byte for byte");
    }

    @Test
    void checkTagRedirectsFailsWhenAStubIsMissingFromTheSiteItIsPointedAt() throws IOException {
        runner("bake").build();
        Path staged = projectDir.resolve("build/site");
        try (Stream<Path> paths = Files.walk(projectDir.resolve("build/jbake"))) {
            for (Path path : paths.toList()) {
                Path target = staged.resolve(projectDir.resolve("build/jbake").relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target);
                }
            }
        }
        Files.delete(staged.resolve("tags/acceptance test.html"));

        BuildResult result = runner("checkTagRedirects", "-PsiteDir=build/site").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":checkTagRedirects").getOutcome());
        assertTrue(result.getOutput().contains("  ! tags/acceptance test.html: missing from the build\n"), result.getOutput());
        assertTrue(result.getOutput().contains("FAIL: 1 problems in build/site"), result.getOutput());
    }

    @Test
    void checkMastheadVariantsPassesWithTheVariantsAndFailsNamingTheMissingOne() throws IOException {
        Path mastheads = projectDir.resolve("src/jbake/assets/img/masthead");
        Files.createDirectories(mastheads);
        for (String name : new String[] {"hero.jpg", "hero-960.jpg", "hero-1440.jpg"}) {
            ImageIO.write(new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB), "jpeg", mastheads.resolve(name).toFile());
        }
        Path about = projectDir.resolve("src/jbake/content/about.md");
        Files.writeString(about, Files.readString(about).replace("type=page\n", "type=page\nmasthead=hero.jpg\n"));

        BuildResult ok = runner("checkMastheadVariants").build();
        assertEquals(TaskOutcome.SUCCESS, ok.task(":checkMastheadVariants").getOutcome());
        assertTrue(ok.getOutput().contains("OK: all 1 mastheads in use have 960/1440px variants\n"), ok.getOutput());

        Files.delete(mastheads.resolve("hero-960.jpg"));
        BuildResult missing = runner("checkMastheadVariants").buildAndFail();
        assertEquals(TaskOutcome.FAILED, missing.task(":checkMastheadVariants").getOutcome());
        assertTrue(missing.getOutput().contains("FAIL: missing masthead variants (run ./gradlew prepareImage -Pimage=<image> -Pvariants):\n"
                + "    src/jbake/assets/img/masthead/hero-960.jpg"), missing.getOutput());
    }

    @Test
    void checkRunsTheReviewAndBothChecks() {
        BuildResult result = runner("check").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":bake").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":reviewChanges").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkTagRedirects").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkMastheadVariants").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":check").getOutcome());
    }

    private GradleRunner runner(String... arguments) {
        String[] withCache = new String[arguments.length + 1];
        System.arraycopy(arguments, 0, withCache, 0, arguments.length);
        withCache[arguments.length] = "--configuration-cache";
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(withCache);
    }
}
