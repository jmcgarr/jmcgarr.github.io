package com.mikemcgarr.jbakesite.review;

import com.mikemcgarr.jbakesite.internal.SiteLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** E5: the image rules (AGENTS.md Rule 4) for a changed image, and a masthead's 960/1440 variants (T071). */
final class ImageChecks {

    static final long MAX_IMAGE_BYTES = 400 * 1024;
    static final int MAX_IMAGE_WIDTH = 1920;
    static final int[] MASTHEAD_WIDTHS = {960, 1440};

    private final SiteLayout site;
    private final Findings findings;

    ImageChecks(SiteLayout site, Findings findings) {
        this.site = site;
        this.findings = findings;
    }

    void checkImage(Path image) {
        String file = site.display(image);
        try {
            long size = Files.size(image);
            if (size > MAX_IMAGE_BYTES) {
                findings.add(true, "E5", file, 0, "The image is " + (size / 1024) + " KB; the target is 400 KB or less (AGENTS.md Rule 4).",
                        "Re-encode it (JPEG quality 80, sRGB, metadata stripped) before adding it.");
            }
            Dimension d = dimensions(image);
            if (d != null && d.width > MAX_IMAGE_WIDTH) {
                findings.add(true, "E5", file, 0, "The image is " + d.width + " px wide; the limit is " + MAX_IMAGE_WIDTH + " px (AGENTS.md Rule 4).",
                        "Resize it to at most " + MAX_IMAGE_WIDTH + " px wide.");
            }
        } catch (IOException e) {
            findings.add(true, "E5", file, 0, "Couldn't read the image: " + e.getMessage(), null);
        }
        if (image.startsWith(site.mastheads()) && !image.getFileName().toString().matches(".*-(960|1440)\\.\\w+")) {
            checkVariants(image, file, 0);
        }
    }

    /** E5 when a masthead lacks a 960 or 1440 variant; {@code file} and {@code line} say where to report it. */
    void checkVariants(Path image, String file, int line) {
        String s = image.toString();
        int dot = s.lastIndexOf('.');
        List<String> missing = new ArrayList<>();
        for (int w : MASTHEAD_WIDTHS) {
            Path variant = Path.of(s.substring(0, dot) + "-" + w + s.substring(dot));
            if (!Files.isRegularFile(variant)) {
                missing.add(variant.getFileName().toString());
            }
        }
        if (!missing.isEmpty()) {
            findings.add(true, "E5", file, line, "The masthead " + image.getFileName() + " has no " + String.join(" or ", missing)
                    + " variant for phones and tablets (CI fails without them).", variantsCommand(site.display(image)));
        }
    }

    /** The command that makes a masthead's missing variants. */
    static String variantsCommand(String image) {
        return "./gradlew prepareImage -Pimage=" + image + " -Pvariants";
    }

    static Dimension dimensions(Path image) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(image.toFile())) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;   // SVG, WebP and other formats the JDK can't read: size is still checked
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        }
    }
}
