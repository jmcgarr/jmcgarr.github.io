# Plugin architecture review

**Branch**: `docs/plugin-review-report` | **Created**: 2026-10-30 | **Status**: Report

## Verdict

This is a good small build tool, and it is cleaner than six rushed PRs usually produce. The Gradle idiom is right where it matters (abstract managed tasks, lazy `Property` wiring, `providers.gradleProperty`, no `Project` at execution time, a build service that closes its thread, `ExecOperations` for the JBake JVM, honest `@UntrackedTask` reasons). The tool classes are plain Java behind a `CommandRunner` seam, so most of the logic is tested without Gradle. The weak spots are modest and mostly about seams between the six PRs: a few helpers copied rather than shared, the `-P` flag parsing done two ways, "public because another package needs it" leaking into the `internal` package as a grab bag, three or four very long methods in the review and writing tools, and Javadoc that reads as a change log of task IDs. Nothing here is a correctness bug I could find. Nothing needs a redesign.

**Verified by running:** `./gradlew :build-logic:check` in a detached worktree of `origin/main` (9b7b731): BUILD SUCCESSFUL, tests, `validatePlugins`, `-Werror` compile all green; a second invocation reused the configuration cache. I did not count the tests, so 185 is unconfirmed. **Not run:** `./gradlew tasks` (my command chain failed and I removed the worktree before retrying) and any `bakePreview`. Everything else below is from reading.

## Strengths

- Task classes are thin: they read properties, build a `SiteLayout`, and call a tool class inside `runTool`. Logic stays testable (`FakeRunner`, fixed `Clock`, temp repos).
- `JbakeSitePlugin.apply` is a short table of contents; defaults are `convention(...)`, so `build.gradle` overrides win. `JbakeClasspath` applies exclusions in `withDependencies`, which is the correct trick for the lack of a lazy exclude API, and it says why.
- `@CacheableTask` only on `CompileSassTask` (deterministic, path-sensitive, owns its output); `@UntrackedTask(because=...)` on the four that mutate the source tree. `bake` is correctly not cached.
- `ToolFailure` with `FAILED`/`COULD_NOT_RUN` mapped once, in `SiteToolTask.runTool` (SiteToolTask.java:~100), to a `GradleException` carrying the same message. Functional tests assert "no stack trace" output.
- `ProcessRunner` drains stderr on its own thread (ProcessRunner.java:51), closes stdin, sets `GH_PROMPT_DISABLED`. Right details.
- `SassPreviewWatcher`: atomic swap, last-good CSS kept on error, daemon thread, `close()` interrupts and joins. Small and correct.
- `--release 21`, `-Xlint:all -Werror`, doclint, `validatePlugins` is a sound quality floor.

## Findings

### Should fix

1. **Two different `-P` flag parsers; the weaker one silently wins.** `SiteToolTask.flag(Property, name)` (SiteToolTask.java, `flag`) treats `""`/`true` as on, `false` as off, anything else as a usage error. `PrepareImageTask` declares its own private `flag(Property)` (PrepareImageTask.java:124-130) as an overload of the inherited static: there `-PdryRun=banana` is *on*, and `-Pvariants=0` is on. For a task that rewrites images in place, a typo turning `-PdryRun=flase` into "dry run" is harmless, but `-Pjpeg=flase` is not. It is also a trap: two methods named `flag` in one class hierarchy with different rules. Fix: delete the private one, call the inherited `flag(getDryRun(), "dryRun")` etc. (the comment says it mimics the old program; the stricter rule is better and ReviewChanges/OpenPr already use it). Add one TestKit case for `-PdryRun=flase`.

