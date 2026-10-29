---
name: cleanup
description: >-
  After a pull request merges: bring main up to date, delete the local branches that are
  merged, prune stale remote refs and worktrees, and report what is left (open PRs, branches
  kept and why). Use when the user says a PR "is merged", "clean up", "update the local
  environment", or runs /cleanup. Never deletes unmerged work, never resets main, and does
  not create branches, commits or PRs.
argument-hint: "[branch]"
allowed-tools: >-
  Bash(git status:*), Bash(git switch:*), Bash(git pull:*), Bash(git fetch:*),
  Bash(git branch:*), Bash(git log:*), Bash(git worktree:*), Bash(git rev-parse:*),
  Bash(gh pr list:*), Bash(gh pr view:*)
---

# Clean up after a merge

This repo's rule (AGENTS.md, Rule 2): after a PR merges, pull `main` before starting anything
else, start the next branch from that updated `main`, and delete the merged local branch.
Your job is exactly that, and a short report. `$ARGUMENTS` may name the branch that just
merged; if it does, deal with that branch first and say so if it turns out not to be merged.

## 1. Make sure nothing is lost

Run `git status --porcelain` and `git rev-parse --abbrev-ref HEAD`.

- Changes to **tracked** files, or untracked files outside `.claude/`, mean there is unsaved
  work. **Stop** and list them. Do not stash, discard or commit them: the user decides.
- Untracked `.claude/` content (worktrees, local settings) is fine and is ignored.

## 2. Update `main`

```sh
git switch main
git pull --ff-only
git fetch --prune
```

If the pull is not a fast-forward, **stop** and report it: local `main` has commits the
remote lacks, and nothing on this repo should ever be committed to `main` (Rule 2). Never
`reset --hard`.

## 3. Delete the merged local branches

For every local branch except `main` (`git branch --format='%(refname:short)'`):

- `git branch -d <branch>`. It deletes only branches merged into the current `HEAD`, which
  is now `main`; let it refuse the rest.
- For each branch it refused, run `gh pr view <branch> --json number,state,url`:
  - **MERGED** (a squash or rebase merge leaves no merge commit for `-d` to see): report it
    as "merged as PR #N, delete with `git branch -D <branch>`?" and wait for the user. Do
    not force-delete on your own.
  - **OPEN** or no PR: keep it. These are usually the owner's drafts (`post/*`); say so.

## 4. Prune worktrees

`git worktree prune`, then `git worktree list`. Report any worktree left under
`.claude/worktrees/`; other sessions or agents may be using them, so remove one only when
the user names it.

## 5. Report

A short message: `main` at `<sha> <subject>`; branches deleted; branches kept and why; open
PRs (`gh pr list`); worktrees left. End with the next step the rules imply: start the next
branch from this `main`.
