package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.internal.ImageRules;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Iterator;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.w3c.dom.Node;

/**
 * The pixels: reading an image with what it carries besides them, converting to sRGB, resizing, the photo test, and
 * writing a JPEG or PNG that carries nothing but pixels (T016, T017, T140; the imaging half of
 * {@code scripts/PrepareImage.java}, split out in T127). Plain {@code javax.imageio} and Java2D, no library:
 *
 * <ul>
 * <li>sRGB from the embedded colour profile with {@link ColorConvertOp} (the JDK's colour engine is LittleCMS, as
 * Pillow's was); ImageIO's PNG reader reads an {@code iCCP} chunk but doesn't apply it, so the pixels are tagged with
 * it here first. Browsers treat an untagged JPEG as sRGB.</li>
 * <li>Resizing is bicubic, in halving steps so a 4000 px photo doesn't alias (Pillow used Lanczos; the difference is
 * invisible at these sizes, and no committed image is re-encoded).</li>
 * <li>A JPEG is written progressive with optimized Huffman tables, stepping the quality down from 82 until the file
 * is under the byte budget, then the width; a PNG with deflate level 9, transparency kept.</li>
 * </ul>
 */
final class Images {

    /** Don't shrink a masthead below this to meet the byte target; warn instead. */
    static final int MIN_WIDTH = 1200;

    /** A PNG with more distinct colours than this is a photo. */
    static final int PHOTO_COLOURS = 50_000;

    /** The JPEG qualities tried, in order, until the file fits the budget. */
    static final int[] QUALITIES = {82, 78, 74, 70, 66, 62};

    private final PrintStream out;

    /**
     * Creates the helper.
     *
     * @param out where a warning about a colour profile that couldn't be applied goes
     */
    Images(PrintStream out) {
        this.out = out;
    }

    /**
     * An image as read, with what the file carried besides pixels.
     *
     * @param image the pixels
     * @param mode a short name for what the pixels are, in Pillow's terms, for the log line
     * @param icc whether the file carried a colour profile
     * @param exif whether it carried EXIF
     * @param xmp whether it carried XMP
     * @param pngProfile a PNG's {@code iCCP} profile, which ImageIO doesn't apply, or null
     */
    record Loaded(BufferedImage image, String mode, boolean icc, boolean exif, boolean xmp, ICC_Profile pngProfile) {
    }

