package com.mikemcgarr.jbakesite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * The preview's SCSS watcher (T043, T134; a Java build service since T126): while {@code bakePreview} runs, it
 * recompiles the CSS straight into the served site whenever a file in the SCSS directory changes (it checks every
 * 0.5 s). Every entry is recompiled, since a partial such as {@code _mixins.scss} can feed any of them. JBake's own
 * watcher only covers its source directory. The page's live reload notices the new CSS and reloads.
 *
 * <p>All entries are compiled before anything is written ({@link SassCompiler#compileAll}), each into a temporary
 * file that is moved into place atomically, so a Sass error is printed and the last good CSS stays, for every
 * entry, and the browser never reads a half-written file. The entries and the compiler are the ones
 * {@code compileSass} uses ({@link SassEntries}, {@link SassCompiler}), so the preview's CSS is the bake's. Gradle
 * closes the service, and the Dart Sass process with it, when the preview stops.
 */
public abstract class SassPreviewWatcher implements BuildService<SassPreviewWatcher.Params>, AutoCloseable {

    /** What the watcher needs to know, set by {@link JbakeSitePlugin} from the extension. */
    public interface Params extends BuildServiceParameters {

        /**
         * The SCSS directory to watch and compile.
         *
         * @return the directory
         */
        DirectoryProperty getSourceDir();

        /**
         * Where the CSS goes: the published CSS directory inside the served site.
         *
         * @return the directory
         */
        DirectoryProperty getTargetDir();

        /**
         * The published CSS directory, for the log lines: an entry is published at {@code /<publishedDir>/<name>.css}.
         *
         * @return the directory, relative to the site root
         */
        Property<String> getPublishedDir();

        /**
         * The SCSS directory as the log lines name it, such as {@code src/scss}.
         *
         * @return the name
         */
        Property<String> getSourceName();
    }

    private static final Logger LOG = Logging.getLogger("sass");
    private static final long POLL_MILLIS = 500;
    private static final long STOP_MILLIS = 5000;

    private volatile Thread thread;
    private volatile boolean stopping;

    /**
     * Starts watching, on a daemon thread of its own. The preview task calls this before JBake's server starts.
     *
     * @throws IllegalStateException if the watcher is already running: one preview per build
     */
    public synchronized void start() {
        if (thread != null) {
            throw new IllegalStateException("The SCSS watcher is already running; one bakePreview per build.");
        }
        Path sourceDir = getParameters().getSourceDir().get().getAsFile().toPath();
        Path targetDir = getParameters().getTargetDir().get().getAsFile().toPath();
        thread = new Thread(() -> watch(sourceDir, targetDir), "scss-preview-watcher");
        thread.setDaemon(true);
        thread.start();
    }

    private void watch(Path sourceDir, Path targetDir) {
        String sourceName = getParameters().getSourceName().get();
        SassCompiler compiler = null;
        try {
            String seen = stamp(sourceDir);
            LOG.lifecycle("Watching {}: a saved change recompiles every entry ({}).", sourceName,
                    SassEntries.in(sourceDir).stream().map(entry -> url(SassEntries.cssName(entry))).collect(Collectors.joining(", ")));
            while (!stopping) {
                Thread.sleep(POLL_MILLIS);
                String now = stamp(sourceDir);
                if (now.equals(seen)) {
                    continue;
                }
                seen = now;
                try {
                    if (compiler == null) {
                        compiler = SassCompiler.start(LOG);
                    }
                    // Every entry is compiled before anything is written: an error in one leaves all as they were
                    Map<String, String> compiled = compiler.compileAll(sourceDir);
                    swapIn(compiled, targetDir);
                    LOG.lifecycle("{} changed: recompiled {}", sourceName,
                            compiled.keySet().stream().map(this::url).collect(Collectors.joining(", ")));
                } catch (SassCompileException e) {
                    LOG.error("{} changed, but Sass failed (the preview keeps the last good CSS):\n{}", sourceName, e.getMessage());
                }
            }
        } catch (InterruptedException ignored) {
            // the preview stopped
        } catch (IOException | RuntimeException e) {
            LOG.error("The {} watcher stopped: {}", sourceName, e.toString());
        } finally {
            if (compiler != null) {
                try {
                    compiler.close();
                } catch (IOException e) {
                    LOG.debug("Dart Sass didn't stop cleanly: {}", e.toString());
                }
            }
        }
    }

    private static void swapIn(Map<String, String> compiled, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        for (Map.Entry<String, String> css : compiled.entrySet()) {
            Path temp = Files.createTempFile(targetDir, css.getKey(), ".tmp");
            Files.writeString(temp, css.getValue());
            Files.move(temp, targetDir.resolve(css.getKey()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
    }

    /** Every file's path, modification time and size: a change to any of them changes the stamp. */
    private static String stamp(Path sourceDir) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            List<Path> files = paths.filter(Files::isRegularFile).sorted().toList();
            StringBuilder stamp = new StringBuilder();
            for (Path file : files) {
                stamp.append(file).append(' ').append(Files.getLastModifiedTime(file).toMillis()).append(' ')
                        .append(Files.size(file)).append('\n');
            }
            return stamp.toString();
        }
    }

    private String url(String cssName) {
        return "/" + getParameters().getPublishedDir().get() + "/" + cssName;
    }

    /** Stops the watcher thread and waits for it, briefly. */
    @Override
    public void close() {
        stopping = true;
        Thread watcher = thread;
        if (watcher == null) {
            return;
        }
        watcher.interrupt();
        try {
            watcher.join(STOP_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
