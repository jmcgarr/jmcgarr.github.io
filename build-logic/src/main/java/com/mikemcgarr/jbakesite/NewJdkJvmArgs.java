package com.mikemcgarr.jbakesite;

import java.util.ArrayList;
import java.util.List;

/**
 * The JVM flags JBake needs on newer JDKs (T113). From JDK 24, JBake's JVM prints two warnings that the bake on 21
 * doesn't: OrientDB's concurrentlinkedhashmap calls {@code sun.misc.Unsafe::objectFieldOffset} ("will be removed in
 * a future release"), and jffi (loaded by OrientDB's jnr-posix) calls {@code System::load} ("Restricted methods
 * will be blocked in a future release"). These flags allow both and silence the warnings; a bake without either
 * still warns (checked on JDK 25 after T102 removed JRuby).
 *
 * <p>Each is added only on a JDK that knows it: JDK 21 refuses to start with
 * {@code --sun-misc-unsafe-memory-access} ("Unrecognized option"), which arrived in JDK 23 (JEP 471).
 * {@code --enable-native-access} is final from JDK 22 (JEP 454); 21 accepts it, but leaving it off keeps 21's
 * command line as it was. The two {@code --add-opens} flags JRuby wanted went with AsciidoctorJ (T102): without
 * them the bake prints nothing extra on JDK 21 or 25.
 */
final class NewJdkJvmArgs {

    /** JEP 454: allows jffi's {@code System::load} without a warning. Final in JDK 22. */
    static final String ENABLE_NATIVE_ACCESS = "--enable-native-access=ALL-UNNAMED";

    /** JEP 471: allows OrientDB's {@code sun.misc.Unsafe} memory access without a warning. New in JDK 23. */
    static final String ALLOW_UNSAFE_MEMORY_ACCESS = "--sun-misc-unsafe-memory-access=allow";

    private static final int FIRST_JDK_WITH_NATIVE_ACCESS = 22;
    private static final int FIRST_JDK_WITH_UNSAFE_FLAG = 23;

    private NewJdkJvmArgs() {
    }

    /**
     * The flags for the JDK that will run JBake.
     *
     * @param javaVersion the JDK's feature version, such as 21 or 25
     * @return the flags, in command-line order; none on JDK 21
     */
    static List<String> forJavaVersion(int javaVersion) {
        List<String> args = new ArrayList<>();
        if (javaVersion >= FIRST_JDK_WITH_NATIVE_ACCESS) {
            args.add(ENABLE_NATIVE_ACCESS);
        }
        if (javaVersion >= FIRST_JDK_WITH_UNSAFE_FLAG) {
            args.add(ALLOW_UNSAFE_MEMORY_ACCESS);
        }
        return List.copyOf(args);
    }

    /**
     * The flags for the JVM running Gradle, which is the one that runs JBake: no toolchain is set, so
     * {@code ExecOperations.javaexec} starts the same {@code java}.
     *
     * @return the flags
     */
    static List<String> forCurrentJvm() {
        return forJavaVersion(Runtime.version().feature());
    }
}
