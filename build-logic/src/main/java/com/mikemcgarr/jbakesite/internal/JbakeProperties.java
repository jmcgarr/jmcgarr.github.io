package com.mikemcgarr.jbakesite.internal;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * What the writing tools read from JBake's own {@code jbake.properties}: {@code site.host}, the address the site is
 * published at, which the stubs' canonical links and {@code newPost}'s "it will live at" line are built from (the
 * programs carried it as a constant; the templates read the same key). The preview reads the server address
 * separately ({@code PreviewPort}).
 */
public final class JbakeProperties {

    /** The key for the published address. */
    public static final String SITE_HOST_KEY = "site.host";

    private JbakeProperties() {
    }

    /**
     * The site's published address, without a trailing slash: {@code https://www.mikemcgarr.com}.
     *
     * @param jbakeProperties the {@code jbake.properties} file
     * @param displayName the file as a message should name it
     * @return the address
     * @throws IOException if the file can't be read
     * @throws ToolFailure if the key is missing
     */
    public static String siteHost(Path jbakeProperties, String displayName) throws IOException {
        Properties config = new Properties();
        try (Reader reader = Files.newBufferedReader(jbakeProperties, StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        String host = config.getProperty(SITE_HOST_KEY, "").strip();
        if (host.isEmpty()) {
            throw ToolFailure.couldNotRun(SITE_HOST_KEY + " isn't set in " + displayName + ".");
        }
        return host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
    }
}
