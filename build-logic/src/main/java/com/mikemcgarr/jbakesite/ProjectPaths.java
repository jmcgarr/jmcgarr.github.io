package com.mikemcgarr.jbakesite;

import java.io.File;
import java.nio.file.Path;
import org.gradle.api.file.Directory;

/**
 * Paths as they read in messages and task inputs: relative to the project when they are under it
 * ({@code src/jbake/jbake.properties}), absolute otherwise. A project-relative path is the same on every machine,
 * so a task input built from it doesn't change between checkouts, and it is what the README calls the file.
 */
final class ProjectPaths {

    private ProjectPaths() {
    }

    /**
     * A file or directory relative to the project directory, when it is under it.
     *
     * @param projectDirectory the project directory
     * @param file the file or directory
     * @return the relative path, or the absolute path for a file outside the project
     */
    static String relativize(Directory projectDirectory, File file) {
        Path project = projectDirectory.getAsFile().toPath().toAbsolutePath();
        Path path = file.toPath().toAbsolutePath();
        return path.startsWith(project) ? project.relativize(path).toString() : path.toString();
    }
}