2. **`prepareImage` and `tagRedirects` have no task-level test.** `SiteToolsFunctionalTest` covers newPost, reviewChanges, the five checks, openPr's two refusals; `tagRedirects` appears only as a no-op on a clean tree. Nothing runs `./gradlew prepareImage -Pimage=... -Pwidth=N -Pvariants` end to end, so the property-to-option wiring (JbakeSitePlugin.java:~280-290: `title`, `png`, `jpeg`, `width`, `keepSource`, `variants`) is checked only by `JbakeSitePluginTest` asserting descriptions and absence. A misspelled `gradleProperty("keepSource")` would pass every test. Fix: one TestKit test that adds a fixture PNG, runs `prepareImage -Pimage=... -PdryRun -Pwidth=800`, and asserts the output names the width; one that passes `-Pwidth=abc` and checks the `-Pwidth needs a number` message (PrepareImageTask.java:149).

3. **The `internal` package is where "needed by two packages" went, and it contains domain rules.** `internal/` holds genuinely shared plumbing (`CommandRunner`, `ProcessRunner`, `Git`, `FrontMatter`, `ToolFailure`, `SiteLayout`) but also `ImageRules` (two constants used by `review` and `writing`), `Tags` (tag splitting and a file scan), `JbakeProperties`. That is fine in size, but because everything must be `public` to cross packages, the "package-private by default" goal is already lost for roughly 20 classes. Concretely, these public types are used only inside the root package and can be package-private: `FileTrees` (used by six other packages, so it must stay public; but see 4), `SassEntries`, `SassCompiler`, `SassCompileException`, `CaseRedirects`, `NewJdkJvmArgs`, `PreviewPort`, `ProjectPaths` (grep: each is referenced only from root-package files and tests in the same package). Also `public static final Map RENAMES` in `TagRedirects` is the site's data living in a tool class (see standalone section). Fix: make those five package-private; keep the rest. Do not add `module-info` or interfaces.

4. **Small utilities are duplicated, with different behaviour.**
   - CR normalisation `.replace("\r\n","\n").replace('\r','\n')` appears in `Tags.spellingsIn` (Tags.java:51) and `TagRedirects.writtenTags` (TagRedirects.java:225); `MarkdownChecks` and `PageChecks` split on `"\n"` themselves (PageChecks.java:80, MarkdownChecks.java:185). Put it in `FrontMatter.parse` (or a `Text.lines(String)` next to it) once.
   - `TagRedirects.readLenient` (TagRedirects.java:~215) reads with replacement of bad bytes, while every other reader uses `Files.readString`, which throws `MalformedInputException` on bad UTF-8. Whether intended or not, the difference should be a decision in one helper, not two local habits.
   - Directory listing with `Files.list`/`Files.walk` + try-with-resources is repeated in `Pages`, `MarkdownChecks`, `TagRedirects` (twice), `NewPost`, `SassEntries`, `CaseRedirects`, `SassPreviewWatcher.stamp`. `FileTrees.regularFilesUnder` exists but only half of them use it. Use it where it fits.
   - `urls.txt` and `src/jbake`, `src/case-redirects` defaults are written twice: `SiteLayout.standard` (SiteLayout.java:26) and `JbakeSitePlugin.setDefaults` (JbakeSitePlugin.java:~140). `standard` is used only by tests; if a default changes, tests keep passing against the old one. Make tests build the layout from the same constants, or have the plugin call `standard` for its conventions.
   - The "last `https://` line of `gh` output" idiom is in `NewPost` (237) and `OpenPr` (231); `GitHubCli` exists for exactly this and should own it.

5. **Four methods are too long to review.** `PageChecks.checkPage` is 127 lines of one scanner loop with seven mutable locals (`skipDepth`, `commentsDepth`, `ignoreDepth`, `pendingIgnore`, ...) (PageChecks.java:65-192). `NewPost.create` 86 lines, `TagRedirects.check` 78, `OpenPr.open` 76, `MarkdownChecks.checkBody` 73, `Review.run` 68. The scanner is the one that matters: its state is the real complexity, and today it is only testable through whole pages. Fix: move the walk state into a small private class (`PageScan` with `enter(tag)`, `leave(tag)`, `inSkippedElement()`), leaving `checkPage` as: read, scan, per-tag `checkLinks`. Do it only with `ReviewTest` green before and after; the other long methods can wait.

### Consider

