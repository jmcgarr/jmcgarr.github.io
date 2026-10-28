package com.mikemcgarr.jbakesite;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Nested;

/**
 * The {@code jbakeSite { ... }} block of {@code build.gradle}: what the site is (where its sources are, what is
 * published where), as opposed to how it is built, which is the plugin's. Modelled on the JBake Gradle plugin's
 * {@code jbake { srcDirName; destDirName }}, typed as directories (docs/06-GRADLE-PLUGIN.md, "The public surface").
 * Gradle creates it with managed properties; {@link JbakeSitePlugin} sets the defaults. T123 created it empty;
 * each slice adds what it needs (T124: {@link #getScss() scss} and {@link #getSource() source}; T125:
 * {@link #getDestination() destination}, {@link #getCaseRedirects() caseRedirects},
 * {@link #getLogbackConfiguration() logbackConfiguration} and {@link #getClasspath() classpath}).
 */
public abstract class JbakeSiteExtension {

    /**
     * JBake's source directory: {@code content/}, {@code templates/}, {@code assets/} and {@code jbake.properties}.
     * Default {@code src/jbake}. The bake renders it ({@link BakeTask}), the preview serves it
     * ({@link BakePreviewTask}), and the CSS guard watches its assets folder ({@link CheckNoHandEditedCssTask}).
     *
     * @return the directory
     */
    public abstract DirectoryProperty getSource();

    /**
     * Where the bake writes the site, and what the preview serves. Default {@code build/jbake}. CI deploys this
     * directory minus the drafts.
     *
     * @return the directory
     */
    public abstract DirectoryProperty getDestination();

    /**
     * Redirect stubs whose names differ from a real page only by letter case, such as {@code tags/DevOps.html}
     * next to {@code tags/devops.html} (T055). The bake copies them into the site after JBake, and only when the
     * destination is on a case-sensitive filesystem ({@link CaseRedirects}). Default {@code src/case-redirects}; a
     * missing or empty directory means the site has none.
     *
     * @return the directory
     */
    public abstract DirectoryProperty getCaseRedirects();

    /**
     * A logback configuration for JBake's JVM, instead of the plugin's own ({@link JbakeTask}: JBake's summary,
     * every warning and error, and OrientDB's and the parser registry's known noise silenced). No default: unset,
     * the bundled one is used.
     *
     * @return the file
     */
    public abstract RegularFileProperty getLogbackConfiguration();

    /**
     * The theme CSS: the SCSS entries and where they are published (T124).
     *
     * @return the {@code scss { ... }} block
     */
    @Nested
    public abstract ScssExtension getScss();

    /**
     * Configures the theme CSS, as {@code scss { ... }} in {@code build.gradle}.
     *
     * @param action the configuration
     */
    public void scss(Action<? super ScssExtension> action) {
        action.execute(getScss());
    }

    /**
     * JBake's classpath, the {@code jbake} configuration: which libraries are left off it and which are replaced
     * (T125). The versions themselves are declared in {@code build.gradle}'s {@code dependencies} block.
     *
     * @return the {@code classpath { ... }} block
     */
    @Nested
    public abstract JbakeClasspathExtension getClasspath();

    /**
     * Configures JBake's classpath, as {@code classpath { ... }} in {@code build.gradle}.
     *
     * @param action the configuration
     */
    public void classpath(Action<? super JbakeClasspathExtension> action) {
        action.execute(getClasspath());
    }
}
