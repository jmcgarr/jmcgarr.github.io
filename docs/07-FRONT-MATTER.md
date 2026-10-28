# Front matter for VS Code authoring

**Branch**: `docs/07-front-matter-options` | **Created**: 2026-10-25 | **Status**: Draft

A decision record on the header at the top of every post and page. The owner has started writing posts in
VS Code, and JBake's header format breaks the Markdown preview there. This doc records the problem, what JBake 2.7
allows, and three ways out. **No option is chosen yet.** Once one is, its steps become tasks in
[`05-REVIEW-FLOW.md`](05-REVIEW-FLOW.md); this doc then moves to `Accepted`.

## Context

### The problem

Every content file under `src/jbake/content/` starts with JBake's header: `key=value` lines ending with a separator
line, `~~~~~~`. From `src/jbake/content/blog/a-new-adventure-begins.md`:

```
title=A new adventure begins
date=2012-12-02
type=post
tags=blackboard
status=published
summary=November 5th was my first day at Blackboard, Inc. ...
~~~~~~
```

The separator is JBake's `header.separator` setting. `src/jbake/jbake.properties` doesn't set it, so JBake's
default, `~~~~~~`, applies.

`~~~~~~` is a tilde code fence in CommonMark. So VS Code's Markdown preview, and GitHub's file view of the source,
treat everything after it as one code block: the post body never renders. markdownlint reports the header too
(MD041, first line not a heading; MD040, fence without a language; and so on). The `key=value` lines themselves are
harmless to Markdown tools; they read as a short paragraph. The fence is the whole problem.

### What JBake 2.7 allows

Verified 2026-10-25 by reading jbake-core 2.7.0's parser classes (`org.jbake.parser`):

- `MarkupEngine` compares each trimmed line with the separator string using `equals`. The string is configurable
  (`header.separator`), so it can be anything that fits on one line.
- `headerSeparatorDemarcatesHeader` finds the first line equal to the separator and requires every line before it to
  contain `=` or be empty. Otherwise the file has no header and is skipped ("Parsing skipped (missing type or status
  value in header meta data)"). **A file cannot start with a `---` line**: real YAML front matter (a `---` block of
  `key: value` lines) cannot be fed to stock JBake.
- Header lines are split on the first `=`; the body is every line after the first separator line.
- `getFileContent`, `hasHeader`, `processDefaultHeader`, `processDefaultBody` and `processHeaderLine` are private. A
  subclass cannot change how the file is read; it has to override `parse(JBakeConfiguration, File)` and reproduce
  that flow, or parse a translated temporary copy of the file.
- Engines are registered per file extension in `Engines`, loaded from
  `META-INF/org.jbake.parser.MarkupEngines.properties` on the classpath. `Engines.register(String extension,
  ParserEngine)` is public static, so a small launcher can register its own engine for `md` and `markdown` and then
  call JBake's own `org.jbake.launcher.Main`. The site already runs JBake that way: `build.gradle` runs
  `org.jbake.launcher.Main` as a `JavaExec` in the `bake` and `preview` tasks (T096), and `scripts/` already holds
  single-file Java programs (`NewPost.java`, `ReviewChanges.java`) that a custom engine could sit beside.
- `YamlEngine` exists, but it is for `.yaml` content files, not for front matter.

### What else knows the separator

Any format change touches all of these:

| Where | What it does with the header |
|---|---|
| `scripts/NewPost.java` | Writes the header of a new draft (the `"~~~~~~"` line) and stops reading tags at it |
| `scripts/ReviewChanges.java` | `SEPARATOR = "~~~~~~"`; the E4 check reads the front matter and reports a missing separator or key |
| `scripts/PrepareImage.java` | Finds mastheads with the regex `^masthead=(.+)$` and rewrites `^masthead=<old>$` to the new image name (the Python `masthead-variants.py` and `prepare-image.py` until T140, 2026-10-27) |

_2026-10-28: the three programs are plugin classes in `build-logic/` since T127: `writing/NewPost`,
`review/MarkdownChecks` (with the header parser in `internal/FrontMatter`, `SEPARATOR`) and `writing/PrepareImage`
with `writing/MastheadVariants`, run as `./gradlew newPost`, `reviewChanges` and `prepareImage`._
| `README.md`, `AGENTS.md` | Describe the format; AGENTS.md Rule 3: "Front matter is `key=value` lines ending with a `~~~~~~` separator" |
| `src/jbake/content/` | 78 files carry the header: 75 published posts, 2 pages (`about.md`, `talks.md`), 1 draft |

