[![Build and publish](https://github.com/jmcgarr/jmcgarr.github.io/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/jmcgarr/jmcgarr.github.io/actions/workflows/gradle.yml?query=event%3Apush)

My Blog
==================
Source for https://www.mikemcgarr.com, built with [JBake](https://jbake.org) and Gradle.

Requirements
============
- **JDK 25** (CI uses 25 too; since 2026-10-12, T113). `.java-version` selects it if you use [jenv](https://www.jenv.be). Works on Apple Silicon.
  With sdkman and jenv: `sdk install java 25.0.4-tem`, then `jenv add ~/.sdkman/candidates/java/25.0.4-tem`.
  JDK 21 still builds the site identically, if you need it.
- Nothing else: `./gradlew` downloads the right Gradle version, and Gradle downloads the Sass compiler (see
  [To change the site's styles](#to-change-the-sites-styles)). No Node or Ruby, and nothing to install for the
  site's own tools: they are tasks of the build plugin in `build-logic/` (T127, 2026-10-28), and the deploy checks
  under `scripts/` are shell.

To preview
==========
```
./gradlew clean bakePreview
```
Then open http://localhost:8080. Stop it with `Ctrl+C`. If port 8080 is already in use, for example by a preview in
another terminal, it stops right away and says so; stop the other one, or set `server.port` in
`src/jbake/jbake.properties`.

**It rebuilds on save, and the open page reloads itself.** While the preview runs, saving a post, a template
(`src/jbake/templates/`), an asset such as CSS (`src/jbake/assets/`), or the theme's SCSS (`src/scss/`) rebuilds the
site with no restart, and the
page open in your browser reloads about 0.5–1.6 s after you save. No browser extension is needed: preview pages carry
a small script (`src/jbake/templates/preview-reload.ftl`) that asks the server every half second whether the page or
its CSS changed. A normal bake leaves the script out, and CI fails if it ever reaches the deploy contents. The
terminal logs each change it picks up, and any template or post error; a page broken by a template error reloads
again once you fix it. If you stop the preview, open pages keep trying for about a minute (and reload once it's back),
then stop; reload them by hand after that.

This is JBake's own server and watcher (`jbake -b -s`), so in the preview the feed, sitemap, and share links point
at `http://localhost:8080`; a normal `./gradlew bake` uses the real address again. Restart the preview after changing
`jbake.properties`, or after deleting or renaming a file (its old page stays in `build/jbake` until `clean`).

`./gradlew bake` runs JBake 2.7 directly in its own JVM (the site's build plugin in `build-logic/`, see [How the
site is built and deployed](#how-the-site-is-built-and-deployed)). If a template or post has an error, the build
fails and prints JBake's message with the file and line.

Gradle's configuration cache is on (`gradle.properties`, T114), so a command's first run ends with "Configuration
cache entry stored" and later runs start with "Reusing configuration cache". Both are normal. Edits to posts,
templates, SCSS, and `jbake.properties` are always picked up; `--no-configuration-cache` turns it off for one run.

Drafts (posts with `status=draft`) are rendered for preview only, at
`http://localhost:8080/blog/drafts/<name>-draft.html`. They are never published.

To write a post
===============
Start a draft with one command, from a clean working tree:
```
./gradlew newPost -Ptitle="My Post Title" -Ptags="management, leadership" -Psummary="A short teaser."
```
It creates `src/jbake/content/blog/drafts/my-post-title.md` with the header below filled in (`status=draft`,
today's date) and switches to a new branch `post/my-post-title` made from `main`. Options: `-Pslug=...` to
choose the URL slug yourself, and `-Pbranch=false` to stay on the current branch.
It refuses a slug that's already a draft, a post, or a published URL, and warns when a tag differs only by
capitalization from an existing one (that would make a second tag page).

Add `-Ppr` to also commit the draft, push `post/my-post-title` to GitHub, and open a **draft** pull request
titled `[WIP] My Post Title`:
```
./gradlew newPost -Ptitle="My Post Title" -Ppr
```
It needs the [GitHub CLI](https://cli.github.com) logged in (`gh auth login`), and checks that first, before it
creates anything. Pull requests never deploy, so the PR is safe to push to as you write. If the commit, push, or
PR step fails, nothing is undone and the error lists the commands to finish by hand. `-Ppr` can't be combined
with `-Pbranch=false`.

To create one by hand instead, make `src/jbake/content/blog/drafts/<slug>.md`, starting with a header like this.
It's JBake's `key=value` format ending in `~~~~~~`, not YAML front matter. Every post and page on the site is
Markdown (since T102, 2026-10-18), and Markdown is the only format the build renders:

```
title=My Post Title
date=2026-10-05
type=post
tags=management, leadership
status=draft
summary=A short teaser shown on the home page.
masthead=three-hills.jpg
mastheadCredit=https://flic.kr/p/rFRzzj
~~~~~~
```

`summary` is optional but recommended: it's the teaser under the title on the home page.

**Tags:** each tag gets a page at `/tags/<tag>.html`, with spaces turned into hyphens (`continuous delivery` →
`/tags/continuous-delivery.html`; `tag.sanitize=true`, T039). Capitals are kept, so write a tag the same way every
time, in lowercase unless it's a name (`devops`, not `DevOps`): `DevOps` and `devops` would make two pages that
each list only some of the posts (T055). Pages show a tag as you wrote it, spaces included ("Tag: continuous
delivery"); only the URL has hyphens. JBake keeps only the hyphenated form, so a tag with a hyphen of its own
(`apt-get`) must be added to `src/jbake/templates/tag-names.ftl`, or it's shown as "apt get"; CI names any such tag.
Every tag is listed with its post count on the **Topics** page, `/tags/` (in the menu; T057).

Markdown is GitHub-style: wrap lines however you like (single line breaks don't break the paragraph), and use
tables, task lists (`- [ ]`), `~~strikethrough~~`, footnotes (`[^1]`), and fenced code blocks with a language:

````
Some text that wraps
across two lines is one paragraph.

| Tool  | Language |
|-------|----------|
| JBake | Java     |

```java
System.out.println("hello");
```

A claim that needs a source.[^1]

[^1]: The source.
````

Write links normally. Links to other sites open in a new tab automatically (the templates add `target="_blank"`,
`rel="noopener noreferrer"` and a screen reader hint at build time), so there's nothing to add by hand.

To embed a YouTube video or a SlideShare deck, put a link to it on a line of its own, with the title as the link
text, and the site turns it into a player at build time (`embeds.ftl`, T115, on posts and pages). The feed shows
the link instead. A link inside a longer paragraph stays a link, and so does a list item that is only a link (the
Talks page lists its videos that way). For example (each on a paragraph of its own):

```
[Video: Ops School](https://www.youtube.com/watch?v=0gx7oQaAy6g)

[Slides: Continuous Delivery](https://www.slideshare.net/slideshow/embed_code/8341276)
```

A plain image is `![alt text](/img/picture.png)`. Markdown can't size, centre or float one, so for that write the
`<img>` tag itself on a line of its own, as the older posts do, with Bootstrap's own classes (no custom CSS to
add): a width and `text-center` for a centred image, `float-right ml-3 mb-3` (or `float-left mr-3 mb-3`) for a
photo the text wraps around, as the About and Talks pages have, a `figure` for a caption, a `card` for an aside.
The figure caption and the card title are styled in `src/scss/_content.scss`. For example:

```
<p class="text-center"><img src="/img/culture.png" alt="Organizational Culture" width="500"></p>

<p><img src="/img/me.jpg" alt="Me" width="300" height="300" class="float-right ml-3 mb-3"></p>

<figure class="figure d-block text-center">
<a href="https://example.com/source"><img src="/img/model.png" alt="The model" width="500" class="figure-img"></a>
<figcaption class="figure-caption">Figure 1: The model (source: Example)</figcaption>
</figure>

<div class="card bg-light mb-4"><div class="card-body">
<p class="card-title">A story</p>

The aside's text, in Markdown, between blank lines.

</div></div>
```

If you use a new header image (`masthead=`), put it in `src/jbake/assets/img/masthead/` and run
```
./gradlew prepareImage -Pimage=src/jbake/assets/img/masthead/<name>.png
```
(or `.jpg`). It converts the image to sRGB, strips the camera metadata, resizes it to ≤ 1920px and ≤ 400 KB, turns a
photo PNG into a `.jpg` and points the post at it, and creates the phone and tablet sizes
(`./gradlew prepareImage -Pimage=<image> -Pvariants` does that last step alone, and never overwrites a variant). The
same command prepares an in-post image, capped at 1000px; add `-Ppng` to keep a screenshot or graphic as PNG,
`-PdryRun` to see what it would do, `-Pwidth=N` for another cap. The work is the plugin's `PrepareImage` class in
`build-logic/` (a single-file Java program from T140, 2026-10-27, until T127, 2026-10-28; before that a script that
needed an image library installed), so nothing is installed. It
refuses a photo PNG that is already published (its URL must keep a PNG, and the JDK has no colour quantizer for the
small fallback): convert such a one by hand, as T016 did.
`masthead=` also takes a full `https://` URL (a remote image, used as-is at every width, so pick one around
1920px wide), or a site path such as `/img/qcon_crowd.jpg` for an image elsewhere under `src/jbake/assets/`.

When it's ready, set `status=published` and move it to `src/jbake/content/blog/`. Its URL will be
`/blog/<slug>.html`. Published URLs are permanent, so pick the slug carefully (see [AGENTS.md](AGENTS.md)).

To review a change
==================
Before opening a pull request, check the pages you changed:
```
./gradlew reviewChanges
```
(`./gradlew check`, Gradle's standard verification task, runs the same review.) It bakes the site, lists every file that differs from `main` (committed or not), and reviews the Markdown and the
rendered pages (T117, design in [docs/05-REVIEW-FLOW.md](docs/05-REVIEW-FLOW.md)). **Errors** fail the command:
a `[link]` followed by a bare URL or with a space before its `(` (E1), brackets left in the rendered text (E2), a
link or image that points at nothing in the baked site (E3), an incomplete header, a CR line ending or a new post
without a `summary=` (E4), an image over 400 KB or 1920 px, or a masthead without its 960 and 1440 variants (E5).
**Warnings** are listed and never fail: a changed page whose `date=` did not move (W2; it's the sitemap's
`lastmod`), a tag spelled unlike an existing one (W3), an image without alt text (W4), a deleted or renamed
published URL (W6, AGENTS.md Rule 1), an existing post without a summary (W7). Each finding names the file and
line, and the fix when it's mechanical. With no changes under `src/jbake`, it says so and passes.

Options: `-Pbase=<branch>` diffs against another branch, `-Pall` reviews every page (a baseline), `-Pstrict`
fails on warnings too. A finding that's right in general but wrong for one line is suppressed by a comment on the
line before it, and the report counts those:
```
<!-- review: ignore E2, the brackets are intended -->
```
Links in a draft are resolved from `blog/`, where the post will live, so a relative link that only works in the
preview is caught. External links aren't checked yet (T119), and spelling is deferred (T120).

CI runs the same review on every pull request (T118, the "Review changed pages" step in
[`.github/workflows/gradle.yml`](.github/workflows/gradle.yml)), against the PR's base branch. An error fails the
job and appears as an annotation on the line in the PR's Files tab; the whole report is in the job summary. A push
to `main` skips the step: the PR already ran it.

To open a pull request
======================
When the change is ready, one command reviews it, pushes the branch and opens the pull request (T121, design in
[docs/05-REVIEW-FLOW.md](docs/05-REVIEW-FLOW.md), "The three entry points"):
```
./gradlew openPr
```
It bakes first (`bake` is a dependency of the task, so the review below has a site to read). It refuses to run on
`main` or `master` (AGENTS.md Rule 2: every change goes on its own branch), and in the middle of a merge with
conflicts. With uncommitted work it prints the files it **would commit** (changed and untracked files under `src/`,
`docs/`, `scripts/`, `build-logic/`, `.github/`, and `README.md`, `AGENTS.md`, `CLAUDE.md`, `build.gradle`,
`settings.gradle`; anything else is listed as not included and left alone) and stops, so you see the list before
anything is committed. Re-run with `-Pmessage="…"` to commit exactly those files with that message (plus the trailer
lines in `jbakeSite.commitTrailers`, none by default): `git commit --only -- <the listed paths>`, so a change you had
staged outside those paths stays staged and out of the commit. Then it runs the same review as `./gradlew
reviewChanges` against `main`; on an error nothing is pushed and the report says what to fix. If the review passes it
pushes the branch (`git push --set-upstream origin <branch>`) and opens a **ready** pull request against `main` with
the [GitHub CLI](https://cli.github.com), printing its URL. The title is `-Ptitle`, or the subject of the branch's
first commit; the description lists the commits, carries the review's summary line ("Reviewed 1 page (about.html)
from 1 changed file. 0 errors, 0 warnings. …") and one `Closes #N` or `Refs #N` line per issue from `-Pcloses=3,16`,
`-Prefs=14` and the commit messages (AGENTS.md Rule 5). `-PdryRun` does everything but commit, push and open, and
prints the pull request it would open; it still needs `gh` installed and logged in, because the readiness checks
(gh, `gh auth status`, the `origin` remote, `main` pushed) run before the commit, whether dry or not.

To convert an old post to Markdown
==================================
There is nothing to convert: every file under `src/jbake/content/` has been Markdown since 2026-10-18, and the build
renders nothing else (revival task T102, the owner's decision of 2026-10-12), after the 63 WordPress-era HTML posts,
the 11 AsciiDoc posts, the 3 AsciiDoc pages and the drafts were converted in batches, each proved against a bake of
`main` so that no published page changed. The converters (`convert.py`, `asciidoc-convert.py`), their proof
(`verify.py`) and the flexmark probe (`render.sh`, `Render.java`) were one-off tools in `scripts/to-markdown/`, retired
on 2026-10-27 (T137 in [docs/08-SCRIPTS-MIGRATION.md](docs/08-SCRIPTS-MIGRATION.md)) because a page in another format
can't come back without first re-adding an engine; `git log -- scripts/to-markdown` has them, and the T102 notes in
[docs/archive/00-REVIVAL.md](docs/archive/00-REVIVAL.md) record each batch. The screenshot tools that came with them
(`screenshots.sh`, `phone-shots.mjs` and the pixel diff) are still used to prove a style change moved no pixels, and
live in `scripts/visual/` (see [To change the site's styles](#to-change-the-sites-styles)).

To change the site's styles
===========================
**Edit `src/scss/`, never the CSS.** All of the site's own CSS is generated from `src/scss/` on every build (T043,
T134). Each file there whose name doesn't start with `_` is an entry, published at `/css/<name>.css`:

| Entry | Published as | What it is |
|---|---|---|
| `clean-blog.scss` | `/css/clean-blog.css`, every page | The theme and the site's own rules, as partials in the cascade order the file lists: tokens in `_variables.scss` (colors, fonts, sizes, breakpoints), the `media-up` breakpoint mixin in `_mixins.scss`, then `_fonts` (the 30 `@font-face` rules for the self-hosted Open Sans, T073, generated from a map of Google's subsets; a separate `fonts.css` until T135), `_global`, `_navbar`, `_masthead`, `_post`, `_content`, `_footer`, `_legacy-asciidoctor`, `_comments`, `_lists`, `_accessibility`. Each partial's header says what it styles and which template. The site-specific additions that were a separate `extra.css` live here since T134 |
| `code.scss` | `/css/code.css`, pages with a code block | Code blocks and the Prism token colors (T056), the language label and the copy button that `templates/code-highlight.ftl` adds (T136) |

None of the CSS is in the repository: `./gradlew compileSass` writes it to `build/generated/sass/css/`, and `bake` and
`bakePreview` copy it into the site at the same URLs as always. A Sass error fails the build (and CI) with the file and
line. The build also refuses to run if a hand-written `clean-blog.css`, `code.css`, `fonts.css` or `extra.css`
reappears under `src/jbake/assets/css/`, since JBake would publish it and it would go stale (the last two were
folded into `clean-blog.css` in T134 and T135, and their URLs are on `docs/baseline/expected-removals.txt`).

A page loads two stylesheets, Bootstrap's and `clean-blog.css`, and a page with a code block `code.css` as well.
The footer and menu icons are inline SVGs from Font Awesome Free (CC BY 4.0) in `templates/footer.ftl` and
`menu.ftl`, so no icon font or stylesheet is loaded (T047, T135). The Font Awesome files under
`src/jbake/assets/vendor/fontawesome-free/` are still published because they are published URLs (Rule 1); nothing
on the site links them.

While `./gradlew bakePreview` runs, saving a file in `src/scss/` recompiles every entry within about a second and the open
page reloads. A Sass error there is printed in the terminal, and the page keeps the last good CSS.

A new rule goes in the partial that styles that part of the page (or a new partial, added to `clean-blog.scss` in
cascade order); a new color or size goes in `_variables.scss`. To prove a refactor changed nothing visible, use the
screenshot-and-pixel-diff method in [`docs/reports/css-review-2026-10-24.md`](docs/reports/css-review-2026-10-24.md),
"How to verify": shoot the same pages from a bake of `main` and from the branch with `scripts/visual/screenshots.sh`
(1280px, headless Chrome) and `scripts/visual/phone-shots.mjs` (a real 375px viewport, Node), then compare the two
folders pixel by pixel with
```
java scripts/visual/PixelDiff.java /tmp/css-before /tmp/css-after /tmp/css-diff
```
It prints `identical (0 pixels differ)` per page and exits 0, or the count and bounding box of the differing pixels
and exits 1, writing a red-on-grey image per differing page into the third folder. (The report, written on 2026-10-24,
names the tools' old folder `scripts/to-markdown/` and the old `pixeldiff.py`; they moved to `scripts/visual/` and the
diff is Java since 2026-10-27, T137 and T138. Nothing to install.)

**The compiler** is [Dart Sass](https://sass-lang.com/dart-sass/), the reference Sass implementation, run from
Gradle by the site's own build plugin (`build-logic/`, see [How the site is built and
deployed](#how-the-site-is-built-and-deployed)) through its Java host,
[`sass-embedded-host`](https://github.com/larsgrefer/dart-sass-java). No Node, Ruby, or separate install:
the host's jar (about 49 MB, from Maven Central, cached by Gradle) carries Dart Sass for macOS, Linux, and Windows.
On first use it unpacks the one for your machine into the system temp folder and runs it as a helper process (a
small Dart runtime plus the compiler), talking to it over stdin/stdout. Dependabot keeps the host current (it was
the freefair Sass plugin's dependency until T124).

To publish
==========
There is no manual publish step. Open a pull request against `main` (`./gradlew openPr` does, after the review). When it's merged,
[GitHub Actions](.github/workflows/gradle.yml) bakes the site, runs the checks, and **deploys it to GitHub Pages**.
Pull requests run the same build and checks but never deploy. To redeploy (for example, to roll back by
re-running an older run), use **Actions → Java CI with Gradle → Run workflow** on `main`.

How the site is built and deployed
==================================
Everything happens in one workflow, [`.github/workflows/gradle.yml`](.github/workflows/gradle.yml)
(shown as **"Java CI with Gradle"** under Actions). It runs on every pull request and every push to `main`.

1. **Bake:** `./gradlew bake` compiles `src/scss/` into `css/clean-blog.css` and `css/code.css` with Dart Sass and renders
   `src/jbake/` into `build/jbake/` with JBake (JDK 25). Drafts are rendered too, as `<name>-draft.html`, but only
   so you can preview them locally. A Sass, template, or post error fails the build here.
2. **Stage:** `build/jbake/` is copied to **`build/site/` without drafts**. `build/site/` is exactly what goes live.
3. **Check:** every check below runs on `build/site/`, so what's checked is what ships. The four plugin checks are
   one step, `./gradlew checkSite -PsiteDir=build/site`.
4. **Upload:** `build/site/` is packaged as the GitHub Pages artifact (pushes and manual runs on `main` only).
5. **Deploy:** a separate `deploy` job publishes the artifact to GitHub Pages (pushes and manual runs on
   `main` only, never two at once).
6. **Smoke test:** after a deploy, `scripts/smoke-test.sh` fetches a few known live URLs (home, a post, tag pages
   including `tags/.NET.html`, the Topics page, feed, sitemap, an image, a font) and expects 200, plus 404 for a draft and `docs/`.
   It retries for a few minutes to allow for the CDN. If it fails, the run goes red **after** the site is live:
   check the listed URLs, then fix forward or roll back (below). Run it yourself any time: `scripts/smoke-test.sh`.

**Pull requests stop after step 3:** they build and check but never deploy. Merging to `main` *is* publishing.

| Check | Protects against |
|---|---|
| Drafts are not published | Unfinished `*-draft.html` posts going live |
| Live reload is not published | The preview's live-reload script (marker `livereload`) going live |
| Published URLs still exist (`./gradlew checkUrls`) | Removing or renaming any page that's live (strict on CI's Linux runner) |
| Docs are not published (`./gradlew checkDocsNotPublished`) | Anything from `docs/` (planning notes) going live |
| Old tag URLs redirect (`./gradlew checkTagRedirects`) | An old tag URL that's missing, or whose redirect stub points at a page that isn't there; a tag page that shows its tag differently from the posts |
| Masthead variants (`./gradlew checkMastheadVariants`) | A header image missing its 960/1440px sizes (phones would get a blank header) |
| Feed and sitemap XML | A broken `feed.xml` or `sitemap.xml` |
| Local links and assets (lychee) | Broken internal links, or missing images, CSS, JS, or fonts |

**The Gradle tasks** (`./gradlew tasks` lists them in three groups, T133, T127). `bake` and `bakePreview` are the JBake
Gradle plugin's names, kept since 2014 ([`docs/06-GRADLE-PLUGIN.md`](docs/06-GRADLE-PLUGIN.md), "The public surface");
the `writing` tasks are this site's own, and the `verification` group (Gradle's name) holds the checks CI runs. All of
JBake's libraries are on one Gradle configuration, `jbake`, where `build.gradle` pins their versions.

| Task | Group | What it does |
|---|---|---|
| `bake` | `jbake` | Bakes the site from `src/jbake/` into `build/jbake/` (what CI deploys, minus drafts) |
| `bakePreview` | `jbake` | Bakes, serves http://localhost:8080, rebuilds on save, and reloads the open page |
| `newPost` | `writing` | Starts a draft post on a new `post/<slug>` branch: `./gradlew newPost -Ptitle="My Post"` |
| `reviewChanges` | `writing` | Reviews the pages this branch changed: Markdown, links, images, header (runs `bake` first) |
| `openPr` | `writing` | Reviews this branch's changes, then pushes it and opens a pull request against `main` (`-Ptitle`, `-Pmessage` for uncommitted work, `-Pcloses`, `-Prefs`, `-PdryRun`) |
| `prepareImage` | `writing` | Prepares a hand-added image: sRGB, metadata, size, masthead variants (`-Pimage=<path>`; `-Pvariants` for the variants alone) |
| `tagRedirects` | `writing` | Writes any missing or outdated redirect stub for an old tag URL, never deleting one (on an approved tag rename) |
| `checkSite` | `verification` | Runs the four checks below, what CI runs on the staged site (CI: `-PsiteDir=build/site`) |
| `checkUrls` | `verification` | Checks that every published URL in `docs/baseline/urls.txt` is still a page in the baked site, or an approved removal in `docs/baseline/expected-removals.txt` (AGENTS.md Rule 1); `-Pstrict` fails on the case-only twins a Mac can't bake |
| `checkDocsNotPublished` | `verification` | Checks that nothing from `docs/` is in the baked site: no `docs/`, no Markdown outside `vendor/`, no `REVIVAL` marker (Rule 4) |
| `checkTagRedirects` | `verification` | Checks that every old tag URL in the baked site is a tag page or a redirect stub to one |
| `checkMastheadVariants` | `verification` | Checks that every header image in use has its 960 and 1440px variants |

Two tasks have no group, so `./gradlew tasks` doesn't list them: `compileSass` (the SCSS to CSS step) and
`checkNoHandEditedCss` (the guard that fails the build if a hand-written copy of the generated CSS is back under
`src/jbake/assets/css/`). Both run as part of `bake` and `bakePreview`. `clean` and `check` are Gradle's own;
`check` runs `bake`, `reviewChanges` and `checkSite` (the review is about the change, `checkSite` about the site, so
CI can run `checkSite` on every push and the review on pull requests only, where there is a base to diff against).

### Where the build is defined: `build.gradle` and `build-logic/`

The build is two things, kept apart on purpose ([`docs/06-GRADLE-PLUGIN.md`](docs/06-GRADLE-PLUGIN.md), done in
October 2026, M10):

- **`build.gradle`** (about 90 lines) says **what the site is**: it applies the plugin, configures it in a
  `jbakeSite { ... }` block (the directories, the published CSS path, the guard's list of retired stylesheets), and
  declares **the versions on JBake's classpath** in `dependencies { jbake ... }`, with a comment on every pin saying
  why it is there (the engines the site uses, the preview's Jetty and commons-vfs2, and the advisories T112 raised
  the transitive versions for). Dependabot proposes updates to these; check one by comparing a bake against `main`.
- **`build-logic/`** says **how it is built**: the site's own Gradle plugin, **`jbake-site`**, an *included build*
  (`settings.gradle`: `pluginManagement { includeBuild 'build-logic' }`) written in Java 21 with its own tests. Every
  task in the table above is a class there, so **`bake` is defined in
  `build-logic/src/main/java/com/mikemcgarr/jbakesite/BakeTask.java`**, registered by `JbakeSitePlugin.java` in the
  same package. Gradle builds the plugin like any other project it includes: its compiled classes live in
  `build-logic/build/` and are reused by every later invocation, across daemons, until a source under `build-logic/`
  changes; then that one build recompiles the plugin (a few seconds) before the site's tasks run.

**What is in the plugin** (`build-logic/src/main/java/com/mikemcgarr/jbakesite/`, by package; every class, with the
`review` package's helpers summed up):

| Package | Classes | What they do |
|---|---|---|
| `jbakesite` | `JbakeSitePlugin`, `JbakeSiteExtension`, `ScssExtension`, `JbakeClasspathExtension` | Registers every task and the `jbake` configuration; the `jbakeSite { }` block and its defaults |
| | `JbakeTask`, `BakeTask`, `BakePreviewTask`, `JbakeClasspath`, `NewJdkJvmArgs`, `CaseRedirects`, `PreviewPort`, `ProjectPaths` | The `jbake` group: JBake's launcher in a fresh JVM (the JVM flags newer JDKs need, jffi's temp dir, logback), the bake with the CSS and case-redirect copies, the preview with its port check |
| | `CompileSassTask`, `SassCompiler`, `SassEntries`, `SassCompileException`, `SassPreviewWatcher`, `CheckNoHandEditedCssTask`, `FileTrees` | The theme CSS: Dart Sass through its Java host, the entry convention, the preview's SCSS watcher, the guard |
| `writing` | `NewPostTask`/`NewPost`/`NewPostOptions`, `OpenPrTask`/`OpenPr`/`OpenPrOptions`, `GitHubCli` (the gh readiness checks both share), `PrepareImageTask`/`PrepareImage`/`PrepareImageOptions`/`Images`/`MastheadVariants`, `TagRedirectsTask`/`TagRedirects` | The `writing` group: each task is a thin class that reads its `-P` options and runs a plain logic class |
| `review` | `ReviewChangesTask`, `ReviewRunner`/`ReviewOutcome` (the entry point `reviewChanges` and `openPr` call), `Review`, `Changes`, `Pages`, `MarkdownChecks`, `PageChecks`, `ImageChecks`, `RemovalChecks`, `Report`, and the helpers `Change`, `Finding`, `Findings`, `HtmlTags`, `Suppressions`, `ReviewOptions`, `ReviewResult` | `reviewChanges`: one class per group of checks, as [docs/05-REVIEW-FLOW.md](docs/05-REVIEW-FLOW.md) lays them out |
| `verification` | `CheckUrlsTask`/`UrlCheck`, `CheckDocsNotPublishedTask`/`DocsNotPublishedCheck`, `CheckTagRedirectsTask`, `CheckMastheadVariantsTask` | The `verification` group: the checks `checkSite`, `check` and CI run (the first two were shell scripts until T141/T143) |
| `internal` | `Git`, `ProcessRunner`, `CommandRunner`, `ToolFailure`, `FrontMatter`, `Tags`, `SiteLayout`, `JbakeProperties`, `ImageRules` | Shared by the writing and verification tools: running git and gh, reading a post's header, the site's paths, the image limits |

`SiteToolTask` (package `jbakesite`) is the base of the writing and verification tasks: it hands each tool the
site's layout and turns the tool's `ToolFailure` (the one-line message the old single-file programs printed before
exiting 1 or 2) into a build failure with that message, so a refusal reads the same as before and never shows a
stack trace. The plugin's one resource, `src/main/resources/com/mikemcgarr/jbakesite/jbake-logback.xml`, is the
logback configuration JBake's JVM runs with (JBake's start and finish lines, every warning and error, OrientDB's
noise silenced); `jbakeSite.logbackConfiguration` points at a file of your own instead.

**The `jbakeSite { }` block.** Every property has a default, and `build.gradle` writes the ones the README names
out anyway, so the paths are in one place:

| Property | Default | What it means |
|---|---|---|
| `source` | `src/jbake` | JBake's source: `content/`, `templates/`, `assets/`, `jbake.properties` |
| `destination` | `build/jbake` | Where the bake writes and what the preview serves; CI deploys it minus drafts |
| `caseRedirects` | `src/case-redirects` | Redirect stubs that differ from a real page only by letter case, copied in after JBake, on a case-sensitive filesystem only (T055); an empty or missing directory means none |
| `publishedUrls` | `docs/baseline/urls.txt` | The baseline of published URLs: `checkUrls` fails when one is missing from the bake; the writing tools read it too (`newPost` refuses a published slug; the review and the tag redirects use it) |
| `expectedRemovals` | `docs/baseline/expected-removals.txt` | Published URLs removed on purpose, with the owner's approval (Rule 1); `checkUrls` accepts a missing URL only when it is listed here. A missing file means none |
| `tagRenames` | the owner's list of 2026-10-09 | Old tag to new, as written in the posts; every old URL of the old tag gets a redirect stub (Rule 1: never remove one) |
| `commitTrailers` | none | Lines `openPr` appends, after a blank line, to the commit it makes from uncommitted work with `-Pmessage` (`Co-Authored-By: …`); the message itself is the author's |
| `logbackConfiguration` | unset (the bundled file) | A logback configuration for JBake's JVM |
| `scss.source` | `src/scss` | The SCSS; each `<name>.scss` not starting with `_` is an entry |
| `scss.publishedDir` | `css` | An entry `<name>.scss` is published at `/<publishedDir>/<name>.css`; the same folder under the assets is the one the guard watches |
| `scss.retiredCss` | none | Hand-written stylesheets that are gone and must not come back under the assets either (`extra.css`, `fonts.css`, the old minified theme CSS) |
| `scss.handEditedCssExplanation` | a sentence naming `scss.source` | What the guard's message says after the offending path |
| `classpath.excludedGroups` | Thymeleaf, Pebble, Jade4j, the Groovy templates, AsciidoctorJ | Maven groups left off JBake's classpath: the engines the site doesn't use (T112). A site that uses one sets the list without it |
| `classpath.substitutions` | `org.lz4:lz4-java` → `at.yawk.lz4:lz4-java:1.12.0` | Modules replaced on the classpath by a maintained fork with the same classes (T112) |

**The `jbake` configuration** is the plugin's (`JbakeClasspath`): one classpath for the bake and the preview, named
as the JBake Gradle plugin named it in 2014. `build.gradle` declares the versions on it; the plugin applies the
exclusions and the substitution when it resolves, so `jbakeSite { classpath { ... } }` can change them after the
plugin is applied, and `configurations.jbake { exclude group: '...' }` still works the Gradle way for more.
`./gradlew dependencies --configuration jbake` shows the result (the lz4 line reads `org.lz4:lz4-java:1.4.0 ->
at.yawk.lz4:lz4-java:1.12.0`).

**The preview's watcher** (`SassPreviewWatcher`, a Gradle build service `bakePreview` starts before JBake's
server): a daemon thread checks the SCSS files' timestamps every 0.5 s and, on a change, compiles every entry (a
partial can feed any of them) with the same compiler and options as `compileSass`, each into a temporary file moved
into `build/jbake/css/` atomically, so the browser never reads a half-written file. A Sass error prints Dart Sass's
message and keeps the last good CSS. JBake's own watcher covers `src/jbake/`; this one covers `src/scss/`.

**The plugin's tests** (`build-logic/src/test/`):
```
./gradlew :build-logic:check        # unit tests, TestKit tests and Gradle's plugin validation; CI runs this before the bake
./gradlew :build-logic:test --tests '*BakeFunctionalTest'   # one class
```
The unit tests (JUnit 6) use `ProjectBuilder` for the plugin's wiring and the real Dart Sass, git and ImageIO for the
tools. The TestKit tests (`*FunctionalTest`) copy the fixture site under `build-logic/src/test/fixtures/site` (two
posts, one page, `clean-blog.scss` with a partial, one case-redirect stub, one tag redirect stub) into a temporary
build and run Gradle on it with the configuration cache on, as the site runs it: the pages render and the CSS is
copied in, the stubs appear on a case-sensitive filesystem and are skipped with the message on a Mac, the guard
fails the bake, `newPost` passes its `-P` options through and maps a refusal to a failure, the review and the checks
pass and fail as they should, and a second run reuses the configuration cache. The first run downloads JBake's
libraries into TestKit's own Gradle home, so it takes a minute. `./gradlew :build-logic:javadoc` runs doclint on
the Javadoc.

**To add a build feature:**
1. Decide what it is. A **task** goes in the package of its group (`writing`, `verification`, or `jbakesite` for the
   bake and preview); a **site setting** it needs goes on `JbakeSiteExtension` (or `ScssExtension`,
   `JbakeClasspathExtension`) with a default set in `JbakeSitePlugin.setDefaults`.
2. Write the task class: `abstract`, extending `SiteToolTask` (a tool that reads the source tree and talks to the
   author) or `DefaultTask`, with managed `Property`/`DirectoryProperty` inputs annotated `@Input`, `@InputFiles`,
   `@OutputDirectory` or `@Internal`, and a `@TaskAction`. Read `-P` options as `providers.gradleProperty("name")`
   in the plugin, never `project.property` in the task, so the configuration cache keeps working. Keep the logic in
   a plain class the task calls, with the paths and the output injected, so a unit test needs no Gradle.
3. Register it in `JbakeSitePlugin` with its group and a one-line description (the text `./gradlew tasks` shows);
   wire it into `check` if CI should run it.
4. Test it: a unit test of the logic class, a case in `JbakeSitePluginTest` for the wiring, and, if it changes what
   a build does, a TestKit case on the fixture site. `./gradlew :build-logic:check` must pass, and a `clean bake`
   must stay byte-identical to a bake of `main` (`diff -rq`, apart from `feed.xml`'s timestamps).
5. Document it: the task table above, AGENTS.md's Commands block, and the section of this README it belongs to.

**GitHub settings and DNS this depends on:**
- **Settings → Pages → Build and deployment → Source: "GitHub Actions".**
- **Custom domain:** `www.mikemcgarr.com`, with **Enforce HTTPS** on (Settings → Pages). The HTTPS certificate is
  issued by Let's Encrypt and managed by GitHub automatically.
- **DNS (GoDaddy):** `www` CNAME → `jmcgarr.github.io`. `@` (bare domain) A records → `185.199.108.153`,
  `185.199.109.153`, `185.199.110.153`, `185.199.111.153`.
- **No secrets or tokens.** The build job has read-only access, and the deploy job can only deploy to Pages.

**Redeploy or roll back:**
- **Redeploy:** Actions → Java CI with Gradle → **Run workflow** (branch `main`).
- **Roll back a bad change:** revert the PR on GitHub and merge the revert. That deploys the previous content.
  For a faster stopgap, open an older successful run on `main` and **Re-run all jobs**, which rebuilds and
  redeploys that commit.
- **Emergency:** Settings → Pages → Source → **"Deploy from a branch: `master`"** serves the last build made
  before the switch to Actions (Oct 2026). `master` is otherwise retired, so don't push to it.

**Dependency updates:** Dependabot (`.github/dependabot.yml`) opens weekly PRs for the workflow's actions and
Gradle. Each runs the same build and checks, and never deploys.

**Run the same checks locally:**
```
./gradlew clean checkSite
```
It bakes and runs the four plugin checks on `build/jbake`: `checkUrls` (no published URL may disappear),
`checkDocsNotPublished` (docs/ must never reach the site), `checkTagRedirects` (old tag URLs are tag pages or
working redirects) and `checkMastheadVariants` (header images have their phone/tablet sizes). `./gradlew check` runs
them too, plus `reviewChanges`. To check exactly what CI deploys, stage the bake without drafts first and point the
checks at it, as the workflow does:
```
rm -rf build/site && rsync -a --prune-empty-dirs --exclude '*-draft.html' build/jbake/ build/site/
./gradlew checkSite -PsiteDir=build/site
xmllint --noout build/site/feed.xml build/site/sitemap.xml
```
The link checker (lychee) runs in CI. To run it locally, install [lychee](https://lychee.cli.rs) and see the
"Check local links and assets" step in the workflow for its options. Until T143 (2026-10-28) the URL and docs checks
were two shell scripts under `scripts/`; the tasks print the same lines.

**Old tag URLs (T039, T055):** tag pages used to be published with spaces (`/tags/acceptance test.html`), and two
tags had a capitalized twin (`/tags/DevOps.html`, `/tags/Groovy.html`). Those 59 URLs (50 until the 2026-10-09 tag merges) are now small redirect pages
(meta refresh, `rel=canonical`, `noindex`, and a visible link) to the new pages, made by `./gradlew tagRedirects`
(the plugin's `TagRedirects` class in `build-logic/`; `scripts/TagRedirects.java` until T127, 2026-10-28, and
`tag-redirects.py` until T139, 2026-10-27) from `docs/baseline/urls.txt` and committed: 48 in `src/jbake/assets/tags/`, and the 2 capitalized ones in
`src/case-redirects/tags/`. Run the task again if a tag in that list stops being used, and keep its stubs.
**Merging or renaming a tag** (needs the owner's approval, AGENTS.md Rule 1): add `old → new` to `RENAMES` in
`TagRedirects.java`, edit the posts' `tags=` lines, and run the task. It writes a stub at every old URL of the old tag: the
baseline one and, for a tag with a space, the hyphenated page published since T039 (`tags/google-calender.html`).
`checkTagRedirects` covers those hyphenated URLs too (derived from the baseline, which they aren't in). The 2026-10-09 merges
and typo fixes (report §H) added 9 stubs and repointed 3, for 59 in all.
**macOS caveat:** macOS filesystems ignore case, so `tags/DevOps.html` and `tags/devops.html` are the same file
there. `./gradlew bake` therefore adds `src/case-redirects/` only when `build/jbake` is case-sensitive (Linux, as
in CI and on GitHub Pages) and says so when it skips them. A local bake on a Mac has the real `devops` and `groovy`
pages but not those 2 stubs, and `checkUrls` and `checkTagRedirects` report them as "can't check here".
They're not JBake assets on purpose: JBake copies assets after rendering, so on a Mac the stub would overwrite the
real page and redirect to itself.

More
====
- [AGENTS.md](AGENTS.md): working rules for this repo (URLs, branches, drafts, issues)
- [docs/05-REVIEW-FLOW.md](docs/05-REVIEW-FLOW.md): the current plan (checking a change before it ships)
- [docs/archive/04-AUTHORS-DESK.md](docs/archive/04-AUTHORS-DESK.md): the owner's own to-do list, closed and archived 2026-10-27
- [docs/07-FRONT-MATTER.md](docs/07-FRONT-MATTER.md): front matter for VS Code authoring, three options (Draft, owner to choose)
- [docs/archive/00-REVIVAL.md](docs/archive/00-REVIVAL.md): the 2026 revival plan, done (its changelog: [docs/archive/03-REVIVAL-CHANGELOG.md](docs/archive/03-REVIVAL-CHANGELOG.md))

Optional mastheads
==================
- https://unsplash.com/photos/tGTVxeOr_Rs
