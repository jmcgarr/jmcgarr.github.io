package com.mikemcgarr.jbakesite.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.mikemcgarr.jbakesite.internal.CommandRunner.Result;
import com.mikemcgarr.jbakesite.internal.CommandRunner.Stderr;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The real process runner's pipes (its git use is in {@link GitTest}). */
class ProcessRunnerTest {

    @TempDir
    Path dir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Test
    void aChildThatFillsStandardOutputBeforeClosingStandardErrorDoesNotHang() {
        ProcessRunner runner = new ProcessRunner(dir, new PrintStream(out, true, StandardCharsets.UTF_8), System.err);
        // 300 KB is well past a pipe's buffer: reading stderr to its end first would wait on a child stuck writing stdout
        List<String> command = List.of("sh", "-c", "head -c 300000 /dev/zero | tr '\\0' a; echo oops >&2; exit 3");

        Result result = assertTimeoutPreemptively(Duration.ofSeconds(30), () -> runner.run(command, Stderr.SHOW));

        assertEquals(3, result.exit());
        assertEquals(300000, result.output().length());
        assertEquals("oops\n", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void discardedStandardErrorIsNeitherCapturedNorShown() {
        ProcessRunner runner = new ProcessRunner(dir, new PrintStream(out, true, StandardCharsets.UTF_8), System.err);

        Result result = assertTimeoutPreemptively(Duration.ofSeconds(30),
                () -> runner.run(List.of("sh", "-c", "echo kept; echo dropped >&2"), Stderr.DISCARD));

        assertEquals("kept", result.output());
        assertEquals("", out.toString(StandardCharsets.UTF_8));
    }
}
