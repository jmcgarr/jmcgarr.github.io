package com.mikemcgarr.jbakesite.writing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

/** Images made at test time, so no binary is committed: a "photo" is a gradient with grain (tens of thousands of colours). */
final class TestImages {

    private TestImages() {
    }

    /** A photo: a smooth gradient with a little grain, so it has more than 50,000 distinct colours yet compresses like one. */
    static BufferedImage photo(int width, int height) {
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = clamp(x * 255 / Math.max(1, width - 1) + random.nextInt(5) - 2);
                int g = clamp(y * 255 / Math.max(1, height - 1) + random.nextInt(5) - 2);
                int b = clamp((x + y) * 255 / Math.max(1, width + height - 2) + random.nextInt(5) - 2);
                im.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return im;
    }

    /** Noise: every pixel random, which no JPEG quality can fit in a budget. */
    static BufferedImage noise(int width, int height) {
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(7);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                im.setRGB(x, y, random.nextInt(0x1000000));
            }
        }
        return im;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** A graphic: two flat colours, and a transparent corner when asked. */
    static BufferedImage graphic(int width, int height, boolean transparent) {
        BufferedImage im = new BufferedImage(width, height, transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.BLUE);
            g.fillRect(width / 4, height / 4, width / 2, height / 2);
        } finally {
            g.dispose();
        }
        if (transparent) {
            im.setRGB(0, 0, 0);
        }
        return im;
    }

    static Path write(BufferedImage im, Path target, String format) throws IOException {
        Files.createDirectories(target.getParent());
        if (!ImageIO.write(im, format, target.toFile())) {
            throw new IOException("no writer for " + format);
        }
        return target;
    }

    /** Whether the bytes carry any of the markers prepareImage promises to leave out. */
    static boolean carriesMetadata(Path file) throws IOException {
        String bytes = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        return bytes.contains("Exif") || bytes.contains("ICC_PROFILE") || bytes.contains("http://ns.adobe.com/xap/");
    }

    /** Progressive JPEGs start their scans with SOF2 (FF C2). */
    static boolean isProgressiveJpeg(Path file) throws IOException {
        byte[] b = Files.readAllBytes(file);
        for (int i = 0; i + 1 < b.length; i++) {
            if ((b[i] & 0xFF) == 0xFF && (b[i + 1] & 0xFF) == 0xC2) {
                return true;
            }
        }
        return false;
    }
}
