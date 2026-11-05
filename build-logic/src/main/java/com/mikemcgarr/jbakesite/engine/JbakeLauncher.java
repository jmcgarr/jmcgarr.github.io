package com.mikemcgarr.jbakesite.engine;

import org.jbake.launcher.Main;
import org.jbake.parser.Engines;

/**
 * JBake's command-line launcher with the site's parser engine in place: registers {@link FrontMatterEngine} for
 * {@code .md} files, replacing JBake's Markdown engine (which it extends), and hands the arguments to
 * {@link Main}. JBake's registry warns about the replacement at startup; the plugin's {@code jbake-logback.xml}
 * keeps that logger at ERROR. {@code JbakeTask} names this class as the main class of the fresh JVM the bake and
 * the preview run.
 */
public final class JbakeLauncher {

    /** The extension JBake's own descriptor maps to its Markdown engine. */
    static final String MARKDOWN_EXTENSION = "md";

    private JbakeLauncher() {
    }

    /**
     * Registers the engine and runs JBake: {@code jbake <source> <destination> <options...>}.
     *
     * @param args JBake's own arguments
     */
    public static void main(String[] args) {
        Engines.register(MARKDOWN_EXTENSION, new FrontMatterEngine());
        Main.main(args);
    }
}
