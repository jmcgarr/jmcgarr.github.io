package com.mikemcgarr.jbakesite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * The convention for which SCSS files are compiled (T043, T134): every {@code <name>.scss} directly in the SCSS
 * directory whose name doesn't start with {@code _} is an entry, compiled to {@code <name>.css}. Partials
 * ({@code _variables.scss}, {@code _mixins.scss}, ...) are only compiled through the entries that use them, and
 * so are files in subdirectories. {@link CompileSassTask} and the preview's SCSS watcher share this, so both
 * compile the same files.
 */
public final class SassEntries {

    private static final String ENTRY_SUFFIX = ".scss";
    private static final String CSS_SUFFIX = ".css";
    private static final char PARTIAL_PREFIX = '_';

    private SassEntries() {
    }

    /**
     * Lists the entries in a directory, sorted by name.
     *
     * @param source the SCSS directory
     * @return the entries, as paths under {@code source}; empty when the directory doesn't exist (the preview's
     *         watcher and tests can see that; the tasks' {@code @InputDirectory} validation fails first)
     * @throws IOException if the directory can't be read
     */
    public static List<Path> in(Path source) throws IOException {
        if (!Files.isDirectory(source)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(source)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> isEntry(file.getFileName().toString()))
                    .sorted()
                    .toList();
        }
    }

    /**
     * Says whether a file name is an entry's: {@code <name>.scss} with a name that doesn't start with {@code _}.
     *
     * @param fileName the file name, without a directory
     * @return true for an entry, false for a partial or another kind of file
     */
    public static boolean isEntry(String fileName) {
        return fileName.endsWith(ENTRY_SUFFIX)
                && fileName.length() > ENTRY_SUFFIX.length()
                && fileName.charAt(0) != PARTIAL_PREFIX;
    }

    /**
     * The name of the CSS file an entry compiles to: {@code clean-blog.scss} gives {@code clean-blog.css}.
     *
     * @param entry an entry
     * @return its CSS file name
     */
    public static String cssName(Path entry) {
        String name = entry.getFileName().toString();
        return name.substring(0, name.length() - ENTRY_SUFFIX.length()) + CSS_SUFFIX;
    }
}
