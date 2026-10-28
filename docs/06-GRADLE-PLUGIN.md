# Build logic as a Gradle plugin

**Branch**: `docs/T123-gradle-plugin-plan` | **Created**: 2026-10-22 | **Status**: Accepted (owner, 2026-10-27; decisions in [Notes](#notes))

A backlog plan to move the custom build logic out of `build.gradle` into a Gradle plugin: first one that lives in
this repository (an included build under `build-logic/`), later, once its shape has settled, a standalone plugin
in its own repository. **Backlog, not scheduled** (owner's request, 2026-10-22): it comes after the Review Flow
plan ([`05-REVIEW-FLOW.md`](05-REVIEW-FLOW.md)) and starts when the owner says so (it did: accepted and started on 2026-10-27, see [Notes](#notes)). It uses the same conventions
([`AGENTS.md`](../AGENTS.md), "Tasks and checklists"); task IDs continue from T123 and checkpoints from CHK047.
The plugin's public surface, the names a user types, follows the JBake Gradle plugin's conventions where they
exist ([the standard](#the-public-surface-the-standard), T133).

## Context

`build.gradle` is 383 lines (431 on 2026-10-27, after T140 added the `prepareImage` task). In 2026-10 it was a 30-line application of the JBake plugin. The revival plan
([`archive/00-REVIVAL.md`](archive/00-REVIVAL.md)) replaced that plugin and then kept adding, one task at a
time, each piece justified on its own:

| Lines (2026-10-22) | What | From |
|---|---|---|
| 1–7 | `base` and `io.freefair.sass-base` plugins | T043 |
| 9–84 | The `jbake` and `jbakePreview` configurations: four excluded template engines, the lz4 substitution, nine version constraints, and the comments that explain each advisory | T096, T112, T102 |
| 86–127 | Paths, the jffi temp dir, `NewJdkJvmArgs` (JDK 22+/23+ flags), and `configureJBake`, shared by the bake and the preview | T024, T096, T113 |
| 129–146 | Case-redirect stubs and `copyTree` | T055 |
| 148–174 | `compileSass` and `checkNoHandEditedCss` | T043 |
| 176–198 | `bake`: JBake in a fresh JVM, then the CSS and (on Linux) the stubs copied in | T096, T043, T055 |
| 200–273 | `SassPreviewWatcher`, a build service with its own thread and Dart Sass compiler | T043 |
| 275–331 | `preview` and `bakePreview`: port check, watcher, live-reload property | T062, T100, T101, T064 |
| 333–352 | `newPost`, an `Exec` of `scripts/NewPost.java` with `-P` options passed through | T060, T061 |
| 354–383 | `reviewChanges`, the same shape for `scripts/ReviewChanges.java`, with exit codes mapped to messages | T117 |

Nothing in it is wrong. The problem is the form: two abstract classes, three closures that act as functions, a
build service, two Gradle `Exec` wrappers with the same argument-passing idiom, and the site's dependency pins, all
in one Groovy script with no tests and no types beyond what Groovy infers. Every addition means reading the whole
file to find where it goes, and the configuration cache (T114) adds a rule the script must follow in places it is
easy to miss (`newPost` needed a fix for it). The next pieces the Review Flow plan adds (`openPr`, T121) would
grow it again.

Two kinds of thing are mixed in the file:

- **Build logic**: how to run JBake, compile Sass, serve a preview, watch SCSS, add redirect stubs, pass options to
  a script. This is the same for any JBake site built this way, and it is the part that belongs in a plugin.
- **Site configuration**: which JBake version and engines, the version pins and their advisories, the source and
  output directories, the published CSS path, the preview port. This belongs in `build.gradle`, and it is short.

**What predates the revival, and what duplicates it.** From 2014 to 2026-10 the build was the JBake Gradle plugin
(`org.jbake.site`, unmaintained since 2022, removed in T096). It gave the repository the names still in use: the
tasks **`bake`** and **`bakePreview`**, and a **`jbake`** dependency configuration for JBake's own classpath. T096
kept those on purpose ("keep the familiar command"). The revival then added two names beside them: the `preview`
task (T062), which `bakePreview` only `dependsOn`, and the `jbakePreview` configuration (T062, T112), which
`extendsFrom jbake` and adds Jetty and commons-vfs2 pins. Checked 2026-10-22: `./gradlew dependencies` resolves
`jbake` and `jbakePreview` to the **same 77 artifacts**, because jbake-core brings Jetty and commons-vfs2 itself and
T112 aligned the pins. So the site has two names for one preview task and two names for one classpath. The owner's
instruction (2026-10-22): consolidate on the standard for JBake where one exists, and define one where it doesn't.

## Options

**Where the plugin lives (Phase 1)**

| Option | For | Against |
|---|---|---|
| `buildSrc/` | Gradle's oldest convention; nothing to wire in `settings.gradle` | Any change invalidates every task's up-to-date state; can't be published or reused; Gradle itself now recommends included builds |
| **Included build `build-logic/`** (`pluginManagement { includeBuild 'build-logic' }` in `settings.gradle`) | Same checkout, same PR; a real Gradle project with its own tests; promotes to a standalone plugin by moving the folder; configuration-cache friendly | One more directory and a second `build.gradle`; Dependabot needs a second entry |
| Standalone plugin now (own repository, published) | The end state in one step | Publishing, versioning and a release flow before the plugin has a stable shape; every tweak is a release; two repositories to keep in step while it is still changing |

**Language**

| Option | For | Against |
|---|---|---|
| **Java** with `java-gradle-plugin` | The owner's language; `NewPost.java` and `ReviewChanges.java` already are; Gradle's own API is Java; lazy properties and `@Input` annotations are explicit | More lines than Groovy |
| Groovy (precompiled script plugin or classes) | Closest to the current file | Keeps the dynamic typing that made the file hard to read; the configuration-cache rules are easier to break unnoticed |
| Kotlin DSL / Kotlin | Good tooling | A new language in the repository for one file's worth of logic |

**How to prove nothing changed**

- Byte-identical output: bake on `main` and on the branch, `diff -r` the two `build/jbake` trees apart from
  `feed.xml`'s timestamps. Every slice below has this as its test. The revival used it for T096, T112 and T113.
- Gradle TestKit functional tests in `build-logic/src/test`: a fixture site of two posts, one page and one SCSS
  file under `build-logic/src/test/fixtures/site`, baked by `GradleRunner` with the configuration cache on. The
  tests assert the output files exist, the CSS was copied in, the stubs appear only on a case-sensitive filesystem,
  the hand-edited-CSS guard fails, and `--configuration-cache` reports reuse on the second run.

## Decision

**Phase 1: an included build in Java, `build-logic/`, with one convention plugin, id `jbake-site`,** applied by
`build.gradle`. The plugin registers the tasks the site has today, with the same names, groups, descriptions and
behavior, configured through a small extension:

```groovy
plugins {
    id 'jbake-site'
}

jbakeSite {
    source = file('src/jbake')                  // default
    caseRedirects = file('src/case-redirects')  // default; empty dir = none
    scss {
        entry = file('src/scss/clean-blog.scss')
        publishedPath = 'css/clean-blog.css'
    }
    // The writing tasks (newPost, reviewChanges, prepareImage, openPr) and the check tasks are the plugin's
    // own classes since the T130 decision of 2026-10-27, so there is nothing to register here. Until then this
    // sketch had a `scripts { register(name, path) { options; dependsOn } }` block for single-file programs.
}

dependencies {
    jbake 'org.jbake:jbake-core:2.7.0'
    // ...the engines, the preview libraries and the constraints, with their comments, as today
}
```

What stays in `build.gradle`: the plugin application, the extension block, and the `dependencies` block with its
pins and advisory comments (the configurations themselves, the engine exclusions and the lz4 substitution move into
the plugin, because they are about *how JBake is run*, not which versions; the exclusions stay overridable). What
moves: everything else, as Java classes under `build-logic/src/main/java/com/mikemcgarr/jbakesite/`:
`JbakeSitePlugin`, `JbakeSiteExtension`, `NewJdkJvmArgs`, `SassPreviewWatcher`, `ScriptTask` (the `Exec` idiom
both script tasks share, with the exit-code mapping), and small helpers for `copyTree`, the port check and the
hand-edited-CSS guard.

### The public surface (the standard)

The names a user types, in `./gradlew …` and in `build.gradle`, are the plugin's contract. Where the JBake Gradle
plugin had a convention, this plugin keeps it, because it is what JBake users know and what this repository has
used since 2014. Where it had none, this table defines one. T133 applies it; the README, AGENTS.md and the CI
comments follow.

| Name | Kind | Today | In the plugin | Why |
|---|---|---|---|---|
| `bake` | task, group `jbake` | exists (group `build`) | **kept** | The JBake plugin's name since 2014 |
| `bakePreview` | task, group `jbake` | exists, but only `dependsOn 'preview'` (group `documentation`) | **kept, as the real task** | The JBake plugin's name; the old build configured its `port` here |
| `preview` | task | exists (T062): the real preview | **removed** | A second name for `bakePreview`. Mentioned twice in the README and once in a CI comment; those change with it |
| `jbake` | configuration | exists | **kept, the only one** | The JBake plugin's name for JBake's classpath; the pins live here |
| `jbakePreview` | configuration | exists, `extendsFrom jbake` | **removed** | Resolves to the same 77 artifacts; its two pins move into `jbake` |
| `newPost`, `reviewChanges`, later `openPr` | tasks, group `writing` | exist (group `writing`) | **kept** | Authoring tasks, not JBake's; one group of their own |
| `prepareImage` | task, group `writing` | exists (T140, 2026-10-27) | **kept** | Authoring, like the others |
| `checkSite`, `checkUrls`, `checkDocsNotPublished`, `checkMastheadVariants`, `checkTagRedirects` | tasks, group `verification` | shell scripts and CI steps | **new** (Phase B of [`08-SCRIPTS-MIGRATION.md`](08-SCRIPTS-MIGRATION.md), 2026-10-27) | Gradle's own group for checks; `check` depends on `checkSite` |
| `scripts/` | directory | four single-file Java programs and shell | **emptied to `smoke-test.sh` and `visual/`** (T127, T130, T143) | The programs become plugin classes; the smoke test runs after deploy without a JDK |
| `compileSass` | task, no group | exists (group `build`) | **kept, hidden** | An implementation detail of `bake`; `./gradlew tasks` doesn't list it |
| `jbakeSite { … }` | extension | none | **new** | Modelled on the JBake plugin's `jbake { srcDirName; destDirName }`, typed as directories. Not named `jbake`, which is the configuration |
| `jbake.properties` | JBake's own config | exists | **unchanged** | Site settings JBake reads itself (host, port, extensions) stay JBake's, not the extension's |

Two task groups, then: **`jbake`** for generating and previewing the site, **`writing`** for the author's
commands. `./gradlew tasks` shows exactly those two groups' tasks and nothing else of ours. _2026-10-27: plus
Gradle's standard **`verification`** group for the check tasks of Phase B, which `check` runs._

The single-file scripts stay as they are in Phase 1. They kept working without Gradle (`java scripts/NewPost.java`),
which the README promised, until T127; moving them into the plugin was a separate decision (T130). _Decided 2026-10-27:
they move into the plugin as task classes (T130, done in T127's PR); the no-Gradle use goes with them._

**Phase 2, sketched only: a standalone plugin** in its own repository, published to the Gradle Plugin Portal (or
GitHub Packages if the owner prefers private). Not started until Phase 1 has run for a while. For it, the plugin
must become generic: no `www.mikemcgarr.com` anywhere (the site host is JBake's `site.host` already), no fixed
`clean-blog.css` name, the case-redirect convention described rather than assumed (or dropped: a site without
`src/case-redirects` gets nothing), the script tasks as a general "run a single-file Java program with `-P`
options" feature, and documentation for someone who isn't the owner. **What would trigger it:** a second site to
build the same way, a wish to share it, or the `build-logic/` folder going three months without a change.

## Consequences

- **The site's output does not change.** Rule 1 is untouched: no URL, file or byte of `build/jbake` moves. Every
  slice is tested by a byte-identical bake, and CI's gates run unchanged.
- **The configuration cache keeps working.** A plugin written in Java with `Property`/`Provider` inputs is the
  shape Gradle designs the cache for; the TestKit tests run with it on, so a regression shows up in `build-logic`'s
  own tests before it reaches a bake.
- **Dependabot** needs a second `gradle` entry for `directory: /build-logic` in `.github/dependabot.yml`, with the
  same major-version ignore. Without it, the Sass host and Gradle plugin API versions there never get PRs.
- **`build.gradle` shrinks** to the plugin application, the extension block and the dependency pins: about 90
  lines, most of them the pins and the advisory comments that document them.
- **The single-file scripts could move into the plugin** as real classes with tests and shared helpers (`run`,
  `git`, `fail` are copied between `NewPost.java` and `ReviewChanges.java` today; `OpenPr.java` would copy them
  again). The loss is running them without Gradle. T130 records that decision; nothing in Phase 1 depends on it.
- **A second Gradle project to understand.** `build-logic/` has its own `build.gradle` (Java, `java-gradle-plugin`,
  the Sass host as an implementation dependency, JUnit and TestKit for tests) and its own build directory. The
  README's "How the site is built" section explains it.
- **Not a platform change.** The site stays on JBake ([`01-PLATFORM-DECISION.md`](archive/01-PLATFORM-DECISION.md)). The
  plugin wraps the way JBake is run today; it does not re-open how the site is generated.
- **Two names go away**: the `preview` task and the `jbakePreview` configuration (T133). Neither is a published URL
  or a documented command beyond two README sentences and a CI comment, which T133 updates. `./gradlew bakePreview`,
  the documented command, keeps working throughout.
- **The included build is compiled on every invocation that needs it**, including `bake` in CI. Compiling a
  handful of Java classes adds a few seconds once per daemon; Gradle caches it afterwards.

## Tasks

Task format and ticking rules: [`AGENTS.md`](../AGENTS.md), "Tasks and checklists". IDs continue the plan
numbering (T122 and CHK046 were the last used, in `05-REVIEW-FLOW.md`).

### M10: Build logic as a plugin

**Goal**: `build.gradle` says what the site is (source, CSS, scripts, dependency pins) and a plugin in
`build-logic/` says how it is built, with tests of its own, and the site's output is byte-identical throughout.

**Independent Test**: on a clean clone of `main`, `./gradlew clean bake` produces a `build/jbake` identical
(`diff -r`, apart from `feed.xml`'s timestamps) to one baked at the commit before M10 started; `./gradlew bake`
twice in a row prints "Reusing configuration cache"; `./gradlew :build-logic:test` passes; `wc -l build.gradle`
is under 120.

**Order**: T133 first; it needs no plugin and can run before M10 is scheduled, on its own, if the owner wants
the duplicates gone sooner. Then T123, then T124–T128 one at a time, each its own PR (a slice is done when its bake is
byte-identical and CI is green). T129 can start after T124 and grow with each slice. T131 after T128. T130 is a
decision, any time after T127. T132 closes the milestone.

_2026-10-27 (owner): a few larger PRs rather than one per slice, each merged before the next because they all touch
`build.gradle`: (1) T133; (2) T123 + T124; (3) T125 + T126; (4) T127 + T130, the four programs moving in as task
classes; (5) T128 + T129 + T131; (6) Phase B of [`08-SCRIPTS-MIGRATION.md`](08-SCRIPTS-MIGRATION.md) (T141, T143;
T142 is superseded) together with T121 `openPr` from [`05-REVIEW-FLOW.md`](05-REVIEW-FLOW.md). Every PR still passes
the byte-identical check, and the plugin's Java gets a code review before the owner sees it._

**The byte-identical check**, used by every slice's Test (the revival's method from T096):

```sh
git switch main && ./gradlew clean bake -q && cp -R build/jbake /tmp/bake-main
git switch <branch> && ./gradlew clean bake -q
diff -r /tmp/bake-main build/jbake | grep -v 'feed.xml'     # must print nothing
```

#### Improvements

- [x] T123 [IMP] **Scaffold the included build.** `build-logic/` with `settings.gradle`, a `build.gradle` applying
  `java-gradle-plugin` (JDK 21 toolchain, matching `.java-version`'s fallback), an empty `JbakeSitePlugin` with id
  `jbake-site` and an empty `JbakeSiteExtension`; `pluginManagement { includeBuild 'build-logic' }` in the root
  `settings.gradle`; `plugins { id 'jbake-site' }` in `build.gradle`, with everything else still in the script;
  a `build-logic` entry in `.github/dependabot.yml` (`settings.gradle`, `build.gradle`, `build-logic/`,
  `.github/dependabot.yml`)
  - **Test:** the byte-identical check passes. `./gradlew bake` twice prints "Reusing configuration cache" on the
    second run. `./gradlew :build-logic:build` succeeds. CI on the PR is green.
  - _2026-10-27: **done** (`chore/T123-T124-plugin-scaffold-sass`, PR 2 of the owner's order, with T124). `build-logic/`
    is an included build (`settings.gradle` names it `build-logic`; `build.gradle` applies `java-gradle-plugin`, JUnit
    6.1.3 with `useJUnitPlatform`, `gradleTestKit()` for the functional tests, `-Xlint:all -Werror` less the `classfile`
    group, which the Sass host's jar trips, and javadoc with `-Xdoclint:all,-missing`, since Gradle creates the
    extensions and tasks through implicit constructors) with the plugin `jbake-site`, class `com.mikemcgarr.jbakesite.JbakeSitePlugin`,
    and the `jbakeSite` extension (`JbakeSiteExtension`). The root `settings.gradle` has `pluginManagement { includeBuild
    'build-logic' }` with its repositories kept; `build.gradle` applies `jbake-site` beside `base`; `.github/dependabot.yml`
    has a second `gradle` entry for `/build-logic` (weekly, semver-major ignored, group `build-logic`, prefix
    `chore(build-logic)`). `.gitignore` already covers `build-logic/build/` and `build-logic/.gradle/` (its `build/` and
    `.gradle/` patterns match at any depth). **Deviation:** the plugin is compiled with `options.release = 21` on whatever
    JDK runs Gradle (21 or 25), not a JDK 21 *toolchain*: a toolchain would make Gradle look for, or download, a JDK the
    machine may not have, and the release flag gives the same guarantee (Java 21 bytecode and API) without it. **Test
    passed** at the scaffold commit (3c28496): the bake was byte-identical to the base (`diff -rq`: only `feed.xml`), the
    second `bake` printed "Reusing configuration cache", `:build-logic:build` passed. Not verified locally: CI (Linux)._
- [x] T124 [IMP] **Move the theme CSS.** `compileSass` (the Sass host becomes a dependency of `build-logic`, so the
  `io.freefair.sass-base` plugin leaves the root `plugins` block), the hand-edited-CSS guard, and the copy of the
  compiled CSS into the bake, driven by `jbakeSite.scss { entry; publishedPath }` (`build-logic/`, `build.gradle`,
  `src/scss/` unchanged)
  - **Test:** the byte-identical check passes, including `css/clean-blog.css`. Putting a `clean-blog.css` under
    `src/jbake/assets/css/` makes `./gradlew bake` fail with the T043 message; removing it restores the bake. A Sass
    syntax error in `_variables.scss` fails the build with Dart Sass's message.
  - _2026-10-27: **done** (same branch as T123). In the plugin, package `com.mikemcgarr.jbakesite`: `CompileSassTask`
    (`compileSass`, cacheable: compiles every entry, then writes exactly the entries' CSS to
    `build/generated/sass/<publishedDir>`), `CheckNoHandEditedCssTask` (`checkNoHandEditedCss`: fails when a file under
    `<source>/assets/<publishedDir>` has an entry's CSS name or a retired name; message unchanged from T043), `SassCompiler`
    (Dart Sass through `de.larsgrefer.sass:sass-embedded-host:4.6.1`, the version freefair 9.8.2 pulled, with the site's
    options: expanded, no source map, written as-is), `SassCompileException` (Dart Sass's formatted message),
    `SassEntries` (the convention: top-level `<name>.scss` not starting with `_` → `<name>.css`), `FileTrees` (the
    `copyTree` closure, now shared by the CSS copy and the case-redirect copy) and `ScssExtension`.
    `SassCompiler.compileAll(dir)` compiles every entry before anything is returned; the task writes its result and
    the preview's watcher atomically swaps it in, so the compile-then-write logic exists once (code review). `io.freefair.sass-base`
    is gone from `build.gradle`. **The extension** reads `jbakeSite { scss { source; publishedDir; retiredCss;
    handEditedCssExplanation } }`: a directory convention rather than the sketch's single `entry`/`publishedPath`, because
    the site has two entries and the README already states the convention ("each file not starting with `_` is an entry,
    published at `/css/<name>.css`"); `retiredCss` and `handEditedCssExplanation` carry the site-specific part of the
    guard (the retired names and the T043/T134/T135 sentence), so the plugin's message is the same text as before while
    the plugin itself knows no file names. `jbakeSite.source` (default `src/jbake`) arrived early, for the guard's assets
    folder. **What stayed in `build.gradle` until T125/T126:** `bake` (now `dependsOn checkNoHandEditedCss`, takes
    `compileSass`'s output as its `themeCss` input, and copies it with `FileTrees.copy` after JBake, as before),
    `bakePreview` (same, plus the watcher) and `SassPreviewWatcher`, whose own Dart Sass setup is replaced by the plugin's
    `SassCompiler` and `SassEntries`, so the preview compiles with exactly the task's options; the extension is read
    lazily there (`parameters.sourceDir = jbakeSite.scss.source`, the site's CSS directory as a provider of
    `build/jbake/<publishedDir>`, and the watcher's `/css/` log prefix from `publishedDir`). 19 unit tests (JUnit 6, with
    `ProjectBuilder` for the plugin wiring and the guard; the real Dart Sass on small SCSS) and 4 TestKit tests in a temp
    build with the configuration cache on: the CSS is compiled and the guard runs; a Sass error in a partial fails
    `compileSass` with Dart Sass's `file line:col` message and writes nothing; a second run is `UP-TO-DATE` and prints
    "Reusing configuration cache"; a renamed entry leaves no stale CSS. **CI** runs `./gradlew :build-logic:check bake` on
    every PR (the "Bake with Gradle" step), so the plugin's unit tests and `validatePlugins` gate the bake from this PR
    on; T129's "add it to the workflow" is thereby done for the check, and T129 still owns the fixture site and the
    remaining functional tests. **Test passed:** the bake is byte-identical
    to the base commit's (`diff -rq`: only `feed.xml`; `cmp` of `css/clean-blog.css` and `css/code.css` identical); a
    `clean-blog.css` under `src/jbake/assets/css/` fails `bake` in `:checkNoHandEditedCss` with the T043 message word for
    word, and the bake passes once it is deleted; `$oops: ;` in `_variables.scss` fails `:compileSass` with Dart Sass's
    message (the caret, `_variables.scss 55:8`, the `@use` trace); `./gradlew bake` twice reuses the configuration cache;
    `:build-logic:build` and `:build-logic:javadoc` pass with no warnings; `./gradlew tasks` shows the Jbake and Writing
    groups exactly as after T133 and no `compileSass`; `clean bakePreview` served :8080, recompiled both entries on a saved
    partial (the watcher path) and stopped cleanly, and the next `clean bake` has no `livereload`; `check -Pbase=origin/main`
    0 errors; `test ! -e build/jbake/docs` and `scripts/check-docs-not-published.sh` pass; no CR line endings. Not
    verified locally: the Linux bake (CI). CI still runs no `:build-logic:test`; that step is T129's._
- [x] T125 [IMP] **Move the JBake run.** The `jbake` configuration (the only one after T133) with the engine exclusions and
  the lz4 substitution, `NewJdkJvmArgs`, the jffi and logback settings, the case-redirect copy with its
  case-sensitivity check, and the `bake` task, driven by `jbakeSite.source` and `jbakeSite.caseRedirects`. The
  `dependencies` block with the pins stays in `build.gradle` (`build-logic/`, `build.gradle`, `gradle/jbake-logback.xml`)
  - **Test:** the byte-identical check passes on macOS and, through CI, on Linux (the stubs under
    `src/case-redirects` are in the Linux bake and `scripts/check-urls.sh` passes there). On JDK 21 and JDK 25 a
    clean bake prints no JVM warning. `./gradlew dependencies --configuration jbake` lists the same resolved
    versions as on `main` (diff the two listings).
  - _2026-10-28: **done** (`chore/T125-T126-plugin-bake-preview`, PR 3 of the owner's order, with T126). In the plugin,
    package `com.mikemcgarr.jbakesite`: `JbakeClasspath` (creates the `jbake` configuration: declarable and resolvable, not
    consumable; the five group exclusions and the lz4 substitution are applied as it resolves, read from
    `jbakeSite.classpath { excludedGroups; substitutions }`, `JbakeClasspathExtension`, whose defaults are T112's lists, so
    a site that uses one of the engines can have it back; more exclusions still go in `configurations.jbake { exclude … }`
    as before), `JbakeTask` (what the bake and the preview share: JBake's launcher in a fresh JVM through
    `ExecOperations.javaexec`, in the project directory, on the `jbake` classpath, with `jvmArguments` defaulting to
    `NewJdkJvmArgs.forCurrentJvm()`, jffi's temp dir under `build/tmp/jffi`, the logback configuration, and, since the
    review, the compiled theme CSS and its copy into the site, `copyThemeCssInto`), `NewJdkJvmArgs`
    (JDK feature version → flags, a final helper now instead of a `CommandLineArgumentProvider`), `BakeTask` (`bake`: JBake
    `-b`, then the compiled CSS copied in, then the stubs), `CaseRedirects` (the probe and the copy: the probe writes a
    temporary file into the output and looks for its upper-case twin, instead of checking for `INDEX.HTML`, so it doesn't
    depend on which pages a site renders; a missing or empty stubs directory means none, quietly) and `ProjectPaths` (the
    project-relative paths the messages and the task descriptions use). **The extension** reads `jbakeSite { source;
    destination; caseRedirects; logbackConfiguration; classpath { excludedGroups; substitutions } }` besides `scss`;
    `destination` defaults to `build/jbake`, `caseRedirects` to `src/case-redirects`. **Logback:** `gradle/jbake-logback.xml`
    moved unchanged into the plugin as a resource (`build-logic/src/main/resources/com/mikemcgarr/jbakesite/`), passed to
    JBake's JVM as the resource's URL, which logback accepts like a path; `jbakeSite.logbackConfiguration` names a file
    instead when a site wants its own. **Inputs and outputs:** `bake` declares the source directory, the stubs, the compiled
    CSS, the classpath (`@Classpath`), the JVM flags and the optional logback file as inputs and the destination as its
    output directory, so a second `bake` is UP-TO-DATE and the bake after a preview runs again (the preview rewrites the
    output with the local host and the live-reload script). It is **not** `@CacheableTask`: JBake stamps `feed.xml` with
    the time of the bake, so the output isn't a function of the inputs and a cached copy would be a stale one. **Judgment
    calls:** `ExecOperations.javaexec` in a plain task rather than a `JavaExec` subclass, so the two tasks share one small
    base class and declare only the inputs they mean; no `JavaLauncher`/toolchain (none was set before: JBake runs on the
    JVM running Gradle, and `NewJdkJvmArgs` reads that JVM's version); the `bake` description is the literal text from
    `main` (the review dropped a version built from the extension, the plugin's only eager extension read); the helpers
    `JbakeClasspath`, `NewJdkJvmArgs`, `CaseRedirects` and `PreviewPort` are package-private (review), the plugin, the
    extensions, the task types and the watcher public. **What stayed in `build.gradle` for this commit, until T126:**
    `bakePreview` as a `JavaExec` configured inline with the plugin's `NewJdkJvmArgs` and logback resource, the Groovy
    watcher and the port check. **Test passed:** the bake is byte-identical to the base commit's (`diff -rq`: only
    `feed.xml`; `cmp` of `css/clean-blog.css` and `css/code.css` identical); `./gradlew dependencies --configuration jbake`
    before and after differ only in the configuration's new description line, every resolved module and version the same;
    a clean bake on JDK 25.0.4 and on JDK 21.0.3 (`JAVA_HOME`) prints no JVM warning and is byte-identical too;
    `./gradlew tasks` shows the Jbake and Writing groups as after T133; `:build-logic:check` passes with 39 tests: the
    unit tests of `NewJdkJvmArgs`, `CaseRedirects` and `JbakeClasspath` (the exclusions are applied on resolve and the
    extension overrides them), the plugin wiring, and the TestKit `BakeFunctionalTest` on the fixture site
    (`build-logic/src/test/fixtures/site`: one post, one page, one SCSS entry with a partial, one stub; JBake really runs
    under `--configuration-cache`: the pages render, the CSS is copied in, the stub is added only where the filesystem is
    case-sensitive and the macOS message prints otherwise, a second run is UP-TO-DATE and reuses the configuration cache,
    a changed post rebakes, and the `dependencies` report shows the lz4 substitution and none of the excluded groups).
    Not verified locally: the Linux bake with the stubs and `scripts/check-urls.sh` (CI)._
- [x] T126 [IMP] **Move the preview.** `SassPreviewWatcher` as a Java build service, the port check (reading
  `server.hostname`/`server.port` from `jbake.properties`), the live-reload system property, and the `bakePreview`
  task (the only preview task after T133) (`build-logic/`, `build.gradle`)
  - **Test:** `./gradlew clean bakePreview` serves http://localhost:8080, a saved post shows in the open page
    within about 2 s, a saved SCSS change recompiles the CSS (the log says so), a Sass error keeps the last good
    CSS, Ctrl+C leaves no process, and a second preview fails with the port message (T101). After the preview,
    `./gradlew clean bake` output has no live-reload script (`grep -rli livereload build/jbake` is empty).
  - _2026-10-28: **done** (same branch as T125, the second commit). In the plugin: `BakePreviewTask` (`bakePreview`, a
    `JbakeTask` like the bake: the port check, the compiled CSS put in place, the watcher started, then JBake `-b -s` with
    `preview.livereload=true` in its JVM; no outputs, so it runs every time; depends on the guard, and on `compileSass`
    through its CSS input), `SassPreviewWatcher` (the build service in Java, the same behaviour: a daemon thread stamps
    the SCSS files every 0.5 s, recompiles every entry with the plugin's `SassCompiler.compileAll` into temporary files
    that are moved into the served CSS directory atomically, keeps the last good CSS and prints Dart Sass's message on an
    error, and the same four log lines, the SCSS directory named through a `sourceName` parameter set to its
    project-relative path, `src/scss`) and `PreviewPort` (`server.hostname`/`server.port` from `jbake.properties`, JBake's
    defaults `localhost`/8820, a bad port names the file as the README does, `src/jbake/jbake.properties`; the probe socket binds with address reuse as Jetty does and fails
    with the T101 message, which names `src/jbake/jbake.properties` through the task's `ProjectLayout`). `build.gradle`
    now holds the plugin application, the `jbakeSite { }` block, the `dependencies { jbake … }` pins with their comments
    and the three writing tasks (T127's), 174 lines; the Groovy watcher, the port check, the paths, `configureJBake` and
    `NewJdkJvmArgs` are gone from it. **Judgment calls:** the `bakePreview` description keeps its literal text (the
    address would have to be read from `jbake.properties` at configuration time to compute it); the watcher is registered
    by the plugin with `registerIfAbsent` and the task declares `usesService`; no TestKit test of the live server, which
    would need the port (T129 may), `PreviewPort` is unit-tested against a socket that really listens. **Test passed:**
    `./gradlew clean bakePreview` served http://localhost:8080 after 3 s with the live-reload script; a line appended to a
    draft showed in the served page 0.4 s later (JBake's watcher); a comment appended to `_variables.scss` logged
    "src/scss changed: recompiled /css/clean-blog.css, /css/code.css" 0.2 s later, the CSS byte-identical to before; `$oops: ;`
    there logged "src/scss changed, but Sass failed (the preview keeps the last good CSS):" with Dart Sass's message and
    `cmp` showed the served CSS unchanged; a second `./gradlew bakePreview` while the first ran failed with "Port 8080 is
    already in use (is another bakePreview running?). Stop it, or set server.port in src/jbake/jbake.properties."; Ctrl+C
    (SIGINT to the gradlew client) ended the client with 130 and within 1 s there was no JBake JVM and nothing listening on
    8080; the next `./gradlew clean bake` has no `livereload` (`grep -rli` empty) and is byte-identical to the base
    commit's (only `feed.xml`); `./gradlew bake` twice prints "Reusing configuration cache" and `:bake UP-TO-DATE`;
    `./gradlew tasks` shows the Jbake group (`bake`, `bakePreview`) and the Writing group as after T133; `:build-logic:check`
    passes with 45 tests (T125's 39, `PreviewPortTest` and the preview's wiring); `check -Pbase=origin/main` 0 errors;
    `test ! -e build/jbake/docs` and `scripts/check-docs-not-published.sh` pass; no CR line endings. Not verified
    locally: the Linux bake (CI)._
  - _2026-10-28 (review of the branch, "meets the bar", no must-fix; applied in the commit "Apply the review findings to
    the bake and preview tasks"): the theme-CSS input, its published directory and the copy loop, declared twice, moved to
    `JbakeTask` (`copyThemeCssInto`; only `destination` stays per task, output for the bake, internal for the preview) and
    the plugin wires them once in `withType(JbakeTask)`; `JbakeClasspath`, `NewJdkJvmArgs`, `CaseRedirects` and `PreviewPort`
    package-private; `PreviewPort.read` takes the display name too, so a bad `server.port` says "in
    src/jbake/jbake.properties"; `BakeFunctionalTest` fails outside Gradle with "fixtures.dir is set by
    build-logic/build.gradle; run the tests through Gradle" instead of an NPE, and gained a case for a JBake error (a
    broken `page.ftl`: `:bake` FAILED, JBake's `ERROR` line naming `page.ftl`, and the task's "finished with non-zero exit
    value"); the `bake` description is literal; a comment says why the exclude rules go in `withDependencies`; the watcher's
    service name carries the project path and `start()` throws on a second call. 46 tests. **Re-run:** the bake is
    byte-identical to a fresh bake of `origin/main` (only `feed.xml`; both CSS files `cmp` identical); `bake` twice:
    "Reusing configuration cache." and `:bake UP-TO-DATE`; `./gradlew tasks` groups unchanged; `clean bakePreview` served
    after 3 s with live reload, recompiled both entries 0.2 s after a saved partial, and Ctrl+C ended the client with 130
    with no JBake JVM and nothing on 8080 a second later. **Observed** (asked by the review): switching `JAVA_HOME`
    25 → 21 → 25 prints "configuration cache cannot be reused because JVM has changed", and JBake's command line (`--info`)
    has `--enable-native-access`/`--sun-misc-unsafe-memory-access` on 25 and neither on 21, so the `jvmArguments`
    convention follows the JVM; no code change needed._
- [x] T127 [IMP] **Move the script tasks.** One `ScriptTask` type (a single-file Java program run with the Gradle
  JVM's `java`, `-P` options passed as `--name=value`, exit codes mapped to one-line failures), registered from
  `jbakeSite.scripts { register(name, path) { options; dependsOn } }`; `newPost` and `reviewChanges` become two
  entries (`build-logic/`, `build.gradle`; `scripts/*.java` unchanged)
  - **Test:** `./gradlew newPost -Ptitle="M10 Test"` creates the draft on `post/m10-test` as before (then delete
    the branch and the file). `./gradlew reviewChanges` on a clean tree prints "Nothing to review"; with the #139
    About file (`git show 756245f:src/jbake/content/about.md > src/jbake/content/about.md`) it fails with the same
    one-line message as on `main`. `./gradlew newPost` with no title fails with NewPost's own message, not a stack trace.
  - _2026-10-27: with T130 decided (move), T127 moves the four programs (`NewPost`, `ReviewChanges`, `PrepareImage`,
    `TagRedirects`) into `build-logic/` as task classes with shared helpers and unit tests, instead of wrapping them
    as `Exec`s; the `-P` option passing and the exit-code mapping stay as described. CI's
    `java scripts/ReviewChanges.java` and `java scripts/TagRedirects.java --check` steps become Gradle tasks._
  - _2026-10-28: **done** (branch `chore/T127-T130-scripts-into-plugin`). The four programs are task classes in
    `build-logic/`, each a plain logic class with the paths, git, the clock and the output injected, run by a thin
    managed task: `writing` (`NewPost`/`NewPostTask`, `PrepareImage` + `Images` + `MastheadVariants`/`PrepareImageTask`,
    `TagRedirects`/`TagRedirectsTask`), `review` (the design doc's structure: `Changes`, `Pages`, `MarkdownChecks`,
    `ImageChecks`, `RemovalChecks`, `PageChecks`, `HtmlTags`, `Suppressions`, `Findings`, `Report`, `Review`,
    `ReviewChangesTask`), `verification` (`CheckTagRedirectsTask`, `CheckMastheadVariantsTask`: the programs' `--check`
    modes, so **T141 in `08` now covers only `checkUrls`, `checkDocsNotPublished` and the `checkSite` aggregate**) and
    `internal` (`Git`, `ProcessRunner`, `ToolFailure`, `FrontMatter`, `Tags`, `SiteLayout`, `JbakeProperties`, once).
    `SiteToolTask` is their base. `build.gradle` is down to the plugin application, the `jbakeSite { }` block and the
    pins (95 lines); the four Exec blocks and `scripts/{NewPost,ReviewChanges,PrepareImage,TagRedirects}.java` are gone,
    and CI's three `java scripts/…` steps are `./gradlew reviewChanges -Pbase=…`, `checkTagRedirects -PsiteDir=build/site`
    and `checkMastheadVariants`. **Strings that changed on purpose:** the `newPost:`/`reviewChanges:`/`PrepareImage:`
    prefixes (Gradle names the task); `--all`/`--base=`/`--online`/`--keep-source` read `-Pall`/`-Pbase=`/`-Ponline`/
    `-PkeepSource` in messages; the E5 hints and the masthead FAIL line say `./gradlew prepareImage -Pimage=<image>
    -Pvariants` (the new spelling of `--variants`); `prepareImage` fails with "Not every image could be prepared (see
    above)." where the program exited 1 silently. **Judgment calls:** `-Pvariants` added; `site.host` read from
    `jbake.properties` instead of a constant (byte-identical here); `jbakeSite.publishedUrls` and `jbakeSite.tagRenames`
    (default: the owner's `RENAMES`) so the fixture site can differ; the W3 check skips a missing `blog/drafts/`
    instead of crashing. **Tests passed:** the bake is byte-identical to `main`'s (feed.xml's timestamp aside); on a
    clean tree `tagRedirects` writes nothing (59 stubs, `git status` clean) and regenerates a deleted stub byte for
    byte; `checkTagRedirects` prints the program's counts (176 tag pages, 229 old URLs: 170 real, 57 stubs, 2 not
    checkable on macOS) and fails on a copy with a stub missing ("missing from the build") or edited ("redirects
    to"); `checkMastheadVariants` passes (15) and names a variant moved aside; `prepareImage` on the Maui PNG
    (scratch commit) dry-runs clean and writes a 1920x691 q82 130K progressive JPEG with no Exif/ICC/XMP bytes, the
    variants, the repointed post, and removes the PNG, 28 committed variants untouched; `hawaii.png -Pjpeg` is
    refused with the D2 message; `reviewChanges` matches the program's output line for line on a clean tree, the
    #139 About file (3 findings, with `-Pstrict`, and with the GitHub annotations and step summary), `-Pall` (78
    pages, 0/0) and the usage errors; `newPost -Ptitle="M10 Test"` writes the same draft on `post/m10-test`;
    `:build-logic:check` passes (140 tests, TestKit included); `./gradlew tasks` shows the groups jbake, writing and
    verification only._
- [x] T128 [IMP] **Shrink `build.gradle`.** Remove every helper, class and comment that moved; keep the dependency
  pins and their advisory comments; add a short header comment pointing at `build-logic/` and this plan
  (`build.gradle`)
  - **Test:** `wc -l build.gradle` is under 120. The byte-identical check passes. `./gradlew tasks --group jbake`
    and `--group writing` list exactly the surface table's tasks, with the same descriptions as after T133.
  - _2026-10-28: **done** (`chore/T128-T129-T131-shrink-tests-docs`, PR 5 of the owner's order, with T129 and T131).
    After T127 the file was 95 lines and held nothing that had moved; what changed is the form: a five-line header
    (what the file is, what `build-logic/` is, where the plan and the README are), `id 'base'` dropped (the plugin
    applies Gradle's `base` plugin, so `clean` and `check` come with `jbake-site`; a comment on the plugin line says
    so), the `jbakeSite` block's comments cut to what the site is (the stubs' and the guard's mechanics are the
    plugin's Javadoc, `CaseRedirects` and `CheckNoHandEditedCssTask`), and the T096 history (why JBake runs from a
    plain configuration and not the `org.jbake.site` plugin) kept in four lines above the pins, which it explains.
    The pins and their advisory comments are unchanged line for line (`git diff` touches no `jbake '...'` line).
    86 lines. **Test passed:** `wc -l build.gradle` 86; `clean bake` byte-identical to a bake of the base commit
    (`diff -rq`: only `feed.xml`); `./gradlew tasks` shows Jbake (`bake`, `bakePreview`), Writing (`newPost`,
    `prepareImage`, `reviewChanges`, `tagRedirects`) and Verification (`check`, `checkMastheadVariants`,
    `checkTagRedirects`) with the descriptions of the T133 and T127 notes, `clean` still under Build, and no
    `compileSass`._
- [x] T129 [IMP] **TestKit tests for the plugin.** A fixture site under `build-logic/src/test/fixtures/site` (two
  posts, one page, one tag, `clean-blog.scss` with one partial, one case-redirect stub) and `GradleRunner` tests,
  run with `--configuration-cache`: the bake renders the pages and copies the CSS; the stubs appear when the
  filesystem is case-sensitive and are skipped with the message when it isn't; the hand-edited-CSS guard fails;
  a script task passes `-P` options through and maps exit 1 to a failure; the second run reuses the cache
  (`build-logic/src/test/`)
  - **Test:** `./gradlew :build-logic:test` passes locally and in CI (add it to the workflow before "Bake with
    Gradle"). Breaking the CSS copy on purpose fails exactly the test that covers it.
  - _2026-10-28: **done** (same branch as T128). The tests grew with each slice (T124's `JbakeSitePluginFunctionalTest`,
    T125's `BakeFunctionalTest`, T127's `SiteToolsFunctionalTest`, all `GradleRunner` with `--configuration-cache`);
    this task audited them against the list above and filled the gaps. **The fixture** now matches the text: two posts
    (`first-post.md`, tagged `fixture` and `acceptance test` for the tag redirect stub of T127's tests, and the new
    `second-post.md`, tagged `fixture`, so one tag page lists both), one page, `clean-blog.scss` (was `site.scss`;
    the generic-name case lives on in `JbakeSitePluginFunctionalTest`, which writes its own `site.scss`) with the
    `_variables.scss` partial, the `About.html` case-redirect stub, and `blog/second-post.html` in its baseline.
    **New cases:** the hand-edited-CSS guard as a TestKit run (a `clean-blog.css` under the fixture's assets fails
    `:checkNoHandEditedCss` with the plugin's default sentence naming `src/scss`, `:bake` never starts, nothing is
    baked, and deleting the file restores the bake; until now only a `ProjectBuilder` test covered the guard);
    `newPost -Ptitle -Ptags -Psummary -Pbranch=false` writes the draft with every option in its header and the
    `site.host` of the fixture's `jbake.properties` in its last line (the `-P` pass-through); `newPost` with no title
    fails the task with the program's one-line message and no `ToolFailure` in the output (exit 1 mapped to a
    failure). The bake test also asserts the second post, the shared tag page and the stylesheet link. Already
    covered and kept: the stubs by filesystem case-sensitivity (the macOS message or the stub beside the real page),
    UP-TO-DATE and "Reusing configuration cache." on the second run, a changed post rebaked, a JBake error, the
    `jbake` configuration's exclusions and substitution, the review, both checks, `tagRedirects`, `check`. **CI**
    has run `./gradlew :build-logic:check bake` since T124's PR; the step comment now says that includes the TestKit
    tests. **Test passed:** `./gradlew :build-logic:test` passes, 145 tests (142 before this task; 17 of them TestKit:
    4 + 6 + 7); with `copyThemeCssInto(destination)` commented out in `BakeTask`, `--continue` reports "145 tests
    completed, 1 failed", `bakesThePagesCopiesTheCssInAndAddsTheStubsWhereItCan` (`NoSuchFileException:
    build/jbake/css/clean-blog.css`), and nothing else; reverted. By design, not in TestKit: `prepareImage`'s `-P`
    pass-through is covered at unit level only (`PrepareImageTest` on the options; the TestKit pass-through case is
    `newPost`'s), and `bakePreview` has no TestKit test because it needs a free port and a running server with
    timing, so there is no flakiness risk from either. Not verified locally: the Linux run (CI, where the stub
    branch of the bake test is the case-sensitive one)._
- [x] T130 [IMP] _(Decision)_ **Move `NewPost.java` and `ReviewChanges.java` into the plugin, or keep them as
  single-file programs.** For: one place, shared `run`/`git`/`fail` helpers (copied between the two today, and
  `OpenPr.java` would copy them a third time), unit tests. Against: `java scripts/X.java` without Gradle stops
  working, and the README promises it. Record the decision and, if moving, add a task per script
  (`docs/06-GRADLE-PLUGIN.md`)
  - **Test:** a dated note under this task records the decision and why, with the owner's say.
  - _2026-10-27: **move** (owner: "the Java scripts should be moved to the plugin. Move them to the gradle plugin as
    java files"). Since 2026-10-22 the count grew to four programs (`PrepareImage`, `TagRedirects` joined in Phase A of
    `08`), each copying the same helpers, and `OpenPr` would be a fifth. Consequences: `java scripts/X.java` without
    Gradle stops working and the README promise goes (T131); the helpers exist once, with unit tests; `OpenPr`
    (T121) is born as a plugin class. The move itself is T127's PR._
  - _2026-10-28: **done** in T127's PR: the four programs are task classes in `build-logic/`, and `scripts/` holds
    `smoke-test.sh`, the two shell checks (until T143) and `visual/`._
- [x] T131 [IMP] **Docs.** README "How the site is built and deployed" describes `build-logic/` (what is in the
  plugin, what is in `build.gradle`, how to run its tests); AGENTS.md's Generator line and Layout tree mention
  `build-logic/`; the T096/T112/T113 notes in the archived revival plan are left as history (`README.md`,
  `AGENTS.md`)
  - **Test:** a new reader can find, from the README alone, where `bake` is defined and how to run the plugin's
    tests. `scripts/check-docs-not-published.sh` still passes.
  - _2026-10-28: **done** (same branch as T128). README: the "full description comes with the last slice" paragraph is
    now a subsection, "Where the build is defined: `build.gradle` and `build-logic/`": the two halves and why they are
    apart; `bake` is `BakeTask.java`, registered by `JbakeSitePlugin.java`; a table of the plugin's classes by package
    and what each group does; `SiteToolTask` and the `ToolFailure` mapping; the logback resource; a table of every
    `jbakeSite { }` property with its default and meaning; the `jbake` configuration, the exclusions and the
    substitution and how to override them; how the preview's watcher works; the tests (`./gradlew :build-logic:check`,
    what the unit and TestKit tests cover, the fixture site); a five-step "To add a build feature" recipe. The two
    groupless tasks (`compileSass`, `checkNoHandEditedCss`) are named under the task table; "To preview" says the
    fresh JVM is the plugin's, not `build.gradle`'s. AGENTS.md: the Generator line names the plugin and what
    `build.gradle` still is; the Layout tree has `build.gradle`, `build-logic/` and what is left in `scripts/` (the two
    shell checks until T143, `smoke-test.sh`, `visual/`); the Commands block adds `tagRedirects` and
    `:build-logic:check` and spells out what `check` runs; Rule 1's "once it exists" about `check-urls.sh` (true since
    T006) is a real command. Also the last `./gradlew preview` outside the archive, a comment in
    `src/jbake/jbake.properties`, says `bakePreview` (CHK052). The archived revival plan's T096/T112/T113 notes are
    untouched. **Test passed:** `grep -n 'BakeTask.java\|:build-logic:check' README.md` finds both in the new
    subsection; `scripts/check-docs-not-published.sh build/jbake` OK and `test ! -e build/jbake/docs`; the bake after
    the `jbake.properties` comment change is byte-identical to the base commit's (only `feed.xml`); no CR line
    endings in the edited files._
- [x] T133 [IMP] **Consolidate on the standard: one preview task, one configuration, two groups.** Apply the surface
  table in `build.gradle` as it is today, before any plugin exists: register the real preview task as `bakePreview`
  and drop `preview`; move the Jetty and commons-vfs2 pins into `jbake` and drop `jbakePreview` (the resolution
  strategy then applies once); put `bake` and `bakePreview` in group `jbake`, leave `newPost` and `reviewChanges` in
  `writing`, and give `compileSass` no group; update the two README sentences and the CI comment that say
  `./gradlew preview`; add the surface table's summary to the README's "How the site is built" (`build.gradle`,
  `README.md`, `.github/workflows/gradle.yml`). Added 2026-10-22 at the owner's request; it is the first slice
  because every later one builds on these names
  - **Test:** the byte-identical check passes. `./gradlew dependencies --configuration jbake` lists the same 77
    resolved artifacts and versions as `--configuration jbakePreview` did on `main` (save that listing first).
    `./gradlew clean bakePreview` serves http://localhost:8080 with live reload and SCSS recompile as before, and
    `./gradlew preview` fails with Gradle's "task 'preview' not found". `./gradlew tasks` shows a `Jbake tasks`
    group with `bake` and `bakePreview`, a `Writing tasks` group with `newPost` and `reviewChanges`, and no
    `compileSass`. `grep -rn 'gradlew preview\|jbakePreview' README.md AGENTS.md .github build.gradle` is empty.
  - _2026-10-27: **done** (`chore/T133-consolidate-names`). `bakePreview` is the real preview task and `preview` is gone;
    the Jetty and commons-vfs2 pins are direct `jbake` dependencies (the two constraints that repeated them went too) and
    `jbakePreview` is gone; the lz4 substitution sits inside the `jbake` configuration; `bake` and `bakePreview` are in
    group `jbake`, `newPost`, `reviewChanges` and `prepareImage` (T140, added after this table) in `writing`,
    `compileSass` has no group. README: the two `./gradlew preview` sentences and a new task table under "How the site
    is built and deployed"; the CI live-reload comment. **Test passed:** the bake is byte-identical to main's (only
    `feed.xml`'s timestamps; `diff -rq` is the unambiguous form of the check); `--configuration jbake` resolves the same 77
    artifacts and versions `jbakePreview` did on `main`; `clean bakePreview` served :8080 with the live-reload script,
    recompiled both entries on a saved partial and left no process after Ctrl+C, and the next `clean bake` has no
    `livereload`; `./gradlew preview` says "Task 'preview' not found"; `./gradlew tasks` shows exactly the Jbake and
    Writing groups; the grep is empty; a second `bake` reuses the configuration cache; `check -Pbase=origin/main` 0
    errors. Not verified locally: the Linux bake (CI)._
- [ ] T132 [IMP] _(Decision)_ **Phase 2: standalone plugin, or not yet.** After Phase 1 has run for a while, check
  the triggers above (a second site, a wish to share, three quiet months) and decide. If yes, a new numbered doc
  plans the extraction: repository, publishing (Plugin Portal or GitHub Packages), what becomes generic, and how
  this repository consumes a published version (`docs/`)
  - **Test:** a dated note under this task records the decision and the trigger that fired, or that none did.

#### Checkpoint: M10

- [ ] CHK047 A clean bake on `main` is byte-identical (apart from `feed.xml`'s timestamps) to the bake at the commit
  before T123, on macOS and on Linux (CI's `build/site`, compared through the `check-urls.sh` and link checks plus
  one manual `diff -r` of a downloaded artifact)
  - _Partially verified 2026-10-28, on the branch `chore/T128-T129-T131-shrink-tests-docs` (T123–T131 all in, not
    yet on `main`): a macOS `clean bake` of the branch is byte-identical to the bake at 360bef7 (the T133 merge, the
    last commit before T123, baked in a detached worktree): `diff -rq` prints only `feed.xml`, 496 files each, `cmp`
    of `css/clean-blog.css` and `css/code.css` identical. Linux: CI's gates (`check-urls.sh` strict,
    `checkTagRedirects`, lychee) ran green on every M10 PR, but the manual `diff -r` of a downloaded artifact is not
    done yet; it is done after this PR merges, against `main`, and ticks the box._
- [x] CHK048 `./gradlew bake` twice in a row prints "Reusing configuration cache"; `bakePreview`, `newPost` and
  `reviewChanges` run with no configuration-cache problems reported
  - _2026-10-28: `bake` twice: the second prints "Reusing configuration cache." and `:bake UP-TO-DATE`.
    `bakePreview` served http://localhost:8080 after 2.6 s with the live-reload script, the watcher's "Watching
    src/scss" line and no problem report; SIGINT ended the client with 130, and a second later no JBake JVM and
    nothing on 8080. `reviewChanges`: 6 pages from 4 changed files (PR 171's templates), 0 errors, 0 warnings,
    "Configuration cache entry stored", no problems. `newPost -Ptitle="CHK048 Test"` wrote
    `drafts/chk048-test.md` on `post/chk048-test` from `main`, no problems; branch and file deleted after._
- [x] CHK049 `./gradlew :build-logic:test` passes and runs in CI before the bake
  - _2026-10-28: 145 tests pass locally (JDK 25); CI's "Bake with Gradle" step is `./gradlew :build-logic:check bake`,
    so the tests and `validatePlugins` run before the bake on every PR and push (since T124's PR, #169). Not verified
    here: this branch's own CI run (it is pushed for review before its PR opens)._
- [x] CHK050 `wc -l build.gradle` is under 120, and `.github/dependabot.yml` has the `build-logic` entry
  - _2026-10-28: 86 lines (T128); the `gradle` entry for `directory: /build-logic` (weekly, semver-major ignored,
    group `build-logic`) has been in `.github/dependabot.yml` since T123._
- [ ] CHK051 T130 and T132 each have a recorded decision
- [x] CHK052 `./gradlew tasks` lists exactly the surface table (groups `jbake` and `writing`), and `preview` and
  `jbakePreview` appear nowhere outside the archived docs (T133)
  - _2026-10-28: `./gradlew tasks` shows Jbake (`bake`, `bakePreview`), Writing (`newPost`, `prepareImage`,
    `reviewChanges`, `tagRedirects`) and Verification (`check`, `checkMastheadVariants`, `checkTagRedirects`; the group
    the 2026-10-27 note added), nothing else of ours, and no `compileSass`. `grep -rn 'gradlew preview\|jbakePreview'`
    outside `docs/archive` finds the two names only where they are the subject: this plan (the Context table, the
    surface table, T133 and its notes) and the dated report `docs/reports/platform-check-2026-10-11.md`, which the
    conventions say is not edited after the fact. The one stale use, a comment in `src/jbake/jbake.properties`, is
    fixed in T131._

### GitHub Issues

No open issue maps to this plan. The one open issue, [#14](https://github.com/jmcgarr/jmcgarr.github.io/issues/14)
(mastheads for older posts), is T059 in [`archive/04-AUTHORS-DESK.md`](archive/04-AUTHORS-DESK.md). If the owner files an issue
for the plugin work, add its row here with the task and the `Closes #N` line, per AGENTS.md Rule 5.

| Issue | Title | GitHub label | Task(s) | Milestone | Put in the resolving PR |
|---|---|---|---|---|---|
| _none yet_ | | | | | |

## Notes

- _2026-10-22: written at the owner's request, as backlog. The owner's framing: "In looking at the `build.gradle`
  file, it has expanded quite a bit with some additional complexity derived from the various custom tasks we've
  added over time. It's pretty unwieldy. Let's add a project to backlog to extract all of this complexity into a
  gradle plugin. At first, this plugin should be hosted within the project. Then, once we refine the functionality,
  we can then extract it into another plugin." Phase 1 is the in-repository plugin (T123–T131); Phase 2 is the
  standalone one (T132 decides when)._
- _2026-10-22 (later): **the standard.** The owner, on the open question of the `jbake`/`jbakePreview` configurations:
  "I am fairly certain these tasks predated the current revival project. If their use has been supplanted by new
  tasks (like preview), we should merge or consolidate on the standard for jbake. If there is none, we should define
  one." Checked: they are configurations, not tasks, and both are the revival's (T062/T096/T112); what predates it
  is the JBake plugin's `bake`, `bakePreview` and `jbake` names. The two configurations resolve identically (77
  artifacts each), and `preview` duplicates `bakePreview`. Hence the surface table above and T133, which can run
  before the rest of M10._
- _2026-10-27: **accepted and started** (owner). Decisions the same day: T130 is "move" (above); the work goes in a
  few larger PRs in the order given under M10; the check tasks of Phase B of `08-SCRIPTS-MIGRATION.md` take Gradle's
  `verification` group and run under `check`; T121 (`openPr`, `05-REVIEW-FLOW.md`) is built here after T127. The
  owner's bar for the plugin: "solid, well organized clean Java code". T133 started first, on its own branch._
