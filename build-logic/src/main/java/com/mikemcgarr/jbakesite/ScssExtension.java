package com.mikemcgarr.jbakesite;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * The {@code scss { ... }} block of {@code jbakeSite}: where the site's SCSS is and where its CSS is published
 * (T043, T134; in the plugin since T124).
 *
 * <p>Every {@code <name>.scss} directly under {@link #getSource() source} whose name doesn't start with {@code _} is
 * an <em>entry</em> ({@link SassEntries}): {@code compileSass} compiles it with Dart Sass, and the bake publishes it
 * at {@code /<publishedDir>/<name>.css}. The partials ({@code _*.scss}) are compiled only through the entries. None
 * of the CSS is committed; {@code checkNoHandEditedCss} fails the bake if a hand-written copy comes back under
 * JBake's assets ({@link CheckNoHandEditedCssTask}). The defaults are set by {@link JbakeSitePlugin}.
 */
public abstract class ScssExtension {

    /** Created by Gradle for the {@code jbakeSite} extension; configure it in {@code build.gradle}. */
    public ScssExtension() {
    }

    /**
     * The SCSS directory. Default {@code src/scss}.
     *
     * @return the directory
     */
    public abstract DirectoryProperty getSource();

    /**
     * Where the compiled CSS is published, relative to the site root: an entry {@code <name>.scss} becomes
     * {@code /<publishedDir>/<name>.css}. The same path under JBake's assets folder is the one the guard watches.
     * Default {@code css}.
     *
     * @return the directory, relative to the site root
     */
    public abstract Property<String> getPublishedDir();

    /**
     * Hand-written stylesheets that are gone from the site (folded into an entry, or replaced) and must not come
     * back under the assets' {@link #getPublishedDir() publishedDir} either: file names such as {@code extra.css}.
     * The entries' own {@code .css} names are always guarded. Default: none.
     *
     * @return the file names
     */
    public abstract ListProperty<String> getRetiredCss();

    /**
     * What the guard says after the offending file's path, in one sentence: where the CSS comes from now and what
     * each guarded name became. The plugin appends the fix ("Move any change into the SCSS, then delete this
     * file."). Default: a generic sentence naming the SCSS directory.
     *
     * @return the sentence
     */
    public abstract Property<String> getHandEditedCssExplanation();
}
