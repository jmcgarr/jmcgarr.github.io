package com.mikemcgarr.jbakesite;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;

/**
 * The {@code classpath { ... }} block of {@code jbakeSite}: what the plugin does to the {@code jbake} configuration
 * beyond the versions {@code build.gradle} declares on it (the settings are T112's). The defaults
 * are {@link JbakeClasspath}'s; a site that uses one of the excluded engines, or wants a library back as JBake
 * asks for it, overrides them here.
 */
public abstract class JbakeClasspathExtension {

    /**
     * Maven groups left off JBake's classpath. jbake-core's Gradle module metadata makes every template engine and
     * content parser a plain runtime dependency, and JBake only loads an engine whose classes are present, so a
     * site lists the groups of the engines it doesn't use, together with their libraries, some of which have
     * advisories. Default: {@link JbakeClasspath#DEFAULT_EXCLUDED_GROUPS} (Thymeleaf, Pebble, Jade4j, the Groovy
     * templates and AsciidoctorJ), which leaves FreeMarker and Markdown.
     *
     * @return the groups
     */
    public abstract ListProperty<String> getExcludedGroups();

    /**
     * Modules replaced on JBake's classpath, as {@code group:name} to the {@code group:name:version} that stands
     * in for it: a maintained fork with the same classes, for a library JBake depends on that is no longer kept
     * up. Default: {@link JbakeClasspath#DEFAULT_SUBSTITUTIONS} ({@code org.lz4:lz4-java} to its fork
     * {@code at.yawk.lz4:lz4-java}).
     *
     * @return the substitutions
     */
    public abstract MapProperty<String, String> getSubstitutions();
}
