package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The preview's address from jbake.properties, and the port check against a socket that really listens (T101). */
class PreviewPortTest {

    @TempDir
    Path dir;

    @Test
    void readsTheServerAddressFromJbakeProperties() throws IOException {
        Path properties = write("site.host=https://example.test\nserver.hostname = 127.0.0.1 \nserver.port= 8080\n");

        InetSocketAddress address = PreviewPort.read(properties, "jbake.properties");

        assertEquals("127.0.0.1", address.getHostString());
        assertEquals(8080, address.getPort());
    }

    @Test
    void usesJbakesDefaultsWhenTheKeysAreMissing() throws IOException {
        InetSocketAddress address = PreviewPort.read(write("site.host=https://example.test\n"), "jbake.properties");

        assertEquals(PreviewPort.DEFAULT_HOSTNAME, address.getHostString());
        assertEquals(PreviewPort.DEFAULT_PORT, address.getPort());
    }

    @Test
    void aBadPortNamesTheFile() throws IOException {
        Path properties = write("server.port=eighty\n");

        GradleException e = assertThrows(GradleException.class, () -> PreviewPort.read(properties, "src/jbake/jbake.properties"));

        assertEquals("server.port=eighty in src/jbake/jbake.properties is not a port number (1 to 65535).", e.getMessage(),
                "the file as the README names it, not its absolute path");
    }

    @Test
    void aFreePortPasses() {
        assertDoesNotThrow(() -> PreviewPort.checkFree(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), "jbake.properties"));
    }

    @Test
    void aPortInUseFailsWithTheFix() throws IOException {
        try (ServerSocket taken = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), taken.getLocalPort());

            GradleException e = assertThrows(GradleException.class,
                    () -> PreviewPort.checkFree(address, "src/jbake/jbake.properties"));

            assertEquals("Port " + taken.getLocalPort() + " is already in use (is another bakePreview running?). "
                    + "Stop it, or set server.port in src/jbake/jbake.properties.", e.getMessage());
        }
    }

    private Path write(String content) throws IOException {
        Path properties = dir.resolve("jbake.properties");
        Files.writeString(properties, content);
        return properties;
    }
}