6. **`ProcessRunner.run` returns `null` for "could not start".** (ProcessRunner.java:63-67; `Git` and `FakeRunner` repeat `if (result == null)`.) Null as a signal in a tiny interface invites an NPE the first time a caller forgets. Throw `IOException` from `run` (it already declares `InterruptedException`), delete the three null checks, and make `FakeRunner` throw for an unscripted command, which also makes tests fail loudly on a command nobody scripted.

7. **Flags as `Property<String>` with `@Input @Optional` on untracked tasks.** The string is justified (`-Pstrict` with no value arrives as ""), but the many `@Input @Optional` on tasks that are `@UntrackedTask` or have no outputs are noise (validatePlugins needs some annotation, so `@Internal` is the honest one for an untracked task). `SiteToolTask`'s own properties already use `@Internal` with a reason; be consistent. A bundle such as `OptionalFlags` would also shrink `ReviewChangesTask`, `PrepareImageTask`, `NewPostTask` by about 60 lines of Javadoc-heavy getters, but only do that if a standalone plugin happens.

8. **`System.out`/`System.err` and `System.setProperty("java.awt.headless")` in task actions** (PrepareImageTask.java:106; every `new ProcessRunner(root, System.out, System.err)`). Gradle does redirect `System.out` during task execution, so it works, and the `ProcessRunner` Javadoc explains why `inheritIO` would not. But the headless property is a JVM-global mutation in a long-lived daemon. Set it in `PrepareImage` only if not already set, or pass `-Djava.awt.headless=true` is not available in the daemon; at least restore the old value. Low risk, so only a note.

9. **`JbakeSitePlugin` is 347 lines, about 190 of them Javadoc and constants.** The registration code itself is fine as private static methods per slice. What is too much is the class Javadoc, which narrates T123 through T141 (JbakeSitePlugin.java:28-45), and 18 public `*_TASK_NAME` constants. Constants are used by tests and the descriptions; the history belongs in `docs/06-GRADLE-PLUGIN.md`. Same pattern in `JbakeTask`, `FileTrees`, `BakePreviewTask`: Javadoc accurate, but half of each paragraph is "T124 moved it here from build.gradle". That will be wrong the day the plan is archived. Keep why, drop who-moved-what.
   Also, `registerWritingTasks` (53 lines) could use a small helper `gradleProp(providers, "title")` to remove forty repeated `providers.gradleProperty` calls; plain, optional.

10. **`BakePreviewTask`, `CompileSassTask` and `BakeTask` rely on the fixture, not the unit level.** That is right for tasks. But `BakePreviewTask.serve` has no test at all (acceptable: it blocks), and nothing proves `usesService(watcher)` closes the service on Ctrl+C. Accept this; document it as a known manual check in the README.

11. **`ReviewChangesTask` reads `GITHUB_ACTIONS`/`GITHUB_STEP_SUMMARY` through `ProviderFactory`** (good for the configuration cache) but builds the map by hand with `HashMap` containing nulls (ReviewChangesTask.java:~100). `Map.of` rejects nulls, so someone will "simplify" it into an NPE. Pass `Provider<String>` values or build a `Map` of present values only.

### Nits

- `ToolFailure` is a runtime exception with two int constants; an enum `Kind {FAILED, COULD_NOT_RUN}` would read better, but the exit code is never used by the task layer (both become `GradleException`). Either use it or drop the field.
- `ImageRules` (18 lines, two constants) could sit in `ImageChecks` or `Images`.
- `TagRedirects.MARKER` still names a Python script that no longer exists; the Javadoc says why (byte-identical stubs), which is acceptable but worth one line in the constant's own comment.
- `PrepareImageTask.width()` returns a boxed `Integer` that may be null; `OptionalInt` is the plain Java for it.
- `ProcessRunner.nullDevice()` handles Windows; the rest of the plugin (shell paths, `gh`) is not Windows-tested. Say "macOS/Linux only" or drop the branch.
- `SassPreviewWatcher` polls every 500 ms by walking the tree and string-building a stamp; fine for this repo, `WatchService` is not worth adding.
- `OpenPr.COMMITTED_PATHS` hardcodes `scripts/`, `CLAUDE.md`, `AGENTS.md` (OpenPr.java:43).

