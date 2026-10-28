package com.mikemcgarr.jbakesite.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The paths the tools work in, and how they read in a message; plus site.host from jbake.properties. */
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

    @Test
    void theSiteHostComesFromJbakePropertiesWithoutATrailingSlash() throws IOException {
        Path properties = Files.writeString(root.resolve("jbake.properties"), "render.tags=true\nsite.host=https://www.example.test/\n");

        assertEquals("https://www.example.test", JbakeProperties.siteHost(properties, "src/jbake/jbake.properties"));
    }

    @Test
    void aMissingSiteHostIsACouldNotRunFailureNamingTheFile() throws IOException {
        Path properties = Files.writeString(root.resolve("jbake.properties"), "render.tags=true\n");

        ToolFailure e = assertThrows(ToolFailure.class, () -> JbakeProperties.siteHost(properties, "src/jbake/jbake.properties"));

        assertEquals(ToolFailure.COULD_NOT_RUN, e.exitCode());
        assertEquals("site.host isn't set in src/jbake/jbake.properties.", e.getMessage());
    }
}
