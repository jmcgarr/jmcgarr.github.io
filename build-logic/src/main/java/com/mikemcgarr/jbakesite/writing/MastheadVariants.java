package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.FileTrees;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The 960 and 1440 px variants of a header image that {@code masthead.ftl} serves by screen width (T071;
 * {@code scripts/masthead-variants.py} with Pillow until T140, then part of {@code scripts/PrepareImage.java}, in the
 * plugin since T127). A variant is a progressive JPEG at quality 80 with no metadata, {@code <name>-960.jpg} beside
 * its source; a source narrower than the variant is copied as it is.
 *
 * <ul>
 * <li>{@link #makeAll}: both variants of a masthead {@code prepareImage} has just written, replacing stale ones.</li>
 * <li>{@link #makeMissing}: only the variants a masthead lacks ({@code -Pvariants}); an existing one is never
 * overwritten, so the 28 committed variants stay as Pillow wrote them.</li>
 * <li>{@link #check()}: fails naming every masthead in use that lacks one ({@code checkMastheadVariants}; CI runs it).</li>
 * </ul>
 *
 * <p>A masthead in use is named by {@code masthead=} in a content file or {@code <#assign masthead = "...">} in a
 * template; a remote one ({@code http...}) has no variants.
 */
public final class MastheadVariants {

    /** The widths served to phones and to tablets and small laptops ({@code masthead.ftl}; the review's E5 checks for both). */
    public static final int[] WIDTHS = {960, 1440};

    /** The JPEG quality of a variant. */
    static final int QUALITY = 80;

    private static final Pattern MASTHEAD_LINE = Pattern.compile("(?m)^masthead=(.+)$");
    private static final Pattern MASTHEAD_ASSIGN = Pattern.compile("<#assign masthead = \"([^\"]+)\">");

    private final SiteLayout site;
    private final Images images;
    private final PrintStream out;

    /**
     * Creates the tool.
     *
     * @param site where the content, the templates and the mastheads are
     * @param out where the tool reports
     */
    public MastheadVariants(SiteLayout site, PrintStream out) {
        this.site = site;
        this.images = new Images(out);
        this.out = out;
    }

    /**
     * Where a variant goes: {@code <name>-<width>.<ext>} beside the source.
     *
     * @param source the masthead
     * @param width 960 or 1440
     * @return the variant's path
     */
    public static Path variantPath(Path source, int width) {
        String s = source.toString();
        int dot = s.lastIndexOf('.');
        return Path.of(s.substring(0, dot) + "-" + width + s.substring(dot));
    }

    /** Writes the {@code width} px variant of {@code source} as a progressive JPEG (quality 80), or copies a narrower source. */
    private String makeVariant(Path source, Path target, int width) throws IOException {
        Images.Loaded loaded = images.load(source);
        if (loaded.image().getWidth() <= width) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return "copied (source is narrower)";
        }
        BufferedImage im = Images.fit(Images.toRgb(images.toSrgb(loaded)), width);
        Files.write(target, Images.encodeJpeg(im, QUALITY));
        return width + "x" + im.getHeight();
    }

    /**
     * Makes both variants of a masthead, replacing stale ones of the same image (the end of {@code prepareImage}).
     *
     * @param source the masthead, just written
     * @param dryRun say what would happen, change nothing
     * @throws IOException if a file can't be read or written
     */
    public void makeAll(Path source, boolean dryRun) throws IOException {
        for (int w : WIDTHS) {
            Path target = variantPath(source, w);
            if (dryRun) {
                out.println("  would write " + site.display(target));
                continue;
            }
            Files.deleteIfExists(target);
            out.println("  wrote " + site.display(target) + "  " + makeVariant(source, target, w));
        }
    }

    /**
     * Makes the variants a masthead is missing; an existing one is never overwritten (delete it to regenerate).
     *
     * @param source the masthead
     * @param dryRun say what would happen, change nothing
     * @return false when there is no such file
     * @throws IOException if a file can't be read or written
     */
    public boolean makeMissing(Path source, boolean dryRun) throws IOException {
        if (!Files.isRegularFile(source)) {
            out.println(site.display(source) + ": no such file");
            return false;
        }
        int made = 0;
        for (int w : WIDTHS) {
            Path target = variantPath(source, w);
            if (Files.isRegularFile(target)) {
                continue;
            }
            if (dryRun) {
                out.println("would create " + site.display(target));
                continue;
            }
            String how = makeVariant(source, target, w);
            out.println("created " + site.display(target) + "  " + Files.size(target) / 1024 + "K  " + how);
            made++;
        }
        if (made == 0 && !dryRun) {
            out.println(site.display(source) + ": already has its " + WIDTHS[0] + "/" + WIDTHS[1] + " variants (delete one to regenerate it)");
        }
        return true;
    }

    /**
     * Every local masthead named by {@code masthead=} in content or {@code <#assign masthead = "...">} in a template.
     *
     * @return the names as written, sorted, without remote ones
     * @throws IOException if a file can't be read
     */
    public List<String> inUse() throws IOException {
        TreeSet<String> names = new TreeSet<>();
        for (Path root : List.of(site.content(), site.templates())) {
            for (Path p : FileTrees.regularFilesUnder(root)) {
                String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8).replace("\r", "\n");
                Matcher m = MASTHEAD_LINE.matcher(text);
                while (m.find()) {
                    names.add(m.group(1).strip());
                }
                m = MASTHEAD_ASSIGN.matcher(text);
                while (m.find()) {
                    names.add(m.group(1));
                }
            }
        }
        names.removeIf(n -> n.startsWith("http"));
        return new ArrayList<>(names);
    }

    /**
     * The file a masthead name points at: a site path ({@code /img/x.jpg}) under the assets, else a file in the
     * masthead directory.
     *
     * @param name the name as written
     * @return the file
     */
    Path source(String name) {
        return (name.startsWith("/") ? site.assets().resolve(name.substring(1)) : site.mastheads().resolve(name)).normalize();
    }

    /**
     * Every masthead in use that is missing, or is missing a variant, as the check reports it.
     *
     * @return the problems, each a path (with a note for a missing masthead itself); empty when all is well
     * @throws IOException if a file can't be read
     */
    public List<String> missing() throws IOException {
        return missing(inUse());
    }

    /** {@link #missing()} for the given mastheads in use. */
    private List<String> missing(List<String> inUse) {
        List<String> missing = new ArrayList<>();
        for (String name : inUse) {
            Path source = source(name);
            if (!Files.isRegularFile(source)) {
                missing.add(site.display(source) + " (the masthead itself is missing)");
                continue;
            }
            for (int w : WIDTHS) {
                Path target = variantPath(source, w);
                if (!Files.isRegularFile(target)) {
                    missing.add(site.display(target));
                }
            }
        }
        return missing;
    }

    /**
     * {@code checkMastheadVariants}: every masthead in use exists and has both variants (CI runs this on every build).
     *
     * @throws IOException if a file can't be read
     * @throws ToolFailure naming each missing file, when any is
     */
    public void check() throws IOException {
        List<String> inUse = inUse();
        List<String> missing = missing(inUse);
        if (!missing.isEmpty()) {
            throw ToolFailure.failed("FAIL: missing masthead variants (run ./gradlew prepareImage -Pimage=<image> -Pvariants):\n  "
                    + String.join("\n  ", missing));
        }
        out.println("OK: all " + inUse.size() + " mastheads in use have " + WIDTHS[0] + "/" + WIDTHS[1] + "px variants");
    }
}
