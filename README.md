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
  tools under `scripts/`: they are single-file Java programs (`java scripts/<Name>.java`) and shell.

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

`./gradlew bake` runs JBake 2.7 directly in its own JVM (`build.gradle`). If a template or post has an error, the
build fails and prints JBake's message with the file and line.

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
(`java scripts/PrepareImage.java --variants <image>` does that last step alone, and never overwrites a variant). The
same command prepares an in-post image, capped at 1000px; add `-Ppng` to keep a screenshot or graphic as PNG,
`-PdryRun` to see what it would do, `-Pwidth=N` for another cap. The work is `scripts/PrepareImage.java`, a single-file
Java program like `newPost`'s (T140, 2026-10-27; before that a script that needed an image library installed), so nothing is installed. It
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
fails on warnings too. Without Gradle, after a bake: `java scripts/ReviewChanges.java`. A finding that's right in
general but wrong for one line is suppressed by a comment on the line before it, and the report counts those:
```
<!-- review: ignore E2, the brackets are intended -->
```
Links in a draft are resolved from `blog/`, where the post will live, so a relative link that only works in the
preview is caught. External links aren't checked yet (T119), and spelling is deferred (T120).

CI runs the same review on every pull request (T118, the "Review changed pages" step in
[`.github/workflows/gradle.yml`](.github/workflows/gradle.yml)), against the PR's base branch. An error fails the
job and appears as an annotation on the line in the PR's Files tab; the whole report is in the job summary. A push
to `main` skips the step: the PR already ran it.

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
Gradle by the [freefair Sass plugin](https://plugins.gradle.org/plugin/io.freefair.sass-base) through its
Java host, [`sass-embedded-host`](https://github.com/larsgrefer/dart-sass-java). No Node, Ruby, or separate install:
the host's jar (about 49 MB, from Maven Central, cached by Gradle) carries Dart Sass for macOS, Linux, and Windows.
On first use it unpacks the one for your machine into the system temp folder and runs it as a helper process (a
small Dart runtime plus the compiler), talking to it over stdin/stdout. Dependabot keeps the plugin current.

To publish
==========
There is no manual publish step. Open a pull request against `main`. When it's merged,
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
3. **Check:** every check below runs on `build/site/`, so what's checked is what ships.
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
| Docs are not published | Anything from `docs/` (planning notes) going live |
| Masthead variants | A header image missing its 960/1440px sizes (phones would get a blank header) |
| Published URLs still exist (`scripts/check-urls.sh`) | Removing or renaming any page that's live (strict on CI's Linux runner) |
| Old tag URLs redirect (`scripts/TagRedirects.java --check`) | An old tag URL that's missing, or whose redirect stub points at a page that isn't there; a tag page that shows its tag differently from the posts |
| Feed and sitemap XML | A broken `feed.xml` or `sitemap.xml` |
| Local links and assets (lychee) | Broken internal links, or missing images, CSS, JS, or fonts |

**The Gradle tasks** (`./gradlew tasks` lists them in two groups, T133). `bake` and `bakePreview` are the JBake Gradle
plugin's names, kept since 2014 ([`docs/06-GRADLE-PLUGIN.md`](docs/06-GRADLE-PLUGIN.md), "The public surface"); the
`writing` tasks are this site's own. All of JBake's libraries are on one Gradle configuration, `jbake`, where
`build.gradle` pins their versions.

| Task | Group | What it does |
|---|---|---|
| `bake` | `jbake` | Bakes the site from `src/jbake/` into `build/jbake/` (what CI deploys, minus drafts) |
| `bakePreview` | `jbake` | Bakes, serves http://localhost:8080, rebuilds on save, and reloads the open page |
| `newPost` | `writing` | Starts a draft post on a new `post/<slug>` branch: `./gradlew newPost -Ptitle="My Post"` |
| `reviewChanges` | `writing` | Reviews the pages this branch changed: Markdown, links, images, header (runs `bake` first) |
| `prepareImage` | `writing` | Prepares a hand-added image: sRGB, metadata, size, masthead variants (`-Pimage=<path>`) |

`compileSass` (the SCSS to CSS step) has no group: it runs as part of `bake` and `bakePreview`, so `./gradlew tasks`
doesn't list it. `clean` and `check` are Gradle's own; `check` runs `bake` and `reviewChanges`.

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
./gradlew clean bake
rm -rf build/site && rsync -a --prune-empty-dirs --exclude '*-draft.html' build/jbake/ build/site/
scripts/check-docs-not-published.sh build/site   # docs/ must never reach the site
scripts/check-urls.sh build/site                 # no published URL may disappear
java scripts/TagRedirects.java --check build/site   # old tag URLs are tag pages or working redirects
java scripts/PrepareImage.java --check-variants  # header images have their phone/tablet sizes
xmllint --noout build/site/feed.xml build/site/sitemap.xml
```
The link checker (lychee) runs in CI. To run it locally, install [lychee](https://lychee.cli.rs) and see the
"Check local links and assets" step in the workflow for its options.

**Old tag URLs (T039, T055):** tag pages used to be published with spaces (`/tags/acceptance test.html`), and two
tags had a capitalized twin (`/tags/DevOps.html`, `/tags/Groovy.html`). Those 50 URLs are now small redirect pages
(meta refresh, `rel=canonical`, `noindex`, and a visible link) to the new pages, made by `scripts/TagRedirects.java`
(`java scripts/TagRedirects.java`, nothing to install; `tag-redirects.py` until T139, 2026-10-27) from `docs/baseline/urls.txt` and committed: 48 in `src/jbake/assets/tags/`, and the 2 capitalized ones in
`src/case-redirects/tags/`. Run the program again if a tag in that list stops being used, and keep its stubs.
**Merging or renaming a tag** (needs the owner's approval, AGENTS.md Rule 1): add `old → new` to `RENAMES` in the
program, edit the posts' `tags=` lines, and run it. It writes a stub at every old URL of the old tag: the
baseline one and, for a tag with a space, the hyphenated page published since T039 (`tags/google-calender.html`).
`--check` covers those hyphenated URLs too (derived from the baseline, which they aren't in). The 2026-10-09 merges
and typo fixes (report §H) added 9 stubs and repointed 3, for 59 in all.
**macOS caveat:** macOS filesystems ignore case, so `tags/DevOps.html` and `tags/devops.html` are the same file
there. `./gradlew bake` therefore adds `src/case-redirects/` only when `build/jbake` is case-sensitive (Linux, as
in CI and on GitHub Pages) and says so when it skips them. A local bake on a Mac has the real `devops` and `groovy`
pages but not those 2 stubs, and `check-urls.sh` and `TagRedirects.java --check` report them as "can't check here".
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
