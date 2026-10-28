package com.mikemcgarr.jbakesite.writing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
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

/** The 960/1440 variants: which mastheads are in use, which lack one, and what -Pvariants and the check do. */
class MastheadVariantsTest {

    @TempDir
    Path root;

    private SiteLayout site;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private MastheadVariants variants;

    @BeforeEach
    void aSiteWithThreeMastheads() throws IOException {
        site = SiteLayout.standard(root);
        Files.createDirectories(site.blog());
        Files.createDirectories(site.templates());
        Files.createDirectories(site.mastheads());
        Files.writeString(site.blog().resolve("a.md"), "title=A\nmasthead=  wide.jpg \nstatus=published\n~~~~~~\n");
        Files.writeString(site.blog().resolve("b.md"), "title=B\nmasthead=https://example.com/remote.jpg\nstatus=published\n~~~~~~\n");
        Files.writeString(site.content().resolve("about.md"), "title=About\nmasthead=/img/about.jpg\n~~~~~~\n");
        Files.writeString(site.templates().resolve("index.ftl"), "<#assign masthead = \"home.jpg\">\n<#include \"masthead.ftl\">\n");
        TestImages.write(TestImages.photo(2000, 800), site.mastheads().resolve("wide.jpg"), "jpeg");
        TestImages.write(TestImages.photo(2000, 800), site.mastheads().resolve("wide-960.jpg"), "jpeg");
        TestImages.write(TestImages.photo(2000, 800), site.mastheads().resolve("wide-1440.jpg"), "jpeg");
        TestImages.write(TestImages.photo(600, 300), site.mastheads().resolve("home.jpg"), "jpeg");
        TestImages.write(TestImages.photo(600, 300), site.mastheads().resolve("home-960.jpg"), "jpeg");
        TestImages.write(TestImages.photo(600, 300), site.mastheads().resolve("home-1440.jpg"), "jpeg");
        TestImages.write(TestImages.photo(1600, 600), site.images().resolve("about.jpg"), "jpeg");
        TestImages.write(TestImages.photo(1600, 600), site.images().resolve("about-960.jpg"), "jpeg");
        TestImages.write(TestImages.photo(1600, 600), site.images().resolve("about-1440.jpg"), "jpeg");
        variants = new MastheadVariants(site, new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    @Test
    void variantPathGoesBesideTheSource() {
        assertEquals(Path.of("a/b/photo-960.jpg"), MastheadVariants.variantPath(Path.of("a/b/photo.jpg"), 960));
        assertEquals(Path.of("x.y/photo-1440.png"), MastheadVariants.variantPath(Path.of("x.y/photo.png"), 1440));
    }

    @Test
    void findsTheMastheadsInUseInContentAndTemplatesButNotRemoteOnes() throws IOException {
        assertEquals(List.of("/img/about.jpg", "home.jpg", "wide.jpg"), variants.inUse());
        assertEquals(site.images().resolve("about.jpg"), variants.source("/img/about.jpg"));
        assertEquals(site.mastheads().resolve("home.jpg"), variants.source("home.jpg"));
    }

    @Test
    void checkPassesWhenEveryMastheadHasBothVariants() throws IOException {
        variants.check();

        assertEquals("OK: all 3 mastheads in use have 960/1440px variants\n", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void checkNamesEveryMissingFileAndFails() throws IOException {
        Files.delete(site.mastheads().resolve("wide-960.jpg"));
        Files.delete(site.images().resolve("about.jpg"));

        ToolFailure e = assertThrows(ToolFailure.class, variants::check);

        assertEquals(ToolFailure.FAILED, e.exitCode());
        assertEquals("FAIL: missing masthead variants (run ./gradlew prepareImage -Pimage=<image> -Pvariants):\n"
                + "  src/jbake/assets/img/about.jpg (the masthead itself is missing)\n"
                + "  src/jbake/assets/img/masthead/wide-960.jpg", e.getMessage());
        assertEquals("", out.toString(StandardCharsets.UTF_8), "nothing else is printed");
    }

    @Test
    void makeMissingCreatesOnlyWhatIsMissingAndNeverOverwrites() throws IOException {
        Path keep = site.mastheads().resolve("wide-1440.jpg");
        byte[] before = Files.readAllBytes(keep);
        Files.delete(site.mastheads().resolve("wide-960.jpg"));

        assertTrue(variants.makeMissing(site.mastheads().resolve("wide.jpg"), false));

        Path made = site.mastheads().resolve("wide-960.jpg");
        assertTrue(Files.isRegularFile(made));
        assertEquals(960, ImageIO.read(made.toFile()).getWidth());
        assertTrue(TestImages.isProgressiveJpeg(made));
        assertFalse(TestImages.carriesMetadata(made));
        assertArrayEquals(before, Files.readAllBytes(keep), "the existing variant is untouched");
        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.matches("created src/jbake/assets/img/masthead/wide-960.jpg  \\d+K  960x384\n"), printed);

        out.reset();
        assertTrue(variants.makeMissing(site.mastheads().resolve("wide.jpg"), false));
        assertEquals("src/jbake/assets/img/masthead/wide.jpg: already has its 960/1440 variants (delete one to regenerate it)\n",
                out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void makeMissingCopiesANarrowSourceDryRunsAndRefusesAMissingOne() throws IOException {
        Files.delete(site.mastheads().resolve("home-1440.jpg"));
        Files.delete(site.mastheads().resolve("home-960.jpg"));

        assertTrue(variants.makeMissing(site.mastheads().resolve("home.jpg"), true));
        assertEquals("would create src/jbake/assets/img/masthead/home-960.jpg\nwould create src/jbake/assets/img/masthead/home-1440.jpg\n",
                out.toString(StandardCharsets.UTF_8));
        assertFalse(Files.exists(site.mastheads().resolve("home-960.jpg")), "a dry run writes nothing");

        out.reset();
        assertTrue(variants.makeMissing(site.mastheads().resolve("home.jpg"), false));
        assertArrayEquals(Files.readAllBytes(site.mastheads().resolve("home.jpg")), Files.readAllBytes(site.mastheads().resolve("home-1440.jpg")));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("home-1440.jpg  "), out.toString());
        assertTrue(out.toString(StandardCharsets.UTF_8).endsWith("K  copied (source is narrower)\n"), out.toString());

        out.reset();
        assertFalse(variants.makeMissing(site.mastheads().resolve("nope.jpg"), false));
        assertEquals("src/jbake/assets/img/masthead/nope.jpg: no such file\n", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void makeAllReplacesStaleVariants() throws IOException {
        Path stale = site.mastheads().resolve("wide-960.jpg");
        byte[] before = Files.readAllBytes(stale);

        variants.makeAll(site.mastheads().resolve("wide.jpg"), false);

        assertFalse(java.util.Arrays.equals(before, Files.readAllBytes(stale)), "rewritten from the source");
        assertEquals(960, ImageIO.read(stale.toFile()).getWidth());
        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.startsWith("  wrote src/jbake/assets/img/masthead/wide-960.jpg  960x384\n  wrote src/jbake/assets/img/masthead/wide-1440.jpg  1440x576\n"), printed);
    }
}
