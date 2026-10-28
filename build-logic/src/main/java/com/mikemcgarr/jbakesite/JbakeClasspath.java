package com.mikemcgarr.jbakesite;

import java.util.List;
import java.util.Map;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencySubstitutions;
import org.gradle.api.artifacts.component.ModuleComponentSelector;

/**
 * The {@code jbake} configuration: JBake and its libraries, the classpath the bake and the preview run in a fresh
 * JVM. The name is the JBake Gradle plugin's, in use here since 2014; since T096 JBake's own launcher runs on it
 * directly, since T133 it is the only configuration, and T125 moved its setup here from {@code build.gradle}. The
 * versions stay in {@code build.gradle}'s {@code dependencies { jbake ... } } block, with the advisory comments
 * that explain each pin; this class holds what is about <em>how JBake is run</em>, overridable through
 * {@code jbakeSite { classpath { ... } }} ({@link JbakeClasspathExtension}):
 *
 * <ul>
 * <li><b>Excluded engines.</b> jbake-core 2.7.0 brings every template engine and content parser JBake supports.
 * This site only uses FreeMarker ({@code *.ftl}) and Markdown ({@code *.md}, flexmark), so the others and their
 * libraries, some of which have advisories (T112), are left out: Thymeleaf ({@code *.thyme}, with ognl, javassist
 * and attoparser), Pebble ({@code *.peb}), Jade4j ({@code *.jade}, with commons-jexl), the Groovy templates
 * ({@code *.gsp}, {@code *.tpl}: groovy, -templates, -xml, -dateutil; Groovy is used by nothing else) and
 * AsciidoctorJ ({@code *.adoc}; it left with T102, 2026-10-18, when every post and page became Markdown, taking
 * JRuby, its native jffi/jnr libraries, ASM, jcodings and joni with it). JBake only loads an engine whose classes
 * are on the classpath, and skips the rest.</li>
 * <li><b>The lz4 substitution.</b> OrientDB (JBake's content store) depends on {@code org.lz4:lz4-java}, which is
 * no longer maintained and has advisories. Its maintained fork, {@code at.yawk.lz4:lz4-java}, has the same
 * {@code net.jpountz} classes, so it is a drop-in replacement (T112).</li>
 * </ul>
 *
 * <p>Both are applied when the configuration resolves, so {@code build.gradle} can change the extension's lists
 * after the plugin is applied. More exclusions can also be added the Gradle way, with
 * {@code configurations.jbake { exclude group: '...' }}.
 */
public final class JbakeClasspath {

    /** The configuration's name, {@code jbake}: the JBake Gradle plugin's name for JBake's classpath. */
    public static final String CONFIGURATION_NAME = "jbake";

    /** The groups left off the classpath by default: the engines this site doesn't use. */
    public static final List<String> DEFAULT_EXCLUDED_GROUPS = List.of(
            "org.thymeleaf",         // Thymeleaf (*.thyme), with ognl, javassist, attoparser
            "io.pebbletemplates",    // Pebble (*.peb)
            "de.neuland-bfi",        // Jade4j (*.jade), with commons-jexl
            "org.codehaus.groovy",   // Groovy templates (*.gsp, *.tpl): groovy, -templates, -xml, -dateutil
            "org.asciidoctor");      // AsciidoctorJ, with JRuby and its 20-odd libraries (gone since T102)

    /** The modules replaced by default: lz4-java by its maintained fork (T112). */
    public static final Map<String, String> DEFAULT_SUBSTITUTIONS = Map.of("org.lz4:lz4-java", "at.yawk.lz4:lz4-java:1.12.0");

    private static final String SUBSTITUTION_REASON = "unmaintained (T112); replaced through jbakeSite.classpath.substitutions";

    private JbakeClasspath() {
    }

    /**
     * Creates the {@code jbake} configuration, with the exclusions and substitutions the settings name applied as
     * it resolves. Dependencies are declared against it in {@code build.gradle}; it is never consumed by another
     * project.
     *
     * @param configurations the project's configurations
     * @param settings the {@code classpath { ... }} block, read when the configuration resolves
     * @return the configuration
     */
    public static Configuration create(ConfigurationContainer configurations, JbakeClasspathExtension settings) {
        Configuration jbake = configurations.create(CONFIGURATION_NAME);
        jbake.setDescription("JBake and its libraries: the classpath the bake and the preview run "
                + "(versions pinned in build.gradle's dependencies block).");
        jbake.setCanBeConsumed(false);
        jbake.withDependencies(dependencies -> {
            for (String group : settings.getExcludedGroups().get()) {
                jbake.exclude(Map.of("group", group));
            }
        });
        jbake.getResolutionStrategy().dependencySubstitution(substitutions -> substitute(substitutions, settings));
        return jbake;
    }

    private static void substitute(DependencySubstitutions substitutions, JbakeClasspathExtension settings) {
        substitutions.all(details -> {
            if (!(details.getRequested() instanceof ModuleComponentSelector requested)) {
                return;
            }
            String replacement = settings.getSubstitutions().get().get(requested.getGroup() + ":" + requested.getModule());
            if (replacement != null) {
                details.useTarget(substitutions.module(replacement), SUBSTITUTION_REASON);
            }
        });
    }
}
