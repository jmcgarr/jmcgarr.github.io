package com.mikemcgarr.jbakesite.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The paths the tools work in, and how they read in a message. */
class SiteLayoutTest {

    @TempDir
    Path root;

    @Test
    void theStandardLayoutIsTheRepositorys() {
        SiteLayout site = SiteLayout.standard(root);

        assertEquals(root.resolve("src/jbake/content/blog/drafts"), site.drafts());
        assertEquals(root.resolve("src/jbake/templates"), site.templates());
        assertEquals(root.resolve("src/jbake/assets/img/masthead"), site.mastheads());
        assertEquals(root.resolve("src/case-redirects"), site.caseRedirects());
        assertEquals(root.resolve("docs/baseline/urls.txt"), site.publishedUrls());
    }

    @Test
    void displaysAPathUnderTheRootRelativeToItAndOthersAbsolute() {
        SiteLayout site = SiteLayout.standard(root);

        assertEquals("src/jbake/content/about.md", site.display(site.content().resolve("about.md")));
        assertEquals("src/jbake/content/about.md", site.display(site.resolve("src/jbake/content/./about.md")));
        Path elsewhere = root.resolveSibling("elsewhere").resolve("x.md");
        assertEquals(elsewhere.toAbsolutePath().toString(), site.display(elsewhere));
        assertEquals("img/masthead/photo.jpg", SiteLayout.relativePath(site.assets(), site.mastheads().resolve("photo.jpg")));
    }
}