A format change never changes a URL: the output path comes from the file name, not the header, so Rule 1 is
unaffected. But it must be proven the way the T102 Markdown conversion was proven
([`archive/00-REVIVAL.md`](archive/00-REVIVAL.md), T102): bake before and after, and show the HTML identical.

```sh
./gradlew clean bake && cp -R build/jbake /tmp/bake-before
# ...change the format, then...
./gradlew clean bake && diff -r /tmp/bake-before build/jbake
```

The `diff` must be empty, apart from the timestamps in `feed.xml`.

## Options

| | 1. HTML-comment separator | 2. Configure VS Code only | 3. YAML front matter, custom engine |
|---|---|---|---|
| Effort | One property, two strings in `scripts/`, one `sed` over 78 files, a lint config | One lint config file | About 80 lines of Java, a conversion script, five scripts and docs updated, 78 files converted |
| VS Code gains | Preview renders the body; markdownlint quiet (with config); header shows as a paragraph | markdownlint quiet; preview still broken | Preview, markdownlint, Markdown All in One, and GitHub's file view all understand it natively |
| Risk | Low: no new code; a one-line change in every file | None | Medium: custom code copies JBake's parse flow and a future jbake-core bump could break it |
| Reversible | Yes: `sed` back and reset the property | Yes: delete the file | Yes, by script, but nobody would |
| Proof | Before/after bake identical | markdownlint output | Prototype on a scratch bake first, then before/after bake identical |

### Option 1: change the separator to an HTML comment

Set `header.separator=<!-- end of header -->` (any one-line comment works) and change every file's `~~~~~~` line to
match. Markdown hides HTML comments, so the preview shows the header as one short paragraph of `title=...` lines
at the top, and the body renders correctly below it. No new code, no URL change. The wart is that paragraph.

Steps:

1. Add `header.separator=<!-- end of header -->` to `src/jbake/jbake.properties`, with a comment saying why.
2. Replace the separator line in every content file:
   ```sh
   grep -rl '^~~~~~~$' src/jbake/content | xargs sed -i '' 's/^~~~~~~$/<!-- end of header -->/'
   ```
3. Put the same string in `scripts/NewPost.java` (the written header and the `startsWith` check) and
   `scripts/ReviewChanges.java` (`SEPARATOR`). _2026-10-28: now `internal/FrontMatter.SEPARATOR` in `build-logic/`,
   which `NewPost` and the review share (T127)._
4. Add `.markdownlint-cli2.jsonc` at the repo root that either disables MD041 or sets a `frontMatter` regex matching
   from the first line through the comment line, so the linter skips the header.
5. Update README and AGENTS.md Rule 3 to name the new separator.

Verification: the before/after bake `diff -r` above is empty; `./gradlew check` passes; the VS Code preview of a
post shows the body rendered with only the header paragraph above it.

### Option 2: keep the format and configure VS Code only

Nothing in the site changes. A `.markdownlint-cli2.jsonc` at the repo root with a `frontMatter` regex that matches
from the first line through the `~~~~~~` line makes markdownlint ignore the header:

```jsonc
{
  "frontMatter": "^(?:.*\\n)*?~~~~~~\\n"
}
```

The preview cannot be fixed this way: the tilde fence is CommonMark itself, not a lint rule. Half a solution, zero
risk, fully reversible. It is also a sensible first step before option 1 or 3, and it stays useful after either.

Steps:

1. Add `.markdownlint-cli2.jsonc` with the `frontMatter` regex.
2. Open a post in VS Code and confirm the header warnings are gone.

Verification: markdownlint shows no header warnings in VS Code on a post. The preview is unchanged (still a code
block), which is expected.

### Option 3: adopt YAML front matter with a custom parser engine

Posts start with a YAML block:

```yaml
---
title: A new adventure begins
date: 2012-12-02
type: post
tags: [blackboard]
status: published
summary: November 5th was my first day at Blackboard, Inc. ...
---
```