    /**
     * Reads an image.
     *
     * @param path the file
     * @return the image and what the file carried
     * @throws IOException if the file isn't an image ImageIO can read
     */
    Loaded load(Path path) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(path.toFile())) {
            if (in == null) {
                throw new IOException("can't open it");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("not an image ImageIO can read");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                BufferedImage image = reader.read(0);
                boolean[] flags = new boolean[3];   // icc, exif, xmp
                ICC_Profile[] pngProfile = new ICC_Profile[1];
                try {
                    IIOMetadata meta = reader.getImageMetadata(0);
                    if (meta != null) {
                        inspect(meta.getAsTree(meta.getNativeMetadataFormatName()), flags, pngProfile);
                    }
                } catch (IOException | RuntimeException e) {
                    // metadata we can't read is metadata we won't write; the pixels are what matter
                }
                if (!image.getColorModel().getColorSpace().isCS_sRGB()) {
                    flags[0] = true;
                }
                return new Loaded(image, mode(image), flags[0], flags[1], flags[2], pngProfile[0]);
            } finally {
                reader.dispose();
            }
        }
    }

    /** Walks ImageIO's native metadata tree for the markers and chunks that carry a profile, EXIF or XMP. */
    private void inspect(Node node, boolean[] flags, ICC_Profile[] pngProfile) {
        String name = node.getNodeName();
        Object user = node instanceof IIOMetadataNode n ? n.getUserObject() : null;
        switch (name) {
            case "app2ICC" -> flags[0] = true;                                   // JPEG APP2 ICC_PROFILE
            case "iCCP" -> {                                                    // PNG colour profile, zlib-compressed
                flags[0] = true;
                if (user instanceof byte[] compressed) {
                    try {
                        pngProfile[0] = ICC_Profile.getInstance(inflate(compressed));
                    } catch (RuntimeException | DataFormatException e) {
                        out.println("  warning: couldn't read the PNG's colour profile (" + e + "); treating the pixels as sRGB");
                    }
                }
            }
            case "unknown" -> {                                                 // JPEG APP1: Exif or XMP
                if ("225".equals(attr(node, "MarkerTag")) && user instanceof byte[] b) {
                    String head = new String(b, 0, Math.min(b.length, 32), StandardCharsets.ISO_8859_1);
                    if (head.startsWith("Exif")) {
                        flags[1] = true;
                    } else if (head.startsWith("http://ns.adobe.com/xap/")) {
                        flags[2] = true;
                    }
                }
            }
            case "iTXtEntry" -> {                                               // PNG XMP
                if ("XML:com.adobe.xmp".equals(attr(node, "keyword"))) {
                    flags[2] = true;
                }
            }
            case "UnknownChunk" -> {                                            // PNG eXIf
                if ("eXIf".equals(attr(node, "type"))) {
                    flags[1] = true;
                }
            }
            default -> { }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            inspect(child, flags, pngProfile);
        }
    }

    private static byte[] inflate(byte[] compressed) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!inflater.finished()) {
            int n = inflater.inflate(buf);
            if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                break;
            }
            out.write(buf, 0, n);
        }
        inflater.end();
        return out.toByteArray();
    }

    private static String attr(Node node, String name) {
        Node a = node.getAttributes() == null ? null : node.getAttributes().getNamedItem(name);
        return a == null ? null : a.getNodeValue();
    }

    /** A short name for what the pixels are, in Pillow's terms (for the log line). */
    static String mode(BufferedImage im) {
        ColorModel cm = im.getColorModel();
        if (cm instanceof IndexColorModel) {
            return "P";
        }
        boolean grey = cm.getColorSpace().getType() == ColorSpace.TYPE_GRAY;
        String base = grey ? "L" : "RGB";
        if (cm.hasAlpha()) {
            base += "A";
        }
        return cm.getComponentSize(0) > 8 ? base + " 16-bit" : base;
    }

    /**
     * The image's pixels in sRGB ({@code TYPE_INT_RGB}, or {@code TYPE_INT_ARGB} when it has transparency), the
     * profile applied.
     *
     * @param loaded the image as read
     * @return the sRGB pixels
     */
    BufferedImage toSrgb(Loaded loaded) {
        BufferedImage im = loaded.image();
        boolean alpha = im.getColorModel().hasAlpha();
        int type = alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage src = im;
        try {
            if (loaded.pngProfile() != null && im.getColorModel().getColorSpace().isCS_sRGB()) {
                // ImageIO's PNG reader reads the iCCP chunk but doesn't apply it: tag the pixels with it ourselves
                ICC_Profile profile = loaded.pngProfile();
                if (profile.getColorSpaceType() != ColorSpace.TYPE_RGB || profile.getNumComponents() != 3) {
                    throw new IllegalArgumentException("not an RGB profile");
                }
                BufferedImage bytes = new BufferedImage(im.getWidth(), im.getHeight(),
                        alpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
                copy(im, bytes);
                ColorModel tagged = new ComponentColorModel(new ICC_ColorSpace(profile), alpha, false,
                        alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                src = new BufferedImage(tagged, bytes.getRaster(), false, null);
            }
            if (!src.getColorModel().getColorSpace().isCS_sRGB()) {
                BufferedImage out = new BufferedImage(im.getWidth(), im.getHeight(), type);
                new ColorConvertOp(null).filter(src, out);
                return out;
            }
        } catch (RuntimeException e) {
            out.println("  warning: couldn't apply the colour profile (" + e + "); treating the pixels as sRGB");
        }
        BufferedImage out = new BufferedImage(im.getWidth(), im.getHeight(), type);
        copy(im, out);
        return out;
    }

    private static void copy(BufferedImage from, BufferedImage to) {
        Graphics2D g = to.createGraphics();
        try {
            g.drawImage(from, 0, 0, null);
        } finally {
            g.dispose();
        }
    }

    /** Drops the alpha channel without compositing (Pillow's convert("RGB")). */
    static BufferedImage toRgb(BufferedImage im) {
        if (im.getType() == BufferedImage.TYPE_INT_RGB) {
            return im;
        }
        int w = im.getWidth();
        int h = im.getHeight();
        int[] px = im.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < px.length; i++) {
            px[i] &= 0xFFFFFF;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }

    /** Resizes to at most {@code width} px wide, keeping the aspect ratio: bicubic, halving first for large reductions. */
    static BufferedImage fit(BufferedImage im, int width) {
        if (im.getWidth() <= width) {
            return im;
        }
        int targetH = (int) Math.rint((double) im.getHeight() * width / im.getWidth());
        BufferedImage cur = im;
        while (cur.getWidth() / 2 >= width) {
            cur = scale(cur, cur.getWidth() / 2, cur.getHeight() / 2);
        }
        return scale(cur, width, Math.max(1, targetH));
    }

    private static BufferedImage scale(BufferedImage im, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, im.getType());
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(im, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    /** More than {@link #PHOTO_COLOURS} distinct colours and no transparency. */
    static boolean looksLikePhoto(BufferedImage im) {
        int w = im.getWidth();
        int h = im.getHeight();
        int[] px = im.getRGB(0, 0, w, h, null, 0, w);
        if (im.getColorModel().hasAlpha()) {
            for (int p : px) {
                if ((p >>> 24) < 255) {
                    return false;
                }
            }
        }
        BitSet seen = new BitSet(1 << 24);
        int distinct = 0;
        for (int p : px) {
            int rgb = p & 0xFFFFFF;
            if (!seen.get(rgb)) {
                seen.set(rgb);
                if (++distinct > PHOTO_COLOURS) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A progressive JPEG with optimized Huffman tables at {@code quality} (0-100, libjpeg's scale), and nothing but pixels. */
    static byte[] encodeJpeg(BufferedImage rgb, int quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            JPEGImageWriteParam param = (JPEGImageWriteParam) writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality / 100f);
            param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            param.setOptimizeHuffmanTables(true);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(rgb, null, null), param);   // no metadata: no EXIF, XMP or profile
            }
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    /** Writes im as a progressive sRGB JPEG of at most {@link ImageRules#MAX_BYTES}, lowering quality then width. Returns a note. */
    static String saveJpeg(BufferedImage im, Path target, int width) throws IOException {
        BufferedImage rgb = toRgb(im);
        int w = Math.min(width, rgb.getWidth());
        while (true) {
            BufferedImage sized = fit(rgb, w);
            byte[] bytes = null;
            int q = 0;
            for (int quality : QUALITIES) {
                bytes = encodeJpeg(sized, quality);
                q = quality;
                if (bytes.length <= ImageRules.MAX_BYTES) {
                    Files.write(target, bytes);
                    return sized.getWidth() + "x" + sized.getHeight() + " q" + q + " " + bytes.length / 1024 + "K";
                }
            }
            if (w - 200 < MIN_WIDTH) {
                Files.write(target, bytes);
                return sized.getWidth() + "x" + sized.getHeight() + " q" + q + " " + bytes.length / 1024
                        + "K  WARNING: still over " + ImageRules.MAX_BYTES / 1024 + " KB";
            }
            w -= 200;
        }
    }

    /** Writes im as a PNG (deflate level 9), resized to at most {@code width}; transparency kept. Returns a note. */
    static String savePng(BufferedImage im, Path target, int width) throws IOException {
        BufferedImage sized = fit(im, width);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            try {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.0f);   // the PNG writer maps 0.0 to deflate level 9
            } catch (UnsupportedOperationException e) {
                param = writer.getDefaultWriteParam();
            }
            try (ImageOutputStream out = ImageIO.createImageOutputStream(target.toFile())) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(sized, null, null), param);
            }
        } finally {
            writer.dispose();
        }
        return sized.getWidth() + "x" + sized.getHeight() + " " + Files.size(target) / 1024 + "K";
    }
}
