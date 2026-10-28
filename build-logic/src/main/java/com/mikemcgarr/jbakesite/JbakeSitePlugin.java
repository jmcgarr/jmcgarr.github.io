package com.mikemcgarr.jbakesite;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * The {@code jbake-site} plugin: how the site is built, as Java classes with tests, applied by the root
 * {@code build.gradle} and configured through the {@link JbakeSiteExtension} ({@code jbakeSite { ... }}).
 *
 * <p>The build logic moves in from {@code build.gradle} one slice at a time (docs/06-GRADLE-PLUGIN.md, M10); this
 * class registers each slice's tasks with the names the site has used since 2014. T123 created it.
 */
public final class JbakeSitePlugin implements Plugin<Project> {

    /** The name of the extension, {@code jbakeSite}, as it reads in {@code build.gradle}. */
    public static final String EXTENSION_NAME = "jbakeSite";

    @Override
    public void apply(Project project) {
        project.getExtensions().create(EXTENSION_NAME, JbakeSiteExtension.class);
    }
}
