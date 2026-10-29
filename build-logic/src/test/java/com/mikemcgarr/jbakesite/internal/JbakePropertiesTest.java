package com.mikemcgarr.jbakesite.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@code site.host} from jbake.properties. */
class JbakePropertiesTest {

    @TempDir
    Path root;

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
