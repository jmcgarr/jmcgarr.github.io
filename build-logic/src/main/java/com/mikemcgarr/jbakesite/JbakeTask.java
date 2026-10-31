package com.mikemcgarr.jbakesite;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.ExecOperations;

/**
 * What the bake and the preview share: JBake's own launcher, run in a fresh JVM per task on the {@code jbake}
 * configuration ({@link JbakeClasspath}), with these settings:
 *
 * <ul>
 * <li>a <b>fresh JVM</b>, because the JBake Gradle plugin ran JBake inside the Gradle daemon and leaked memory on
 * every bake until the fourth failed with "Java heap space" (T099), and because its own exit code and log are then
 * JBake's;</li>
 * <li>the <b>JVM flags for newer JDKs</b> ({@link NewJdkJvmArgs}, T113), as {@link #getJvmArguments()} so a site
 * can add its own;</li>
 * <li>a <b>jffi temp dir</b> under the build directory (T024): jffi (jnr-ffi, which OrientDB's jnr-posix brings)
 * extracts its native library to a temporary file while loading and removes it afterwards; keeping that under
 * {@code build/tmp}, which {@code clean} removes, keeps it out of the project root;</li>
 * <li><b>logback's configuration</b>: jbake-core ships logback without one, which logs everything at DEBUG. The
 * plugin's bundled {@code jbake-logback.xml} keeps
 * JBake's start/finish summary and every warning or error from JBake and the templates, and silences known noise
 * from the embedded OrientDB and from JBake's parser registry; {@code jbakeSite.logbackConfiguration} replaces
 * it.</li>
 * </ul>
 *
 * <p>Subclasses declare the destination (the bake's output, the preview's served directory) and call
 * {@link #runJbake}.
 */
public abstract class JbakeTask extends DefaultTask {

    /** JBake's command-line launcher. */
    static final String MAIN_CLASS = "org.jbake.launcher.Main";

    private static final String BUNDLED_LOGBACK_CONFIGURATION = "jbake-logback.xml";

    /**
     * JBake's source directory: {@code content/}, {@code templates/}, {@code assets/} and {@code jbake.properties}.
     *
     * @return the directory
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSource();

    /**
     * JBake and its libraries: the {@code jbake} configuration.
     *
     * @return the classpath
     */
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    /**
     * The compiled theme CSS, {@code compileSass}'s output (T043): the bake copies it into the site after JBake has
     * run, the preview before JBake's server starts ({@link #copyThemeCssInto}).
     *
     * @return the directory of {@code .css} files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getThemeCss();

    /**
     * Where the theme CSS goes, relative to the site root ({@link ScssExtension#getPublishedDir()}).
     *
     * @return the directory, such as {@code css}
     */
    @Input
    public abstract Property<String> getThemeCssPublishedDir();

    /**
     * The flags JBake's JVM starts with. Default: {@link NewJdkJvmArgs#forCurrentJvm()}.
     *
     * @return the flags
     */
    @Input
    public abstract ListProperty<String> getJvmArguments();

    /**
     * A logback configuration instead of the bundled one ({@link JbakeSiteExtension#getLogbackConfiguration()}).
     *
     * @return the file, usually unset
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getLogbackConfiguration();

    /**
     * Where jffi extracts its native library while JBake runs. Not an input: a temporary directory, machine-specific.
     *
     * @return the directory
     */
    @Internal
    public abstract DirectoryProperty getJffiDirectory();

    /**
     * Gradle's process operations, for the fresh JVM.
     *
     * @return the service
     */
    @Inject
    protected abstract ExecOperations getExecOperations();

    /**
     * The project's layout, for messages that name a file the way the README does, relative to the project.
     *
     * @return the layout
     */
    @Inject
    protected abstract ProjectLayout getLayout();

    /**
     * Runs JBake's launcher: {@code jbake <source> <destination> <options...>}, in a fresh JVM on
     * {@link #getClasspath()}, in the project directory. A non-zero exit fails the task with the process's exit
     * value; JBake's own messages are above it in the log.
     *
     * @param destination where JBake writes
     * @param options the launcher's options, such as {@code -b} to bake and {@code -s} to serve
     * @param systemProperties properties for JBake's JVM besides the jffi and logback ones, such as the preview's
     *        live-reload switch
     * @throws IOException if the jffi directory can't be created
     */
    protected final void runJbake(Path destination, List<String> options, Map<String, String> systemProperties)
            throws IOException {
        Path jffiDirectory = getJffiDirectory().get().getAsFile().toPath();
        Files.createDirectories(jffiDirectory);
        String logbackConfiguration = logbackConfiguration();
        List<String> arguments = new ArrayList<>();
        arguments.add(getSource().get().getAsFile().getAbsolutePath());
        arguments.add(destination.toAbsolutePath().toString());
        arguments.addAll(options);

        getExecOperations().javaexec(spec -> {
            spec.getMainClass().set(MAIN_CLASS);
            spec.setClasspath(getClasspath());
            spec.setWorkingDir(getLayout().getProjectDirectory().getAsFile());
            spec.jvmArgs(getJvmArguments().get());
            spec.systemProperty("jffi.extract.dir", jffiDirectory.toAbsolutePath().toString());
            spec.systemProperty("logback.configurationFile", logbackConfiguration);
            systemProperties.forEach(spec::systemProperty);
            spec.args(arguments);
        });
    }

    /**
     * Copies the compiled theme CSS into the site, at {@link #getThemeCssPublishedDir()} under the destination,
     * replacing what is there; nothing JBake copies replaces it afterwards (the bake), or deletes it (the preview).
     *
     * @param destination the site
     * @throws IOException if a file can't be copied
     */
    protected final void copyThemeCssInto(Path destination) throws IOException {
        Path target = destination.resolve(getThemeCssPublishedDir().get());
        for (File cssDirectory : getThemeCss().getFiles()) {
            FileTrees.copy(cssDirectory.toPath(), target);
        }
    }

    /**
     * A path or directory as it reads in a message: relative to the project when it is under it.
     *
     * @param file the file or directory
     * @return the path to show
     */
    protected final String projectRelative(File file) {
        return ProjectPaths.relativize(getLayout().getProjectDirectory(), file);
    }

    /** The configured file's path, or the bundled resource's URL (logback accepts either). */
    private String logbackConfiguration() {
        if (getLogbackConfiguration().isPresent()) {
            return getLogbackConfiguration().get().getAsFile().getAbsolutePath();
        }
        URL bundled = JbakeTask.class.getResource(BUNDLED_LOGBACK_CONFIGURATION);
        if (bundled == null) {
            throw new GradleException("The plugin's " + BUNDLED_LOGBACK_CONFIGURATION + " is missing from its jar; "
                    + "set jbakeSite.logbackConfiguration to a logback configuration file for JBake.");
        }
        return bundled.toExternalForm();
    }
}
