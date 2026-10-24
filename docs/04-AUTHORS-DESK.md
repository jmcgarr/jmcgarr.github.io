# Author's Desk

**Branch**: `docs/revival-changelog` | **Created**: 2026-10-12 | **Status**: Accepted

The owner's own to-do list for the site: everything only the author can write, choose or click. These tasks
were milestone M8 of the [revival plan](archive/00-REVIVAL.md). On 2026-10-12 the owner moved them into this doc,
because it's the owner's own work rather than shared work with an agent. They keep their task and checkpoint IDs
(`T###`, `CHK###`) from the revival plan, and new ones continue that numbering. The conventions are the same
([`AGENTS.md`](../AGENTS.md)): every change on its own branch and PR, every task with a `Test:` line, and ticks
only after the test passes.

The material for most tasks is already gathered in the content report,
[`reports/content-2026-10-09.md`](reports/content-2026-10-09.md): §F (alt text), §I.1 (About, Talks, Bio),
§I.2 (drafts), §I.3 (mastheads), §I.4 (slug typos) and §I.5. The report predates T102 (done 2026-10-18), which
converted every post and page to Markdown: a file it names as `.html` or `.asciidoc` is now `.md` with the same
base name, its line numbers may be off by a few, and every image on the site is an HTML `<img>` tag inside the
Markdown, so an alt-text change is always an `alt="…"` edit. The findings themselves are unchanged (checked
2026-10-18, see the notes under each task).

**Goal**: Everything only the author can write, choose or click: current bio and talks, alt text in the
author's words, the drafts, the old posts' header images, and the account settings only the owner can reach.
Agents can prepare material for each task, such as proposed wording or a list of candidates, but the owner makes the call.

**Independent Test**: About and Talks are current (Speaker Bio was removed 2026-10-22); every draft has a decision; every
meaningful image has alt text the owner approved; and issue #14 is closed.

### Bugs

