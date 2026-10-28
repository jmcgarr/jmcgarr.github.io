// PixelDiff: compare before/after screenshots pixel by pixel (the pixeldiff.py of T102, in Java since T138).
//
// Usage: java scripts/visual/PixelDiff.java <before_dir> <after_dir> [<diff_out_dir>]
//
// For every PNG in before_dir with the same name in after_dir, prints the size, the number of differing pixels
// and their bounding box. With diff_out_dir, writes a red-on-grey diff image for each pair that differs.
// Exit status 1 if any pair differs (or a file is missing or sized differently), 0 if every pair is identical.
//
// The numbers are the ones pixeldiff.py printed, so old reports stay comparable: the bounding box is of every
// pixel where any channel differs (Pillow's ImageChops.difference(...).getbbox(), as (left, top, right, bottom)
// with right and bottom exclusive), and the count is of the pixels whose difference has a non-zero luma
// (Pillow's convert("L") of the difference: L = (R*19595 + G*38470 + B*7471 + 0x8000) >> 16), which is also the
// mask the diff image paints red. The two can disagree only on a pixel whose channels differ by one or two
// units in blue alone; on screenshots that doesn't happen.
//
// The screenshots come from screenshots.sh (1280px) and phone-shots.mjs (375px) in this folder; the method is in
// docs/reports/css-review-2026-10-24.md, "How to verify". A single-file Java program (JDK 21 or newer), like
// scripts/NewPost.java: nothing to install.

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

public class PixelDiff {

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            System.err.println("usage: java scripts/visual/PixelDiff.java <before_dir> <after_dir> [<diff_out_dir>]");
            System.exit(2);
        }
        Path before = Path.of(args[0]);
        Path after = Path.of(args[1]);
        Path out = args.length > 2 ? Path.of(args[2]) : null;
        if (!Files.isDirectory(before)) {
            System.err.println("PixelDiff: not a directory: " + before);
            System.exit(2);
        }
        List<Path> files;
        try (Stream<Path> s = Files.list(before)) {
            files = s.filter(p -> p.getFileName().toString().endsWith(".png")).sorted().toList();
        }
        int differs = 0;
        for (Path a : files) {
            String name = a.getFileName().toString();
            Path b = after.resolve(name);
            if (!Files.exists(b)) {
                System.out.println(name + ": missing in " + after);
                differs++;
                continue;
            }
            BufferedImage imgA = read(a);
            BufferedImage imgB = read(b);
            int w = imgA.getWidth(), h = imgA.getHeight();
            if (w != imgB.getWidth() || h != imgB.getHeight()) {
                System.out.println(name + ": size (" + w + ", " + h + ") vs (" + imgB.getWidth() + ", " + imgB.getHeight() + ")");
                differs++;
                continue;
            }
            int[] pa = imgA.getRGB(0, 0, w, h, null, 0, w);
            int[] pb = imgB.getRGB(0, 0, w, h, null, 0, w);
            int left = w, top = h, right = -1, bottom = -1;   // bounding box of any-channel differences
            int count = 0;                                      // pixels whose difference has non-zero luma
            boolean[] mask = out != null ? new boolean[pa.length] : null;
            for (int i = 0; i < pa.length; i++) {
                int dr = Math.abs(((pa[i] >> 16) & 255) - ((pb[i] >> 16) & 255));
                int dg = Math.abs(((pa[i] >> 8) & 255) - ((pb[i] >> 8) & 255));
                int db = Math.abs((pa[i] & 255) - (pb[i] & 255));
                if ((dr | dg | db) == 0) {
                    continue;
                }
                int x = i % w, y = i / w;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
                if (((dr * 19595 + dg * 38470 + db * 7471 + 0x8000) >> 16) != 0) {
                    count++;
                    if (mask != null) {
                        mask[i] = true;
                    }
                }
            }
            if (right < 0) {
                System.out.println(name + ": " + w + "x" + h + ", identical (0 pixels differ)");
                continue;
            }
            System.out.println(name + ": " + count + " pixels differ, bounding box (" + left + ", " + top + ", "
                    + (right + 1) + ", " + (bottom + 1) + ")");
            differs++;
            if (out != null) {
                Files.createDirectories(out);
                BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                int[] px = new int[pa.length];
                for (int i = 0; i < pa.length; i++) {
                    if (mask[i]) {
                        px[i] = 0xFF0000;
                    } else {
                        int l = (((pa[i] >> 16) & 255) * 19595 + ((pa[i] >> 8) & 255) * 38470 + (pa[i] & 255) * 7471 + 0x8000) >> 16;
                        px[i] = (l << 16) | (l << 8) | l;
                    }
                }
                diff.setRGB(0, 0, w, h, px, 0, w);
                ImageIO.write(diff, "png", out.resolve(name).toFile());
            }
        }
        System.exit(differs > 0 ? 1 : 0);
    }

    /** Reads a PNG as opaque RGB (what pixeldiff.py's convert("RGB") did: the alpha channel is dropped, not composited). */
    static BufferedImage read(Path file) throws IOException {
        BufferedImage img = ImageIO.read(file.toFile());
        if (img == null) {
            throw new IOException(file + ": not an image ImageIO can read");
        }
        return img;
    }
}
