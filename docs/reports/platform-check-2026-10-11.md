# Platform Check 2026-10-11 (T098)

**Status**: Done | **Run**: 2026-10-11, ahead of the 2027-10 schedule (owner's request) | **Against**: `main` at `dced3b6`

The yearly check of the re-evaluation triggers in [`01-PLATFORM-DECISION.md`](../01-PLATFORM-DECISION.md#decision).
**No trigger fired. Verdict: stay on JBake; don't start the Roq spike.**
Security comes closest: the libraries this site names are clean, but jbake-core 2.7.0 pulls in vulnerable
transitive libraries, a few with no public fix. They run only at build time, and most can be fixed in our own `build.gradle` (T112).

## Triggers

| Trigger | Result | Evidence |
|---|---|---|
| **1. Builds on the newest Java LTS** | Not fired | **The newest LTS is JDK 25** (GA 2025-09-16, Adoptium `most_recent_lts: 25`). The newest feature release is 27; the next LTS is 29. Gradle runs on JVM 17–27 (25 since 9.1.0, 27 since 9.8.0).<br>**Bake on 25:** Temurin 25.0.4.1+1 (SHA-256 matched Adoptium), `JAVA_HOME=<jdk25> ./gradlew clean bake --warning-mode all`: BUILD SUCCESSFUL, 268 items. `diff -r` against a JDK 21 bake (504 files) shows only `feed.xml`'s timestamps. JDK 27 (Temurin 27+35) gave the same result.<br>**Two JVM warnings on 25/27:** `sun.misc.Unsafe::objectFieldOffset … will be removed` (from `concurrentlinkedhashmap-lru 1.4.2`, via OrientDB) and `System::load … Restricted methods will be blocked` (from `jffi 1.3.13`, via JRuby).<br>**Preview on 25** (on a temporary port): pages, feed, tags and CSS answered 200, the live-reload script was present, and an edit was served in 0.29–1.21 s. |
| **2. Builds on the newest Gradle major** | Not fired | **Gradle 9.8.1 is current** (`services.gradle.org/versions/current`, built 2026-10-07), and this repo is on it. **No Gradle 10:** there's no 10.x release, RC or milestone; next is 9.9.0 (milestone-2, 2026-09-18).<br>**Warnings:** `clean bake --warning-mode all` prints no deprecation warnings, and `tasks --all --warning-mode fail` passes (it configures every task). `clean bake --configuration-cache` works. |
| **3. A security issue goes unfixed** | Not fired (borderline) | **No advisories for the named parts** (OSV, GitHub advisories, NVD by keyword): `jbake-core 2.7.0`, `orientdb-core 3.1.20` (its CVEs are for 2.x/3.0.x server and Studio), `asciidoctorj 2.5.13`, `flexmark 0.64.8`, `freemarker 2.3.35`. CVE-2026-84939 (FreeMarker, 2026-09-10) is fixed in 2.3.35, the version this build pins; jbake-core's own default, 2.3.31, would be affected.<br>**Transitive libraries:** OSV flags 23 of the 121 resolved ones (`./gradlew dependencies --configuration jbake` / `jbakePreview`).<br>**No public fix:** Jetty 9.4.58 (CVE-2026-2332 HIGH; fixed in 9.4.60, which isn't on Maven Central), `pebble 3.1.5` (CVE-2022-37767), `commons-lang 2.6` (needed by OrientDB), `org.lz4:lz4-java 1.4.0` (fixed only in the `at.yawk.lz4` fork).<br>**Fixed only in branches JBake can't use:** logback 1.2.x (fixes are in 1.3/1.5, which need SLF4J 2).<br>**Fix available:** JRuby 9.4.7.0, CVE-2025-46551 (MODERATE), fixed in 9.4.12.1.<br>**Context:** all of this runs at build time only, on the owner's Mac and in CI, against the owner's own files. The only listener is the preview's Jetty, on 127.0.0.1. A fork's pull request can already run any code through `build.gradle`. **Practical risk: low.** |
| **4. M7's live reload isn't good enough** | Not fired | **What exists:** T062 serves and watches every 1 s, T063 measured a 0.78 s median from save to served page, and T064 reloads the open page (Chrome 0.4–1.6 s; the owner confirmed Firefox and Safari on 2026-10-08). CI guards against the reload script reaching the live site.<br>**Port check:** T101 adds a clear error when the port is taken.<br>**Writing:** the newest published post is from 2020, so the flow hasn't been used much yet. |
| **5. Roq matures** (a 3.x line, a broader maintainer base, documented custom tag URL patterns) | Not fired | **Releases:** the latest is 2.1.9 (2026-09-02); there's no 3.x release, branch or milestone. There were 10 releases from 2.1.0 (2026-04-14) to 2.1.9, and none in the 39 days since.<br>**Maintainers:** broader but still one-led: 24 human authors in the year, with ia3andy about 55% of the human commits.<br>**Tag URLs:** the docs show only `/posts/tag/<tag>` plus a `lowercase` option. An undocumented `link` key in the tag layout can override it. Tags are always slugified (`some topic` → `some-topic`, which matches this site's hyphenated tag URLs since T039). `.NET` and case handling would still need the spike. |

