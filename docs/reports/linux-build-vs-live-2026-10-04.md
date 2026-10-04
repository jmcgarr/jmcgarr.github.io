# Linux CI Build vs. Live Site

**Branch**: `fix/T008-publish-only-on-push` | **Created**: 2026-10-04 | **Status**: Done

**Question:** before merging PR [#18](https://github.com/jmcgarr/jmcgarr.github.io/pull/18) into `source`
(which publishes), would a publish from the Linux CI build change the live site in any unexpected way?

**Answer:** No. The CI build matches the live site except for the RSS feed's build timestamps.

## Method

Throwaway draft PR [#19](https://github.com/jmcgarr/jmcgarr.github.io/pull/19) (closed and its branch deleted) ran
the PR #18 stack in CI with the publish step replaced by a dry-run `echo`. A final step extracted the live
`master` (`dd49867`, = tag `live-2026-10`) with `git archive` and compared it with `build/jbake` using
`diff -rq`, then ran `scripts/check-urls.sh` (strict on Linux's case-sensitive filesystem).

- Control run: [37226118033](https://github.com/jmcgarr/jmcgarr.github.io/actions/runs/37226118033) (Temurin 11.0.32, ubuntu-latest)

## Results

| Check | Result |
|---|---|
| Files | live 1,824 · build 1,823 |
| Only on live (a publish would delete) | `.gitignore`, a repo file added to `master` by hand in 2014. It survived every publish since, including 2024-05-27 with the same git-publish setup, so it isn't actually removed. |
| Only in build (a publish would add) | none |
| Changed | `feed.xml` only: `<pubDate>` and `<lastBuildDate>` (build time) |
| `check-urls.sh` | `OK: all 272 baseline URLs accounted for`, including `tags/DevOps.html` and `tags/Groovy.html`, which macOS bakes can't produce (T055) |

Content diff, in full:

```diff
9,10c9,10
<     <pubDate>Mon, 27 May 2024 15:59:24 +0000</pubDate>
<     <lastBuildDate>Mon, 27 May 2024 15:59:24 +0000</lastBuildDate>
---
>     <pubDate>Sun, 4 Oct 2026 18:53:21 +0000</pubDate>
>     <lastBuildDate>Sun, 4 Oct 2026 18:53:21 +0000</lastBuildDate>
```

## Conclusion

Merging PR #18 publishes a site identical to today's apart from the feed timestamps, so RSS readers won't
see any new or changed items. To confirm after merging, check that the push run's "Publish content" step
succeeded, then `git fetch origin master && git diff --stat live-2026-10 origin/master`. It should list
only `feed.xml`.
