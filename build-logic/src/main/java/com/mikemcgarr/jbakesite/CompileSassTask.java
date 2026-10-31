package com.mikemcgarr.jbakesite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * {@code compileSass}: compiles every entry in the SCSS directory ({@link SassEntries}) into
 * {@code <destinationDirectory>/<name>.css} with Dart Sass ({@link SassCompiler}). The bake and the preview copy
 * that directory into the site, so each entry is published at the same URL as before the SCSS (T043, T134). A Sass
 * error fails the build, and CI, with Dart Sass's message.
 *
 * <p>All entries are compiled before anything is written, and the destination holds exactly the entries' CSS
 * afterwards: a renamed or removed entry leaves no stale file behind. The output is the same for the same sources,
 * so the task is cacheable.
 */
@CacheableTask
public abstract class CompileSassTask extends DefaultTask {

    /**
     * The SCSS directory: the entries, and the partials they use.
     *
     * @return the directory
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSource();

    /**
     * Where the CSS goes: one {@code <name>.css} per entry.
     *
     * @return the directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getDestinationDirectory();

    /**
     * Gradle's file operations, for clearing the destination.
     *
     * @return the service
     */
    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    /**
     * Compiles the entries.
     *
     * @throws IOException if a file can't be read or written, or Dart Sass can't be started
     */
    @TaskAction
    public void compile() throws IOException {
        Path source = getSource().get().getAsFile().toPath();
        Path destination = getDestinationDirectory().get().getAsFile().toPath();

        Map<String, String> compiled;
        try (SassCompiler compiler = SassCompiler.start(getLogger())) {
            getLogger().info("Compiling {} with {}", source, compiler.version());
            compiled = compiler.compileAll(source);
        } catch (SassCompileException e) {
            throw new GradleException(e.getMessage(), e);
        }

        getFileSystemOperations().delete(spec -> spec.delete(destination));
        Files.createDirectories(destination);
        for (Map.Entry<String, String> css : compiled.entrySet()) {
            Files.writeString(destination.resolve(css.getKey()), css.getValue());
        }
        getLogger().info("Compiled {} into {}: {}", compiled.size() == 1 ? "1 entry" : compiled.size() + " entries",
                destination, String.join(", ", compiled.keySet()));
    }
}
