package com.mikemcgarr.jbakesite.engine;

import com.mikemcgarr.jbakesite.internal.FrontMatter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jbake.app.configuration.JBakeConfiguration;
import org.jbake.model.DocumentModel;
import org.jbake.parser.MarkdownEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JBake's Markdown engine for files with YAML front matter (T145, docs/archive/07-FRONT-MATTER.md). Stock JBake reads
 * only its own header, {@code key=value} lines ending with the {@code header.separator} line ({@code ~~~~~~} by
 * default), and skips a file that starts with {@code ---}. This engine reads the front matter
 * ({@link FrontMatter#parse}), writes the same file with that header in JBake's form ({@link FrontMatter#jbake})
 * to a temporary file, and
 * lets JBake parse <em>that</em>: the date, the tags (sanitized as {@code tag.sanitize} says), a JSON value and
 * the Markdown body all go through JBake's own code, unchanged. A file that doesn't start with {@code ---} is
 * handed to JBake as it is.
 *
 * <p>The translation, not a reimplementation of the parse, is what keeps a jbake-core upgrade safe: the engine
 * touches two public entry points, {@code parse(JBakeConfiguration, File)} and {@code header.separator}, and
 * nothing of JBake's private parsing flow (which a subclass can't reach). The temporary file is the price: one
 * per Markdown file per bake, deleted as soon as JBake has read it.
 *
 * <p>Front matter JBake can't use is reported here, against the real file (JBake's own messages would name the
 * temporary one), and the file is skipped as JBake skips a file without a header: an error in the log, no page,
 * and the URL check fails CI.
 */
public final class FrontMatterEngine extends MarkdownEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontMatterEngine.class);

    @Override
    public DocumentModel parse(JBakeConfiguration config, File file) {
        String text;
        try {
            text = Files.readString(file.toPath(), Charset.forName(config.getRenderEncoding()));
        } catch (IOException e) {
            LOGGER.error("Error while opening file {}", file, e);
            return null;
        }
        FrontMatter frontMatter = FrontMatter.parse(text);
        if (frontMatter.legacy()) {
            return super.parse(config, file);
        }
        if (!frontMatter.separated()) {
            LOGGER.error("Parsing skipped for file {}: the front matter has no closing {} line", file, FrontMatter.DELIMITER);
            return null;
        }
        if (frontMatter.error() != null) {
            LOGGER.error("Parsing skipped for file {}: line {}: {}", file, frontMatter.errorLine(), frontMatter.error());
            return null;
        }
        Path translated;
        try {
            translated = Files.createTempFile(file.getName() + "-", ".jbake");
            Files.writeString(translated, frontMatter.jbake(text, config.getHeaderSeparator()),
                    Charset.forName(config.getRenderEncoding()));
        } catch (IOException e) {
            LOGGER.error("Error while translating the front matter of {}", file, e);
            return null;
        }
        try {
            return super.parse(config, translated.toFile());
        } finally {
            try {
                Files.deleteIfExists(translated);
            } catch (IOException e) {
                LOGGER.warn("Couldn't delete the temporary file {}", translated, e);
            }
        }
    }
}
