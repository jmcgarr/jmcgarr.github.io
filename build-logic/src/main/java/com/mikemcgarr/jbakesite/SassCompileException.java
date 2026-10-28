package com.mikemcgarr.jbakesite;

/**
 * A Sass error in an entry or one of its partials. The message is Dart Sass's own: the error, the line with a
 * caret under the place, and the {@code @use} trace to the entry, so it names the file and the line to fix.
 * {@link CompileSassTask} fails the build with it; the preview's SCSS watcher prints it and keeps the last good CSS.
 */
public final class SassCompileException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param formatted Dart Sass's formatted message
     */
    SassCompileException(String formatted) {
        super(formatted.stripTrailing());
    }
}
