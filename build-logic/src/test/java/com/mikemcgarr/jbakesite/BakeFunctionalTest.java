package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Gradle TestKit: {@code bake} end to end on the fixture site ({@code src/test/fixtures/site}: one post, one page,
 * one SCSS entry with a partial, one case-redirect stub, two tag pages and a tag redirect stub), with the configuration
 * cache on, as the site runs it.
 * JBake really runs, in a fresh JVM on the {@code jbake} configuration the fixture's {@code build.gradle} pins, so
 * the first run downloads JBake's libraries into TestKit's own Gradle home. Covers what {@link BakeTask} and
 * {@link JbakeClasspath} document (T125); T129 owns the rest of the functional tests.
 */
class BakeFunctionalTest {

    private static final Path FIXTURE = Path.of(Objects.requireNonNull(System.getProperty("fixtures.dir"),
            "fixtures.dir is set by build-logic/build.gradle; run the tests through Gradle"), "site");

    @TempDir
    Path projectDir;

    @BeforeEach
    void copyTheFixtureSite() throws IOException {
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
    }

    @Test
    void bakesThePagesCopiesTheCssInAndAddsTheStubsWhereItCan() throws IOException {
        BuildResult result = runner("bake").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileSass").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkNoHandEditedCss").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":bake").getOutcome());
        assertTrue(result.getOutput().contains("Baked 5 items"), "index, two pages, two tag pages: " + result.getOutput());
        assertTrue(Files.readString(site("index.html")).contains("<a href=\"blog/first-post.html\">First Post</a>"));
        assertTrue(Files.readString(site("blog/first-post.html")).contains("<p>The first post.</p>"));
        assertTrue(Files.readString(site("about.html")).contains("<h1>About</h1>"), "the real page, never the stub");
        assertEquals("User-agent: *\nAllow: /\n", Files.readString(site("robots.txt")), "JBake's assets");
        assertEquals("a {\n  color: #0085a1;\n}", Files.readString(site("css/site.css")), "compileSass's CSS, copied in after JBake");

        if (CaseRedirects.isCaseSensitive(site(""))) {
            assertTrue(Files.readString(site("About.html")).contains("http-equiv=\"refresh\""), "the stub, beside the real page");
            assertFalse(result.getOutput().contains("Case-insensitive filesystem"));
        } else {
            assertTrue(result.getOutput().contains("Case-insensitive filesystem: left out the redirect stubs in src/case-redirects"
                    + " (they would replace the real pages with the same name in another case). CI adds them."), result.getOutput());
        }
    }

    @Test
    void aSecondBakeIsUpToDateAndReusesTheConfigurationCache() {
        runner("bake").build();

        BuildResult second = runner("bake").build();

        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":bake").getOutcome(), "nothing changed, so JBake isn't run again");
        assertTrue(second.getOutput().contains("Reusing configuration cache."), second.getOutput());
    }

    @Test
    void aChangedPostIsBakedAgain() throws IOException {
        runner("bake").build();
        Path post = projectDir.resolve("src/jbake/content/blog/first-post.md");
        Files.writeString(post, Files.readString(post).replace("The first post.", "The first post, edited."));

        BuildResult result = runner("bake").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":bake").getOutcome());
        assertTrue(Files.readString(site("blog/first-post.html")).contains("<p>The first post, edited.</p>"));
    }

    @Test
    void aJbakeErrorFailsTheBakeWithJbakesMessage() throws IOException {
        Path template = projectDir.resolve("src/jbake/templates/page.ftl");
        Files.writeString(template, Files.readString(template).replace("${content.title}", "${content.title"));

        BuildResult result = runner("bake").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":bake").getOutcome());
        assertTrue(result.getOutput().contains("ERROR"), "JBake's own message, from logback's console appender");
        assertTrue(result.getOutput().contains("page.ftl"), result.getOutput());
        assertTrue(result.getOutput().contains("finished with non-zero exit value"), "the task fails with the process's exit value");
    }

    @Test
    void theJbakeConfigurationLeavesOutTheUnusedEnginesAndReplacesLz4() {
        BuildResult result = runner("dependencies", "--configuration", "jbake").build();

        String report = result.getOutput();
        assertTrue(report.contains("org.jbake:jbake-core:2.7.0"), report);
        assertTrue(report.contains("org.freemarker:freemarker:2.3.35"), report);
        assertTrue(report.contains("org.lz4:lz4-java:1.4.0 -> at.yawk.lz4:lz4-java:1.12.0"), "the substitution (T112)");
        for (String group : JbakeClasspath.DEFAULT_EXCLUDED_GROUPS) {
            assertFalse(report.contains(group + ":"), "excluded: " + group);
        }
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

    private Path site(String path) {
        return projectDir.resolve("build/jbake").resolve(path);
    }
}
