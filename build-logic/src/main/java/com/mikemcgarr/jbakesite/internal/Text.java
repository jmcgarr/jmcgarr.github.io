package com.mikemcgarr.jbakesite.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * How the tools read text: one UTF-8 policy and one line-ending normalisation, where each tool used to have its
 * own.
 *
 * <p><b>Files the author writes</b> (content, templates) are read with {@link #read}: UTF-8, with a malformed byte
 * replaced by U+FFFD rather than thrown. The tools exist to report what is wrong with such a file (the review's E4,
 * the tag checks), and a stray byte in a 2009 export must not stop them before they can say so. <b>Files the build
 * writes</b> (the baked site, the redirect stubs, the baseline of published URLs) are read with
 * {@link Files#readString(Path, java.nio.charset.Charset) Files.readString} and
 * {@link Files#readAllLines(Path, java.nio.charset.Charset) Files.readAllLines}, which fail on a malformed byte:
 * there it is a bug worth a stack trace, not a finding. One exception: {@code prepareImage} reads content and
 * templates strictly, because it writes them back and must not turn a byte it couldn't decode into U+FFFD.
 *
 * <p><b>Line endings</b>: content is LF only (AGENTS.md Rule 3; the review's E4 reports a CR), but the tools accept
 * any kind and work on LF ({@link #lines}), so a line number means the same everywhere.
 */
public final class Text {

    private Text() {
    }

    /**
     * Reads a file the author wrote as UTF-8, replacing a malformed byte with U+FFFD. Line endings are kept.
     *
     * @param file the file
     * @return its text
     * @throws IOException if the file can't be read
     */
    public static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    /**
     * Text with every line ending made LF: {@code CRLF} and a lone {@code CR} both become {@code LF}.
     *
     * @param text the text
     * @return the text with LF line endings
     */
    public static String lf(String text) {
        return text.indexOf('\r') < 0 ? text : text.replace("\r\n", "\n").replace('\r', '\n');
    }

    /**
     * The lines of a text, whatever its line endings, a trailing empty line included (as {@code split("\n", -1)}
     * gives it), so index {@code i} is line {@code i + 1}.
     *
     * @param text the text
     * @return its lines
     */
    public static String[] lines(String text) {
        return lf(text).split("\n", -1);
    }
}
