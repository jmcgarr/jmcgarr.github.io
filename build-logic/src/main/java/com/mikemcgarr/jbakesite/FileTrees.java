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
 * output, and a second task writing into it would make both tasks' up-to-date checks wrong. T124 moved it here from
 * {@code build.gradle}'s {@code copyTree} closure.
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
        List<Path> files;
        try (Stream<Path> paths = Files.walk(from)) {
            files = paths.filter(Files::isRegularFile).toList();
        }
        for (Path file : files) {
            Path target = to.resolve(from.relativize(file).toString());
            Files.createDirectories(target.getParent());
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