**Context (not a trigger): JBake upstream is dormant.** v2.7.0 (2025-12-26) is the latest release, and the last commit on `master` was on 2025-12-30. There have been no commits in 2026 and no maintainer replies in the newest issue comments, with 145 open issues and 24 open PRs. So dependency fixes have to come from our own `build.gradle`, not from upstream.

## Throwaway hardening test (evidence for T112)

I made an uncommitted, since-reverted edit to `build.gradle`:
- **Excluded** the unused template engines from `configurations.jbake`: `org.thymeleaf`, `io.pebbletemplates`, `de.neuland-bfi` and `org.codehaus.groovy`.
- **Substituted** `org.lz4:lz4-java` → `at.yawk.lz4:lz4-java:1.12.0`.
- **Pinned** these versions:
  - commons-text 1.15.0, commons-configuration2 2.15.1, commons-lang3 3.20.0, commons-beanutils 1.11.0, commons-io 2.20.0
  - logback 1.2.13, jsoup 1.23.2, jruby 9.4.15.0, snakeyaml 2.7
  - jetty-server 9.4.58.v20250814, commons-vfs2 2.10.0

**Results:**
- **The bake is unchanged:** with these edits it still builds on JDK 21, and its output is byte-identical apart from `feed.xml`'s timestamps.
- **Three of 96 packages still have advisories** after the change: logback-core 1.2.13, commons-lang 2.6 and Jetty 9.4.58.
- **Jetty can't be excluded.** JBake's launcher needs it even for `-b`, and excluding it fails with `NoClassDefFoundError: org/eclipse/jetty/server/Connector`.
- **A `build.gradle` comment is wrong.** It says jbake-core "marks its engines … as optional", but Gradle reads jbake-core's `.module` metadata, where they are plain runtime dependencies. So the production bake also carries commons-vfs2 2.9.0's `hadoop-hdfs-client 3.3.1`, `jackson-databind 2.10.5.1` and `okhttp 2.7.5`, which the preview's classpath doesn't.

## Watch for the next check (2027-10)

- **The JDK after 27:** does it turn the `sun.misc.Unsafe` or native-access warnings into errors? If it does, and there's no small fix, trigger 1 fires before JDK 29 (the next LTS, due 2027-09). T113 covers the flags that keep 25 quiet.
- **Roq:** a 3.x line, documented tag URL patterns, and how it handles `.NET` and capitalized tags.

## Not verified

- The JDK 25 and 27 runs were on macOS aarch64 only. The CI gates weren't run on them.
- The preview on 25 was checked with curl, on a temporary port. No browser reload was tested.
- Gradle 9.9.0's milestone wasn't tried.
- NVD was searched by keyword for the named parts only. The transitive libraries were checked through OSV and GitHub advisories only.
- Not checked for Roq: the 2.0.0 GA date, and whether the undocumented `link` override can produce `/tags/<Tag>.html`.