## Tests

- **Balance is good:** about 60% pure unit tests against `FakeRunner`, `TestRepositories` (real temp git repos), and temp directories; TestKit used for wiring, up-to-date, configuration-cache reuse, failure messages. The fixture site is minimal and a copy per test, so there is no shared mutable state.
- **Names read as sentences** (`aSecondBakeIsUpToDateAndReusesTheConfigurationCache`, `openPrRefusesOnMainNamingRuleTwo`). Good. `ReviewTest` (376 lines, 18 tests) and `OpenPrTest` (424) are the largest; consider splitting `ReviewTest` by check code (E1.., W..).
- **Flakiness risks, all small:** `TestRepositories` shells out to `git` with no `Assumptions` guard (fine on CI, an unhelpful failure elsewhere). `PreviewPortTest` correctly binds port 0, not 8080. `CaseRedirectsTest`/`BakeFunctionalTest` branch on `isCaseSensitive`, so a Mac run and a Linux run test different things; that is reasonable but means the Linux-only half is exercised only in CI. Clock is injected where it matters (`ReviewRunnerTest`). No `gh` dependence: `OpenPr` takes a `CommandRunner`.
- **Gaps that matter:** `prepareImage -P` wiring and `tagRedirects` writing a stub through Gradle (finding 2); `BakePreviewTask` and the watcher thread (`SassPreviewWatcher` has no test of its own beyond `SassCompiler`); the `ToolFailure` exit-2 path as a task failure is covered only for newPost.
- `FakeRunner` returning null for an unscripted command (finding 6) can hide a test that never scripted a call it makes.

## Toward a standalone plugin (T132, deferred)

Already generic: `jbakeSite` extension with conventions, `JbakeClasspath` (exclusions and substitutions are configuration), `CompileSassTask`, `JbakeTask`/`BakeTask`/`BakePreviewTask`, `SassPreviewWatcher`, `FileTrees`, `ProcessRunner`/`Git`, `FrontMatter` (the `~~~~~~` key=value format is JBake's, not the site's).

Site-specific and would need to become configuration or move out: `TagRedirects.RENAMES` (the owner's approved renames, as a Java constant, with a default of the site's data); `OpenPr` constants (`main`, `COMMITTED_PATHS`, `PROTECTED_BRANCHES`, repo name via `gh`), `NewPost` (`post/` branch prefix, `main`, `blog/<slug>.html`), `ReviewOptions` default base `main`, messages that cite `AGENTS.md Rule 1/2/3/4` (about 10 places) and `docs/baseline`, `MastheadVariants` and `ImageRules` (400 KB / 1920 px / 960 and 1440 variants, `img/masthead`), the `archived-comments` class in `PageChecks`, and the whole review (Markdown checks E1..E5/W..) which is really "this author's linter". A standalone release would likely be two plugins: a small generic JBake plugin (bake, preview, Sass, classpath) and a site-policy one. Not worth any work now; the existing package split (`writing`, `review`, `verification`) already draws that line.

## The five changes I would make first

1. Delete `PrepareImageTask`'s private `flag` and use the inherited strict one; add a TestKit test for `prepareImage -P...` wiring (findings 1, 2).
2. Make `ProcessRunner.run` throw `IOException` instead of returning null; remove the null checks in `Git` and the fake (6).
3. Pull the duplicated small helpers into one place: CR/line splitting, `gh` URL extraction, one UTF-8 reading policy, defaults shared by `SiteLayout.standard` and the plugin (4).
4. Extract the scanner state out of `PageChecks.checkPage` into a small private class, with `ReviewTest` as the safety net (5).
5. Make the root-package helpers package-private (`SassEntries`, `SassCompiler`, `SassCompileException`, `CaseRedirects`, `NewJdkJvmArgs`, `PreviewPort`, `ProjectPaths`) and trim the task-ID history out of class Javadoc, keeping the reasons (3, 9).
