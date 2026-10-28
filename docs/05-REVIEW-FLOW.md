# Review Flow: check a change before it ships

**Branch**: `docs/review-flow-spec` | **Created**: 2026-10-21 | **Status**: Accepted (owner, 2026-10-21; decisions in [Notes](#notes))

A plan to add one command, `./gradlew reviewChanges`, that reviews **the pages a branch changed** for broken
Markdown, missing links and images, stale front matter, and (as advice, never as a failure) spelling. The same
check runs in CI on every pull request, so a PR with a defect turns red before the owner merges it, and a later
`./gradlew openPr` runs it before anything is pushed. It continues the revival plan
([`archive/00-REVIVAL.md`](archive/00-REVIVAL.md), done 2026-10-18) and uses its conventions: task IDs
continue from T117 and checkpoints from CHK043; the format is in [`AGENTS.md`](../AGENTS.md), "Tasks and checklists".

## Context

On 2026-10-21 the owner rewrote the About page (#139). CI was green and the PR merged, yet the live page showed
literal `[Booz Allen Hamilton]` followed by a bare URL: the Markdown link was missing its `(`. #140 fixed it the
same day. Nothing in the build or the CI gates could have caught it:

- The bake has no opinion on prose. flexmark rendered the text it was given, and the result was valid HTML.
- CI's lychee step checks the **links** in the built site, offline. The one link in that sentence (a bare URL to
  boozallen.com) was a perfectly good link. The defect was in the *text around it*.
- The other gates (URLs still exist, drafts and docs not published, masthead variants, feed and sitemap XML) are
  about the site as a whole, not about what a change did to it.

The same shape of defect sat on the live site for years: the three `\[… 2017\]` labels on Talks (noted under T048 in
[`archive/04-AUTHORS-DESK.md`](archive/04-AUTHORS-DESK.md), made plain text in #142 on 2026-10-21). And the things a reviewer *would* check on a changed page have no
tool today: does every link on it still answer, does every image exist, did the page's `date=` move (it is the
sitemap's `lastmod`), is a new tag spelled like an existing one, is a word misspelled.

The gap is a **change-scoped review**: look at what this branch touched, as a reader would, and say what is wrong.

## Goal

A writer or an agent runs one command before opening a PR and gets a short, file-and-line report of what is
broken on the pages they changed. The identical check runs on the PR, so the owner can merge a green PR without
re-reading the diff for syntax slips. Nothing in it rewrites or judges the author's prose (AGENTS.md Rule 3).

**Independent Test**: check out the About commit from #139 (`756245f`) on a throwaway branch and run
`./gradlew reviewChanges`. It fails, naming `src/jbake/content/about.md`, the line, and the broken Booz Allen
Hamilton link. Run it on a clean `main`: it passes with no findings on the pages in a typical PR. Open a PR with
that commit: the CI check is red with an annotation on that line.

## Design

### What is reviewed

The review is scoped to **the change**: every file that differs between the branch and its merge base with
`main`, committed or not (`git diff --name-only $(git merge-base main HEAD)`, plus untracked files under the
content and asset directories). CI uses the PR's base branch instead of `main`. Changed files map to pages:

| Changed file | Pages reviewed |
|---|---|
| `src/jbake/content/<dir>/<name>.md` | `build/jbake/<dir>/<name>.html`, and the source file itself |
| `src/jbake/content/blog/drafts/<name>.md` | `build/jbake/blog/drafts/<name>-draft.html` (the preview page; drafts are never published, but they are written to be) |
| `src/jbake/templates/*.ftl` | a fixed sample: home, Archive, Topics, About, the newest post, one 2009 post (the four the Definition of Done names, plus the two layouts that differ most) |
| `src/jbake/assets/img/**` | the files themselves (size and dimensions), plus every page that references them |
| anything else (`build.gradle`, `src/scss/`, `docs/`) | nothing; the review says so and exits 0 |

The review reads `build/jbake`, so the Gradle task depends on `bake`. It never changes a file.

### What is checked

Two severities. **Errors** are defects that are wrong in every context and whose fix is mechanical; they fail
the command and the CI check. **Warnings** are things a reader should look at and the author decides; they are
listed and never fail anything (`--strict` promotes them, for an agent that wants a clean slate).

Each check has an ID so a task, a report line and a suppression can name it.

**Errors (fail the review)**

| ID | Check | Where it looks | Would have caught |
|---|---|---|---|
| E1 | **Markdown link syntax.** `]` directly followed by `http`, `www.` or `/` (missing `(`); `] (` (a space before the URL); `[text]()` and `[]()`; link text with leading or trailing whitespace (`[ US Marine Corps]`); an unmatched `[` or `]` outside code spans, fenced blocks and `\[` escapes | changed `.md` files, by line | #139's Booz Allen link, the `[ US Marine Corps]` space |
| E2 | **Rendered leftovers.** Visible text (not inside `<code>`, `<pre>` or `<script>`) containing `](`, `]http`, or a `[` … `]` pair | the rendered pages, by element | #139 again, and the three `\[… 2017\]` labels Talks had until #142 |
| E3 | **Local links and assets resolve.** Every `href`, `src` and `srcset` that is same-origin or relative points at a file in `build/jbake` (or at a URL in `docs/baseline/urls.txt`, for the case-redirect stubs that only exist on Linux); `#fragment` links point at an `id` on that page | the rendered pages | a renamed image, a typo in a post's own slug, a link to a draft |
| E4 | **Front matter is complete.** `title`, `date` (ISO), `type` (`post`/`page`), `status` (`draft`/`published`), the `~~~~~~` separator, no CR line endings, and `summary` on a post that is **new in the diff** (T095 made its absence harmless on the home page, but the feed and share cards still want it; an existing post without one gets W7, because 60 of 74 published posts have none) | changed `.md` files | a post that would bake but publish half-formed |
| E5 | **Images fit the rules.** A new or changed file under `src/jbake/assets/img/` is ≤ 1920 px wide and ≤ 400 KB (AGENTS.md Rule 4), a photo is a JPEG, and a new masthead has its 960 and 1440 variants (`scripts/masthead-variants.py --check` already fails CI for this; here it fails locally, before the push) | changed image files | a 4 MB phone photo |

**Warnings (listed, never fail)**

| ID | Check | Where it looks | Notes |
|---|---|---|---|
| W1 | **External links answer.** `HEAD`, then `GET` on a method-not-allowed, 10 s timeout, a browser-like `User-Agent`; 4xx, 5xx, timeouts and TLS failures are listed with their status | links on the changed pages only | **Off by default locally**, on with `--online` / `-Ponline` (owner, 2026-10-21: local runs stay fast and work offline). **Always on in CI**, so a dead link is still caught before merge. CI's site-wide lychee stays offline because of the link count and flakiness; a changed page has a handful of links. Hosts that answer scripts with a bot challenge (SlideShare, LinkedIn, X) are listed as "not checkable" rather than as failures |
| W2 | **A changed page's `date=` did not move.** The page's only use of `date` is the sitemap `lastmod` | changed `type=page` files | Suggests today's date. Posts are exempt: their date is the publication date and must not change (Rule 3) |
| W3 | **A tag is spelled or cased unlike an existing tag** (`DevOps` next to `devops`), or is new | changed `.md` files | The same check `newPost` makes (`NewPost.warnAboutTagSpellings`); a new tag makes a new `/tags/` page and is worth a second look |
| W4 | **An `<img>` has no `alt`, or `alt` is a file name** | changed `.md` files | T109 wording is the owner's; the check only points |
| W5 | **Spelling** _(deferred by the owner 2026-10-21, see T120)_. Words that are known misspellings (`grammer`, `teh`, `recieve`), from the codespell dictionary of common mistakes, minus a project ignore list for names (`McGarr`, `Kaanapali`, `Lana'i`, `Gradle`…) | changed `.md` files, by line | Only if `codespell` is installed; otherwise one line says it was skipped and how to install it. A dictionary of *mistakes* fits a blog: it never flags a name it doesn't know, unlike a full-dictionary checker. It would not have flagged "routed" (a real word) or "Housing and Development" (a fact), and this plan does not promise to |
| W6 | **A published URL would change.** A changed file under `src/jbake/content/` or `src/jbake/assets/` that is a rename or a delete (Rule 1) | the diff | `scripts/check-urls.sh` (`./gradlew checkUrls` since T143, 2026-10-28) already fails CI for this; here it is a warning before the push, with the Rule 1 wording |
| W7 | **An existing published post has no `summary=`** | changed `type=post` files that already exist on `main` | The feed and share cards use it. Adding one is the author's call; a new post without one is E4 |

Grammar is **not** in scope. Vale with the write-good style is a one-binary option, LanguageTool is better and far
heavier, and neither would have caught the two real wording slips on About (a department's name, a missing word).
T122 records the decision and keeps the door open.

### The report

One block per finding, sorted errors first, then by file and line:

```
ERROR E1 src/jbake/content/about.md:16
  Link text "[Booz Allen Hamilton]" is followed by "https://…" with no "(": it renders as literal brackets.
  Fix: [Booz Allen Hamilton](https://www.boozallen.com/)

WARN  W2 src/jbake/content/about.md:4
  This page changed but date=2014-08-30 did not. The sitemap reports it as lastmod. Today is 2026-10-21.

Reviewed 1 changed page (about.html). 1 error, 1 warning. External links not checked (-Ponline).
```

Exit codes: `0` nothing to fix, `1` at least one error (or a warning with `--strict`), `2` usage or environment
problem (no `build/jbake`, not a git checkout). In GitHub Actions (detected from `GITHUB_ACTIONS`) each finding is
also printed as a workflow command (`::error file=…,line=…,title=E1::…`), which GitHub shows inline on the PR's
Files tab, and the whole report is appended to `$GITHUB_STEP_SUMMARY`.

### Where the code lives

**`scripts/ReviewChanges.java`**, a single-file Java 21 program like `scripts/NewPost.java` (T060): the owner's
language, no dependencies beyond the JDK that the build already needs, runnable as `java scripts/ReviewChanges.java`
and from Gradle. Everything an error check needs is in the JDK: `git` via `ProcessBuilder`, HTML parsing with a
small tag scanner (the pages are our own templates' output, not arbitrary HTML), `ImageIO` for image dimensions,
`java.net.http.HttpClient` for W1. The only external tool is codespell for W5, and it is optional.
_2026-10-28: the program is now the plugin's `review` package in `build-logic/` (`Review`, one class per group of
checks, `ReviewChangesTask`), run as `./gradlew reviewChanges`; there is no `java scripts/…` form any more (T127)._

Options considered for the checks and rejected or deferred:

- **lychee for E3/W1.** Already in CI, good, but a Homebrew or cargo install locally, and it cannot see E1/E2 at
  all. The link checks are a few dozen lines in Java and then the local command has nothing to install.
- **markdownlint.** Style rules (heading levels, list markers) that the 63 WordPress-era posts break everywhere,
  needs Node, and has no rule for E1's exact shape. Noise, not signal, for this site.
- **cspell / aspell / hunspell for W5.** Full-dictionary checkers flag every name, product and term the author uses
  until the ignore list is longer than the posts. codespell's mistake dictionary flags only misspellings.
- **A git `pre-push` hook.** Per-clone, not committed, silent when missing. The Gradle task is in the repo and the
  CI check is the real gate; `openPr` (T121) gives the hook's "stop before the push" without the hook.

### The three entry points

1. **`./gradlew reviewChanges`** (group `writing`, next to `newPost` and `bakePreview`): `dependsOn bake`, runs the
   script against `main`. Options as Gradle properties, like `newPost`: `-Pbase=<ref>`, `-Ponline`, `-Pstrict`,
   `-Pall` (review every page, not just changed ones; for a baseline run and for template work).
2. **CI**, a step after "Check local links and assets" in the `build` job, on `pull_request` only, with
   `fetch-depth: 0` so the merge base exists: `java scripts/ReviewChanges.java --base origin/$GITHUB_BASE_REF`.
   _2026-10-28: the step runs `./gradlew reviewChanges -Pbase="origin/$GITHUB_BASE_REF"` since T127._
   Errors fail the job; the deploy job already needs `build` to pass, so on a push to `main` nothing changes (the
   step is skipped there: a push has no base to diff against, and the PR already ran it).
3. **`./gradlew openPr`**: run `reviewChanges`, and only if it passes push the branch and open a **ready** (not
   draft) PR with `gh pr create` (title from `-Ptitle`, or the branch's first commit subject; body from a template
   that includes the review's summary line and the `Closes #N` lines found in the commits). If the review fails,
   nothing is pushed and the report says what to fix. **Uncommitted work** (owner's decision 2026-10-21): the
   command first prints the list of changed and untracked files it would commit (`git status --short`, limited to
   `src/`, `docs/`, `scripts/`, `README.md`, `AGENTS.md`, `build.gradle` and `.github/`). Without `-Pmessage` it
   stops there, so the author sees the list before anything is committed, and says to re-run with
   `-Pmessage="…"`. With `-Pmessage` it commits exactly the listed files with that message and goes on. A file
   outside those paths is listed as "not included" and left alone. Reuses the readiness checks and the
   stop-and-explain pattern of `NewPost.checkReadyToOpenPr` and `commitPushAndOpenPr`, as a second single-file
   program `scripts/OpenPr.java`. It refuses to run on `main` (Rule 2).

### Suppressions

A finding that is right for the site but wrong for one line gets an HTML comment on the line before it in the
Markdown: `<!-- review: ignore E2, the brackets are intended -->`. The report counts suppressions so they stay
visible. There is no global off switch for an error check; a check that is wrong in general gets fixed or removed.

## Consequences

- A PR with a rendering defect in changed prose is red before the owner reads it. The owner's review time goes to
  the words, where only the owner can judge.
- Local and CI runs give the same answer from the same code, so "it passed for me" means it will pass there.
- New dependencies: none. Spelling (codespell) is deferred (T120). The CI job gains one step and `fetch-depth: 0`
  (the repo is small).
- The review inspects **only changed pages** by design. Defects already live (Talks' three labels) surface when
  that page is next touched, or on a `-Pall` run. The first `-Pall` run is a checkpoint below and its findings go
  to the Author's Desk or to a fix PR.
- A single-file Java program grows. `NewPost.java` is 370 lines; this one will be larger. If `OpenPr.java` ends up
  copying more than the small `run`/`git`/`fail` helpers, the three programs move to `scripts/src/` and a tiny
  `sourceSets` block, which the configuration cache allows. Not before.
- The author's prose is never edited and never judged beyond "this word is a known misspelling". Rule 3 holds.

## Tasks

Task format and ticking rules: [`AGENTS.md`](../AGENTS.md), "Tasks and checklists". IDs continue the revival
plan's numbering (T116 and CHK042 were the last used).

### M9: Review Flow

**Goal**: One command reviews the pages a change touched, the same check gates every PR, and a PR can be opened
only after it passes.

**Independent Test**: as in [Goal](#goal) above: `756245f` fails locally and in CI on the Booz Allen link with the
right file and line; a clean `main` passes.

**Order**: T117 first; T118 and T119 can follow in parallel; T121 last, since it depends on the review's exit
codes. T120 and T122 are decisions, any time.

_2026-10-27 (owner): T119 is **won't do**; T121 **will be built**, after T127 in [`06-GRADLE-PLUGIN.md`](06-GRADLE-PLUGIN.md)
so it is born as a plugin script task; T120 and T122 are **optional improvements** for later, not part of M9's done
criteria. M9 is done when T121 and CHK045 are; CHK043, CHK044 and CHK046 passed on 2026-10-27._

#### Improvements

- [x] T117 [IMP] **The review script and Gradle task.** `scripts/ReviewChanges.java` with the change scoping,
  checks E1–E5 and W2–W4, W6, W7 (everything that needs no network and no external tool), the report format, exit
  codes and suppressions above; `./gradlew reviewChanges` (`dependsOn bake`) with `-Pbase`, `-Pstrict`, `-Pall`;
  a "To review a change" section in the README and `reviewChanges` added to the Definition of Done in
  `AGENTS.md` (`scripts/ReviewChanges.java`, `build.gradle`, `README.md`, `AGENTS.md`)
  - **Test:** on a throwaway branch, `git cherry-pick 756245f` (the #139 About change) then `./gradlew reviewChanges`
    exits 1 and reports E1 on `about.md` with the Booz Allen line and E2 on `about.html`, plus W2 for the unmoved
    date. On the same branch, seed `<img src="img/does-not-exist.jpg">` into a post and a `tags=Devops` line: E3
    and W3 are reported. On a clean `main` with no changes, it prints "nothing changed" and exits 0.
    Editing an old post without `summary=` reports W7, not E4; a new draft without one reports E4.
    `./gradlew reviewChanges -Pall` on `main` lists nothing under E1–E5 (the Talks labels went in #142); anything
    it does list is either a real finding, recorded in the task note, or a false positive fixed before ticking.
    `./gradlew clean bake` output is unchanged.
  - _Done 2026-10-22 (`feat/T117-review-changes`). `scripts/ReviewChanges.java` (single-file Java, like `NewPost.java`) and
    `./gradlew reviewChanges` (`dependsOn bake`; `-Pbase`, `-Pall`, `-Pstrict`; `-Ponline` accepted and ignored until T119).
    **Test passed**, with two differences from the text above: the About line is 14, not 16, and W2 was proved with a
    prose-only edit to Speaker Bio, because `main`'s About already has today's date since #140, so the cherry-picked
    header no longer leaves the date unmoved. The #139 About file (`git show 756245f:…/about.md`) gives E1 on line 14
    ("[Booz Allen Hamilton]" followed by the URL, fix shown), E1 for the padded "[ US Marine Corps]", and E2 mapped
    back to the same source line; a seeded `<img src="img/does-not-exist.jpg">` and `tags=…, Devops` in a post give E3
    (resolved from `blog/`) and W3 ("already used as [devops]") plus W4; an edit to `alm.md` gives W7; a new draft with
    `summary=` empty gives E4, and `[world] (url)` / `[gone]()` in it give E1 (and E3 for the empty href); a masthead
    copy without variants and a 2000 px PNG give E5; `git rm` of a post and `git mv` of an image give W6. A suppression
    comment is honoured and counted. A clean tree prints "Nothing to review" and exits 0; errors exit 1, and the Gradle
    task turns that into a one-line failure; `--strict` exits 1 on warnings. **`-Pall` on `main`: 0 errors, 8 warnings**
    on all 89 pages: the 8 are W4, the eight `alt="<file name>"` images the content report lists for T109 (the owner's
    wording; recorded there already, so CHK044 needs nothing more for them). Three false positives were fixed during the run: raw `<pre>` blocks in old posts are skipped
    by E1 (`maven-cobertura-and-aspectj`), brackets the author escaped as `\[…\]` are not E2 (`test-method-names`), and
    a draft's own canonical link to its future URL is not E3. Also learned: E2 and E3 skip the archived Disqus comments
    (readers' text); a renamed or deleted image pulls in the pages that still reference its old name; and because a
    bake never deletes, E3 also requires an asset's source file to exist, or a stale copy in `build/jbake` hides the break._
- [x] T118 [IMP] **The CI check.** A step in `.github/workflows/` on `pull_request` that runs the script against
  the PR's base with annotations and a step summary; `fetch-depth: 0` on the checkout (`.github/workflows/`)
  - **Test:** a test PR with `756245f` cherry-picked shows a red "Review changed pages" step, an inline annotation
    on the `about.md` line in the Files tab, and the report in the job summary. Pushing the fix from #140 to that
    PR turns it green. A push to `main` runs no review step. The test PR is closed, not merged.
  - _2026-10-22: done in #146. The step runs `java scripts/ReviewChanges.java --base="origin/$GITHUB_BASE_REF"` after
    the lychee step, `if: github.event_name == 'pull_request'`, on a `fetch-depth: 0` checkout. Test PR #147 (based
    on #146's branch, About as #139 shipped it): run 39879798105 red, "Review changed pages" failed with three
    annotations on `src/jbake/content/about.md:14` (E1 Booz Allen, E1 `[ US Marine Corps]`, E2) and the report in
    the job summary; after pushing the fixed page, run 39879950034 green ("Nothing to review", the PR's net diff
    is empty). #147 closed, branch deleted. #146's own run (no content change) passed the step the same way. The
    "push to `main` runs no step" clause follows from the `if:` and is checked on the run that #146's merge
    triggers; it could not run before the merge._
- [x] T119 **Won't do (owner decision, 2026-10-27).** [P] [IMP] **External links on changed pages (W1).** `HttpClient` with the limits above, off by default
  and on with `--online` / `-Ponline`, the "not checkable" host list, and CI always running online
  (`scripts/ReviewChanges.java`, `.github/workflows/`)
  - **Test:** a throwaway change adding `[gone](https://www.mikemcgarr.com/no-such-page.html)` to a post reports
    W1 with `404` under `-Ponline` and still exits 0; without `-Ponline` the report has one line saying external
    links were not checked and how to turn them on. A SlideShare link in a changed
    post is listed as "not checkable", not as a failure. The whole W1 pass on a 30-link page takes under 60 s.
  - _2026-10-27: **won't do** (owner). CI already checks every outbound link with lychee (T050), so a second, local
    W1 pass was not worth its own implementation. `-Ponline` stays accepted and ignored, and the report's
    "External links not checked (T119)" line stays as the pointer to this decision._
- [ ] T120 [IMP] _(Optional, later)_ **Spelling (W5).** The design: run codespell on the changed `.md` files when
  it is installed, with a `.codespellrc` carrying the ignore list seeded from the names and terms in the current
  posts; CI installs it; the local report says how to install it when it is missing. **Deferred by the owner on
  2026-10-21**: not now; revisit once T117–T119 have run on a few PRs. If taken up, the test is: a throwaway
  change with "grammer" in a post reports W5 with the suggestion "grammar" and exits 0; About, Talks and the 2026
  posts report no W5 hits on `-Pall`; with codespell uninstalled, the report has one "skipped" line and the exit
  code is unchanged (`scripts/ReviewChanges.java`, `.codespellrc`, `.github/workflows/`, `README.md`)
  - **Test:** a dated note under this task records the decision (take it up, or not) and why.
  - _2026-10-27: made an **optional improvement** by the owner: not part of M9's done criteria, no decision due. If
    taken up later, the design and test above stand._
- [x] T121 [IMP] **`./gradlew openPr`.** `scripts/OpenPr.java` as designed above: refuse on `main`; with
  uncommitted work, list the files and stop unless `-Pmessage` is given, then commit exactly those files; run the
  review; stop before the push on errors; else push and `gh pr create` (ready, not draft) with the title, body
  template, `Closes` lines and the review summary (`scripts/OpenPr.java`, `build.gradle`, `README.md`)
  - **Test:** on a branch with `756245f` cherry-picked, `./gradlew openPr` exits 1, prints the E1 finding, and
    `git status -sb` shows the branch has no upstream (nothing pushed). With an uncommitted edit to a post and no
    `-Pmessage`, it prints that file under "would commit" and stops without committing; with
    `-Pmessage="Edit"`, `git log -1 --stat` shows a commit with only that file. After fixing the link,
    `./gradlew openPr -Ptitle="Test PR"` pushes and opens a PR whose body contains "Reviewed 1 changed page",
    whose base is `main`, and for which `gh pr view --json isDraft` returns `false`. On `main`, it refuses with a
    one-line message naming Rule 2. The test PR is closed.
  - _2026-10-27: **confirmed by the owner, to be built.** Scheduled after T127 in
    [`06-GRADLE-PLUGIN.md`](06-GRADLE-PLUGIN.md) (the plugin's `ScriptTask`), so `openPr` is written once, as a
    plugin script task, instead of as a `build.gradle` Exec that T127 would then move. CHK045 waits with it._
  - _2026-10-28: **done** (`chore/T141-T143-T121-checks-openpr`, PR 6 of M10's order in `06-GRADLE-PLUGIN.md`). Born as a
    plugin class, not `scripts/OpenPr.java`: package `writing`, `OpenPr` (the flow), `OpenPrOptions` (`-Ptitle`,
    `-Pmessage`, `-Pcloses`, `-Prefs`, `-PdryRun`) and `OpenPrTask` (`openPr`, group `writing`, `dependsOn bake`),
    following `NewPost`'s structure; `GitHubCli` holds the gh readiness checks and the stop-and-explain helpers that
    `NewPost.checkReadyToOpenPr` and `commitPushAndOpenPr` had, now shared (`NewPost`'s messages are unchanged). The
    review runs in-process through the `review` package's new public entry point, `ReviewRunner` (with
    `ReviewOutcome`: passed, and the report's summary line), which `reviewChanges` now goes through too; nothing else
    of the package became public. **The flow, as designed:** refuses on `main` and `master` with one line naming Rule
    2 (and refuses a detached HEAD); lists the uncommitted work as `git status --porcelain` prints it under "Would
    commit:" (files under `src/`, `docs/`, `scripts/`, `.github/`, `README.md`, `AGENTS.md`, `build.gradle`, and, added
    for M10, `build-logic/`, `settings.gradle`, `CLAUDE.md`), the rest under "Not included (… left as they are)", and
    stops there without `-Pmessage`, before gh is even asked; with `-Pmessage` it runs the readiness checks (gh
    installed and logged in, origin, the repository, the committer, `main` pushed), then `git add -- <the listed
    paths>` (never `-A`; both sides of a rename) and `git commit --quiet -m <message>`, plus a second `-m` with the
    lines of the new `jbakeSite.commitTrailers` (default none: the tool is the owner's, so no attribution is built in;
    a session that wants `Co-Authored-By:` lines sets them in `build.gradle`); runs the review against `main`, the
    same scope as `reviewChanges`, and stops before the push on errors; pushes with `--set-upstream origin <branch>`
    and runs `gh pr create --repo <owner/name> --base main --head <branch> --title … --body-file …` (ready, not
    draft), printing the URL. **Title:** `-Ptitle`, else the subject of the branch's first commit after the merge
    base with `main` (for a one-commit branch, that commit's). **Description** (the template, fixed here since the
    design left it open): "## What changed" with a bullet per commit subject; "## Review" with the summary line
    ("Reviewed 1 page (about.html) from 1 changed file. 0 errors, 1 warning. …", or the clean-tree sentence) and a
    line saying `openPr` ran `reviewChanges` and CI runs it again; then one `Closes #N`/`Refs #N` line per issue from
    `-Pcloses`, `-Prefs` and every such line in the commit messages, each once (Rule 5). A failed push or `gh pr
    create` says what is in place and the commands that finish, as `newPost -Ppr` does. `-PdryRun` does everything
    but commit, push and open, and prints the pull request it would open. **Tests:** `OpenPrTest` with a scripted git
    and gh for every branch of the flow (main and master; no checkout, detached, no main; the listing and the stop
    without a message, before any gh call; with a message, `git add` of exactly the listed files and the commit with
    and without trailers, after the readiness checks; review errors stop before the push; the push and `gh pr create`
    with their arguments and order; the title; nothing to open; gh missing, logged out, no origin, unpushed main;
    failed push and failed PR; the dry run; the issue lines; the description; the committed paths), one case on a real
    working tree, `ReviewRunnerTest`, the plugin wiring, and TestKit on the fixture: `openPr` on `main` refuses
    naming Rule 2, and on a branch with an edited page and a scratch file prints the two lists and commits nothing.
    **Test passed** on a throwaway branch of this repository, 2026-10-28 (the results are in the pull request's
    description and in the agent's report; the test PR was closed and its branch deleted): see the note below once
    the real run is recorded._
- [ ] T122 [IMP] _(Optional, later)_ **Grammar checking: Vale, LanguageTool, or neither.** Try Vale with the write-good
  and proselint styles on the 2026 posts and About; count true findings against noise. Record the decision here.
  If adopted, it becomes W7 in a later task, advisory only (`docs/05-REVIEW-FLOW.md`)
  - **Test:** a dated note under this task records the tool tried, the finding and noise counts on at least three
    pages, and the decision. If the decision is "neither", the note says what would change it.
  - _2026-10-27: made an **optional improvement** by the owner, like T120: not part of M9's done criteria, no
    experiment due now._

#### Checkpoint: M9

- [x] CHK043 The #139 defect is caught: `756245f` on a throwaway branch fails `./gradlew reviewChanges` locally
  and the CI check on a test PR, each naming `about.md` and the line (T117, T118)
  _(passed: T117's local test on 2026-10-22 and T118's test PR #147 the same day, both naming `about.md:14`; ticked 2026-10-27)_
- [x] CHK044 `./gradlew reviewChanges -Pall` on `main` has been run once, and every finding is either fixed, recorded
  in [`archive/04-AUTHORS-DESK.md`](archive/04-AUTHORS-DESK.md) as the owner's, or suppressed with a reason on the line
  _(passed 2026-10-27: `./gradlew reviewChanges -Pall` on `main` at 7b2aff9 reviewed 78 pages, 0 errors, 0 warnings;
  the 8 W4 alt-text warnings of the first run on 2026-10-22 were resolved under T109)_
- [ ] CHK045 README's "To review a change" and AGENTS.md's Definition of Done describe the command, and a PR opened
  with `./gradlew openPr` has merged _(waits for T121)_
- [x] CHK046 `./gradlew clean bake` output is byte-identical to before M9 apart from `feed.xml`'s timestamps (the
  review reads the site and never changes it)
  _(passed: T117's test on 2026-10-22 compared the bake before and after the task; on 2026-10-27, at 7b2aff9, the
  496 files of a clean bake (all but `feed.xml`) had identical SHA-256 sums before and after `reviewChanges -Pall`)_

### GitHub Issues

No open issue maps to this plan. The one open issue, [#14](https://github.com/jmcgarr/jmcgarr.github.io/issues/14)
(mastheads for older posts), is T059 in [`archive/04-AUTHORS-DESK.md`](archive/04-AUTHORS-DESK.md). If the owner files an issue
for the review flow, add its row here with the task and the `Closes #N` line, per AGENTS.md Rule 5.

| Issue | Title | GitHub label | Task(s) | Milestone | Put in the resolving PR |
|---|---|---|---|---|---|
| _none yet_ | | | | | |

## Notes

- _2026-10-21: written after #139/#140 (the About link). The owner's framing: "a feature to the build or release
  process that links the pages changed, checking for broken links and images, spelling and grammar and other
  issues. It should work locally too. Maybe a companion to `newPost` that will `reviewChanges`, or add it to a
  `createPR` command, stopping it before it is pushed." That is T117, T118 and T121; grammar is T122._
- _2026-10-21 (later): **the owner's decisions**, after skimming the plan. (1) The script is single-file Java, like
  `NewPost.java`. (2) The CI step blocks from day one. (3) External links are off by default locally and on with
  `-Ponline`; CI always checks them. (4) A missing `summary=` is E4 only for a post new in the diff and W7 for an
  existing one (60 of 74 published posts have none). (5) `openPr` lists uncommitted files first, commits them only
  with `-Pmessage`, and opens a ready PR. (6) Spelling is deferred (T120 is a decision now). (7) `02-ROQ-MIGRATION.md`
  and `03-REVIVAL-CHANGELOG.md` stay at the top level of `docs/` (on 2026-10-23 the owner decided to stay on JBake and moved
  `02-ROQ-MIGRATION.md` to `docs/archive/`, the renamed `completed/`; `03` stays). (8) The three Talks labels became plain text (#142)
  rather than waiting for T048, so the first `-Pall` run has a clean E2._
- _2026-10-25: `03-REVIVAL-CHANGELOG.md` moved to `docs/archive/` at the owner's request, now that its Status is Done
  (the "`03` stays" in item (7) above was the position on 2026-10-23). Its relative links were adjusted for the new
  location; the number stays 03._
- _2026-10-27: `04-AUTHORS-DESK.md` moved to `docs/archive/` after the owner reviewed it and considered it closed
  (Status: Done). Its relative links were adjusted; the number stays 04. T059 (#14, CHK039) and T111 were still
  unticked and stay on record there; the owner decides whether they happen._
- _2026-10-27: the owner settled the open M9 items. T119 (external links) is won't do: lychee in CI already covers
  outbound links. T121 (`openPr`) will be built, but after T127 in `06-GRADLE-PLUGIN.md`, so it is written once as a
  plugin script task. T120 (spelling) and T122 (grammar) become optional improvements outside M9's done criteria.
  CHK043, CHK044 and CHK046 were verified and ticked the same day. The agreed order of work: close out this plan
  (done with this note), then Phase A of `08-SCRIPTS-MIGRATION.md` (no Python), then M10 of `06-GRADLE-PLUGIN.md`
  with T121 and Phase B inside it._