Stock JBake rejects a file that starts with `---` (see "What JBake 2.7 allows"), so a custom engine does the
translation. A single Java file, say `scripts/jbake/FrontMatterMarkdownEngine.java` (engine and launcher in one
file, or two files), registers an engine for `md` and `markdown` through `Engines.register`. The engine translates
a leading `---` block into JBake's `key=value` + separator form, then delegates to JBake's Markdown engine. It must
get around the private methods either by overriding `parse(JBakeConfiguration, File)` and reproducing that flow, or
by writing a translated temporary copy and parsing that. The `bake` and `preview` tasks in `build.gradle` run the
launcher instead of `org.jbake.launcher.Main`; the launcher registers the engine and then calls `Main` itself.

Benefits: VS Code's preview, markdownlint, Markdown All in One, GitHub's file view (which renders front matter as a
table) and every other static-site tool understand the format natively. The site's content stops depending on a
JBake-only convention.

Costs and risks: roughly 80 lines of Java that copy JBake's parse flow. A future jbake-core bump (Dependabot
proposes them) could change `MarkupEngine` and break the engine, so the engine needs a test and the bump needs the
before/after bake. The conversion is a one-time script over 78 files, the same shape as T102.

Steps:

1. Prototype: write the engine and launcher, convert one post by hand on a scratch branch, bake, and confirm the
   rendered page is identical to the `main` bake. Stop here if the private-method problem has no clean answer.
2. Write the conversion script (`scripts/to-front-matter/convert.py`, or similar) that turns every `key=value`
   header into a YAML block. Decide how `tags=a, b` becomes YAML (`tags: [a, b]` or `tags: a, b`) and how the
   engine reads it back into JBake's comma-separated `tags`.
3. Convert all 78 files; prove it with the before/after bake `diff -r` (empty) and the URL list unchanged.
4. Switch `bake` and `preview` in `build.gradle` to the launcher.
5. Update `scripts/NewPost.java` (write YAML), `scripts/ReviewChanges.java` (E4 reads YAML), the `masthead=` regexes
   in `scripts/PrepareImage.java` (now `masthead:`), README and AGENTS.md Rule 3. _2026-10-28: in `build-logic/`
   since T127: `internal/FrontMatter`, `writing/NewPost`, `review/MarkdownChecks`, `writing/PrepareImage` and
   `writing/MastheadVariants`._
6. Add `.markdownlint-cli2.jsonc` if markdownlint still needs a `frontMatter` setting (its default already matches a
   `---` block).

Verification: the prototype page is identical to its `main` bake; after conversion, `diff -r` of the before/after
bake is empty and `./gradlew check` passes; the VS Code preview of a post renders the body with no header artifacts;
`./gradlew newPost -Ptitle="Test"` writes a YAML header and `./gradlew reviewChanges` accepts it.

## Decision

**Open.** The owner decides between:

- **Option 1** for quick relief: a one-line change per file, no new code, and a visible header paragraph in the
  preview.
- **Option 3** if the owner will author in VS Code for the long run: the format every Markdown tool expects, at the
  cost of a small custom parser engine to maintain across jbake-core upgrades. This is the recommended option in
  that case, and a working prototype comes before the conversion.
- **Option 2** on its own, or as a first step before either of the others.

When an option is chosen: record the choice and date here, set the Status to `Accepted`, and add the steps as
tasks (with `Test:` lines) to [`05-REVIEW-FLOW.md`](05-REVIEW-FLOW.md).

## Consequences

Whichever option is chosen:

- No published URL changes (Rule 1 is unaffected), but the change touches every content file, so it goes on its
  own branch and ships with the before/after bake proof, like T102.
- `scripts/NewPost.java`, `scripts/ReviewChanges.java` and AGENTS.md Rule 3 change in the same PR as the content,
  so a draft written after the merge has the right header and the review flow checks it. _2026-10-28: those are
  the plugin's `internal/FrontMatter`, `writing/NewPost` and `review/` classes in `build-logic/` since T127._
- Option 3 adds a maintained piece of Java to the build. Every jbake-core bump then needs the before/after bake
  check, and the engine belongs in the future build plugin ([`06-GRADLE-PLUGIN.md`](06-GRADLE-PLUGIN.md)) when that
  work starts.
- Option 2 alone leaves the preview broken; the owner keeps using another preview (the `./gradlew bakePreview`
  site at http://localhost:8080) to see a post rendered.

## Notes

- _2026-10-25: written after the owner moved to VS Code for authoring and found the preview showing the whole post
  as a code block. The JBake constraints above were checked against jbake-core 2.7.0's source the same day._
