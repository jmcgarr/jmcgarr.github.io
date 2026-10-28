package com.mikemcgarr.jbakesite;

import com.sass_lang.embedded_protocol.OutboundMessage.VersionResponse;
import com.sass_lang.embedded_protocol.OutputStyle;
import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.SassCompilerFactory;
import de.larsgrefer.sass.embedded.logging.Slf4jLoggingHandler;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;

/**
 * Dart Sass, the reference Sass compiler, with the site's options (T043): expanded output, like the CSS the site
 * served before the SCSS, and no source map (no {@code .map} file and no {@code sourceMappingURL} comment in the
 * published CSS). One instance is one compiler process; {@link #close()} stops it.
 *
 * <p>The compiler runs through its Java host ({@code de.larsgrefer.sass:sass-embedded-host}). No Node or Ruby: the
 * host's jar bundles Dart Sass for macOS, Linux and Windows, unpacks the one for this machine into a temp folder on
 * first use, and talks to it over stdin/stdout. {@link CompileSassTask} and the preview's SCSS watcher both compile
 * through this class, so the preview's CSS is byte for byte what the bake publishes. T124 moved it here from
 * {@code build.gradle}, where the freefair Sass plugin's task and the watcher each set the same options.
 */
public final class SassCompiler implements AutoCloseable {

    private final de.larsgrefer.sass.embedded.SassCompiler host;

    private SassCompiler(de.larsgrefer.sass.embedded.SassCompiler host) {
        this.host = host;
    }

    /**
     * Starts a compiler process.
     *
     * @param log where Sass's {@code @warn}, {@code @debug} and deprecation messages go (a Gradle task's or build
     *        service's logger, which is an SLF4J logger too)
     * @return the compiler; close it when done
     * @throws IOException if Dart Sass can't be unpacked or started
     */
    public static SassCompiler start(Logger log) throws IOException {
        de.larsgrefer.sass.embedded.SassCompiler host = SassCompilerFactory.bundled();
        host.setOutputStyle(OutputStyle.EXPANDED);
        host.setGenerateSourceMaps(false);
        host.setLoggingHandler(new Slf4jLoggingHandler(log));
        return new SassCompiler(host);
    }

    /**
     * The compiler's name and version, such as {@code dart-sass 1.93.2}.
     *
     * @return the version line
     * @throws IOException if the compiler process can't be reached
     */
    public String version() throws IOException {
        VersionResponse version = host.getVersion();
        return version.getImplementationName() + " " + version.getImplementationVersion();
    }

    /**
     * Compiles an entry. {@code @use} and {@code @import} resolve relative to the entry's directory, so partials
     * next to it are found.
     *
     * @param entry the {@code .scss} file
     * @return the CSS, exactly as Dart Sass produced it (no trailing newline is added)
     * @throws SassCompileException for a Sass error, with Dart Sass's message
     * @throws IOException if the compiler process fails
     */
    public String compile(Path entry) throws SassCompileException, IOException {
        try {
            return host.compileFile(entry.toFile()).getCss();
        } catch (SassCompilationFailedException e) {
            throw new SassCompileException(e.getCompileFailure().getFormatted());
        }
    }

    /**
     * Compiles every entry in a directory ({@link SassEntries#in}), all of them before anything is returned, so an
     * error in one leaves the caller with nothing half done: {@code compileSass} writes the result, and the
     * preview's watcher swaps it in while keeping the last good CSS on an error.
     *
     * @param sourceDir the SCSS directory
     * @return each entry's CSS file name ({@link SassEntries#cssName}) to its CSS, in entry order
     * @throws SassCompileException for a Sass error in any entry, with Dart Sass's message
     * @throws IOException if the directory can't be read or the compiler process fails
     */
    public Map<String, String> compileAll(Path sourceDir) throws SassCompileException, IOException {
        Map<String, String> compiled = new LinkedHashMap<>();
        for (Path entry : SassEntries.in(sourceDir)) {
            compiled.put(SassEntries.cssName(entry), compile(entry));
        }
        return compiled;
    }

    /**
     * Compiles SCSS given as a string, with no file to resolve {@code @use} against. For tests, mostly.
     *
     * @param scss the SCSS source
     * @return the CSS
     * @throws SassCompileException for a Sass error, with Dart Sass's message
     * @throws IOException if the compiler process fails
     */
    public String compileString(String scss) throws SassCompileException, IOException {
        try {
            return host.compileScssString(scss).getCss();
        } catch (SassCompilationFailedException e) {
            throw new SassCompileException(e.getCompileFailure().getFormatted());
        }
    }

    /**
     * Stops the compiler process.
     *
     * @throws IOException if the process doesn't stop cleanly
     */
    @Override
    public void close() throws IOException {
        host.close();
    }
}
