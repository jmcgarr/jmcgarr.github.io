# Scripts: into the Gradle plugin, and no Python

**Branch**: `docs/08-scripts-migration` | **Created**: 2026-10-25 | **Status**: Draft

A plan for the 17 tracked files under `scripts/`: which of them belong in the pending Gradle plugin
([`06-GRADLE-PLUGIN.md`](06-GRADLE-PLUGIN.md)), and how to get to **no Python in the repository**, the owner's
stated end state (2026-10-25). Research only so far; the owner decides ([Decisions needed](#blockers-and-owner-decisions)).
Conventions as in [`AGENTS.md`](../AGENTS.md), "Tasks and checklists"; task IDs continue from T136
([`archive/04-AUTHORS-DESK.md`](archive/04-AUTHORS-DESK.md)) and checkpoints from CHK052 ([`06-GRADLE-PLUGIN.md`](06-GRADLE-PLUGIN.md)).

## Context

The README's "Requirements" promises a JDK and "**Nothing else**". That stopped being true on 2026-10-25, when
`./gradlew prepareImage` (`build.gradle` lines 379–399) started running `scripts/prepare-image.py` through `uv` or
`python3` with Pillow. CI also runs two Python scripts on every build (`.github/workflows/gradle.yml` lines 83 and
92). The plugin design (written 2026-10-22) predates `prepareImage` and lists only `newPost` and `reviewChanges` as
script tasks (its lines 29–30 and 100–104), so it needs a row for it either way.

Inventory, 2026-10-25 (`git ls-files scripts`): 3 shell, 4 Python and 2 Java files at the top level; under
`scripts/to-markdown/` 4 Python, 2 shell, 1 Node and 1 Java file (plus `classes/` and `flexmark.classpath`, both
git-ignored). Eight Python files in all. Local Pythons here have neither Pillow nor html5lib installed
(`python3 -c 'import PIL'` fails; `uv` is present), which is the daily cost of the status quo.

## Findings, script by script

Each entry: what it does → who runs it → dependencies → plugin fit → replacement → risk.

**`check-docs-not-published.sh`** (46 lines, T004). Fails if the site dir has `docs/`, any `*.md` outside
`vendor/`, or the string `REVIVAL`. Recurring: CI line 79 on `build/site`; README "Run the same checks locally".
bash, `find`, `grep`. **Fits the plugin** as a verification task (pure file walk; `Files.walk` plus a string search).
Risk: none (reads only).

**`check-urls.sh`** (81 lines, T006). Diffs the `.html` files in the site dir against `docs/baseline/urls.txt`
minus `expected-removals.txt`; strict on Linux, warns on case collisions on macOS (the `INDEX.HTML` probe).
Recurring: CI line 87; README; AGENTS.md Rule 1 step 3 (line 95); the design doc's T125 test (line 229). bash,
`comm`, `sort`, `grep`. **Fits the plugin** as a `checkUrls` task taking `baseline`, `expectedRemovals` and the
site dir (generic: any site has a URL baseline). `ReviewChanges.java` already reads the same baseline (W6). Risk:
none for bytes; it *is* the Rule 1 gate, so the Java version must be proven against the shell one on both OSes
before the shell one goes.

**`smoke-test.sh`** (75 lines, T092). After a deploy, fetches 19 live URLs with `curl` and retries for the CDN.
Recurring: the `smoke-test` CI job (line 168), which sparse-checks out only `scripts/` and sets up no JDK or
Gradle. bash, `curl`, `date`. **Does not fit the plugin**: it runs against the live site, not a bake, in a job with
no build. Keep as shell; a Java `HttpClient` version would only add a `setup-java` step. Risk: none.

**`disqus-archive.py`** (140 lines, T045). Turned the Disqus export (never committed) into the 10 `.ftl` files in
`src/jbake/templates/archived-comments/blog/`. **One-off**: run on 2026-10-05; nothing invokes it; without the
export it can't run again. Standard library only. Not a plugin candidate. **Retire**: the output is committed and
the script is in git history. References to update: AGENTS.md line 26, `post.ftl` line 31 (a FreeMarker comment,
not rendered), and the first line of each generated `.ftl` (also a FreeMarker comment; leave as provenance).
Risk: none to output (comments only); the bake stays byte-identical.

**`masthead-variants.py`** (79 lines, T071). Finds every local `masthead=` (content) and `<#assign masthead>`
(templates), and makes or checks the `-960` and `-1440` JPEG variants. Recurring: CI line 83 (`--check`);
`prepare-image.py` imports it for the generate step; README; AGENTS.md line 89; `ReviewChanges.java` names it in
two fix hints (lines 399 and 417); `head-meta.ftl` line 8 and `masthead.ftl` line 34 mention it; T059's Test in
`archive/04-AUTHORS-DESK.md` runs it. Pillow (`Image`, `ImageCms`). **Fits the plugin** in both modes: `--check` is a
verification task (file names and regexes only, no imaging; `ReviewChanges.checkVariants` already does it for
changed pages), and generation belongs with `prepareImage`. Replacement: Java, see [the image work](#the-image-work-pillow-against-javaximageio).
Risk: generation writes image bytes, but **never overwrites an existing variant** (its docstring); the 28
committed variants are untouched by a port. Only new images get the new encoder.

**`prepare-image.py`** (251 lines, 2026-10-25, no plan task). sRGB conversion, metadata strip, resize to the
width cap, JPEG re-encode under a 400 KB budget (quality 82→62, then width), photo-PNG → JPEG with the "option B"
fallback PNG at 1024 px/256 colours when the PNG is already in `main`, reference rewriting in content and
templates, then the masthead variants. Recurring: `./gradlew prepareImage` (`build.gradle` 379–399, the only
Python-launching task); README "To write a post"; AGENTS.md line 86. Pillow, `git cat-file` (for `published()`),
imports `masthead-variants.py`. **Fits the plugin** as a `jbakeSite.scripts` entry (the T127 `ScriptTask`
mechanism) once it is Java; today's task is a special-cased `Exec` that the design doesn't cover. Risk: it writes
image bytes and rewrites `masthead=` lines, but only for the image the author names; it only touches a published
PNG in the fallback path (the four published PNG mastheads already have their fallbacks).

**`tag-redirects.py`** (273 lines, T039/T055). Two modes. *Write*: regenerates the 59 redirect stubs (57 in
`src/jbake/assets/tags/`, 2 in `src/case-redirects/tags/`) from `docs/baseline/urls.txt`, the posts' tags and the
`RENAMES` map; idempotent, never deletes. *Check*: every old tag URL in a baked site is a tag page or a stub whose
target is a real tag page with the right canonical, plus the T057 shown-name check. Recurring: CI line 92
(`--check`); README; AGENTS.md line 69; `jbake.properties` line 5; `tag-names.ftl` line 10. Write mode runs only
on an owner-approved tag rename. Standard library only. **Fits the plugin** as a script task (`--check` after
`bake`); it is site-specific (the stub format, `RENAMES`, `og:title`), so it stays a program in `scripts/`, not a
generic plugin task. Risk: **the stubs are published URLs and committed bytes.** The Java write mode must
reproduce all 59 byte for byte (`git status` clean after a run), including the percent-encoding that mirrors
FreeMarker's `?url_path` (`quote(path, safe="/-_.!~*'()")`, `'` → `%27` in the href), which Java's `URLEncoder`
does *not* do (it writes `+` for a space and encodes `~`). Hand-roll it and test against the committed files.

**`NewPost.java`** (372 lines) and **`ReviewChanges.java`** (1,121 lines). Already Java, already in the plugin
design (T127, T130). `run`/`git`/`fail` are duplicated between them (design doc line 258–264). No change here
beyond what M10 plans; `ReviewChanges` lines 399 and 417 change when the Python command they recommend goes.

**`scripts/to-markdown/`** (T102, done 2026-10-18). `convert.py` (828 lines, html5lib), `asciidoc-convert.py`
(998 lines), `verify.py` (619 lines, html5lib): the converters and their proof. **One-off**: every file under
`src/jbake/content/` is Markdown and the build renders nothing else, so "a page that comes back in another
format" (README) cannot happen without first re-adding an engine. Nothing invokes them. **Retire** all three
(their design notes live in their docstrings; git history keeps them). `render.sh` + `Render.java`: a flexmark
probe with a machine-specific classpath file; retire with them. `pixeldiff.py` (51 lines, Pillow): compares two
screenshot folders pixel by pixel. **Still in use**: the CSS review method
([`reports/css-review-2026-10-24.md`](reports/css-review-2026-10-24.md), "How to verify") and the pending CSS
tasks T135/T136 rely on it. **Port to Java** (`ImageIO` reads PNG, `getRGB` compares, writes the red-on-grey
diff; about the same length). `screenshots.sh` (headless Chrome) and `phone-shots.mjs` (Node, DevTools
protocol): the other half of that method; shell and Node, not Python; keep, moved next to `PixelDiff.java` in a
`scripts/visual/` folder so `to-markdown/` can be deleted whole. None of these fit the plugin (they drive Chrome).

## Summary table

| Script | Recurring / one-off | Language | Fits plugin? | Proposed replacement | Effort | Risk |
|---|---|---|---|---|---|---|
| `check-docs-not-published.sh` | recurring (CI 79) | shell | yes, verification task | Java plugin task `checkDocsNotPublished` | S | none |
| `check-urls.sh` | recurring (CI 87, Rule 1) | shell | yes, verification task | Java plugin task `checkUrls` | M | Rule 1 gate: prove on macOS and Linux first |
| `smoke-test.sh` | recurring (CI 168, post-deploy) | shell | no (live site, no build in that job) | keep as shell | – | none |
| `disqus-archive.py` | one-off (2026-10-05) | Python | no | retire; output committed | S | none (comments only) |
| `masthead-variants.py` | recurring (CI 83, prepareImage) | Python (Pillow) | yes, check task + part of prepareImage | Java: check in plugin, generate in `PrepareImage.java` | M | writes new image bytes only |
| `prepare-image.py` | recurring (`./gradlew prepareImage`) | Python (Pillow) | yes, `jbakeSite.scripts` entry | `scripts/PrepareImage.java` | L | writes new image bytes; rewrites `masthead=` |
| `tag-redirects.py` | recurring `--check` (CI 92); write on approved renames | Python (stdlib) | yes, script task for `--check` | `scripts/TagRedirects.java`, both modes | M | 59 published stubs must stay byte-identical |
| `NewPost.java` | recurring | Java | yes (T127) | unchanged | – | none |
| `ReviewChanges.java` | recurring (CI 121) | Java | yes (T127) | two fix-hint strings change | S | none |
| `to-markdown/convert.py` | one-off (T102) | Python (html5lib) | no | retire | S | none |
| `to-markdown/asciidoc-convert.py` | one-off (T102) | Python | no | retire | S | none |
| `to-markdown/verify.py` | one-off (T102) | Python (html5lib) | no | retire | S | none |
| `to-markdown/pixeldiff.py` | recurring (CSS verification) | Python (Pillow) | no (Chrome workflow) | `scripts/visual/PixelDiff.java` | S | none |
| `to-markdown/render.sh` + `Render.java` | one-off probe | shell + Java | no | retire | S | none |
| `to-markdown/screenshots.sh` | recurring (CSS verification) | shell (Chrome) | no | keep, move to `scripts/visual/` | S | none |
| `to-markdown/phone-shots.mjs` | recurring (CSS verification) | Node (Chrome) | no | keep, move to `scripts/visual/` | S | none |

## The two counts

- **Plugin fit: 7 of the 10 top-level scripts** can be integrated into the plugin design: three as generic
  plugin verification tasks (`check-docs-not-published.sh`, `check-urls.sh`, `masthead-variants.py --check`) and
  four through the design's `jbakeSite.scripts` registration (`NewPost.java`, `ReviewChanges.java`, and the Java
  successors of `prepare-image.py` and `tag-redirects.py`). The three that don't: `smoke-test.sh` (post-deploy),
  `disqus-archive.py` (one-off) and everything under `to-markdown/` (one-off, or Chrome tooling).
- **Python: 8 files. 4 convert to Java** (`prepare-image.py` and `masthead-variants.py` into one program,
  `tag-redirects.py`, `pixeldiff.py`), **4 retire** (`disqus-archive.py`, `convert.py`, `asciidoc-convert.py`,
  `verify.py`). None needs to stay Python, and none needs shell: shell gains nothing over the JDK the build
  already requires. After Phase A below, `git ls-files '*.py'` is empty and `uv`/`python3` leave `build.gradle`,
  CI and the README.

## The image work: Pillow against `javax.imageio`

Checked on this machine (JDK 25.0.4, a probe against `img/masthead/hawaii.jpg`, 1920×1440, and
`santa-cruz.png`, 1024×768, 256 colours), step by step against what `prepare-image.py` does:

| Step | Pillow today | Plain JDK | Verdict |
|---|---|---|---|
| Read JPEG/PNG with its ICC profile | `Image.open`, `info["icc_profile"]` | `ImageIO.read` gives an `ICC_ColorSpace`; the reader's metadata exposes `app2ICC` | OK |
| Convert to sRGB | `ImageCms.profileToProfile` (LittleCMS) | `ColorConvertOp(source, CS_sRGB)`; the JDK's colour engine is LittleCMS too | OK |
| Drop EXIF/XMP/ICC | `out.info = {}` | writing with `IIOImage(img, null, null)` emits JFIF only: the probe's output has no `Exif`, no `ICC_PROFILE` | OK; stricter than Pillow, which re-embeds an sRGB profile (~3 KB) |
| Resize | `Image.LANCZOS` | `Graphics2D` bicubic or `SCALE_AREA_AVERAGING`; **no Lanczos** | OK for new images; not pixel-identical to Pillow. TwelveMonkeys `ResampleOp` has Lanczos if parity is ever wanted (verify) |
| Progressive JPEG, quality q, optimized Huffman | `save(..., quality=q, optimize=True, progressive=True)` | `JPEGImageWriteParam`: `setCompressionQuality`, `setProgressiveMode(MODE_DEFAULT)`, `setOptimizeHuffmanTables(true)`; probe wrote a SOF2 file, 118 KB at q0.80 for the 960 variant (Pillow's committed one: 111 KB) | OK |
| Byte budget loop (quality, then width) | loop over `QUALITIES` | same loop over a `ByteArrayOutputStream` | OK |
| Photo heuristic (>50,000 colours, no alpha) | `getcolors` | a `BitSet(1<<24)` over `getRGB`; probe counted 87,596 and 256 | OK |
| Lossless PNG resize + `optimize=True` | Pillow's filter search, level 9 | `PNGImageWriteParam` compression quality 0.0 = deflate level 9; no filter search, so files run larger | OK, slightly bigger files |
| **Fallback PNG: 256 colours, Floyd–Steinberg** (option B) | `quantize(256, MEDIANCUT, FLOYDSTEINBERG)` | **no quantizer in the JDK** (an indexed image can be *written*, not *made*) | Gap: ~100 lines of median-cut, a library (verify candidates), `pngquant`, or drop the step (D2) |
| `published()` | `git cat-file -e main:path` | `ReviewChanges.git(...)` helper | OK |
| Reference rewriting | regex over content and templates | the same regexes in Java | OK |

No ImageMagick is needed. No third-party library is needed unless the owner wants Lanczos parity or the fallback
quantizer. Rule 1: none of this re-encodes a committed image; the ports run only on images the author adds.

## Tasks

### M11: Scripts into Java, then into the plugin

**Goal**: no `.py` file in the repository, every recurring script in the language the build already needs, and
every check CI runs on a bake available as a Gradle task; the site's output is byte-identical throughout.

**Independent Test**: `git ls-files '*.py'` prints nothing; `grep -rn 'python3\|uv run\|Pillow' build.gradle
.github README.md AGENTS.md` prints nothing; a clean bake is byte-identical to one from the commit before M11
(the method in `06-GRADLE-PLUGIN.md`, "The byte-identical check"); CI is green on Linux with the stubs present.

**Order**: Phase A (T137–T140) needs no plugin and can start now, one PR each, in this order: T137, T138, T139,
T140. Phase B (T141–T143) runs inside M10 after T127 and T129 exist. Decisions D1–D4 first.

#### Phase A: no Python (independent of the plugin)

- [x] T137 [IMP] **Retire the one-off conversion and comment tooling.** Delete `scripts/disqus-archive.py`,
  `scripts/to-markdown/convert.py`, `asciidoc-convert.py`, `verify.py`, `render.sh`, `Render.java`; move
  `screenshots.sh`, `phone-shots.mjs` to `scripts/visual/` (and `pixeldiff.py` with them, until T138); drop the
  `scripts/to-markdown/*` and `.venv/` lines from `.gitignore`. README: replace "To convert an old post to
  Markdown" with three sentences of history pointing at T102 and git; AGENTS.md line 26 says the comments were
  generated in T045 from the Disqus export (script retired, in history); `post.ftl` line 31 likewise
  (`scripts/`, `.gitignore`, `README.md`, `AGENTS.md`, `src/jbake/templates/post.ftl`)
  - **Test:** the byte-identical check passes (the `.ftl` edit is a FreeMarker comment). `git ls-files scripts`
    lists 10 files, none under `to-markdown/`. `scripts/check-docs-not-published.sh` passes on a bake.
  - _2026-10-27: **done** (`chore/T137-T140-no-python`, D1 taken on the plan's recommendation, see Blockers). The six
    one-offs are deleted; `screenshots.sh`, `phone-shots.mjs` and (until T138) `pixeldiff.py` moved to `scripts/visual/`.
    Neither moved tool had a path of its own to fix, and both ran from the new folder (4 pages at 1280 px with
    `screenshots.sh`, 2 at 375 px with `phone-shots.mjs`, against `jwebserver` on port 8202). README's conversion
    section is three sentences of history under the same heading (the archived revival plan names it); AGENTS.md
    line 26 and `post.ftl` line 31 say T045 and the Disqus export. **Test passed:** a clean bake is byte-identical to
    one of `origin/main` (496 files, every SHA-256 equal; `feed.xml` equal apart from its two build timestamps), and
    `scripts/check-docs-not-published.sh build/jbake` is OK. One correction to the Test line: `git ls-files scripts`
    lists **11** files, not 10 (17 − 6 deleted: 8 at the top level, 3 under `visual/`), none under `to-markdown/`._
- [x] T138 [IMP] **`PixelDiff.java`.** `scripts/visual/PixelDiff.java`, a single-file program with
  `pixeldiff.py`'s interface (`<before> <after> [<diff_out>]`, same per-file line, exit 1 on a difference, the
  red-on-grey diff image); delete `pixeldiff.py`; the script headers of `screenshots.sh` and `phone-shots.mjs`
  name it; README "To change the site's styles" gives the Java command (`scripts/visual/`, `README.md`)
  - **Test:** on the `/tmp/css-before` and `/tmp/css-after` folders from the CSS review method (or two freshly
    shot folders), `java scripts/visual/PixelDiff.java` reports the same per-file counts and bounding boxes as
    `pixeldiff.py` did from a venv with Pillow, and exits 0 for identical folders, 1 otherwise.
  - _2026-10-27: **done**. `scripts/visual/PixelDiff.java` (ImageIO, `getRGB`; about 120 lines with its header) keeps
    `pixeldiff.py`'s interface, per-file lines and exit codes, and reproduces Pillow's numbers exactly: the bounding
    box is of any-channel differences (`getbbox`), the count and the red mask use Pillow's luma rounding of the
    difference (`convert("L")`), as its header explains. **Test passed** on two freshly shot folders of 6 pages (4 at
    1280 px, 2 at 375 px, from a bake of `origin/main`) and a third folder with deliberate differences: on the identical
    pair both programs print `identical (0 pixels differ)` six times and exit 0; on the differing pair both print the
    same `missing in`, `size (375, 3743) vs (375, 2123)`, `1187667 pixels differ, bounding box (274, 175, 1005, 4111)`
    and `944589 pixels differ, bounding box (0, 0, 1280, 2632)` lines and exit 1, and the two red-on-grey diff images
    each wrote are pixel-identical to Pillow's (checked with PixelDiff itself). The Python ran as
    `uv run --with pillow python3 -I scripts/visual/pixeldiff.py` before it was deleted. Not verified: the
    `/tmp/css-before` and `/tmp/css-after` folders of the CSS review no longer existed; fresh shots were used._
- [x] T139 [IMP] **`TagRedirects.java`.** `scripts/TagRedirects.java` with both modes (`--check <site>` and write),
  the `RENAMES` map, the `?url_path`-compatible percent-encoder and `html.escape` equivalents; delete
  `tag-redirects.py`; CI line 92 becomes `java scripts/TagRedirects.java --check build/site`; update README (two
  places), AGENTS.md line 69, `jbake.properties` line 5, `tag-names.ftl` line 10 (`scripts/`,
  `.github/workflows/gradle.yml`, `README.md`, `AGENTS.md`, `src/jbake/jbake.properties`,
  `src/jbake/templates/tag-names.ftl`)
  - **Test:** `java scripts/TagRedirects.java` (write mode) on a clean tree leaves `git status` clean: all 59 stubs
    regenerate byte for byte. `--check build/site` prints the same counts as the Python version on the same bake
    ("59 old tag URLs ... 57 redirect stubs, 2 not checkable here" on macOS) and passes on Linux in CI. Removing a
    stub makes it fail with "missing from the build"; editing a stub's target makes it fail with "redirects to".
  - _2026-10-27: **done**. `scripts/TagRedirects.java` carries both modes, `RENAMES`, a hand-rolled `?url_path`
    percent-encoder (the unreserved set plus `/-_.!~*'()`, uppercase hex, `'` → `%27` in the href only) and
    `html.escape`/`unescape` equivalents; `java.net.URLEncoder` was not used. The stub marker still reads
    `scripts/tag-redirects.py`: the 59 stubs are committed bytes, so the marker is kept as history rather than
    rewritten (the Java header says why). **Test passed (Rule 1 gate):** write mode on a clean tree printed
    "59 tag redirect stubs (0 written, 59 already up to date)" and `git status` stayed clean; with
    `tags/acceptance test.html` deleted it wrote that one stub back and `git status` was clean again (byte for byte).
    `--check build/jbake` on the same bake prints output identical to the Python's, line for line (`diff` empty:
    "229 old tag URLs (181 baseline, 48 hyphenated since T039): 170 real tag pages, 57 redirect stubs, 2 not checkable
    here", 176 tag names checked; the Test line's "59 old tag URLs" was the plan's shorthand for the 59 stubs). On a
    copy of the bake with one stub removed and another's target edited it fails with
    "tags/acceptance test.html: missing from the build" and "tags/unit testing.html: redirects to tags/unit-tests.html,
    expected tags/unit-testing.html" (plus the canonical line). CI's step is `java scripts/TagRedirects.java --check
    build/site`; README (table, local checks, the old-tag-URLs paragraph), AGENTS.md line 69, `jbake.properties`
    line 5, `tag-names.ftl` line 10 and the `build.gradle` comment at line 130 name the Java program. The bake is
    byte-identical (the two template/properties edits are comments). Not verified: the Linux run of `--check` with the
    two case-only stubs present happens in CI on this PR._
- [x] T140 [IMP] **`PrepareImage.java`, with the masthead variants inside it.** `scripts/PrepareImage.java`:
  `prepare-image.py`'s options and steps per the table above (bicubic resize; no ICC profile written; D2 decides
  the fallback PNG), plus `--variants <image>` (make the 960/1440 variants, never overwriting) and
  `--check-variants` (every masthead in use has both). Delete `prepare-image.py` and `masthead-variants.py`.
  `build.gradle` 379–399 becomes the `Exec`-of-`java` shape `newPost` uses (lines 358–371), with `image`, `dryRun`,
  `png`, `jpeg`, `width`, `keepSource` passed as `--name=value`; CI line 83 becomes
  `java scripts/PrepareImage.java --check-variants`; `ReviewChanges.java` lines 399 and 417 recommend the new
  command; README "To write a post" and "Run the same checks locally", AGENTS.md lines 86 and 89, `head-meta.ftl`
  line 8, `masthead.ftl` line 34, T059's Test in `archive/04-AUTHORS-DESK.md`; remove `__pycache__/` from `.gitignore`;
  README "Requirements" is true again (`scripts/`, `build.gradle`, `.github/workflows/gradle.yml`, `README.md`,
  `AGENTS.md`, `docs/archive/04-AUTHORS-DESK.md`, `src/jbake/templates/head-meta.ftl`, `masthead.ftl`, `.gitignore`)
  - **Test:** `git ls-files '*.py'` is empty. `java scripts/PrepareImage.java --check-variants` passes on `main`
    and fails naming the file after `-960.jpg` of one masthead is moved aside. On a copy of
    `maui-sunset.png` from `git show da470cc^:...` placed under `img/masthead/` on a scratch branch,
    `./gradlew prepareImage -Pimage=...` writes a `.jpg` ≤ 400 KB and ≤ 1920 px, a `-960` and a `-1440` file,
    repoints the post's `masthead=`, and the three JPEGs contain no `Exif`, `ICC_PROFILE` or XMP marker and are
    progressive (`java` probe or `file`); `./gradlew reviewChanges` reports no E5. `-PdryRun` changes nothing
    (`git status` clean). The 28 committed variants are untouched (`git status` clean after `--check-variants`).
  - _2026-10-27: **done** (D2 taken on the plan's recommendation, see Blockers). `scripts/PrepareImage.java` (about
    560 lines with its header) has `prepare-image.py`'s options (`--dry-run`, `--png`, `--jpeg`, `--width N`,
    `--keep-source`, and Gradle's `--name=value` spellings) and steps, plus `--variants <image>` (missing variants
    only, never overwriting) and `--check-variants`; `build.gradle`'s task is the `newPost`-shaped `java` Exec; CI's
    step is `java scripts/PrepareImage.java --check-variants`; `ReviewChanges.java`'s two E5 hints, README ("To write
    a post", "Run the same checks locally", "Requirements"), AGENTS.md lines 86 and 89, `head-meta.ftl`, `masthead.ftl`,
    `.gitignore` and a dated note under T059 in `archive/04-AUTHORS-DESK.md` follow; `07-FRONT-MATTER.md`'s two rows
    name the Java program too. Accepted as the plan said: bicubic resampling (in halving steps, so a 4000 px photo
    doesn't alias) and no ICC profile in new JPEGs. ImageIO's PNG reader doesn't apply an `iCCP` chunk, so the program
    tags the pixels with it itself before `ColorConvertOp`; a photo PNG that is already in `main` is refused with
    "convert it by hand or add the quantizer" and nothing is changed. **Test passed:** `git ls-files '*.py'` is empty;
    `--check-variants` passes on the branch ("OK: all 15 mastheads in use have 960/1440px variants") and fails naming
    `src/jbake/assets/img/masthead/egypt-960.jpg` with that file moved aside. On a scratch commit with
    `maui-sunset.png` from `da470cc^` under `img/masthead/`, the post pointing at it and the three committed Maui
    JPEGs removed, `./gradlew prepareImage -Pimage=... -PdryRun` printed the plan and left `git status` clean; the real
    run wrote `maui-sunset.jpg` 1920×691 at q82, 130 KB (Pillow's: 132 KB), `-960` 960×346 (43 KB) and `-1440`
    1440×518 (80 KB), repointed `masthead=`, removed the never-published PNG; the three JPEGs have `ffc2` (SOF2,
    progressive) and no `Exif`, `ICC_PROFILE` or `ns.adobe.com/xap` bytes (Pillow's committed file carries an
    `ICC_PROFILE`), and `./gradlew reviewChanges -Pbase=origin/main` reported 0 errors (no E5; three W6 for the
    scratch's own deleted-then-regenerated tracked files). The scratch commit was dropped and the committed Maui
    files restored; the 28 committed variants are untouched (`git status` clean). A clean bake is byte-identical
    (the two template edits are comments). Not verified: a PNG or JPEG with an embedded non-sRGB profile (none was at
    hand; the `ColorConvertOp` path is the plan's probe), and the Linux run of `--check-variants`, which CI does on
    this PR._

#### Phase B: into the plugin (inside M10, after T127 and T129)

_2026-10-27: Phase B is PR (6) in M10's order (`06-GRADLE-PLUGIN.md`), together with T121 `openPr`; T142 is
superseded by the T130 decision there._

- [x] T141 [IMP] **Verification tasks in the plugin.** `checkDocsNotPublished`, `checkUrls` (inputs: site dir,
  baseline file, expected-removals file; the macOS case-collision warning kept) and `checkMastheadVariants`
  (content and template roots, masthead dir) as Java tasks in `build-logic/`, each with a TestKit test in the
  fixture site; an aggregate `checkSite` task that stages `build/jbake` minus drafts into `build/site` and runs
  them (and, if D3 says so, the inline CI steps: drafts, CR line endings, live-reload marker); `check` depends on
  it. The three CI steps at lines 78–89 (docs, masthead variants, URLs) become one `./gradlew checkSite` step
  (with D3, the staging and inline steps at lines 43–77 too); the shell scripts stay until T143 (`build-logic/`,
  `build.gradle`, `.github/workflows/gradle.yml`)
  - **Test:** `./gradlew checkSite` on `main` passes on macOS (with the case warning) and in CI on Linux
    (strict); deleting `build/jbake/about.html` before it fails `checkUrls` naming `about.html`; adding
    `build/jbake/docs/x.md` fails `checkDocsNotPublished`; moving a `-1440` variant aside fails
    `checkMastheadVariants`. `./gradlew :build-logic:test` covers each. The byte-identical check passes.
  - _2026-10-28: `checkMastheadVariants` (the `--check-variants` mode of `PrepareImage`) and `checkTagRedirects`
    (CI's `java scripts/TagRedirects.java --check` step) **shipped with T127**, each a `verification` task that
    `check` runs, with TestKit tests on the fixture site; CI calls them. T141's scope is now `checkUrls`,
    `checkDocsNotPublished` and the `checkSite` aggregate. For T143: the four Java programs are already gone, so
    `smoke-test.sh` and `scripts/visual/` are what will be left._
  - _2026-10-28: **done** (`chore/T141-T143-T121-checks-openpr`, PR 6 of M10's order, with T143 and T121). In the
    plugin's `verification` package: `UrlCheck` (the port of `check-urls.sh`: the `.html` pages of the site against
    the baseline minus the expected removals; missing fails, new is information, a baseline URL that matches a built
    page case-insensitively is the macOS warning when the home page's `INDEX.HTML` twin exists, as the script tested)
    with `CheckUrlsTask` (`checkUrls`: `-PsiteDir`, the baseline from `jbakeSite.publishedUrls`, the removals from the
    new `jbakeSite.expectedRemovals`, default `docs/baseline/expected-removals.txt`, and `-Pstrict`, which reports a
    case collision as missing even on a Mac; the script had no flag, Linux made it strict); `DocsNotPublishedCheck`
    (the port of `check-docs-not-published.sh`: a `docs` entry, Markdown outside `vendor/`, the `REVIVAL` marker in a
    text file, where a NUL byte in the first 32 KiB means binary, as `grep -I`) with `CheckDocsNotPublishedTask`
    (`checkDocsNotPublished`, `-PsiteDir`); and the lifecycle task `checkSite`, which depends on `checkUrls`,
    `checkDocsNotPublished`, `checkTagRedirects` and `checkMastheadVariants`. `check` depends on `checkSite` and,
    directly as before, on `reviewChanges`: the review is about the change and CI runs it on pull requests only (a
    push to `main` has no base), `checkSite` is about the site and runs on every push, so the two stay apart
    (**decision**, recorded here). The `-P` flag reading moved from `ReviewChangesTask` to `SiteToolTask.flag`, shared.
    Every message is the script's, line for line; what a script wrote to standard error (the exit-2 `ERROR:` lines,
    the Rule 1 and Rule 4 sentences after a `FAIL`) is the task's failure message, so it reads under "What went
    wrong". **D3:** the inline CI steps (drafts, CR line endings, live reload) stay shell. **CI:** the four steps
    (docs, masthead variants, URLs, tag redirects) are one step, `./gradlew checkSite -PsiteDir=build/site`, strict on
    Linux as before. **Equivalence (Rule 1 gate), shell against Java on the same macOS bake, 2026-10-28:** on
    `build/jbake` both print the same 11 expected removals, 56 new URLs, the WARNING for `tags/DevOps.html` and
    `tags/Groovy.html`, and `OK: all 272 baseline URLs accounted for in build/jbake`, exit 0; on a copy with
    `about.html` deleted both print the same lists then `FAIL: published URLs missing from the build (1):`, `  !
    about.html`, `Restore them, add redirect stubs, or (with approval) list them in docs/baseline/expected-removals.txt.`,
    exit 1; on a copy with `blog/brand-new-page.html` added both list 57 new URLs with it and pass; the docs check
    prints `OK: no docs/ content in build/jbake` on the bake and, on the copy with `docs/x.md` holding the marker,
    the same three `FAIL:` lines naming `build/eq-c/docs` and `build/eq-c/docs/x.md` twice, then `docs/ content must
    never be published. See AGENTS.md (Rule 4).`, exit 1. `diff` of the shell output against the task's lines was
    empty in every case apart from Gradle's own lines (JBake's four bake lines when `bake` reran, and "Get more help").
    The Linux half of the comparison is this PR's CI run, where `checkSite` is strict and the 2 case-only stubs are in
    the bake. **Tests:** `UrlCheckTest` (the comparison, the removals file with comments and blanks, the pages of a
    bake, pass with removals and new URLs, fail with the Rule 1 sentence, the case-collision warning or failure by
    filesystem, the two exit-2 cases), `DocsNotPublishedCheckTest` (clean, the three failures at once with the Rule 4
    sentence, binary files, no site), the plugin wiring (`checkSite`'s and `check`'s dependencies, the extension's two
    new properties), and in `SiteToolsFunctionalTest`: `checkUrls` passes on the fixture (6 baseline URLs), fails on a
    staged copy without `about.html` naming it, passes when `about.html` is in `expected-removals.txt`;
    `checkDocsNotPublished` passes and fails on a leaked `docs/x.md`; `checkSite` runs the four and not the review;
    `check` runs the review and every check. 160 tests (145 before). **Test passed** as written, apart from the
    masthead case, which T127 covered: `./gradlew checkSite` passes on macOS with the case warning; deleting
    `about.html` from a copy fails `checkUrls` naming `about.html`; `docs/x.md` fails `checkDocsNotPublished`; the bake
    is byte-identical to the base commit's (`diff -rq`: only `feed.xml`, 496 files). Not verified locally: CI (Linux)._
- [x] T142 **Superseded (2026-10-27).** [IMP] **Register the Java scripts through `jbakeSite.scripts`.** `prepareImage` (options `image`,
  `dryRun`, `png`, `jpeg`, `width`, `keepSource`) and `tagRedirects` (`check`, `dependsOn 'bake'`) join `newPost`
  and `reviewChanges`; the special-cased `Exec` from T140 goes; `06-GRADLE-PLUGIN.md`'s table (lines 29–30) and
  extension sketch (lines 100–104) gain the two rows (`build.gradle`, `docs/06-GRADLE-PLUGIN.md`)
  - **Test:** `./gradlew prepareImage -Pimage=<path> -PdryRun` and `./gradlew tagRedirects -Pcheck` run the
    programs with the same arguments as before (`--info` shows the command line); `./gradlew tasks --group writing`
    lists `newPost`, `reviewChanges`, `prepareImage`, `tagRedirects`; `wc -l build.gradle` is still under 120.
  - _2026-10-27: **superseded** by T130's decision in `06-GRADLE-PLUGIN.md`: the four programs move into the plugin
    as task classes (T127), so there is nothing to register and no `jbakeSite.scripts` block. The surface rows went
    into `06`'s table the same day._
- [x] T143 [IMP] **Remove the shell checks and finish the docs.** After T141 has run green in CI for at least one
  merged content PR: delete `check-docs-not-published.sh` and `check-urls.sh` (D4), point AGENTS.md Rule 1 step 3
  (line 95) and the design doc's T125 test (line 229) at `./gradlew checkUrls`, and make README "Run the same
  checks locally" one command; `smoke-test.sh`, `scripts/visual/` and the four Java programs are what's left
  (`scripts/`, `README.md`, `AGENTS.md`, `docs/06-GRADLE-PLUGIN.md`)
  - **Test:** `git ls-files scripts` lists `NewPost.java`, `ReviewChanges.java`, `PrepareImage.java`,
    `TagRedirects.java`, `smoke-test.sh` and `visual/{PixelDiff.java,screenshots.sh,phone-shots.mjs}` and nothing
    else; `grep -rn 'check-urls.sh\|check-docs-not-published.sh' README.md AGENTS.md docs/0*.md .github` is empty.
  - _2026-10-28: **done** (same branch as T141, the next commit; D4, the owner's decision of 2026-10-27, "go": one
    implementation per check). The "after T141 has run green in CI for at least one merged content PR" wait was
    replaced, with the coordinator's instruction, by the equivalence check recorded under T141 (shell against Java on
    the same bake, on a bake with a page removed and on one with a page added, identical output and exit codes), since
    the two implementations would otherwise have shared one PR's CI run anyway. `scripts/check-urls.sh` and
    `scripts/check-docs-not-published.sh` are deleted (`git rm`); `git ls-files scripts` is `smoke-test.sh` and
    `visual/{PixelDiff.java,phone-shots.mjs,screenshots.sh}`, the four Java programs having moved into the plugin in
    T127. AGENTS.md: Rule 1 step 3 is `./gradlew checkUrls`, with what it compares and when it warns rather than
    fails; the Layout tree's `scripts/` line and the Commands block (`checkSite`). README: "Run the same checks
    locally" is `./gradlew clean checkSite`, with the staged-site variant CI uses; the check table and the macOS
    caveat name the tasks. `06-GRADLE-PLUGIN.md`: dated notes under T125's Test line and CHK047 instead of rewrites;
    `05-REVIEW-FLOW.md`'s W6 row likewise. The review's W6 finding ends "CI's checkUrls fails otherwise." instead of
    naming the script (a deliberate string change; `ReviewTest` follows). **Test:** `git ls-files scripts` as above
    (the Test line's list predates T127's move of the Java programs); the grep over README.md, AGENTS.md and `.github`
    is empty; in `docs/0*.md` the two names remain only where they are the subject: this plan's findings, table and
    notes, and the dated notes added by this task. `docs/baseline/expected-removals.txt`'s comments still say
    "check-urls.sh doesn't track it" about non-HTML URLs; a baseline file changes only at a milestone checkpoint,
    and the sentence stays true of the task._

#### Checkpoint: M11

- [x] CHK053 `git ls-files '*.py'` is empty, and `grep -rni 'python\|pillow\|uv run' build.gradle .github README.md AGENTS.md` is empty
  _(passed: both commands empty on `main` after #166 merged; confirmed by the owner on 2026-10-27)_
- [x] CHK054 A clean bake on `main` is byte-identical (apart from `feed.xml`'s timestamps) to the bake at the commit before T137; the 59 tag stubs and the 28 masthead variants are unchanged in git since then
  _(2026-10-29: a clean macOS bake of `main` at 2f43289 (after PR #174, the end of Phase B) against the bake at
  2f3e9f4 (`main` before T137: the first parent of PR #166's merge, baked in a detached worktree): `diff -rq` prints
  only `feed.xml`. `git diff --stat 2f3e9f4 main -- src/jbake/assets/tags src/case-redirects` and the `-960`/`-1440`
  masthead files is empty: the 59 stubs (57 under `assets/tags` plus the two case-redirect stubs) and the 28 variants
  are unchanged. CHK047 in 06 adds the Linux half: the deployed artifacts before and after M10 are identical apart
  from the feed's timestamps)_
- [x] CHK055 CI runs no script outside `scripts/*.java`, `scripts/smoke-test.sh` and Gradle tasks, and is green on Linux
  _(2026-10-28: the first half holds since T143: `.github/workflows/gradle.yml` runs Gradle tasks, inline shell (the
  D3 steps: drafts, CR line endings, live reload, xmllint), lychee and `scripts/smoke-test.sh`, and `scripts/*.java`
  no longer exists outside `visual/`. The Linux run is pending this PR's CI; the owner ticks after it is green)_
  _(2026-10-29: green on Linux. PR #174's CI (run 40803018355, after one test fix: the `checkUrls` TestKit case now
  expects the fixture's case-redirect stub among the new URLs on a case-sensitive filesystem) and the `main` deploy
  at 2f43289 (run 40803541310) both passed, with the single `checkSite` step printing the four OK lines, 185 plugin
  tests, and the smoke test seeing all 19 live URLs. Ticked with the merge, on the owner's instruction to update the
  environment after it)_
- [x] CHK056 D1–D4 each have a dated decision in the Notes
  _(2026-10-27: D1 and D2 recorded with Phase A, D3 and D4 decided by the owner the same day; see Blockers and Notes)_

## Blockers and owner decisions

Nothing blocks the "no Python" goal technically. Four decisions shape it:

- **D1: retire or port the one-offs.** The plan retires `disqus-archive.py`, the two converters and `verify.py`
  (T137). The alternative, porting them to Java "for the record", costs ~2,500 lines of Python to translate for
  tools that cannot run again (no Disqus export; no non-Markdown engine). Recommendation: retire; git history is the
  record.
  - _2026-10-27: **retire**, taken on the plan's recommendation to start Phase A (T137); the owner can overrule. The
    six files are in git history (`git log -- scripts/to-markdown scripts/disqus-archive.py`)._
- **D2: the option B fallback PNG.** The JDK has no colour quantizer. Options: (a) write a small median-cut with
  Floyd–Steinberg in `PrepareImage.java` (~100 lines, no dependency); (b) a library (verify which Java PNG
  quantizers are maintained before choosing); (c) shell out to `pngquant` when installed; (d) drop the step, since
  the only PNGs that can need it are ones already published, and the four published PNG mastheads (`hawaii.png`,
  `london-alley.png`, `london-view.png`, `santa-cruz.png`) are already 800–1024 px, 256-colour fallbacks from T016
  that no page references (checked 2026-10-25). A never-published PNG is simply removed, as today. Recommendation: (d) now, with a clear
  refusal message ("this PNG is published; convert it by hand or add the quantizer"), and (a) if the case arises.
  - _2026-10-27: **(d)**, taken on the plan's recommendation to start Phase A (T140); the owner can overrule.
    `PrepareImage.java` refuses a photo PNG that is in `main` ("convert it by hand or add the quantizer") and removes
    a never-published one after conversion, as before. (a) stays the answer if the case ever arises._
- **D3: scope of `checkSite`.** Only the three scripted checks (T141 as written), or also the inline CI steps
  (drafts, CR line endings, live-reload marker) so CI is `./gradlew bake checkSite` plus lychee and xmllint.
  The inline steps are five lines of shell each and already run on `build/site`; moving them is tidy, not
  necessary.
  - _2026-10-27: **the three scripted checks first** (owner, on the recommendation): T141 as written; the inline CI
    steps may follow later if it turns out tidy._
- **D4: keep shell twins.** Whether `check-urls.sh` and `check-docs-not-published.sh` stay after T141 as a
  Gradle-free fallback, or go (T143). Recommendation: go, once a content PR has merged green with the plugin tasks;
  two implementations of the Rule 1 gate would drift.
  - _2026-10-27: **go** (owner): the shell twins are deleted in T143 so each check has one implementation. No
    published URL is affected; `check-urls.sh` is a tool, not a page._

Two things to accept rather than decide: new images will be resampled bicubically, not with Lanczos (no visible
difference at these sizes; the committed images are not re-encoded), and new JPEGs will carry no sRGB profile
(browsers treat untagged JPEGs as sRGB; this is closer to "nothing but pixels is published" than Pillow's output).

## GitHub Issues

`gh issue list` on 2026-10-25: one open issue. It is T059's, not this plan's; T140 only keeps the tool T059's
Test uses working.

| Issue | Title | GitHub label | Task(s) | Milestone | Put in the resolving PR |
|---|---|---|---|---|---|
| [#14](https://github.com/jmcgarr/jmcgarr.github.io/issues/14) | Add mastheads for older posts | enhancement | T140 (the masthead tooling T059 depends on; T059 itself is in `archive/04-AUTHORS-DESK.md`) | M11 | `Refs #14` |

## Notes

- _2026-10-25: written at the owner's request ("review the scripts in the scripts folder and determine how many can
  be integrated into the pending Gradle plugin design. Also, how many Python scripts can be converted to Java (or
  shell)? The ideal end state is NO Python in the repo at all"). Research only: no script, `build.gradle` or
  workflow was changed. The `javax.imageio` table comes from a probe run on this machine's JDK 25.0.4 against two
  committed mastheads; the probe wrote only to a scratch folder._
- _2026-10-27: Phase A started at the owner's request, on one branch (`chore/T137-T140-no-python`, one commit per
  task). D1 and D2 are taken on the plan's own recommendations, pending the owner's confirmation; D3 and D4 wait for
  Phase B._
- _2026-10-27 (later): Phase A merged (#166); CHK053 confirmed by the owner. D3 (scripted checks first) and D4 (delete
  the shell twins) decided by the owner; T142 superseded, since the programs move into the plugin as classes
  (`06-GRADLE-PLUGIN.md`, T130). Phase B runs as the last PR of M10._
