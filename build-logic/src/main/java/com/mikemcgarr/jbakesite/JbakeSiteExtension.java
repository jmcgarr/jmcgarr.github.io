package com.mikemcgarr.jbakesite;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Nested;

/**
 * The {@code jbakeSite { ... }} block of {@code build.gradle}: what the site is (where its sources are, what is
 * published where), as opposed to how it is built, which is the plugin's. Modelled on the JBake Gradle plugin's
 * {@code jbake { srcDirName; destDirName }}, typed as directories (docs/06-GRADLE-PLUGIN.md, "The public surface").
 * Gradle creates it with managed properties; {@link JbakeSitePlugin} sets the defaults. T123 created it empty;
 * each slice adds what it needs (T124: {@link #getScss() scss} and {@link #getSource() source}).
 */
public abstract class JbakeSiteExtension {

    /**
     * JBake's source directory: {@code content/}, {@code templates/}, {@code assets/} and {@code jbake.properties}.
     * Default {@code src/jbake}. T124 uses it to find the assets folder the CSS guard watches; T125 will bake it.
     *
     * @return the directory
     */
    public abstract DirectoryProperty getSource();

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
}
