#!/usr/bin/env bash
#
# Compares the HTML pages in the baked site against the URL inventory of the
# live site, so a change can't silently remove a published URL.
# See docs/00-REVIVAL.md, task T006, and AGENTS.md (Rule 1).
#
#   missing = in docs/baseline/urls.txt, not in the build, not an expected removal  -> FAIL
#   new     = in the build, not in the baseline                                     -> info only
#
# Usage: scripts/check-urls.sh [site-dir]   (default: build/jbake)

set -euo pipefail

site_dir="${1:-build/jbake}"
repo_root="$(cd "$(dirname "$0")/.." && pwd)"
baseline="$repo_root/docs/baseline/urls.txt"
expected_removals="$repo_root/docs/baseline/expected-removals.txt"

if [[ ! -d "$site_dir" ]]; then
  echo "ERROR: site directory '$site_dir' not found. Run ./gradlew bake first." >&2
  exit 2
fi
if [[ ! -f "$baseline" ]]; then
  echo "ERROR: baseline '$baseline' not found." >&2
  exit 2
fi

export LC_ALL=C
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

(cd "$site_dir" && find . -type f -name '*.html' | sed 's|^\./||') | sort > "$tmp/built"
sort "$baseline" > "$tmp/baseline"
if [[ -f "$expected_removals" ]]; then
  grep -v -e '^#' -e '^[[:space:]]*$' "$expected_removals" | sort > "$tmp/allowed" || true
else
  : > "$tmp/allowed"
fi

comm -23 "$tmp/baseline" "$tmp/built" > "$tmp/gone"
comm -23 "$tmp/gone" "$tmp/allowed" > "$tmp/missing"
comm -12 "$tmp/gone" "$tmp/allowed" > "$tmp/removed"
comm -13 "$tmp/baseline" "$tmp/built" > "$tmp/new"

# On a case-insensitive filesystem (macOS default), pages whose names differ
# only by case (e.g. tags/DevOps.html and tags/devops.html) overwrite each other
# during the bake, so one of each pair can never exist locally. Report those as
# warnings here; CI runs on Linux, where the check stays strict.
: > "$tmp/case_collisions"
if [[ -e "$site_dir/INDEX.HTML" ]]; then
  : > "$tmp/still_missing"
  while IFS= read -r url; do
    if grep -qixF -- "$url" "$tmp/built"; then
      echo "$url" >> "$tmp/case_collisions"
    else
      echo "$url" >> "$tmp/still_missing"
    fi
  done < "$tmp/missing"
  mv "$tmp/still_missing" "$tmp/missing"
fi

if [[ -s "$tmp/removed" ]]; then
  echo "Expected removals ($(wc -l < "$tmp/removed" | tr -d ' ')):"
  sed 's/^/  - /' "$tmp/removed"
fi
if [[ -s "$tmp/new" ]]; then
  echo "New URLs ($(wc -l < "$tmp/new" | tr -d ' ')):"
  sed 's/^/  + /' "$tmp/new"
fi
if [[ -s "$tmp/case_collisions" ]]; then
  echo "WARNING: case-insensitive filesystem; these differ only by case from a built page and can't be verified here (CI checks them):"
  sed 's/^/  ~ /' "$tmp/case_collisions"
fi
if [[ -s "$tmp/missing" ]]; then
  echo "FAIL: published URLs missing from the build ($(wc -l < "$tmp/missing" | tr -d ' ')):"
  sed 's/^/  ! /' "$tmp/missing"
  echo "Restore them, add redirect stubs, or (with approval) list them in docs/baseline/expected-removals.txt." >&2
  exit 1
fi

echo "OK: all $(wc -l < "$tmp/baseline" | tr -d ' ') baseline URLs accounted for in $site_dir"
