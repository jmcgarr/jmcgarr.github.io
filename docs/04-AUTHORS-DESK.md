# Author's Desk

**Branch**: `docs/revival-changelog` | **Created**: 2026-10-12 | **Status**: Accepted

The owner's own to-do list for the site: everything only the author can write, choose or click. These tasks
were milestone M8 of the [revival plan](00-REVIVAL.md). On 2026-10-12 the owner moved them into this doc,
because it's the owner's own work rather than shared work with an agent. They keep their task and checkpoint IDs
(`T###`, `CHK###`) from the revival plan, and new ones continue that numbering. The conventions are the same
([`AGENTS.md`](../AGENTS.md)): every change on its own branch and PR, every task with a `Test:` line, and ticks
only after the test passes.

The material for most tasks is already gathered in the content report,
[`reports/content-2026-10-09.md`](reports/content-2026-10-09.md): §F (alt text), §I.1 (About, Talks, Bio),
§I.2 (drafts), §I.3 (mastheads), §I.4 (slug typos) and §I.5.

**Goal**: Everything only the author can write, choose or click: current bio and talks, alt text in the
author's words, the drafts, the old posts' header images, and the account settings only the owner can reach.
Agents can prepare material for each task, such as proposed wording or a list of candidates, but the owner makes the call.

**Independent Test**: About, Talks, and Speaker Bio are current; every draft has a decision; every
meaningful image has alt text the owner approved; and issue #14 is closed.

### Bugs

- [ ] T048 [BUG] The About page is out of date (it says "currently at Slack", from 2018). Review the Talks and Speaker Bio pages too (`src/jbake/content/about.asciidoc`, `talks.asciidoc`, `speaker-bio.asciidoc`)
  - **Test:** A manual read-through confirms roles, employers, and dates are current, and each page's `date=` is updated.
  - _2026-10-09: the out-of-date sentences on About, Speaker Bio and Talks are quoted in [`reports/content-2026-10-09.md`](reports/content-2026-10-09.md) §I.1 (no wording proposed; the author writes it), awaiting owner._
  - _2026-10-11: this review also covers the three `[… 2017]` link labels with no URL that show as literal text on Talks (§I.1). (The dead Vimeo embed, E6, is already gone: removed as A151 in #91.)_
  - _2026-10-11: moved from M6 to M8 (owner's request: the tasks only the owner can do go last)._

### Improvements

- [ ] T022 [IMP] **Deferred 2026-10-05 (owner):** don't change fonts yet. The owner wants to settle on the *right* fonts first, and is open to optimizing font downloads afterwards (for example, Google Fonts loads 10 Open Sans + 4 Lora variants, which are used only for the nav, buttons, and header subheadings). Was: Self-host Lora and Open Sans, or drop them for the system font stack (this continues the May 2024 font changes) (`src/jbake/templates/header.ftl`, `src/scss/_variables.scss` and `_mixins.scss`, which generate `clean-blog.css` since T043)
  - **Test:** The DevTools Network tab shows no requests to `fonts.googleapis.com` or `fonts.gstatic.com`. Headings and body text look right on home and on a post.
  - _2026-10-11: moved from M2 to M8, because it waits on the owner's choice of fonts._
- [ ] T053 [P] [IMP] Triage the 12-post drafts backlog: finish and publish, keep as drafts, or delete (`drafts/` after T009)
  - **Test:** Every draft has a recorded decision (a short table in `docs/reports/drafts-triage.md`).
  - _2026-10-11: moved from M6 to M8 (owner's request: the tasks only the owner can do go last)._
- [ ] T054 [P] [IMP] _(Optional)_ Fix the slug typos (`relections-and-projections-2019`, `vagrant-cheatsheat`, `developer-reading-lis`), but **only** together with redirect stubs at the old URLs (`src/jbake/content/blog/`)
  - **Test:** Each old URL redirects to the new one. `check-urls.sh` passes with the old URLs still present as stubs.
  - _2026-10-11: moved from M6 to M8 (owner's request: the tasks only the owner can do go last)._
- [ ] T059 [P] [IMP] Give older posts their own mastheads. On 2026-10-04, 65 of 74 published posts used the default image. This is manual, creative work: choose an image for each post, with credit, that follows the image size rule in `AGENTS.md`, or use a remote image once T037 supports it. Only add `masthead=` and `mastheadCredit=` lines, and keep each file's existing line endings (`src/jbake/content/blog/`)
  - **Issue:** Closes #14
  - **Test:** `for f in src/jbake/content/blog/*.*; do tr '\r' '\n' < "$f" | grep -q '^masthead=' || echo "$f"; done | wc -l` returns `0` (baseline: 65). No newly added image in `src/jbake/assets/img/masthead/` is over 400 KB. Every new masthead has a `mastheadCredit=`.
  - _2026-10-11: moved from M6 to M8 (owner's request: the tasks only the owner can do go last)._
- [ ] T109 [P] [IMP] Alt text in the author's words for the 34 images whose `alt` is empty, a file name, or "Profile" (report [§F](reports/content-2026-10-09.md#f-alt-text-accessibility--owner-approves-wording): proposed wording for each, F1–F34). Found by T046 (M5). The owner approves or rewrites each one; logos next to text that names the organization may get `alt=""` instead. F5 is moot (C3 removed that image) (`src/jbake/content/`)
  - **Test:** each F item has the owner's wording (or "decorative") recorded, and `grep -rnE '<img[^>]*alt=""' src/jbake/content` plus `grep -rn 'Profile' src/jbake/content/*.asciidoc` list only images the owner marked decorative.
- [ ] T110 [P] [IMP] Check the SlideShare embeds and links in a browser. SlideShare answers scripts with a bot challenge, so about 30 URLs (the 12 slide embeds on Talks and in posts, and the other SlideShare links) can't be checked automatically (report §I.5) (`src/jbake/content/`)
  - **Test:** a note under this task lists each SlideShare deck as working or not, from one look in a browser; any dead one gets a T050-style fix (live replacement, snapshot, or the link removed and the text kept).
- [ ] T111 [IMP] Verify `mikemcgarr.com` for GitHub Pages, so nobody else can claim the domain on GitHub Pages. GitHub account → Settings → Pages → Add a domain, then add the DNS `TXT` record it shows at GoDaddy (`domaincontrol.com`). Optional; came up during T068 and the owner deferred it (2026-10-05) (no repository change)
  - **Test:** GitHub's Pages settings list `mikemcgarr.com` as **Verified**, and `dig +short TXT _github-pages-challenge-jmcgarr.mikemcgarr.com` returns the record.

### Checkpoint

- [ ] CHK030 About, Talks, and Speaker Bio have been reviewed and are current
- [ ] CHK032 Every draft has a triage decision
- [ ] CHK039 Issue #14 is closed by a merged PR
- [ ] CHK042 Every F item in report §F has the owner's alt text or a "decorative" decision (T109)
