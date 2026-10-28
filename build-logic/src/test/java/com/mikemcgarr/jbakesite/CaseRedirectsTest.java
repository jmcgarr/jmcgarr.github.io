package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The stubs go in only on a case-sensitive filesystem (T055). The temp directory is whatever the machine has (APFS
 * is case-insensitive by default, Linux's ext4 case-sensitive), so the tests check the outcome against the probe
 * and the files against the outcome.
 */
class CaseRedirectsTest {

    @TempDir
    Path stubs;

    @TempDir
    Path site;

    @Test
    void theProbeLeavesNothingBehind() throws IOException {
        CaseRedirects.isCaseSensitive(site);

        try (Stream<Path> files = Files.list(site)) {
            assertTrue(files.findAny().isEmpty());
        }
    }

    @Test
    void theProbeAgreesWithTheFilesystem() throws IOException {
        Files.writeString(site.resolve("devops.html"), "real");

        assertEquals(!Files.exists(site.resolve("DEVOPS.HTML")), CaseRedirects.isCaseSensitive(site));
    }

    @Test
    void noStubsMeansNone() throws IOException {
        assertEquals(CaseRedirects.Outcome.NONE, CaseRedirects.copy(stubs.resolve("missing"), site), "a missing directory");
        assertEquals(CaseRedirects.Outcome.NONE, CaseRedirects.copy(stubs, site), "an empty directory");
    }

    @Test
    void stubsAreCopiedOnlyWhereTheyCannotReplaceTheRealPage() throws IOException {
        Files.createDirectories(stubs.resolve("tags"));
        Files.writeString(stubs.resolve("tags/DevOps.html"), "stub");
        Files.createDirectories(site.resolve("tags"));
        Files.writeString(site.resolve("tags/devops.html"), "real");

        CaseRedirects.Outcome outcome = CaseRedirects.copy(stubs, site);

        assertEquals("real", Files.readString(site.resolve("tags/devops.html")), "the real page is never replaced");
        if (CaseRedirects.isCaseSensitive(site)) {
            assertEquals(CaseRedirects.Outcome.COPIED, outcome);
            assertEquals("stub", Files.readString(site.resolve("tags/DevOps.html")));
        } else {
            assertEquals(CaseRedirects.Outcome.SKIPPED, outcome);
            assertFalse(Files.readString(site.resolve("tags/DevOps.html")).equals("stub"), "the same file as devops.html here");
        }
    }
}
