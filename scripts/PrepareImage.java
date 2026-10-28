// Prepares an image added by hand under src/jbake/assets/img/ so it passes the site's image rules (revival tasks
// T016 and T017, AGENTS.md Rule 1 "Images" and Rule 4), and makes the 960/1440 px header-image variants that
// masthead.ftl serves by screen width (T071). Java since T140 (docs/08-SCRIPTS-MIGRATION.md); before that
// scripts/prepare-image.py and scripts/masthead-variants.py with Pillow. Run it from the repository root, through
// Gradle:
//
//   ./gradlew prepareImage -Pimage=src/jbake/assets/img/masthead/photo.png[,other.jpg] [-PdryRun] [-Ppng|-Pjpeg]
//                          [-Pwidth=1000] [-PkeepSource]
//
// or directly (Gradle passes the same options as --name=value; both spellings work):
//
//   java scripts/PrepareImage.java [--dry-run] [--png|--jpeg] [--width N] [--keep-source] <image>...
//   java scripts/PrepareImage.java --variants <image>...   make a masthead's missing 960/1440 variants (never overwrites one)
//   java scripts/PrepareImage.java --check-variants        exit 1 if any masthead in use is missing a variant (CI runs this)
//
//   --dry-run       say what would happen, change nothing
//   --png           keep a PNG as PNG (a graphic, a screenshot, a logo)
//   --jpeg          turn a PNG into a JPEG even if it looks like a graphic
//   --width N       cap the width at N px (default: 1920 for a masthead, 1000 for an inline image, the T016/T017 limits)
//   --keep-source   don't remove a new PNG after writing its JPEG
//
// What it does to each image:
// - Converts the pixels to sRGB from the embedded colour profile (ColorConvertOp; the JDK's colour engine is
//   LittleCMS, as Pillow's was), then writes nothing but pixels: no profile, EXIF, XMP, GPS or DPI. Browsers treat an
//   untagged JPEG as sRGB.
// - Resizes to at most the width cap, keeping the aspect ratio: bicubic, in halving steps so a 4000 px photo doesn't
//   alias (Pillow used Lanczos; the difference is invisible at these sizes, and no committed image is re-encoded).
// - A JPEG is re-encoded in place under the same name: progressive, optimized Huffman tables, quality stepping
//   down from 82 until the file is 400 KB or less (then the width, down to 1200 px, if quality 62 is still too big).
// - A PNG that is a photo (more than 50,000 distinct colours and no transparency, or --jpeg) becomes <name>.jpg
//   next to it, and every `masthead=`, Markdown image and `src=` that named the PNG in src/jbake/content/ and
//   src/jbake/templates/ is pointed at the JPEG. A PNG that was never published (not in `main`) is then removed
//   (--keep-source keeps it). A PNG that *is* published must keep a PNG at its URL (option B: a small 256-colour
//   fallback), and the JDK has no colour quantizer, so this program refuses it and changes nothing: convert it by
//   hand, or add the quantizer (decision D2 in docs/08-SCRIPTS-MIGRATION.md). The four published PNG mastheads
//   already have their fallbacks from T016.
// - A PNG that is a graphic (or --png) stays a PNG: resized, deflate level 9, transparency kept.
// - A masthead (anything under src/jbake/assets/img/masthead/) also gets its 960 and 1440 px variants (progressive
//   JPEG, quality 80, no metadata; a source narrower than a variant is copied as is); stale variants of the same
//   image are replaced. --variants makes only the missing ones and never overwrites: the 28 committed variants
//   stay as Pillow wrote them.
//
// Then run `./gradlew reviewChanges` to confirm, and look at the result in `./gradlew bakePreview`.
//
// A single-file Java program (JDK 21 or newer), like scripts/NewPost.java: nothing to install.

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
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
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

public class PrepareImage {

