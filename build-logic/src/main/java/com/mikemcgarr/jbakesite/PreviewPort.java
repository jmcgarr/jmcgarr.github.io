package com.mikemcgarr.jbakesite;

import java.io.IOException;
import java.io.Reader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.gradle.api.GradleException;

/**
 * The address the preview serves, and whether it is free (T101). JBake's server listens where
 * {@code server.hostname} and {@code server.port} in {@code jbake.properties} say (its own defaults are
 * {@code localhost} and 8820). If the port is taken, say by a preview in another terminal or clone, JBake only says
 * "unable to start the server" and exits with 4. So the preview checks first: it binds the address the way JBake's
 * Jetty does and fails with a message that names the fix when that doesn't work. T125/T126 moved this here from
 * {@code build.gradle}'s {@code checkPreviewPortFree} closure.
 */
final class PreviewPort {

    static final String HOSTNAME_KEY = "server.hostname";
    static final String PORT_KEY = "server.port";
    static final String DEFAULT_HOSTNAME = "localhost";
    static final int DEFAULT_PORT = 8820;

    private PreviewPort() {
    }

    /**
     * Reads the server address from JBake's properties.
     *
     * @param jbakeProperties the {@code jbake.properties} file
     * @param displayName the file as a message should name it, such as {@code src/jbake/jbake.properties}
     * @return the address
     * @throws IOException if the file can't be read
     * @throws GradleException if {@code server.port} isn't a port number, naming the file
     */
    static InetSocketAddress read(Path jbakeProperties, String displayName) throws IOException {
        Properties config = new Properties();
        try (Reader reader = Files.newBufferedReader(jbakeProperties, StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        String host = config.getProperty(HOSTNAME_KEY, DEFAULT_HOSTNAME).trim();
        String port = config.getProperty(PORT_KEY, Integer.toString(DEFAULT_PORT)).trim();
        try {
            return new InetSocketAddress(host, Integer.parseInt(port));
        } catch (IllegalArgumentException e) {
            throw new GradleException(PORT_KEY + "=" + port + " in " + displayName + " is not a port number (1 to 65535).", e);
        }
    }

    /**
     * Fails unless the address can be bound. The probe socket allows address reuse, as Jetty does, so a preview
     * that just stopped doesn't count as "in use".
     *
     * @param address the address JBake's server will bind
     * @param displayName the file the message tells the user to change, as it should read in the message
     * @throws IOException if the socket can't be opened or closed for a reason other than the port being in use
     * @throws GradleException when the port is in use
     */
    static void checkFree(InetSocketAddress address, String displayName) throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(address);
        } catch (BindException e) {
            throw new GradleException("Port " + address.getPort() + " is already in use (is another bakePreview running?). "
                    + "Stop it, or set " + PORT_KEY + " in " + displayName + ".", e);
        }
    }
}
