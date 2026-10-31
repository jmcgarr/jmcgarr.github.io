package com.mikemcgarr.jbakesite;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * {@code checkNoHandEditedCss}, the guard the bake and the preview run first (T043, T134, T135). The site's CSS
 * used to be hand-edited under JBake's assets, and the bake would still publish a copy that
 * came back there, silently going stale (the bake replaces it with the compiled one). So the task fails when a
 * {@code .css} file directly in the assets' CSS folder (not in a subfolder, as the guard in {@code build.gradle}
 * checked) has the name of an entry's CSS ({@link SassEntries#cssName}) or of a retired stylesheet, and says what
 * to do: move the change into the SCSS and delete the file.
 *
 * <p>The task has no outputs, so it runs on every build; it is cheap.
 */
public abstract class CheckNoHandEditedCssTask extends DefaultTask {

    /** What the message ends with: the fix. */
    static final String FIX = "Move any change into the SCSS, then delete this file.";

    /**
     * The SCSS directory, for the entries whose CSS names are guarded.
     *
     * @return the directory
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getScssSource();

    /**
     * The {@code .css} files directly in the assets' CSS folder, where the generated CSS must not be. Usually none.
     *
     * @return the files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getAssetsCss();

    /**
     * Retired stylesheets' file names, guarded besides the entries' ({@link ScssExtension#getRetiredCss()}).
     *
     * @return the names
     */
    @Input
    public abstract ListProperty<String> getRetiredCss();

    /**
     * The sentence after the offending path(s): where the CSS comes from now
     * ({@link ScssExtension#getHandEditedCssExplanation()}).
     *
     * @return the sentence
     */
    @Input
    public abstract Property<String> getExplanation();

    /**
     * Fails if a guarded file is present.
     *
     * @throws IOException if the SCSS directory can't be read
     */
    @TaskAction
    public void check() throws IOException {
        Set<String> guarded = guardedNames();
        List<File> found = getAssetsCss().getFiles().stream()
                .filter(file -> guarded.contains(file.getName()))
                .sorted()
                .toList();
        if (!found.isEmpty()) {
            throw new GradleException(message(found, getExplanation().get()));
        }
    }

    private Set<String> guardedNames() throws IOException {
        Set<String> names = new LinkedHashSet<>();
        for (var entry : SassEntries.in(getScssSource().get().getAsFile().toPath())) {
            names.add(SassEntries.cssName(entry));
        }
        names.addAll(getRetiredCss().get());
        return names;
    }

    /**
     * The failure message: the paths, the explanation, the fix.
     *
     * @param found the guarded files that exist
     * @param explanation the sentence after the paths
     * @return the message
     */
    static String message(List<File> found, String explanation) {
        return found.stream().map(File::getPath).collect(Collectors.joining(", ")) + ": " + explanation + " " + FIX;
    }
}