    static final Path ASSETS = Path.of("src/jbake/assets");
    static final Path IMG_DIR = ASSETS.resolve("img");
    static final Path MASTHEAD_DIR = IMG_DIR.resolve("masthead");
    static final List<Path> TEXT_ROOTS = List.of(Path.of("src/jbake/content"), Path.of("src/jbake/templates"));
    static final long MAX_BYTES = 400 * 1024;      // ReviewChanges.MAX_IMAGE_BYTES (E5)
    static final int MASTHEAD_WIDTH = 1920;        // ReviewChanges.MAX_IMAGE_WIDTH (E5), T016
    static final int INLINE_WIDTH = 1000;          // T017: twice the widest in-post display width
    static final int MIN_WIDTH = 1200;             // don't shrink a masthead below this to meet the byte target; warn instead
    static final int PHOTO_COLOURS = 50_000;       // a PNG with more distinct colours than this is a photo
    static final int[] QUALITIES = {82, 78, 74, 70, 66, 62};
    static final int[] VARIANT_WIDTHS = {960, 1440};   // ReviewChanges.MASTHEAD_WIDTHS, masthead.ftl
    static final int VARIANT_QUALITY = 80;

    static boolean dryRun, png, jpeg, keepSource;
    static Integer widthCap;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        ImageIO.setUseCache(false);
        List<String> images = new ArrayList<>();
        boolean variants = false, checkVariants = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                images.add(arg);
                continue;
            }
            int eq = arg.indexOf('=');
            String name = (eq < 0 ? arg.substring(2) : arg.substring(2, eq)).toLowerCase(Locale.ROOT).replace("-", "");
            String value = eq < 0 ? null : arg.substring(eq + 1);
            boolean on = value == null || value.isEmpty() || !value.equalsIgnoreCase("false");
            switch (name) {
                case "dryrun" -> dryRun = on;
                case "png" -> png = on;
                case "jpeg" -> jpeg = on;
                case "keepsource" -> keepSource = on;
                case "variants" -> variants = on;
                case "checkvariants" -> checkVariants = on;
                case "width" -> {
                    if (value == null) {
                        if (i + 1 >= args.length) {
                            fail("--width needs a number");
                        }
                        value = args[++i];
                    }
                    try {
                        widthCap = Integer.parseInt(value.strip());
                    } catch (NumberFormatException e) {
                        fail("--width needs a number, not '" + value + "'");
                    }
                }
                case "image" -> {
                    for (String p : (value == null ? "" : value).split(",")) {
                        if (!p.strip().isEmpty()) {
                            images.add(p.strip());
                        }
                    }
                }
                default -> fail("Unknown option " + arg + ". See the header of scripts/PrepareImage.java.");
            }
        }
        if (!Files.isDirectory(IMG_DIR) || !Files.isDirectory(TEXT_ROOTS.get(0))) {
            System.err.println("PrepareImage: run it from the repository root (" + IMG_DIR + " must exist).");
            System.exit(2);
        }
        if (png && jpeg) {
            fail("--png and --jpeg contradict each other.");
        }
        if (checkVariants) {
            checkVariants();
            return;
        }
        if (variants) {
            if (images.isEmpty()) {
                fail("Say which masthead: java scripts/PrepareImage.java --variants src/jbake/assets/img/masthead/photo.jpg");
            }
            boolean ok = true;
            for (String image : images) {
                ok &= makeMissingVariants(Path.of(image).normalize());
            }
            System.exit(ok ? 0 : 1);
        }
        if (images.isEmpty()) {
            fail("Say which image: ./gradlew prepareImage -Pimage=src/jbake/assets/img/masthead/photo.png");
        }
        boolean ok = true;
        for (String image : images) {
            ok &= prepare(Path.of(image).normalize());
        }
        if (!ok) {
            System.exit(1);
        }
        log("Next: ./gradlew reviewChanges, then check the page in ./gradlew bakePreview");
    }

    static void log(String msg) {
        System.out.println(msg);
        System.out.flush();
    }

    static void fail(String message) {
        System.err.println("PrepareImage: " + message);
        System.exit(1);
    }

    static boolean isMasthead(Path path) {
        return path.normalize().startsWith(MASTHEAD_DIR);
    }

    static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    static Path withExtension(Path path, String ext) {
        String s = path.toString();
        return Path.of(s.substring(0, s.lastIndexOf('.')) + ext);
    }

    /** True if the file exists on main or origin/main: its URL is a promise (AGENTS.md Rule 1). */
    static boolean published(Path path) throws IOException, InterruptedException {
        String rel = path.toString().replace(java.io.File.separatorChar, '/');
        for (String ref : List.of("main", "origin/main")) {
            Process p = new ProcessBuilder("git", "cat-file", "-e", ref + ":" + rel).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            if (p.waitFor() == 0) {
                return true;
            }
        }
        return false;
    }

    // ---- reading, with what the file carries besides pixels ------------------------------------------------------

    record Loaded(BufferedImage image, String mode, boolean icc, boolean exif, boolean xmp, ICC_Profile pngProfile) {}

    static Loaded load(Path path) throws IOException {
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
    static void inspect(Node node, boolean[] flags, ICC_Profile[] pngProfile) {
        String name = node.getNodeName();
        Object user = node instanceof IIOMetadataNode n ? n.getUserObject() : null;
        switch (name) {
            case "app2ICC" -> flags[0] = true;                                   // JPEG APP2 ICC_PROFILE
            case "iCCP" -> {                                                    // PNG colour profile, zlib-compressed
                flags[0] = true;
                if (user instanceof byte[] compressed) {
                    try {
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
                        pngProfile[0] = ICC_Profile.getInstance(out.toByteArray());
                    } catch (RuntimeException | java.util.zip.DataFormatException e) {
                        log("  warning: couldn't read the PNG's colour profile (" + e + "); treating the pixels as sRGB");
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

    static String attr(Node node, String name) {
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

    // ---- pixels: sRGB, resize, photo test ---------------------------------------------------------------------------

    /** The image's pixels in sRGB (TYPE_INT_RGB, or TYPE_INT_ARGB when it has transparency), the profile applied. */
    static BufferedImage toSrgb(Loaded loaded) {
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
            log("  warning: couldn't apply the colour profile (" + e + "); treating the pixels as sRGB");
        }
        BufferedImage out = new BufferedImage(im.getWidth(), im.getHeight(), type);
        copy(im, out);
        return out;
    }

    static void copy(BufferedImage from, BufferedImage to) {
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
        int w = im.getWidth(), h = im.getHeight();
        int[] px = im.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < px.length; i++) {
            px[i] &= 0xFFFFFF;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }

    /** Resizes to at most `width` px wide, keeping the aspect ratio: bicubic, halving first for large reductions. */
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

    static BufferedImage scale(BufferedImage im, int w, int h) {
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

    /** More than PHOTO_COLOURS distinct colours and no transparency. */
    static boolean looksLikePhoto(BufferedImage im) {
        int w = im.getWidth(), h = im.getHeight();
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

    // ---- writing ---------------------------------------------------------------------------------------------------

    /** A progressive JPEG with optimized Huffman tables at `quality` (0-100, libjpeg's scale), and nothing but pixels. */
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

    /** Writes im as a progressive sRGB JPEG of at most MAX_BYTES, lowering quality then width. Returns a note. */
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
                if (bytes.length <= MAX_BYTES) {
                    Files.write(target, bytes);
                    return sized.getWidth() + "x" + sized.getHeight() + " q" + q + " " + bytes.length / 1024 + "K";
                }
            }
            if (w - 200 < MIN_WIDTH) {
                Files.write(target, bytes);
                return sized.getWidth() + "x" + sized.getHeight() + " q" + q + " " + bytes.length / 1024
                        + "K  WARNING: still over " + MAX_BYTES / 1024 + " KB";
            }
            w -= 200;
        }
    }

    /** Writes im as a PNG (deflate level 9), resized to at most `width`; transparency kept. Returns a note. */
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

    // ---- references in content and templates -----------------------------------------------------------------------

    /** Points every masthead=, Markdown image and src= at the renamed file. Returns the files changed. */
    static List<Path> references(Path oldName, Path newName) throws IOException {
        String oldBase = oldName.getFileName().toString(), newBase = newName.getFileName().toString();
        String siteOld = "/" + ASSETS.relativize(oldName).toString().replace(java.io.File.separatorChar, '/');
        String siteNew = "/" + ASSETS.relativize(newName).toString().replace(java.io.File.separatorChar, '/');
        Pattern mastheadLine = Pattern.compile("(?m)^(masthead=)" + Pattern.quote(oldBase) + "$");
        List<Path> changed = new ArrayList<>();
        for (Path root : TEXT_ROOTS) {
            for (Path p : filesUnder(root)) {
                String text;
                try {
                    text = Files.readString(p, StandardCharsets.UTF_8);
                } catch (CharacterCodingException e) {
                    continue;
                }
                String updated = text.replace(siteOld, siteNew);
                if (isMasthead(oldName)) {
                    updated = mastheadLine.matcher(updated).replaceAll("$1" + Matcher.quoteReplacement(newBase));
                    updated = updated.replace("<#assign masthead = \"" + oldBase + "\">", "<#assign masthead = \"" + newBase + "\">");
                }
                if (!updated.equals(text)) {
                    changed.add(p);
                    if (!dryRun) {
                        Files.write(p, updated.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        }
        return changed;
    }

    static List<Path> filesUnder(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(Files::isRegularFile).sorted().toList();
        }
    }

    // ---- the masthead variants (T071) ------------------------------------------------------------------------------

    static Path variantPath(Path source, int width) {
        String s = source.toString();
        int dot = s.lastIndexOf('.');
        return Path.of(s.substring(0, dot) + "-" + width + s.substring(dot));
    }

    /** Writes the `width` px variant of `source` as a progressive JPEG (quality 80), or copies a narrower source. */
    static String makeVariant(Path source, Path target, int width) throws IOException {
        Loaded loaded = load(source);
        if (loaded.image().getWidth() <= width) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return "copied (source is narrower)";
        }
        BufferedImage im = fit(toRgb(toSrgb(loaded)), width);
        Files.write(target, encodeJpeg(im, VARIANT_QUALITY));
        return width + "x" + im.getHeight();
    }

    /** In prepare(): make both variants of a masthead, replacing stale ones of the same image. */
    static void mastheadVariants(Path source) throws IOException {
        for (int w : VARIANT_WIDTHS) {
            Path target = variantPath(source, w);
            if (dryRun) {
                log("  would write " + target);
                continue;
            }
            Files.deleteIfExists(target);
            log("  wrote " + target + "  " + makeVariant(source, target, w));
        }
    }

    /** --variants: make the variants `source` is missing; an existing one is never overwritten (delete it to regenerate). */
    static boolean makeMissingVariants(Path source) throws IOException {
        if (!Files.isRegularFile(source)) {
            log(source + ": no such file");
            return false;
        }
        int made = 0;
        for (int w : VARIANT_WIDTHS) {
            Path target = variantPath(source, w);
            if (Files.isRegularFile(target)) {
                continue;
            }
            if (dryRun) {
                log("would create " + target);
                continue;
            }
            String how = makeVariant(source, target, w);
            log("created " + target + "  " + Files.size(target) / 1024 + "K  " + how);
            made++;
        }
        if (made == 0 && !dryRun) {
            log(source + ": already has its " + VARIANT_WIDTHS[0] + "/" + VARIANT_WIDTHS[1] + " variants (delete one to regenerate it)");
        }
        return true;
    }

    /** Every local masthead named by `masthead=` in content or `<#assign masthead = "...">` in a template. */
    static List<String> mastheadsInUse() throws IOException {
        Pattern line = Pattern.compile("(?m)^masthead=(.+)$");
        Pattern assign = Pattern.compile("<#assign masthead = \"([^\"]+)\">");
        TreeSet<String> names = new TreeSet<>();
        for (Path root : TEXT_ROOTS) {
            for (Path p : filesUnder(root)) {
                String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8).replace("\r", "\n");
                Matcher m = line.matcher(text);
                while (m.find()) {
                    names.add(m.group(1).strip());
                }
                m = assign.matcher(text);
                while (m.find()) {
                    names.add(m.group(1));
                }
            }
        }
        names.removeIf(n -> n.startsWith("http"));
        return new ArrayList<>(names);
    }

    static Path mastheadSource(String name) {
        return (name.startsWith("/") ? ASSETS.resolve(name.substring(1)) : MASTHEAD_DIR.resolve(name)).normalize();
    }

    /** --check-variants: every masthead in use exists and has both variants (CI runs this on every build). */
    static void checkVariants() throws IOException {
        List<String> missing = new ArrayList<>();
        List<String> names = mastheadsInUse();
        for (String name : names) {
            Path source = mastheadSource(name);
            if (!Files.isRegularFile(source)) {
                missing.add(source + " (the masthead itself is missing)");
                continue;
            }
            for (int w : VARIANT_WIDTHS) {
                Path target = variantPath(source, w);
                if (!Files.isRegularFile(target)) {
                    missing.add(target.toString());
                }
            }
        }
        if (!missing.isEmpty()) {
            log("FAIL: missing masthead variants (run java scripts/PrepareImage.java --variants <image>):");
            for (String m : missing) {
                log("  " + m);
            }
            System.exit(1);
        }
        log("OK: all " + names.size() + " mastheads in use have " + VARIANT_WIDTHS[0] + "/" + VARIANT_WIDTHS[1] + "px variants");
    }

    // ---- one image ---------------------------------------------------------------------------------------------------

    static boolean prepare(Path path) throws IOException, InterruptedException {
        if (!path.startsWith(IMG_DIR)) {
            log(path + ": skipped, not under " + IMG_DIR);
            return false;
        }
        String ext = extension(path);
        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png")) {
            log(path + ": skipped, only JPEG and PNG are handled");
            return false;
        }
        if (!Files.isRegularFile(path)) {
            log(path + ": no such file");
            return false;
        }
        boolean masthead = isMasthead(path);
        int width = widthCap != null ? widthCap : masthead ? MASTHEAD_WIDTH : INLINE_WIDTH;
        Loaded src;
        try {
            src = load(path);
        } catch (IOException | RuntimeException e) {
            log(path + ": couldn't read it (" + e.getMessage() + "); convert it by hand");
            return false;
        }
        BufferedImage im0 = src.image();
        log(path + ": " + im0.getWidth() + "x" + im0.getHeight() + " " + src.mode() + " " + Files.size(path) / 1024 + "K"
                + (src.icc() ? ", colour profile" : "") + (src.exif() ? ", EXIF" : "") + (src.xmp() ? ", XMP" : "")
                + (masthead ? ", masthead" : "") + "; width cap " + width);
        BufferedImage im = toSrgb(src);
        Path finalPath = path;

        if (ext.equals(".jpg") || ext.equals(".jpeg")) {
            if (dryRun) {
                log("  would re-encode in place as sRGB progressive JPEG, no metadata");
            } else {
                log("  re-encoded in place: " + saveJpeg(im, path, width));
            }
        } else if (png || (!jpeg && !looksLikePhoto(im))) {
            log("  a graphic (few colours or transparency): stays PNG");
            if (dryRun) {
                log("  would resize losslessly and optimize in place");
            } else {
                log("  optimized in place: " + savePng(im, path, width));
            }
        } else {
            finalPath = withExtension(path, ".jpg");
            if (published(path)) {
                log("  a photo, but this PNG is published (it is in main), so a PNG must stay at its URL (AGENTS.md Rule 1,"
                        + " option B) and this program has no colour quantizer for the fallback: convert it by hand or add"
                        + " the quantizer (D2 in docs/08-SCRIPTS-MIGRATION.md). Nothing changed.");
                return false;
            }
            log("  a photo: writing " + finalPath + " (the PNG was never published, so it goes)");
            if (!dryRun) {
                log("  wrote " + finalPath + ": " + saveJpeg(im, finalPath, width));
                if (keepSource) {
                    log("  kept " + path + " (--keep-source); it will be published as-is unless you remove it");
                } else {
                    Files.delete(path);
                    log("  removed " + path);
                }
            }
            for (Path f : references(path, finalPath)) {
                log("  " + (dryRun ? "would point " : "pointed ") + f + " at " + finalPath.getFileName());
            }
        }

        if (masthead) {
            mastheadVariants(finalPath);
        }
        return true;
    }
}
