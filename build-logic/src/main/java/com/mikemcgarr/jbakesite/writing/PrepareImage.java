package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.FileTrees;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ImageRules;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepares an image added by hand under the assets' {@code img/} so it passes the site's image rules (revival tasks
 * T016 and T017, AGENTS.md Rule 1 "Images" and Rule 4), and makes the 960/1440 px header-image variants
 * ({@link MastheadVariants}, T071). {@link PrepareImageTask} runs it:
 *
 * <pre>
 * ./gradlew prepareImage -Pimage=src/jbake/assets/img/masthead/photo.png[,other.jpg] [-PdryRun] [-Ppng|-Pjpeg] [-Pwidth=1000] [-PkeepSource]
 * ./gradlew prepareImage -Pimage=... -Pvariants    make a masthead's missing 960/1440 variants (never overwrites one)
 * </pre>
 *
 * <p>What it does to each image ({@link Images} has the pixels):
 * <ul>
 * <li>Converts the pixels to sRGB from the embedded colour profile, then writes nothing but pixels: no profile, EXIF,
 * XMP, GPS or DPI.</li>
 * <li>Resizes to at most the width cap, keeping the aspect ratio.</li>
 * <li>A JPEG is re-encoded in place under the same name: progressive, quality stepping down from 82 until the file
 * is 400 KB or less (then the width, down to 1200 px, if quality 62 is still too big).</li>
 * <li>A PNG that is a photo (more than 50,000 distinct colours and no transparency, or {@code -Pjpeg}) becomes
 * {@code <name>.jpg} next to it, and every {@code masthead=}, Markdown image and {@code src=} that named the PNG in
 * the content and templates is pointed at the JPEG. A PNG that was never published (not in {@code main}) is then
 * removed ({@code -PkeepSource} keeps it). A PNG that <em>is</em> published must keep a PNG at its URL (option B: a
 * small 256-colour fallback), and the JDK has no colour quantizer, so this refuses it and changes nothing: convert
 * it by hand, or add the quantizer (decision D2 in docs/08-SCRIPTS-MIGRATION.md). The four published PNG mastheads
 * already have their fallbacks from T016.</li>
 * <li>A PNG that is a graphic (or {@code -Ppng}) stays a PNG: resized, deflate level 9, transparency kept.</li>
 * <li>A masthead also gets its 960 and 1440 px variants; stale variants of the same image are replaced.</li>
 * </ul>
 *
 * <p>Then run {@code ./gradlew reviewChanges} to confirm, and look at the result in {@code ./gradlew bakePreview}.
 */
final class PrepareImage {

    /** The width cap for an inline image (T017): twice the widest in-post display width. */
    static final int INLINE_WIDTH = 1000;

    private final SiteLayout site;
    private final Git git;
    private final Images images;
    private final MastheadVariants variants;
    private final PrintStream out;

    /**
     * Creates the tool.
     *
     * @param site where the images, the content and the templates are
     * @param git the repository, asked whether a PNG is published ({@code main} or {@code origin/main})
     * @param out where the tool reports
     */
    PrepareImage(SiteLayout site, Git git, PrintStream out) {
        this.site = site;
        this.git = git;
        this.images = new Images(out);
        this.variants = new MastheadVariants(site, out);
        this.out = out;
    }

    /**
     * Prepares images, and says what to do next.
     *
     * @param paths the images, as given (relative to the project or absolute)
     * @param options how
     * @throws IOException if a file can't be read or written
     * @throws InterruptedException if interrupted while asking git
     * @throws ToolFailure when no image was named, the options contradict each other, or an image couldn't be handled
     */
    void prepare(List<String> paths, PrepareImageOptions options) throws IOException, InterruptedException {
        checkEnvironment(options);
        if (paths.isEmpty()) {
            throw ToolFailure.failed("Say which image: ./gradlew prepareImage -Pimage=" + site.display(site.mastheads()) + "/photo.png");
        }
        boolean ok = true;
        for (String path : paths) {
            ok &= prepare(site.resolve(path), options);
        }
        if (!ok) {
            throw ToolFailure.failed("Not every image could be prepared (see above).");
        }
        out.println("Next: ./gradlew reviewChanges, then check the page in ./gradlew bakePreview");
    }

    /**
     * Makes the variants mastheads are missing ({@code -Pvariants}); never overwrites one.
     *
     * @param paths the mastheads, as given
     * @param options how; only {@code dryRun} matters
     * @throws IOException if a file can't be read or written
     * @throws ToolFailure when no masthead was named, or one doesn't exist
     */
    void makeMissingVariants(List<String> paths, PrepareImageOptions options) throws IOException {
        checkEnvironment(options);
        if (paths.isEmpty()) {
            throw ToolFailure.failed("Say which masthead: ./gradlew prepareImage -Pvariants -Pimage=" + site.display(site.mastheads()) + "/photo.jpg");
        }
        boolean ok = true;
        for (String path : paths) {
            ok &= variants.makeMissing(site.resolve(path), options.dryRun());
        }
        if (!ok) {
            throw ToolFailure.failed("Not every masthead could be handled (see above).");
        }
    }

