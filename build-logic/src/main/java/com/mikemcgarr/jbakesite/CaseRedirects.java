package com.mikemcgarr.jbakesite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Redirect stubs whose names differ from a real page only by letter case, such as {@code tags/DevOps.html} next to
 * {@code tags/devops.html} (T055, made by {@code tagRedirects}: {@link com.mikemcgarr.jbakesite.writing.TagRedirects}). They can't be JBake assets: on a
 * case-insensitive filesystem (macOS) both names are one file, and JBake copies assets after rendering, so the stub
 * would replace the real page with a redirect to itself (reproduced on APFS). The bake adds them after JBake has
 * run, and only when its destination is case-sensitive, as on Linux, where CI builds and GitHub Pages serves. T125
 * moved this here from {@code build.gradle}, where the check was "does {@code INDEX.HTML} exist"; the probe below
 * doesn't depend on which pages the site renders.
 */
final class CaseRedirects {

    /** What happened to the stubs. */
    enum Outcome {
        /** The stubs directory is missing or empty: the site has none. */
        NONE,
        /** The destination is case-insensitive: the stubs were left out. */
        SKIPPED,
        /** The stubs were copied into the destination. */
        COPIED
    }

    private static final String PROBE_PREFIX = "case-probe-";

    private CaseRedirects() {
    }

    /**
     * Copies the stubs into the site when the site's filesystem tells {@code DevOps.html} from {@code devops.html}.
     *
     * @param stubs the stubs directory, usually {@code src/case-redirects}
     * @param site the baked site
     * @return what happened
     * @throws IOException if a file can't be read or written
     */
    static Outcome copy(Path stubs, Path site) throws IOException {
        if (!hasFiles(stubs)) {
            return Outcome.NONE;
        }
        if (!isCaseSensitive(site)) {
            return Outcome.SKIPPED;
        }
        FileTrees.copy(stubs, site);
        return Outcome.COPIED;
    }

    /**
     * Says whether a directory's filesystem tells names apart by case: a temporary file written there is looked up
     * under its name in capitals. The file is removed before this returns.
     *
     * @param directory an existing directory
     * @return true on a case-sensitive filesystem (Linux, usually), false on a case-insensitive one (macOS, Windows)
     * @throws IOException if the probe file can't be written
     */
    static boolean isCaseSensitive(Path directory) throws IOException {
        Path probe = Files.createTempFile(directory, PROBE_PREFIX, ".tmp");
        try {
            Path twin = directory.resolve(probe.getFileName().toString().toUpperCase(Locale.ROOT));
            return !Files.exists(twin);
        } finally {
            Files.deleteIfExists(probe);
        }
    }

    private static boolean hasFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            return files.anyMatch(Files::isRegularFile);
        }
    }
}
