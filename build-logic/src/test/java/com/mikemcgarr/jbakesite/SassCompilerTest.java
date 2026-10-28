package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.logging.Logging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the real Dart Sass on a little SCSS: the site's options, and the error message. */
class SassCompilerTest {

    @TempDir
    Path scss;

    private SassCompiler compiler;

    @BeforeEach
    void startCompiler() throws IOException {
        compiler = SassCompiler.start(Logging.getLogger(SassCompilerTest.class));
    }

    @AfterEach
    void stopCompiler() throws IOException {
        compiler.close();
    }

    @Test
    void compilesExpandedWithoutASourceMap() throws Exception {
        String css = compiler.compileString("$brand: #0085a1;\na { color: $brand; &:hover { text-decoration: underline; } }");

        assertEquals("a {\n  color: #0085a1;\n}\na:hover {\n  text-decoration: underline;\n}", css);
        assertFalse(css.contains("sourceMappingURL"));
        assertFalse(css.endsWith("\n"), "exactly what Dart Sass produces: no trailing newline is added");
    }

    @Test
    void compilesAnEntryWithItsPartials() throws Exception {
        Files.writeString(scss.resolve("_variables.scss"), "$gray: #808080;\n");
        Files.writeString(scss.resolve("site.scss"), "@use 'variables';\nbody { color: variables.$gray; }\n");

        assertEquals("body {\n  color: #808080;\n}", compiler.compile(scss.resolve("site.scss")));
    }

    @Test
    void compileAllCompilesEveryEntryInOrderAndNoPartial() throws Exception {
        Files.writeString(scss.resolve("_variables.scss"), "$gray: #808080;\n");
        Files.writeString(scss.resolve("code.scss"), "pre { color: red; }\n");
        Files.writeString(scss.resolve("clean-blog.scss"), "@use 'variables';\nbody { color: variables.$gray; }\n");

        var compiled = compiler.compileAll(scss);

        assertEquals(java.util.List.of("clean-blog.css", "code.css"), java.util.List.copyOf(compiled.keySet()));
        assertEquals("body {\n  color: #808080;\n}", compiled.get("clean-blog.css"));
        assertEquals("pre {\n  color: red;\n}", compiled.get("code.css"));
    }

    @Test
    void compileAllFailsAsAWholeOnOneBadEntry() throws IOException {
        Files.writeString(scss.resolve("good.scss"), "a { color: red; }\n");
        Files.writeString(scss.resolve("bad.scss"), "a { color: ; }\n");

        assertThrows(SassCompileException.class, () -> compiler.compileAll(scss));
    }

    @Test
    void aSassErrorCarriesDartSassMessageWithTheFileAndLine() throws IOException {
        Files.writeString(scss.resolve("_variables.scss"), "$gray: #808080;\n$oops: ;\n");
        Files.writeString(scss.resolve("site.scss"), "@use 'variables';\n");

        SassCompileException error = assertThrows(SassCompileException.class, () -> compiler.compile(scss.resolve("site.scss")));

        String message = error.getMessage();
        assertTrue(message.startsWith("Error: Expected expression."), message);
        assertTrue(message.contains("_variables.scss 2:8"), message);
        assertTrue(message.contains("site.scss 1:1"), message);
        assertFalse(message.endsWith("\n"));
    }

    @Test
    void reportsTheCompilerVersion() throws IOException {
        assertTrue(compiler.version().matches("dart-sass \\d+\\.\\d+\\.\\d+.*"), compiler.version());
    }
}
