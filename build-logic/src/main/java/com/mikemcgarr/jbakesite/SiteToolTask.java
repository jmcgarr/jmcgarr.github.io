package com.mikemcgarr.jbakesite;

import com.mikemcgarr.jbakesite.internal.JbakeProperties;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

/**
 * What the writing and verification tasks share (T127): where the site's sources are, as the {@code jbakeSite}
 * extension says ({@link JbakeSitePlugin} wires every subclass), handed to the tool as a {@link SiteLayout}; the
 * site's published address from {@code jbake.properties}; the reading of a {@code -P} flag ({@link #flag}); and the
 * mapping of a tool's {@link ToolFailure} (the message the single-file program printed before exiting) to a build
 * failure with that message.
 *
 * <p>The directories are {@link Internal}: the tools validate them themselves, with the programs' messages, and
 * most of these tasks are untracked or have no outputs, so they run every time anyway.
 */
public abstract class SiteToolTask extends DefaultTask {

    private static final String JBAKE_PROPERTIES = "jbake.properties";

    /**
     * JBake's source directory: {@code content/}, {@code templates/}, {@code assets/} and {@code jbake.properties}.
     *
     * @return the directory
     */
    @Internal
    public abstract DirectoryProperty getSource();

    /**
     * The case-redirect stubs ({@link JbakeSiteExtension#getCaseRedirects()}).
     *
     * @return the directory
     */
    @Internal
    public abstract DirectoryProperty getCaseRedirects();

    /**
     * The baseline of published URLs ({@link JbakeSiteExtension#getPublishedUrls()}).
     *
     * @return the file
     */
    @Internal
    public abstract RegularFileProperty getPublishedUrls();

    /**
     * The project's layout: the project directory is the repository root the tools run in.
     *
     * @return the layout
     */
    @Inject
    protected abstract ProjectLayout getLayout();

    /**
     * The project directory.
     *
     * @return the directory, absolute
     */
    protected final Path projectDirectory() {
        return getLayout().getProjectDirectory().getAsFile().toPath().toAbsolutePath();
    }

    /**
     * The site's layout, from the task's properties.
     *
     * @return the layout
     */
    protected final SiteLayout siteLayout() {
        return new SiteLayout(projectDirectory(), getSource().get().getAsFile().toPath().toAbsolutePath(),
                getCaseRedirects().get().getAsFile().toPath().toAbsolutePath(),
                getPublishedUrls().get().getAsFile().toPath().toAbsolutePath());
    }

    /**
     * The site's published address, {@code site.host} in {@code jbake.properties}, without a trailing slash.
     *
     * @param site the layout
     * @return the address
     * @throws IOException if the file can't be read
     */
    protected final String siteHost(SiteLayout site) throws IOException {
        Path jbakeProperties = site.source().resolve(JBAKE_PROPERTIES);
        return JbakeProperties.siteHost(jbakeProperties, site.display(jbakeProperties));
    }

    /**
     * Runs a tool, turning its {@link ToolFailure} into a build failure with the same message.
     *
     * @param tool the tool's work
     * @throws IOException if the tool can't read or write a file
     * @throws InterruptedException if the tool was interrupted while waiting for a command
     * @throws GradleException with the tool's message when it fails
     */
    protected final void runTool(Tool tool) throws IOException, InterruptedException {
        try {
            tool.run();
        } catch (ToolFailure e) {
            throw new GradleException(e.getMessage(), e);
        }
    }

    /**
     * Reads a {@code -P} flag: on when given without a value ({@code -Pstrict}) or as {@code true}, off as
     * {@code false} or when not given; anything else is a usage error.
     *
     * @param property the task property the Gradle property is wired to
     * @param name the property's name, for the message
     * @return whether the flag is on
     * @throws ToolFailure exit 2 for any other value
     */
    protected static boolean flag(Property<String> property, String name) {
        if (!property.isPresent()) {
            return false;
        }
        return switch (property.get().strip().toLowerCase(Locale.ROOT)) {
            case "", "true" -> true;
            case "false" -> false;
            default -> throw ToolFailure.couldNotRun("-P" + name + " takes no value (or true/false), not '" + property.get() + "'.");
        };
    }

    /** A tool's work, for {@link #runTool}. */
    @FunctionalInterface
    protected interface Tool {

        /**
         * Does the work.
         *
         * @throws IOException if a file can't be read or written
         * @throws InterruptedException if interrupted while waiting for a command
         * @throws ToolFailure when the tool stops with a message
         */
        void run() throws IOException, InterruptedException;
    }
}
