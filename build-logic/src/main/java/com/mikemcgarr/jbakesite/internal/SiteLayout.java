package com.mikemcgarr.jbakesite.internal;

import java.io.File;
import java.nio.file.Path;

/**
 * Where the site's sources are, for the writing and verification tools: the project root, JBake's source directory
 * with its {@code content/}, {@code templates/} and {@code assets/}, the case-redirect stubs and the baseline of
 * published URLs ({@code docs/baseline/urls.txt}, Rule 1). The single-file programs hard-coded these as paths
 * relative to the working directory; a task passes them from the {@code jbakeSite} extension, as absolute paths,
 * and the tools name a file the way the README does, relative to the root ({@link #display}).
 *
 * @param root the project directory, absolute
 * @param source JBake's source directory, usually {@code <root>/src/jbake}
 * @param caseRedirects the case-redirect stubs, usually {@code <root>/src/case-redirects}
 * @param publishedUrls the baseline of published URLs, usually {@code <root>/docs/baseline/urls.txt}
 */
public record SiteLayout(Path root, Path source, Path caseRedirects, Path publishedUrls) {

    /** Where JBake's source directory is by default, relative to the project: the {@code jbakeSite.source} convention. */
    public static final String SOURCE = "src/jbake";

    /** Where the case-redirect stubs are by default: the {@code jbakeSite.caseRedirects} convention. */
    public static final String CASE_REDIRECTS = "src/case-redirects";

    /** Where the baseline of published URLs is by default: the {@code jbakeSite.publishedUrls} convention. */
    public static final String PUBLISHED_URLS = "docs/baseline/urls.txt";

    /**
     * The layout the repository uses, and the plugin's defaults: {@link #SOURCE}, {@link #CASE_REDIRECTS},
     * {@link #PUBLISHED_URLS} under the root. Tests build their sites with it, so they test the paths the plugin
     * really defaults to.
     *
     * @param root the project directory
     * @return the layout
     */
    public static SiteLayout standard(Path root) {
        Path absolute = root.toAbsolutePath().normalize();
        return new SiteLayout(absolute, absolute.resolve(SOURCE), absolute.resolve(CASE_REDIRECTS), absolute.resolve(PUBLISHED_URLS));
    }

    /**
     * JBake's content: posts and pages.
     *
     * @return {@code <source>/content}
     */
    public Path content() {
        return source.resolve("content");
    }

    /**
     * JBake's templates.
     *
     * @return {@code <source>/templates}
     */
    public Path templates() {
        return source.resolve("templates");
    }

    /**
     * JBake's assets, copied to the site root as they are.
     *
     * @return {@code <source>/assets}
     */
    public Path assets() {
        return source.resolve("assets");
    }

    /**
     * The posts.
     *
     * @return {@code <content>/blog}
     */
    public Path blog() {
        return content().resolve("blog");
    }

    /**
     * The drafts: rendered for preview, never published.
     *
     * @return {@code <blog>/drafts}
     */
    public Path drafts() {
        return blog().resolve("drafts");
    }

    /**
     * The images.
     *
     * @return {@code <assets>/img}
     */
    public Path images() {
        return assets().resolve("img");
    }

    /**
     * The header images, served by screen width from their 960 and 1440 px variants (T071).
     *
     * @return {@code <images>/masthead}
     */
    public Path mastheads() {
        return images().resolve("masthead");
    }

    /**
     * A path as it is given on the command line or in git's output: relative to the root unless absolute.
     *
     * @param path the path, such as {@code src/jbake/content/about.md}
     * @return the absolute, normalized path
     */
    public Path resolve(String path) {
        return root.resolve(path).normalize();
    }

    /**
     * A path as it reads in a message: relative to the root with {@code /} separators when it is under it (the way
     * the README and git name a file), absolute otherwise.
     *
     * @param path the file or directory
     * @return the path to show
     */
    public String display(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        String shown = absolute.startsWith(root) ? root.relativize(absolute).toString() : absolute.toString();
        return shown.replace(File.separatorChar, '/');
    }

    /**
     * A path relative to a directory, with {@code /} separators: a site path, or an asset's URL path.
     *
     * @param directory the directory, such as the assets
     * @param path a file under it
     * @return the relative path, such as {@code img/masthead/photo.jpg}
     */
    public static String relativePath(Path directory, Path path) {
        return directory.relativize(path).toString().replace(File.separatorChar, '/');
    }
}
