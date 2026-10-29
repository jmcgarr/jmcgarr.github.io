package com.mikemcgarr.jbakesite.writing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.internal.ImageRules;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The pixel operations of prepareImage: the photo test, resizing, and what the written files carry. */
class ImagesTest {

    @TempDir
    Path dir;

    private final Images images = new Images(new PrintStream(new ByteArrayOutputStream(), true));

    @Test
    void aPhotoHasManyColoursAndAGraphicFew() {
        assertTrue(Images.looksLikePhoto(TestImages.photo(300, 200)));
        assertTrue(Images.looksLikePhoto(TestImages.noise(300, 200)));
        assertFalse(Images.looksLikePhoto(TestImages.graphic(300, 200, false)), "two colours");
        assertFalse(Images.looksLikePhoto(TestImages.graphic(300, 200, true)), "transparency means a graphic");
    }

    @Test
    void fitKeepsTheAspectRatioAndNeverEnlarges() {
        BufferedImage im = TestImages.graphic(4000, 3000, false);
        BufferedImage small = Images.fit(im, 1000);
        assertEquals(1000, small.getWidth());
        assertEquals(750, small.getHeight());
        assertSame(im, Images.fit(im, 4000), "wide enough already");
        assertSame(im, Images.fit(im, 5000));
    }

    @Test
    void modeNamesThePixelsLikePillow() {
        assertEquals("RGB", Images.mode(TestImages.graphic(10, 10, false)));
        assertEquals("RGBA", Images.mode(TestImages.graphic(10, 10, true)));
        assertEquals("P", Images.mode(new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED)));
        assertEquals("L", Images.mode(new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY)));
        assertEquals("L 16-bit", Images.mode(new BufferedImage(10, 10, BufferedImage.TYPE_USHORT_GRAY)));
    }

    @Test
    void toRgbDropsTheAlphaChannelWithoutCompositing() {
        BufferedImage rgb = Images.toRgb(TestImages.graphic(10, 10, true));
        assertEquals(BufferedImage.TYPE_INT_RGB, rgb.getType());
        assertEquals(0x000000, rgb.getRGB(0, 0) & 0xFFFFFF, "the transparent pixel's colour, not white");
    }

    @Test
    void saveJpegWritesAProgressiveFileUnderTheBudgetLoweringQualityThenWidth() throws IOException {
        Path target = dir.resolve("photo.jpg");

        String note = Images.saveJpeg(TestImages.photo(2400, 1600), target, 1920);

        assertTrue(Files.size(target) <= ImageRules.MAX_BYTES, note);
        assertTrue(note.matches("1920x1280 q\\d\\d \\d+K"), note);
        assertTrue(TestImages.isProgressiveJpeg(target));
        assertFalse(TestImages.carriesMetadata(target));
        assertEquals(1920, ImageIO.read(target.toFile()).getWidth());
    }

    @Test
    void saveJpegWarnsWhenEvenTheFloorWidthIsOverTheBudget() throws IOException {
        Path target = dir.resolve("noise.jpg");

        String note = Images.saveJpeg(TestImages.noise(1920, 3000), target, 1920);

        // noise can't fit in 400 KB at any quality: the width steps down by 200 and stops before the 1200 floor
        assertTrue(note.matches("1320x2062 q62 \\d+K  WARNING: still over 400 KB"), note);
    }

    @Test
    void savePngKeepsTransparencyAndResizes() throws IOException {
        Path target = dir.resolve("logo.png");

        String note = Images.savePng(TestImages.graphic(2000, 1000, true), target, 1000);

        assertTrue(note.startsWith("1000x500 "), note);
        BufferedImage back = ImageIO.read(target.toFile());
        assertEquals(1000, back.getWidth());
        assertTrue(back.getColorModel().hasAlpha());
    }

    @Test
    void loadReportsWhatAFileCarries() throws IOException {
        Path jpg = TestImages.write(TestImages.photo(50, 40), dir.resolve("plain.jpg"), "jpeg");
        Path png = TestImages.write(TestImages.graphic(50, 40, true), dir.resolve("plain.png"), "png");

        Images.Loaded loadedJpg = images.load(jpg);
        Images.Loaded loadedPng = images.load(png);

        assertEquals(50, loadedJpg.image().getWidth());
        assertFalse(loadedJpg.icc() || loadedJpg.exif() || loadedJpg.xmp(), "nothing but pixels");
        assertEquals("RGBA", loadedPng.mode());
        assertEquals(BufferedImage.TYPE_INT_ARGB, images.toSrgb(loadedPng).getType());
        assertEquals(BufferedImage.TYPE_INT_RGB, images.toSrgb(loadedJpg).getType());
    }
}