    private void checkEnvironment(PrepareImageOptions options) {
        if (!Files.isDirectory(site.images()) || !Files.isDirectory(site.content())) {
            throw ToolFailure.couldNotRun("Run it from the repository root (" + site.display(site.images()) + " must exist).");
        }
        if (options.png() && options.jpeg()) {
            throw ToolFailure.failed("-Ppng and -Pjpeg contradict each other.");
        }
    }

    boolean isMasthead(Path path) {
        return path.normalize().startsWith(site.mastheads());
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
    boolean published(Path path) throws IOException, InterruptedException {
        String rel = site.display(path);
        for (String ref : List.of("main", "origin/main")) {
            if (git.succeeds("cat-file", "-e", ref + ":" + rel)) {
                return true;
            }
        }
        return false;
    }

    /** Points every masthead=, Markdown image and src= at the renamed file. Returns the files changed. */
    List<Path> references(Path oldName, Path newName, boolean dryRun) throws IOException {
        String oldBase = oldName.getFileName().toString();
        String newBase = newName.getFileName().toString();
        String siteOld = "/" + SiteLayout.relativePath(site.assets(), oldName);
        String siteNew = "/" + SiteLayout.relativePath(site.assets(), newName);
        Pattern mastheadLine = Pattern.compile("(?m)^(masthead=)" + Pattern.quote(oldBase) + "$");
        List<Path> changed = new ArrayList<>();
        for (Path root : List.of(site.content(), site.templates())) {
            for (Path p : FileTrees.regularFilesUnder(root)) {
                String text;
                try {
                    // Strict on purpose, unlike Text.read: the text is written back, and a byte that can't be
                    // decoded must not come back as U+FFFD. A file that isn't UTF-8 text is left alone.
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

    /** One image. Returns false when it was skipped or refused (the log says why). */
    boolean prepare(Path path, PrepareImageOptions options) throws IOException, InterruptedException {
        String shown = site.display(path);
        if (!path.startsWith(site.images())) {
            out.println(shown + ": skipped, not under " + site.display(site.images()));
            return false;
        }
        String ext = extension(path);
        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png")) {
            out.println(shown + ": skipped, only JPEG and PNG are handled");
            return false;
        }
        if (!Files.isRegularFile(path)) {
            out.println(shown + ": no such file");
            return false;
        }
        boolean masthead = isMasthead(path);
        int width = options.widthCap() != null ? options.widthCap() : masthead ? ImageRules.MAX_WIDTH : INLINE_WIDTH;
        Images.Loaded src;
        try {
            src = images.load(path);
        } catch (IOException | RuntimeException e) {
            out.println(shown + ": couldn't read it (" + e.getMessage() + "); convert it by hand");
            return false;
        }
        BufferedImage im0 = src.image();
        out.println(shown + ": " + im0.getWidth() + "x" + im0.getHeight() + " " + src.mode() + " " + Files.size(path) / 1024 + "K"
                + (src.icc() ? ", colour profile" : "") + (src.exif() ? ", EXIF" : "") + (src.xmp() ? ", XMP" : "")
                + (masthead ? ", masthead" : "") + "; width cap " + width);
        BufferedImage im = images.toSrgb(src);
        Path finalPath = path;

        if (ext.equals(".jpg") || ext.equals(".jpeg")) {
            if (options.dryRun()) {
                out.println("  would re-encode in place as sRGB progressive JPEG, no metadata");
            } else {
                out.println("  re-encoded in place: " + Images.saveJpeg(im, path, width));
            }
        } else if (options.png() || (!options.jpeg() && !Images.looksLikePhoto(im))) {
            out.println("  a graphic (few colours or transparency): stays PNG");
            if (options.dryRun()) {
                out.println("  would resize losslessly and optimize in place");
            } else {
                out.println("  optimized in place: " + Images.savePng(im, path, width));
            }
        } else {
            finalPath = withExtension(path, ".jpg");
            if (published(path)) {
                out.println("  a photo, but this PNG is published (it is in main), so a PNG must stay at its URL (AGENTS.md Rule 1,"
                        + " option B) and this program has no colour quantizer for the fallback: convert it by hand or add"
                        + " the quantizer (D2 in docs/08-SCRIPTS-MIGRATION.md). Nothing changed.");
                return false;
            }
            out.println("  a photo: writing " + site.display(finalPath) + " (the PNG was never published, so it goes)");
            if (!options.dryRun()) {
                out.println("  wrote " + site.display(finalPath) + ": " + Images.saveJpeg(im, finalPath, width));
                if (options.keepSource()) {
                    out.println("  kept " + shown + " (-PkeepSource); it will be published as-is unless you remove it");
                } else {
                    Files.delete(path);
                    out.println("  removed " + shown);
                }
            }
            for (Path f : references(path, finalPath, options.dryRun())) {
                out.println("  " + (options.dryRun() ? "would point " : "pointed ") + site.display(f) + " at " + finalPath.getFileName());
            }
        }

        if (masthead) {
            variants.makeAll(finalPath, options.dryRun());
        }
        return true;
    }
}
