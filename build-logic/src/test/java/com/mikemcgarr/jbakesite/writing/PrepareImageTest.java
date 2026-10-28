package com.mikemcgarr.jbakesite.writing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.internal.FakeRunner;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ImageRules;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** prepareImage end to end on generated images, with git answered by a script (published or not). */
class PrepareImageTest {

    private static final PrepareImageOptions DEFAULTS = PrepareImageOptions.DEFAULTS;

    @TempDir
    Path root;

    private SiteLayout site;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final FakeRunner git = new FakeRunner();
    private PrepareImage tool;

    @BeforeEach
    void aSiteWithAPostNamingAPngMasthead() throws IOException {
        site = SiteLayout.standard(root);
        Files.createDirectories(site.blog());
        Files.createDirectories(site.templates());
        Files.createDirectories(site.mastheads());
        Files.writeString(site.blog().resolve("sunset.md"), "title=Sunset\nmasthead=sunset.png\nstatus=published\n~~~~~~\n\n"
                + "![The beach](/img/beach.png)\n");
        Files.writeString(site.templates().resolve("index.ftl"), "<#assign masthead = \"sunset.png\">\n<img src=\"/img/beach.png\">\n");
        git.answer("git cat-file -e main:", 128, "").answer("git cat-file -e origin/main:", 128, "");
        tool = new PrepareImage(site, new Git(git), new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    private String printed() {
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void aPhotoPngMastheadBecomesAJpegWithVariantsAndThePagesFollow() throws IOException, InterruptedException {
        Path png = TestImages.write(TestImages.photo(2400, 1000), site.mastheads().resolve("sunset.png"), "png");

        tool.prepare(List.of("src/jbake/assets/img/masthead/sunset.png"), DEFAULTS);

        Path jpg = site.mastheads().resolve("sunset.jpg");
        assertFalse(Files.exists(png), "never published, so it goes");
        assertTrue(Files.size(jpg) <= ImageRules.MAX_BYTES);
        BufferedImage back = ImageIO.read(jpg.toFile());
        assertTrue(back.getWidth() <= ImageRules.MAX_WIDTH);
        assertTrue(TestImages.isProgressiveJpeg(jpg));
        assertFalse(TestImages.carriesMetadata(jpg));
        assertEquals(960, ImageIO.read(site.mastheads().resolve("sunset-960.jpg").toFile()).getWidth());
        assertEquals(1440, ImageIO.read(site.mastheads().resolve("sunset-1440.jpg").toFile()).getWidth());
        assertTrue(Files.readString(site.blog().resolve("sunset.md")).contains("\nmasthead=sunset.jpg\n"));
        assertTrue(Files.readString(site.templates().resolve("index.ftl")).startsWith("<#assign masthead = \"sunset.jpg\">"));
        assertEquals(List.of("git cat-file -e main:src/jbake/assets/img/masthead/sunset.png",
                "git cat-file -e origin/main:src/jbake/assets/img/masthead/sunset.png"), git.calls());
        String log = printed();
        assertTrue(log.startsWith("src/jbake/assets/img/masthead/sunset.png: 2400x1000 RGB "), log);
        assertTrue(log.contains("K, masthead; width cap 1920\n  a photo: writing src/jbake/assets/img/masthead/sunset.jpg (the PNG was never published, so it goes)\n"
                + "  wrote src/jbake/assets/img/masthead/sunset.jpg: "), log);
        assertTrue(log.contains("\n  removed src/jbake/assets/img/masthead/sunset.png\n"
                + "  pointed src/jbake/content/blog/sunset.md at sunset.jpg\n"
                + "  pointed src/jbake/templates/index.ftl at sunset.jpg\n"
                + "  wrote src/jbake/assets/img/masthead/sunset-960.jpg  960x400\n"
                + "  wrote src/jbake/assets/img/masthead/sunset-1440.jpg  1440x600\n"
                + "Next: ./gradlew reviewChanges, then check the page in ./gradlew bakePreview\n"), log);
    }

    @Test
    void aDryRunChangesNothingAndSaysWhatItWould() throws IOException, InterruptedException {
        Path png = TestImages.write(TestImages.photo(2400, 1000), site.mastheads().resolve("sunset.png"), "png");
        byte[] before = Files.readAllBytes(png);
        String post = Files.readString(site.blog().resolve("sunset.md"));

        tool.prepare(List.of("src/jbake/assets/img/masthead/sunset.png"), DEFAULTS.dryRun(true));

        assertArrayEquals(before, Files.readAllBytes(png));
        assertEquals(post, Files.readString(site.blog().resolve("sunset.md")));
        assertFalse(Files.exists(site.mastheads().resolve("sunset.jpg")));
        assertFalse(Files.exists(site.mastheads().resolve("sunset-960.jpg")));
        assertTrue(printed().contains("  a photo: writing src/jbake/assets/img/masthead/sunset.jpg (the PNG was never published, so it goes)\n"
                + "  would point src/jbake/content/blog/sunset.md at sunset.jpg\n"
                + "  would point src/jbake/templates/index.ftl at sunset.jpg\n"
                + "  would write src/jbake/assets/img/masthead/sunset-960.jpg\n"
                + "  would write src/jbake/assets/img/masthead/sunset-1440.jpg\n"), printed());
    }

    @Test
    void aPublishedPhotoPngIsRefusedWithTheD2MessageAndNothingChanges() throws IOException {
        Path png = TestImages.write(TestImages.photo(1200, 600), site.mastheads().resolve("sunset.png"), "png");
        byte[] before = Files.readAllBytes(png);
        git.answer("git cat-file -e main:src/jbake/assets/img/masthead/sunset.png", 0, "");

        ToolFailure e = assertThrows(ToolFailure.class, () -> tool.prepare(List.of("src/jbake/assets/img/masthead/sunset.png"), DEFAULTS));

        assertEquals("Not every image could be prepared (see above).", e.getMessage());
        assertEquals(ToolFailure.FAILED, e.exitCode());
        assertArrayEquals(before, Files.readAllBytes(png));
        assertFalse(Files.exists(site.mastheads().resolve("sunset.jpg")));
        assertTrue(Files.readString(site.blog().resolve("sunset.md")).contains("masthead=sunset.png"));
        assertTrue(printed().endsWith("  a photo, but this PNG is published (it is in main), so a PNG must stay at its URL (AGENTS.md Rule 1,"
                + " option B) and this program has no colour quantizer for the fallback: convert it by hand or add"
                + " the quantizer (D2 in docs/08-SCRIPTS-MIGRATION.md). Nothing changed.\n"), printed());
    }

    @Test
    void aGraphicPngStaysPngResizedAndSoDoesAPhotoWithPng() throws IOException, InterruptedException {
        TestImages.write(TestImages.graphic(2000, 1000, true), site.images().resolve("beach.png"), "png");

        tool.prepare(List.of("src/jbake/assets/img/beach.png"), DEFAULTS);

        BufferedImage back = ImageIO.read(site.images().resolve("beach.png").toFile());
        assertEquals(PrepareImage.INLINE_WIDTH, back.getWidth(), "an inline image's cap");
        assertTrue(back.getColorModel().hasAlpha());
        assertFalse(Files.exists(site.images().resolve("beach.jpg")));
        assertTrue(printed().contains("; width cap 1000\n  a graphic (few colours or transparency): stays PNG\n  optimized in place: 1000x500 "), printed());
        assertTrue(Files.readString(site.blog().resolve("sunset.md")).contains("/img/beach.png"), "no reference changes");

        out.reset();
        TestImages.write(TestImages.photo(1200, 600), site.images().resolve("photo.png"), "png");
        tool.prepare(List.of("src/jbake/assets/img/photo.png"), new PrepareImageOptions(false, true, false, false, 600));
        assertEquals(600, ImageIO.read(site.images().resolve("photo.png").toFile()).getWidth(), "-Pwidth=600");
        assertTrue(printed().contains("; width cap 600\n  a graphic (few colours or transparency): stays PNG\n"), printed());
    }

    @Test
    void jpegForcesAGraphicToJpegAndKeepSourceKeepsThePng() throws IOException, InterruptedException {
        TestImages.write(TestImages.graphic(800, 400, false), site.images().resolve("beach.png"), "png");

        tool.prepare(List.of("src/jbake/assets/img/beach.png"), new PrepareImageOptions(false, false, true, true, null));

        assertTrue(Files.exists(site.images().resolve("beach.png")));
        assertTrue(Files.exists(site.images().resolve("beach.jpg")));
        assertTrue(Files.readString(site.blog().resolve("sunset.md")).contains("![The beach](/img/beach.jpg)"));
        assertTrue(Files.readString(site.templates().resolve("index.ftl")).contains("<img src=\"/img/beach.jpg\">"));
        assertTrue(printed().contains("  kept src/jbake/assets/img/beach.png (-PkeepSource); it will be published as-is unless you remove it\n"), printed());
        assertFalse(printed().contains("wrote src/jbake/assets/img/beach-960"), "not a masthead: no variants");
    }

    @Test
    void aJpegIsReencodedInPlace() throws IOException, InterruptedException {
        Path jpg = TestImages.write(TestImages.photo(2500, 1000), site.mastheads().resolve("hills.jpg"), "jpeg");
        long before = Files.size(jpg);

        tool.prepare(List.of("src/jbake/assets/img/masthead/hills.jpg"), DEFAULTS);

        assertTrue(Files.size(jpg) < before);
        assertTrue(ImageIO.read(jpg.toFile()).getWidth() <= ImageRules.MAX_WIDTH);
        assertTrue(TestImages.isProgressiveJpeg(jpg));
        assertTrue(Files.exists(site.mastheads().resolve("hills-960.jpg")));
        assertTrue(printed().contains("  re-encoded in place: "), printed());
        assertTrue(git.calls().isEmpty(), "no need to ask git about a JPEG");
    }

    @Test
    void refusesToRunWithoutAnImageOrWithContradictingOptionsAndSkipsWhatItCantHandle() throws IOException, InterruptedException {
        ToolFailure none = assertThrows(ToolFailure.class, () -> tool.prepare(List.of(), DEFAULTS));
        assertEquals("Say which image: ./gradlew prepareImage -Pimage=src/jbake/assets/img/masthead/photo.png", none.getMessage());

        ToolFailure both = assertThrows(ToolFailure.class, () -> tool.prepare(List.of("x.png"), new PrepareImageOptions(false, true, true, false, null)));
        assertEquals("-Ppng and -Pjpeg contradict each other.", both.getMessage());

        Files.writeString(site.images().resolve("notes.txt"), "x");
        ToolFailure skipped = assertThrows(ToolFailure.class, () -> tool.prepare(
                List.of("src/jbake/content/about.md", "src/jbake/assets/img/notes.txt", "src/jbake/assets/img/none.jpg"), DEFAULTS));
        assertEquals("Not every image could be prepared (see above).", skipped.getMessage());
        assertEquals("src/jbake/content/about.md: skipped, not under src/jbake/assets/img\n"
                + "src/jbake/assets/img/notes.txt: skipped, only JPEG and PNG are handled\n"
                + "src/jbake/assets/img/none.jpg: no such file\n", printed());

        Files.writeString(site.images().resolve("broken.jpg"), "not a jpeg");
        out.reset();
        assertThrows(ToolFailure.class, () -> tool.prepare(List.of("src/jbake/assets/img/broken.jpg"), DEFAULTS));
        assertTrue(printed().startsWith("src/jbake/assets/img/broken.jpg: couldn't read it ("), printed());
        assertTrue(printed().endsWith("); convert it by hand\n"), printed());
    }

    @Test
    void variantsModeNeedsAMastheadAndFailsOnAMissingOne() throws IOException {
        ToolFailure none = assertThrows(ToolFailure.class, () -> tool.makeMissingVariants(List.of(), DEFAULTS));
        assertEquals("Say which masthead: ./gradlew prepareImage -Pvariants -Pimage=src/jbake/assets/img/masthead/photo.jpg", none.getMessage());

        ToolFailure missing = assertThrows(ToolFailure.class, () -> tool.makeMissingVariants(List.of("src/jbake/assets/img/masthead/none.jpg"), DEFAULTS));
        assertEquals("Not every masthead could be handled (see above).", missing.getMessage());
        assertEquals("src/jbake/assets/img/masthead/none.jpg: no such file\n", printed());

        TestImages.write(TestImages.photo(1600, 600), site.mastheads().resolve("hills.jpg"), "jpeg");
        tool.makeMissingVariants(List.of("src/jbake/assets/img/masthead/hills.jpg"), DEFAULTS);
        assertTrue(Files.exists(site.mastheads().resolve("hills-960.jpg")));
        assertTrue(Files.exists(site.mastheads().resolve("hills-1440.jpg")));
    }

    @Test
    void needsTheRepositoryLayout() {
        PrepareImage elsewhere = new PrepareImage(SiteLayout.standard(root.resolve("nowhere")), new Git(git), new PrintStream(out, true, StandardCharsets.UTF_8));

        ToolFailure e = assertThrows(ToolFailure.class, () -> elsewhere.prepare(List.of("x.png"), DEFAULTS));

        assertEquals(ToolFailure.COULD_NOT_RUN, e.exitCode());
        assertEquals("Run it from the repository root (src/jbake/assets/img must exist).", e.getMessage());
    }
}