- [x] T048 [BUG] The About page is out of date (it says "currently at Slack", from 2018). Review the Talks and Speaker Bio pages too (`src/jbake/content/about.md`, `talks.md`, `speaker-bio.md`; Markdown since T102, see README, "To write a post")
  - **Test:** A manual read-through confirms roles, employers, and dates are current, and each page's `date=` is updated.
  - _2026-10-09: the out-of-date sentences on About, Speaker Bio and Talks are quoted in [`reports/content-2026-10-09.md`](reports/content-2026-10-09.md) §I.1 (no wording proposed; the author writes it), awaiting owner._
  - _2026-10-11: this review also covers the three `[… 2017]` link labels with no URL that show as literal text on Talks (§I.1). (The dead Vimeo embed, E6, is already gone: removed as A151 in #91.)_
  - _2026-10-11: moved from M6 to M8 (owner's request: the tasks only the owner can do go last)._
  - _2026-10-21: **About is done** (#139, owner's rewrite: current through X, the Moonshot Factory; the Slack, Netflix, Blackboard and D.C. paragraphs condensed to one). A follow-up PR fixed the Booz Allen Hamilton link, which was missing its `(` and rendered as literal brackets plus a bare URL, and set About's `date=` to 2026-10-21 (its only use is the sitemap `lastmod`). **Still open:** Speaker Bio (`speaker-bio.md` line 11 still says "currently leads Slack's Frontend Infrastructure team"; its `date=` is `2014-11-03`), Talks (`talks.md`: the three `\[… 2017\]` labels at lines 58, 70 and 94, and `date=2014-09-01`). Two things on About for the owner to confirm, not changed: HUD's name is "Housing and Urban Development" (the page says "Housing and Development"), and "shaped and devour as much as I can" reads as if a word is missing (both pre-date or come from the owner's text)._
  - _2026-10-21 (later): **the three Talks labels link to their recordings now** (`fix/T048-talks-labels`; the owner found the videos while reviewing the review-flow plan): `\[Atlassian Summit 2017\]`, `\[Gradle Summit 2017\]` and `\[DevOpsCon 2017\]` are Markdown links to the YouTube recordings (`watch?v=` form, like the existing DevOpsCon link on the page; list items are not turned into players by `embeds.ftl`, so they render as links). Nothing else on the page changed. **Still open:** Speaker Bio's sentence and date, and Talks' `date=2014-09-01`._
  - _2026-10-18: the three pages are Markdown now (#132, prose unchanged), so edit `about.md`, `talks.md` and `speaker-bio.md`. Checked the same day: About still says "currently a Senior Engineering Manager at Slack" (line 14), Speaker Bio "currently leads Slack's Frontend Infrastructure team" (line 11), and the page dates are still `2014-08-30`, `2014-11-03` and `2014-09-01`. The three labels on Talks are now `\[Atlassian Summit 2017\]` (line 58), `\[Gradle Summit 2017\]` (line 70) and `\[DevOpsCon 2017\]` (line 94); they still render as literal brackets and need a URL each, or the brackets removed._
  - _2026-10-22: **done.** The owner removed the Speaker Bio page (no longer doing public speaking): `speaker-bio.md` deleted, a redirect stub at `/speaker-bio.html` points to `/about.html` (`src/jbake/assets/speaker-bio.html`, Rule 1), nothing on the site linked to it, and Talks' `date=` is 2026-10-21 (its last content change, #142). The two About wording questions above remain the owner's._

### Improvements

- [ ] T022 [IMP] Choose the site's fonts. **Deferred 2026-10-05 (owner):** don't change fonts yet; the owner wants to settle on the *right* fonts first. Where the site stands today: body text and headings use the system font stack (the May 2024 change); Open Sans is self-hosted from `/fonts/open-sans/` (T073) in the three styles the CSS uses, 300 for the header subheadings, 300 italic for post dates and 800 for the nav; Lora is not loaded at all. The original work ("self-host Lora and Open Sans, or drop them for the system font stack") is therefore done, and what's left is the owner's choice: keep this mix, drop Open Sans too, or pick new fonts (`src/jbake/templates/header.ftl`, `src/jbake/assets/css/fonts.css`, `src/scss/_variables.scss` and `_mixins.scss`, which generate `clean-blog.css` since T043)
  - **Test:** the decision is recorded in a note under this task. If the fonts change: the DevTools Network tab shows only same-origin font requests (none to `fonts.googleapis.com` or `fonts.gstatic.com`), and the nav, headings and body text look right on home and on a post.
  - _2026-10-11: moved from M2 to M8, because it waits on the owner's choice of fonts._
  - _2026-10-18: rewritten to match the site. T073 (2026-10-06) self-hosted Open Sans, so the old Test ("no requests to Google Fonts") already passes and no longer proves anything about this task; the old description still said Google Fonts loads 10 Open Sans and 4 Lora variants._
  - _2026-10-24: the owner reviewed the site and has no font changes in mind, but is open to suggestions, and asked for the CSS to be cleaned up, simplified and standardized without changing the look. The CSS review, the font suggestions (S1 keep as is, S2 system stack everywhere, S3 trim `fonts.css` to the Latin subsets; recommendation S1, optionally S3) and the proposed cleanup, delivery and code-block tasks are in [`reports/css-review-2026-10-24.md`](reports/css-review-2026-10-24.md). The fonts decision stays open until the owner picks a suggestion (or "keep as is"); the CSS cleanup will be its own PR._
- [x] T053 [P] [IMP] Triage the 12-post drafts backlog: finish and publish, keep as drafts, or delete (`src/jbake/content/blog/drafts/`; rendered for local preview only since T009)
  - **Test:** Every draft has a recorded decision (a short table in `docs/reports/drafts-triage.md`).
  - _2026-10-11: moved from M6 to M8 (owner's request: the tasks only the owner can do go last)._
  - _2026-10-18: still 12 drafts, all Markdown now (#124, #128, #132). #128 removed the WordPress export debris but kept the author's own placeholder notes verbatim, unclosed `[` and all (`\[might want to add an example here` in `acceptance-test-driven-development.md`, `\[sourcecode` / `\[/sourcecode` in `evolving-features-into-acceptance-tests.md`, `\[Insert Picture with …` in `maven-pipelines.md`): they are part of this triage. Preview any draft at `http://localhost:8080/blog/drafts/<name>-draft.html` with `./gradlew bakePreview`._
  - _2026-10-23: **done** (owner). 11 drafts deleted, 1 kept to work on (`retrospectives-building-collective-ownership.md`); the decisions are in [`reports/drafts-triage-2026-10-23.md`](reports/drafts-triage-2026-10-23.md) (dated, per the `docs/reports/` naming convention, where the Test said `drafts-triage.md`). Drafts were never published, so no URL changed; the smoke test's "drafts never go live" probe now uses the remaining draft._
- [x] T054 **Won't do (owner decision, 2026-10-22).** [P] [IMP] _(Optional)_ Fix the slug typos (`relections-and-projections-2019`, `vagrant-cheatsheat`, `developer-reading-lis`), but **only** together with redirect stubs at the old URLs (`src/jbake/content/blog/`)
  - **Test:** Each old URL redirects to the new one. `scripts/check-urls.sh` passes with the old URLs still present as stubs.
  - _2026-10-11: moved from M6 to M8 (owner's request: the tasks only the owner can do go last)._
  - _2026-10-22: closed, won't do: the owner keeps the slugs as they are; the three URLs stay as published._
- [ ] T059 [P] [IMP] Give older posts their own mastheads. On 2026-10-04, 65 of 74 published posts used the default image (still 65 on 2026-10-18). This is manual, creative work: choose an image for each post, with credit, that follows the image size rule in `AGENTS.md`, or use a remote image (`masthead=` takes a full URL, or a site path starting with `/`, since T037). Only add `masthead=` and `mastheadCredit=` lines to each post's `key=value` header. After adding a local image, run `scripts/masthead-variants.py` to make its 960px and 1440px variants; CI fails without them (remote mastheads need none) (`src/jbake/content/blog/`, `src/jbake/assets/img/masthead/`)
  - **Issue:** Closes #14
  - **Test:** `grep -L '^masthead=' src/jbake/content/blog/*.md | wc -l` returns `0` (baseline: 65). No newly added image in `src/jbake/assets/img/masthead/` is over 400 KB. Every new masthead has a `mastheadCredit=`. `python3 scripts/masthead-variants.py --check` exits 0.
  - _2026-10-11: moved from M6 to M8 (owner's request: the tasks only the owner can do go last)._
  - _2026-10-18: the posts are all Markdown with LF line endings now (T102, T051), so the Test lost its `tr '\r' '\n'` and the task its "keep each file's line endings" caution; the count is unchanged at 65. T037 (done 2026-10-08) made the remote-URL option real, and the variants step comes from T071._
- [x] T109 [P] [IMP] Alt text in the author's words for the 34 images whose `alt` is empty, a file name, or "Profile" (report [§F](reports/content-2026-10-09.md#f-alt-text-accessibility--owner-approves-wording): proposed wording for each, F1–F34). Found by T046 (M5). The owner approves or rewrites each one; logos next to text that names the organization may get `alt=""` instead. F5 is moot (C3 removed that image) (`src/jbake/content/`)
  - **Test:** each F item has the owner's wording (or "decorative") recorded, and `grep -rnE '<img[^>]*alt=""' src/jbake/content` plus `grep -rn 'Profile' src/jbake/content/*.*` list only images the owner marked decorative.
  - _2026-10-22: recommended wording applied to every open F item in the `post/T109-alt-text` PR; the owner approves or edits each in review. F5, F28 and F29–F34 are moot (that image, the second About photo, and the Speaker Bio page were removed). Logos next to text naming the organization (F1, F7–F9, F12, F17, F19, F25, F26) got the short "<Name> logo" form; the owner may make any of them alt="" instead._
  - _2026-10-18: all 33 remaining items are still open (17 empty, 8 file-name, 8 "Profile"; counted after T102). Each is an HTML `<img>` tag in a Markdown file now, so every fix is the `alt="…"` value only; the report's AsciiDoc `image:` instructions no longer apply, and its `.html` and `.asciidoc` file names are the same base names with `.md`. The Test's second grep covers `*.*` for the same reason. The F items and their proposed wording are unchanged._
- [x] T110 [P] [IMP] Check the SlideShare embeds and links in a browser. SlideShare answers scripts with a bot challenge, so about 30 URLs (the 12 slide embeds on Talks and in posts, and the other SlideShare links) can't be checked automatically (report §I.5) (`src/jbake/content/`)
  - **Test:** a note under this task lists each SlideShare deck as working or not, from one look in a browser; any dead one gets a T050-style fix (live replacement, snapshot, or the link removed and the text kept).
  - _2026-10-22: closed as done by the owner, who checked the SlideShare decks in a browser; no dead deck was reported, so no T050-style fix was needed._
  - _2026-10-18: every embed is a Markdown link now, rendered as a player at build time (T115; #125 for the posts, #132 for Talks): 8 SlideShare embeds on Talks and 4 in posts make the 12, and the content holds 30 distinct SlideShare URLs in all. The decks' URLs did not change; check them on the live site in a browser as before._
- [x] T116 [P] [IMP] _(Optional)_ Two posts end a paragraph with a stray `<br />` from WordPress, right before a code block, which T102 kept because the conversion was mechanical: `openjdk-6-install-lucid.md` ("like so:<br />") and `ruby-broke-my-path.md` ("bash_profile*:<br />"). Remove the two tags, or keep them (`src/jbake/content/blog/`)
  - **Test:** `grep -n '<br />$' src/jbake/content/blog/openjdk-6-install-lucid.md src/jbake/content/blog/ruby-broke-my-path.md` lists nothing, and each page looks the same apart from one blank line less above the code block.
  - _2026-10-22: **done** (owner). Both `<br />` tags removed; the Test's grep lists nothing, and each page lost only the blank line above its code block._
- [ ] T111 [IMP] Verify `mikemcgarr.com` for GitHub Pages, so nobody else can claim the domain on GitHub Pages. GitHub account → Settings → Pages → Add a domain, then add the DNS `TXT` record it shows at GoDaddy (`domaincontrol.com`). Optional; came up during T068 and the owner deferred it (2026-10-05) (no repository change)
  - **Test:** GitHub's Pages settings list `mikemcgarr.com` as **Verified**, and `dig +short TXT _github-pages-challenge-jmcgarr.mikemcgarr.com` returns the record.

### Checkpoint

- [x] CHK030 About and Talks have been reviewed and are current; the Speaker Bio page was removed (owner, 2026-10-22)
- [x] CHK032 Every draft has a triage decision (T053, 2026-10-23)
- [ ] CHK039 Issue #14 is closed by a merged PR
- [x] CHK042 Every F item in report §F has the owner's alt text or a "decorative" decision (T109)
