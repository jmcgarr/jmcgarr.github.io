package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The {@code jbake} configuration as the plugin creates it. Resolving it empty (no repositories needed) runs the
 * dependency actions that apply the exclusions; the substitution needs real modules and is covered by the
 * functional test's dependency report.
 */
class JbakeClasspathTest {

    private Project project;
    private Configuration jbake;

    @BeforeEach
    void applyPlugin() {
        project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("jbake-site");
        jbake = project.getConfigurations().getByName(JbakeClasspath.CONFIGURATION_NAME);
    }

    @Test
    void isDeclaredAgainstAndResolvedButNotConsumed() {
        assertTrue(jbake.isCanBeResolved(), "the bake's classpath, and `gradlew dependencies --configuration jbake`");
        assertFalse(jbake.isCanBeConsumed(), "no other project takes JBake's classpath from this one");
        assertTrue(jbake.getDescription().startsWith("JBake and its libraries"));
    }

    @Test
    void excludesTheUnusedEnginesByDefaultWhenItResolves() {
        assertTrue(jbake.getExcludeRules().isEmpty(), "applied as it resolves, so build.gradle can still change the list");

        jbake.resolve();

        assertEquals(Set.copyOf(JbakeClasspath.DEFAULT_EXCLUDED_GROUPS), excludedGroups());
    }

    @Test
    void theExtensionOverridesTheExclusions() {
        project.getExtensions().getByType(JbakeSiteExtension.class)
                .classpath(classpath -> classpath.getExcludedGroups().set(List.of("org.asciidoctor")));

        jbake.resolve();

        assertEquals(Set.of("org.asciidoctor"), excludedGroups(), "a site that uses Groovy templates can have them back");
    }

    private Set<String> excludedGroups() {
        return jbake.getExcludeRules().stream().map(ExcludeRule::getGroup).collect(Collectors.toSet());
    }
}
