package com.mikemcgarr.jbakesite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * Copies a directory tree into another: every regular file, keeping relative paths, replacing what is there. The
 * bake uses it after JBake has run, to put the compiled CSS (T043) and, on Linux, the case-redirect stubs (T055)
 * into {@code build/jbake}, where a Gradle {@code Copy} task can't go: {@code bake} owns that directory as its
 * output, and a second task writing into it would make both tasks' up-to-date checks wrong. It also holds the
 * plugin's one file walk ({@link #regularFilesUnder}, {@link #regularFilesIn}), which every tool lists files with.
 */
public final class FileTrees {

    private FileTrees() {
    }

    /**
     * Copies every file under {@code from} to the same relative path under {@code to}, creating directories and
     * replacing existing files. Nothing is deleted from {@code to}.
     *
     * @param from the source directory
     * @param to the destination directory
     * @throws IOException if a file can't be read or written
     */
    public static void copy(Path from, Path to) throws IOException {
        for (Path file : regularFilesUnder(from)) {
            Path target = to.resolve(from.relativize(file).toString());
            Files.createDirectories(target.getParent());
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Every regular file under a directory, subdirectories included, sorted by path; none for a missing directory.
     * The one file walk in the plugin: every listing goes through it or {@link #regularFilesIn}.
     *
     * @param root the directory
     * @return the files, in a stable order
     * @throws IOException if the directory can't be read
     */
    public static List<Path> regularFilesUnder(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).sorted().toList();
        }
    }

    /**
     * The regular files directly in a directory, not those in its subdirectories, sorted by name; none for a missing
     * directory.
     *
     * @param directory the directory
     * @return the files, in a stable order
     * @throws IOException if the directory can't be read
     */
    public static List<Path> regularFilesIn(Path directory) throws IOException {
        return regularFilesUnder(directory).stream().filter(file -> directory.equals(file.getParent())).toList();
    }
}
