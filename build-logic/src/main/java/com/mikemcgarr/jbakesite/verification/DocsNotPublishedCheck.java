package com.mikemcgarr.jbakesite.verification;

import com.mikemcgarr.jbakesite.FileTrees;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fails if anything from {@code docs/} (planning and internal docs) has leaked into the baked site (AGENTS.md Rule
 * 4; revival task T004): a {@code docs} entry at the site
 * root, a Markdown file anywhere but under {@code vendor/} (third-party packages ship their own README.md), or the
 * marker word {@code REVIVAL} from the planning docs in a text file. The messages are the shell script's; what it
 * wrote to standard error is the {@link ToolFailure}'s message.
 */
final class DocsNotPublishedCheck {

    /** The word the planning docs carry ({@code docs/archive/00-REVIVAL.md}) and no page should. */
    static final String MARKER = "REVIVAL";

    /** Third-party packages under the site root, skipped by the Markdown search (not by the marker search). */
    private static final String VENDOR = "vendor";

    /** How much of a file is read to decide whether it is binary, as {@code grep -I} does: a NUL byte means binary. */
    private static final int BINARY_PROBE_BYTES = 32 * 1024;

    private final SiteLayout site;
    private final PrintStream out;

    /**
     * Creates the check.
     *
     * @param site the site's layout, for the paths in the messages
     * @param out where the findings and the OK line go
     */
    DocsNotPublishedCheck(SiteLayout site, PrintStream out) {
        this.site = site;
        this.out = out;
    }

    /**
     * Checks a baked site.
     *
     * @param bakedSite the site, {@code build/jbake} or CI's staged {@code build/site}
     * @throws IOException if a file can't be read
     * @throws ToolFailure exit 2 without a site; exit 1 with the Rule 4 sentence when anything was found, after the
     *         findings are listed
     */
    void check(Path bakedSite) throws IOException {
        String shownSite = site.display(bakedSite);
        if (!Files.isDirectory(bakedSite)) {
            throw ToolFailure.couldNotRun("ERROR: site directory '" + shownSite + "' not found. Run ./gradlew bake first.");
        }
        boolean failed = false;
        if (Files.exists(bakedSite.resolve("docs"))) {
            out.println("FAIL: '" + shownSite + "/docs' exists.");
            failed = true;
        }
        List<Path> files = FileTrees.regularFilesUnder(bakedSite);
        List<String> markdown = new ArrayList<>();
        List<String> marked = new ArrayList<>();
        Path vendor = bakedSite.resolve(VENDOR);
        for (Path file : files) {
            if (file.getFileName().toString().endsWith(".md") && !file.startsWith(vendor)) {
                markdown.add(site.display(file));
            }
            if (containsMarker(file)) {
                marked.add(site.display(file));
            }
        }
        if (!markdown.isEmpty()) {
            out.println("FAIL: Markdown files found in the site:");
            markdown.forEach(file -> out.println("  " + file));
            failed = true;
        }
        if (!marked.isEmpty()) {
            out.println("FAIL: planning-doc marker '" + MARKER + "' found in:");
            marked.forEach(file -> out.println("  " + file));
            failed = true;
        }
        if (failed) {
            throw ToolFailure.failed("docs/ content must never be published. See AGENTS.md (Rule 4).");
        }
        out.println("OK: no docs/ content in " + shownSite);
    }

    /**
     * Whether a text file contains the marker. A binary file (a NUL byte in its first 32 KiB: images, fonts) never
     * does, as {@code grep -I} treats it.
     *
     * @param file the file
     * @return true for a text file with the marker
     * @throws IOException if the file can't be read
     */
    static boolean containsMarker(Path file) throws IOException {
        byte[] bytes;
        try (InputStream in = Files.newInputStream(file)) {
            byte[] head = in.readNBytes(BINARY_PROBE_BYTES);
            for (byte b : head) {
                if (b == 0) {
                    return false;
                }
            }
            byte[] rest = in.readAllBytes();
            bytes = new byte[head.length + rest.length];
            System.arraycopy(head, 0, bytes, 0, head.length);
            System.arraycopy(rest, 0, bytes, head.length, rest.length);
        }
        // Byte for byte, as grep searches: ISO-8859-1 maps every byte to one char, so no decoding can fail or hide a match
        return new String(bytes, StandardCharsets.ISO_8859_1).contains(MARKER);
    }
}
