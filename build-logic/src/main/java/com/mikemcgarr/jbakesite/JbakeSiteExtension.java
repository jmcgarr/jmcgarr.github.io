package com.mikemcgarr.jbakesite;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Nested;

/**
 * The {@code jbakeSite { ... }} block of {@code build.gradle}: what the site is (where its sources are, what is
 * published where), as opposed to how it is built, which is the plugin's. Modelled on the JBake Gradle plugin's
 * {@code jbake { srcDirName; destDirName }}, typed as directories (docs/06-GRADLE-PLUGIN.md, "The public surface").
 * Gradle creates it with managed properties; {@link JbakeSitePlugin} sets the defaults. T123 created it empty;
 * each slice adds what it needs (T124: {@link #getScss() scss} and {@link #getSource() source}; T125:
 * {@link #getDestination() destination}, {@link #getCaseRedirects() caseRedirects},
 * {@link #getLogbackConfiguration() logbackConfiguration} and {@link #getClasspath() classpath}; T127:
 * {@link #getPublishedUrls() publishedUrls} and {@link #getTagRenames() tagRenames}; T141:
 * {@link #getExpectedRemovals() expectedRemovals}; T121: {@link #getCommitTrailers() commitTrailers}).
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
     * The baseline of the site's published URLs, one site-relative path per line ({@code blog/alm.html}), a snapshot
     * of the live site that only changes at a milestone checkpoint (AGENTS.md Rule 1). The writing tools read it:
     * {@code newPost} refuses a slug that was once published, the review accepts a link to a URL in it that only a
     * Linux bake has (the case-redirect stubs), and the tag redirects are derived from its tag URLs. Default
     * {@code docs/baseline/urls.txt}.
     *
     * @return the file
     */
    public abstract RegularFileProperty getPublishedUrls();

    /**
     * The published URLs removed on purpose, one site-relative path per line with {@code #} comments, each with the
     * owner's approval (AGENTS.md Rule 1). {@code checkUrls} accepts a baseline URL that is missing from the bake
     * only when it is listed here. Default {@code docs/baseline/expected-removals.txt}; a missing file means none.
     *
     * @return the file
     */
    public abstract RegularFileProperty getExpectedRemovals();

    /**
     * Trailer lines {@code openPr} appends to the commit it makes from uncommitted work ({@code -Pmessage}), after a
     * blank line, such as {@code Co-Authored-By: Name <email>}; the commit message is the author's own otherwise.
     * Default: none.
     *
     * @return the lines
     */
    public abstract ListProperty<String> getCommitTrailers();

    /**
     * Tags merged into another tag or renamed on purpose, old to new, both as written in the posts' {@code tags=}
     * lines: every old URL of the old tag gets a redirect stub to the new tag's page ({@code tagRedirects},
     * {@code checkTagRedirects}). Default: this site's owner-approved list of 2026-10-09
     * ({@code com.mikemcgarr.jbakesite.writing.TagRedirects#RENAMES}); a site with none sets {@code tagRenames = [:]}.
     * Never remove an entry: its stubs are published URLs (Rule 1).
     *
     * @return the renames
     */
    public abstract MapProperty<String, String> getTagRenames();

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
