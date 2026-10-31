package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Gradle TestKit: the writing and verification tasks on the fixture site with the configuration cache on:
 * {@code newPost} and {@code prepareImage} (the {@code -P} options passed through, a refusal mapped to a build
 * failure, a misspelled flag refused), {@code reviewChanges} (clean tree, then a broken page), {@code tagRedirects}
 * (nothing on a clean tree, a stub on a rename) with {@code checkTagRedirects}, {@code checkMastheadVariants},
 * {@code checkUrls}, {@code checkDocsNotPublished} and the {@code checkSite} aggregate, and {@code openPr} as far as
 * it goes without GitHub. The fixture is copied into a temporary directory and made a git repository with one
 * commit on {@code main}, as the review and {@code newPost} need.
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
    void newPostPassesItsOptionsThroughAndWritesTheDraft() throws IOException {
        Files.createDirectories(projectDir.resolve("src/jbake/content/blog/drafts"));

        BuildResult result = runner("newPost", "-Ptitle=A Third Post", "-Ptags=fixture, testing", "-Psummary=A teaser.",
                "-Pbranch=false").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":newPost").getOutcome());
        Path draft = projectDir.resolve("src/jbake/content/blog/drafts/a-third-post.md");
        String header = Files.readString(draft);
        // The date is today's by the task's own clock; a regex, so a run that straddles midnight can't fail on it
        assertTrue(header.matches("(?s)title=A Third Post\ndate=\\d{4}-\\d{2}-\\d{2}\ntype=post\ntags=fixture, testing\n"
                + "status=draft\nsummary=A teaser.\n~~~~~~\n\n"), "every -P option in the header: " + header);
        String output = result.getOutput();
        assertTrue(output.contains("Created src/jbake/content/blog/drafts/a-third-post.md (status=draft, never published)"), output);
        assertFalse(output.contains("Switched to the new branch"), "-Pbranch=false stays on main: " + output);
        assertTrue(output.contains("It will live at https://fixture.example/blog/a-third-post.html (permanent)."),
                "site.host from the fixture's jbake.properties: " + output);
    }

    @Test
    void newPostWithoutATitleFailsWithTheToolsMessageAndNoStackTrace() throws IOException {
        Path drafts = projectDir.resolve("src/jbake/content/blog/drafts");
        Files.createDirectories(drafts);

        BuildResult result = runner("newPost").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":newPost").getOutcome(), "exit 1 of the program is a failed task");
        String output = result.getOutput();
        assertTrue(output.contains("> A title is required: ./gradlew newPost -Ptitle=\"My Post Title\""), output);
        assertFalse(output.contains("ToolFailure"), "the message, not the exception: " + output);
        try (Stream<Path> files = Files.list(drafts)) {
            assertEquals(0, files.count(), "nothing written");
        }
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
    void tagRedirectsWritesAStubForARenamedTagAndCheckTagRedirectsAcceptsIt() throws IOException {
        // The owner renames the tag "fixture" to "fixtures" in every post and lists the rename, as the README says to
        for (String post : new String[] {"first-post.md", "second-post.md"}) {
            Path file = projectDir.resolve("src/jbake/content/blog").resolve(post);
            Files.writeString(file, Files.readString(file).replace("tags=fixture", "tags=fixtures"));
        }
        Path buildFile = projectDir.resolve("build.gradle");
        Files.writeString(buildFile, Files.readString(buildFile).replace("tagRenames = [:]", "tagRenames = ['fixture': 'fixtures']"));

        BuildResult write = runner("tagRedirects").build();

        assertEquals(TaskOutcome.SUCCESS, write.task(":tagRedirects").getOutcome());
        assertTrue(write.getOutput().contains("wrote src/jbake/assets/tags/fixture.html -> tags/fixtures.html\n"
                + "2 tag redirect stubs (1 written, 1 already up to date)\n"), write.getOutput());
        String stub = Files.readString(projectDir.resolve("src/jbake/assets/tags/fixture.html"));
        assertTrue(stub.contains("<meta http-equiv=\"refresh\" content=\"0; url=fixtures.html\">"), stub);
        assertTrue(stub.contains("<link rel=\"canonical\" href=\"https://fixture.example/tags/fixtures.html\">"),
                "the canonical is built from the fixture's site.host: " + stub);

        BuildResult check = runner("checkTagRedirects").build();

        assertEquals(TaskOutcome.SUCCESS, check.task(":checkTagRedirects").getOutcome());
        assertTrue(check.getOutput().contains("3 old tag URLs (2 baseline, 1 hyphenated since T039): 1 real tag pages, 2 redirect stubs, 0 not checkable here\n"),
                "the old fixture.html is a stub now, and its target is the real fixtures.html: " + check.getOutput());
    }

    @Test
    void checkTagRedirectsFailsWhenAStubIsMissingFromTheSiteItIsPointedAt() throws IOException {
        Path staged = stageTheBake();
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
    void prepareImagePassesItsOptionsThroughToTheTool() throws IOException {
        Path png = writeGraphic(projectDir.resolve("src/jbake/assets/img/diagram.png"), "png");
        byte[] before = Files.readAllBytes(png);

        BuildResult dryRun = runner("prepareImage", "-Pimage=src/jbake/assets/img/diagram.png", "-PdryRun", "-Pwidth=800", "-Ppng").build();

        assertEquals(TaskOutcome.SUCCESS, dryRun.task(":prepareImage").getOutcome());
        String output = dryRun.getOutput();
        assertTrue(output.contains("src/jbake/assets/img/diagram.png: 16x8 "), "-Pimage: " + output);
        assertTrue(output.contains("; width cap 800\n"), "-Pwidth: " + output);
        assertTrue(output.contains("  a graphic (few colours or transparency): stays PNG\n  would resize losslessly and optimize in place\n"),
                "-Ppng and -PdryRun: " + output);
        assertTrue(output.contains("Next: ./gradlew reviewChanges, then check the page in ./gradlew bakePreview"), output);
        assertArrayEquals(before, Files.readAllBytes(png), "a dry run changes nothing");

        BuildResult jpeg = runner("prepareImage", "-Pimage=src/jbake/assets/img/diagram.png", "-Pjpeg", "-PkeepSource").build();

        assertEquals(TaskOutcome.SUCCESS, jpeg.task(":prepareImage").getOutcome());
        output = jpeg.getOutput();
        assertTrue(output.contains("  a photo: writing src/jbake/assets/img/diagram.jpg (the PNG was never published, so it goes)\n"
                + "  wrote src/jbake/assets/img/diagram.jpg: "), "-Pjpeg forces the JPEG: " + output);
        assertTrue(output.contains("  kept src/jbake/assets/img/diagram.png (-PkeepSource); it will be published as-is unless you remove it\n"),
                "-PkeepSource: " + output);
        assertTrue(Files.isRegularFile(projectDir.resolve("src/jbake/assets/img/diagram.jpg")));
        assertTrue(Files.isRegularFile(png), "kept");
    }

    @Test
    void prepareImageVariantsMakesTheMissingVariantsAndNeverOverwritesOne() throws IOException {
        writeGraphic(projectDir.resolve("src/jbake/assets/img/masthead/hero.jpg"), "jpeg");

        BuildResult made = runner("prepareImage", "-Pvariants", "-Pimage=src/jbake/assets/img/masthead/hero.jpg").build();

        assertEquals(TaskOutcome.SUCCESS, made.task(":prepareImage").getOutcome());
        assertTrue(made.getOutput().contains("created src/jbake/assets/img/masthead/hero-960.jpg  0K  copied (source is narrower)\n"
                + "created src/jbake/assets/img/masthead/hero-1440.jpg  0K  copied (source is narrower)\n"), made.getOutput());

        BuildResult again = runner("prepareImage", "-Pvariants", "-Pimage=src/jbake/assets/img/masthead/hero.jpg").build();

        assertTrue(again.getOutput().contains("src/jbake/assets/img/masthead/hero.jpg: already has its 960/1440 variants (delete one to regenerate it)\n"),
                again.getOutput());
    }

    @Test
    void prepareImageRejectsAMisspelledFlagInsteadOfReadingItAsOn() throws IOException {
        Path png = writeGraphic(projectDir.resolve("src/jbake/assets/img/diagram.png"), "png");
        byte[] before = Files.readAllBytes(png);

        BuildResult result = runner("prepareImage", "-Pimage=src/jbake/assets/img/diagram.png", "-Pjpeg=flase").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":prepareImage").getOutcome());
        assertTrue(result.getOutput().contains("> -Pjpeg takes no value (or true/false), not 'flase'."), result.getOutput());
        assertFalse(result.getOutput().contains("ToolFailure"), "the message, not the exception: " + result.getOutput());
        assertArrayEquals(before, Files.readAllBytes(png), "nothing was converted");
        assertFalse(Files.exists(projectDir.resolve("src/jbake/assets/img/diagram.jpg")));
    }

    @Test
    void checkUrlsPassesOnTheBakeAndFailsNamingAMissingPageUnlessItIsAnExpectedRemoval() throws IOException {
        BuildResult ok = runner("checkUrls").build();

        assertEquals(TaskOutcome.SUCCESS, ok.task(":bake").getOutcome(), "checks the bake it depends on");
        assertEquals(TaskOutcome.SUCCESS, ok.task(":checkUrls").getOutcome());
        assertTrue(ok.getOutput().contains("OK: all 6 baseline URLs accounted for in build/jbake\n"), ok.getOutput());

        Path staged = stageTheBake();
        Files.delete(staged.resolve("about.html"));
        BuildResult missing = runner("checkUrls", "-PsiteDir=build/site").buildAndFail();

        assertEquals(TaskOutcome.FAILED, missing.task(":checkUrls").getOutcome());
        assertTrue(missing.getOutput().contains("FAIL: published URLs missing from the build (1):\n  ! about.html\n"), missing.getOutput());
        assertTrue(missing.getOutput().contains("> Restore them, add redirect stubs, or (with approval) list them in docs/baseline/expected-removals.txt."),
                missing.getOutput());

        Files.writeString(projectDir.resolve("docs/baseline/expected-removals.txt"), "# approved by the owner\nabout.html\n");
        BuildResult allowed = runner("checkUrls", "-PsiteDir=build/site").build();

        assertEquals(TaskOutcome.SUCCESS, allowed.task(":checkUrls").getOutcome());
        // The hyphenated tag page is new since T039 everywhere. On a case-sensitive filesystem (Linux, CI) the bake
        // also publishes the case-redirect stub About.html, which the baseline doesn't list, so it is new there too.
        String newUrls = CaseRedirects.isCaseSensitive(projectDir)
                ? "New URLs (2):\n  + About.html\n  + tags/acceptance-test.html\n"
                : "New URLs (1):\n  + tags/acceptance-test.html\n";
        assertTrue(allowed.getOutput().contains("Expected removals (1):\n  - about.html\n" + newUrls
                + "OK: all 6 baseline URLs accounted for in build/site\n"), allowed.getOutput());
    }

    @Test
    void checkDocsNotPublishedPassesOnTheBakeAndFailsOnALeakedDoc() throws IOException {
        BuildResult ok = runner("checkDocsNotPublished").build();

        assertEquals(TaskOutcome.SUCCESS, ok.task(":checkDocsNotPublished").getOutcome());
        assertTrue(ok.getOutput().contains("OK: no docs/ content in build/jbake\n"), ok.getOutput());

        Path staged = stageTheBake();
        Files.createDirectories(staged.resolve("docs"));
        Files.writeString(staged.resolve("docs/x.md"), "# REVIVAL plan\n");
        BuildResult leaked = runner("checkDocsNotPublished", "-PsiteDir=build/site").buildAndFail();

        assertEquals(TaskOutcome.FAILED, leaked.task(":checkDocsNotPublished").getOutcome());
        String output = leaked.getOutput();
        assertTrue(output.contains("FAIL: 'build/site/docs' exists.\nFAIL: Markdown files found in the site:\n  build/site/docs/x.md\n"
                + "FAIL: planning-doc marker 'REVIVAL' found in:\n  build/site/docs/x.md\n"), output);
        assertTrue(output.contains("> docs/ content must never be published. See AGENTS.md (Rule 4)."), output);
    }

    @Test
    void checkSiteRunsTheFourChecksOfTheBakedSite() {
        BuildResult result = runner("checkSite").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":bake").getOutcome());
        for (String task : new String[] {":checkUrls", ":checkDocsNotPublished", ":checkTagRedirects", ":checkMastheadVariants", ":checkSite"}) {
            assertEquals(TaskOutcome.SUCCESS, result.task(task).getOutcome(), task);
        }
        assertNull(result.task(":reviewChanges"), "the review is check's, not checkSite's");
    }

    @Test
    void checkRunsTheReviewAndEveryCheck() {
        BuildResult result = runner("check").build();

        for (String task : new String[] {":bake", ":reviewChanges", ":checkUrls", ":checkDocsNotPublished", ":checkTagRedirects",
                ":checkMastheadVariants", ":checkSite", ":check"}) {
            assertEquals(TaskOutcome.SUCCESS, result.task(task).getOutcome(), task);
        }
    }

    @Test
    void openPrRefusesOnMainNamingRuleTwo() {
        BuildResult result = runner("openPr").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":openPr").getOutcome());
        assertTrue(result.getOutput().contains("> You're on main: every change goes on its own branch off main, never on main itself"
                + " (AGENTS.md, Rule 2). Run git switch -c <type>/<short-description>, then ./gradlew openPr."), result.getOutput());
        assertFalse(result.getOutput().contains("ToolFailure"), result.getOutput());
    }

    @Test
    void openPrListsTheUncommittedWorkAndStopsWithoutAMessage() throws IOException, InterruptedException {
        TestRepositories.git(projectDir, "switch", "--quiet", "--create", "post/edit");
        Path about = projectDir.resolve("src/jbake/content/about.md");
        Files.writeString(about, Files.readString(about) + "\nAn edit.\n");
        Files.writeString(projectDir.resolve("notes.txt"), "scratch\n");

        BuildResult result = runner("openPr").buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":openPr").getOutcome());
        String output = result.getOutput();
        assertTrue(output.contains("Would commit:\n   M src/jbake/content/about.md\n"
                + "Not included (outside the paths openPr commits; left as they are):\n  ?? notes.txt\n"), output);
        assertTrue(output.contains("> Nothing was committed or pushed. Re-run with -Pmessage=\"<what and why>\" to commit the files listed above"
                + " with that message, or commit them yourself first."), output);
        assertEquals("M src/jbake/content/about.md\n?? notes.txt", TestRepositories.git(projectDir, "status", "--porcelain"),
                "nothing committed (the helper trims git's output, hence the missing leading space)");
        assertEquals("the fixture site", TestRepositories.git(projectDir, "log", "-1", "--format=%s"));
    }

    /** A 16x8 two-colour graphic (so {@code prepareImage} keeps a PNG as PNG unless told otherwise), written where asked. */
    private static Path writeGraphic(Path target, String format) throws IOException {
        Files.createDirectories(target.getParent());
        BufferedImage image = new BufferedImage(16, 8, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 8; y++) {
                image.setRGB(x, y, x < 8 ? 0xFFFFFF : 0x0000FF);
            }
        }
        ImageIO.write(image, format, target.toFile());
        return target;
    }

    /** Bakes and copies the bake to {@code build/site}, as CI stages it, so a test can break the copy. */
    private Path stageTheBake() throws IOException {
        runner("bake").build();
        Path bake = projectDir.resolve("build/jbake");
        Path staged = projectDir.resolve("build/site");
        try (Stream<Path> paths = Files.walk(bake)) {
            for (Path path : paths.toList()) {
                Path target = staged.resolve(bake.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target);
                }
            }
        }
        return staged;
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
