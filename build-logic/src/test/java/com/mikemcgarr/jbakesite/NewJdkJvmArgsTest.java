package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The flags per JDK (T113): none on 21, native access from 22, the Unsafe flag from 23. */
class NewJdkJvmArgsTest {

    @Test
    void jdk21GetsNoFlags() {
        assertEquals(List.of(), NewJdkJvmArgs.forJavaVersion(21), "21 refuses --sun-misc-unsafe-memory-access, and its command line stays as it was");
    }

    @Test
    void jdk22GetsNativeAccessOnly() {
        assertEquals(List.of(NewJdkJvmArgs.ENABLE_NATIVE_ACCESS), NewJdkJvmArgs.forJavaVersion(22));
    }

    @Test
    void jdk23AndLaterGetBothInCommandLineOrder() {
        List<String> both = List.of(NewJdkJvmArgs.ENABLE_NATIVE_ACCESS, NewJdkJvmArgs.ALLOW_UNSAFE_MEMORY_ACCESS);

        assertEquals(both, NewJdkJvmArgs.forJavaVersion(23));
        assertEquals(both, NewJdkJvmArgs.forJavaVersion(25));
    }

    @Test
    void theCurrentJvmIsTheOneThatRunsJbake() {
        assertEquals(NewJdkJvmArgs.forJavaVersion(Runtime.version().feature()), NewJdkJvmArgs.forCurrentJvm());
    }
}
