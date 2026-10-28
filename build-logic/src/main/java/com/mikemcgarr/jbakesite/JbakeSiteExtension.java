package com.mikemcgarr.jbakesite;

/**
 * The {@code jbakeSite { ... }} block of {@code build.gradle}: what the site is (where its sources are, what is
 * published where), as opposed to how it is built, which is the plugin's. Modelled on the JBake Gradle plugin's
 * {@code jbake { srcDirName; destDirName }}, typed as directories. Gradle creates it with managed properties
 * (docs/06-GRADLE-PLUGIN.md, "The public surface"). T123 created it empty; each slice adds what it needs.
 */
public abstract class JbakeSiteExtension {
}
