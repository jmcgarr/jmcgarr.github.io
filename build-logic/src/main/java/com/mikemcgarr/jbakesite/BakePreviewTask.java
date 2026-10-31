package com.mikemcgarr.jbakesite;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * {@code bakePreview}, the local preview (the JBake Gradle plugin's task name since 2014): {@code jbake -b -s}
 * bakes into the destination, serves it
 * with Jetty at the address in {@code jbake.properties} (127.0.0.1 only; {@link PreviewPort}), drafts included,
 * and watches content, templates and assets: a save is live in about 1 s (it polls every second, then rebakes
 * everything, or copies a changed asset). In this mode JBake sets {@code site.host} to the local address, so feed,
 * sitemap and share links stay local. Unlike the JDK's jwebserver (used before), Jetty serves dot-file paths such
 * as {@code /tags/.NET.html}. One JVM serves the whole session; Ctrl+C stops it. Restart after changing
 * {@code jbake.properties}, or after deleting or renaming a source file (its old page stays in the destination
 * until {@code clean}). The next {@code bake} notices the changed output and rebakes with the real
 * {@code site.host}, without the live-reload script.
 *
 * <p>Before JBake starts: the port check (T101), the compiled CSS put in place (JBake never deletes it), and the
 * SCSS watcher ({@link SassPreviewWatcher}) started, which keeps the CSS current while the preview runs. Live
 * reload (T064): JBake's JVM gets {@code preview.livereload=true}, the templates see it as
 * {@code config.preview_livereload}, and {@code header.ftl} then adds a small script
 * ({@code templates/preview-reload.ftl}) that reloads the page after each rebuild. Only this task sets it, so
 * {@code bake} never outputs the script, and CI fails if it ever reaches the deploy contents.
 *
 * <p>The task has no outputs, so it runs every time.
 */
public abstract class BakePreviewTask extends JbakeTask {

    /** The system property the live-reload script is switched on with. */
    static final String LIVE_RELOAD_PROPERTY = "preview.livereload";

    private static final String JBAKE_PROPERTIES = "jbake.properties";

    /**
     * Where the preview bakes and what it serves; the same directory as the bake's.
     *
     * @return the directory
     */
    @Internal
    public abstract DirectoryProperty getDestination();

    /**
     * The SCSS watcher, a shared build service the plugin registers; the task declares that it uses it.
     *
     * @return the service
     */
    @Internal
    public abstract Property<SassPreviewWatcher> getScssWatcher();

    /**
     * Checks the port, puts the CSS in place, starts the watcher, and runs JBake's server until Ctrl+C.
     *
     * @throws IOException if a file can't be copied, or the port check can't open a socket
     */
    @TaskAction
    public void serve() throws IOException {
        Path source = getSource().get().getAsFile().toPath();
        Path destination = getDestination().get().getAsFile().toPath();
        Path jbakeProperties = source.resolve(JBAKE_PROPERTIES);

        String jbakePropertiesName = projectRelative(jbakeProperties.toFile());
        InetSocketAddress address = PreviewPort.read(jbakeProperties, jbakePropertiesName);
        PreviewPort.checkFree(address, jbakePropertiesName);

        copyThemeCssInto(destination);   // before JBake starts; its server never deletes it
        getScssWatcher().get().start();

        runJbake(destination, List.of("-b", "-s"), Map.of(LIVE_RELOAD_PROPERTY, "true"));
    }
}
