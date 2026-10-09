#!/usr/bin/env python3
"""Redirect stubs that keep the site's old tag page URLs working (T039, T055).

Usage:
  scripts/tag-redirects.py                  write any missing or outdated stub (never deletes one)
  scripts/tag-redirects.py --check SITE     check a baked site (build/jbake or build/site), exit 1 on a problem

Tag pages used to be published under the tag exactly as written, spaces included
(tags/acceptance test.html), and a few tags existed in two spellings (DevOps and devops, Groovy and
groovy). With `tag.sanitize=true` JBake turns each space into a hyphen (tags/acceptance-test.html),
and the capitalized spellings were lowercased in the posts. Every old URL in docs/baseline/urls.txt
that no longer has a real tag page gets a small HTML page at the old path that sends readers (and
search engines) on to the new one: a meta refresh, rel=canonical, robots noindex, and a visible link.

Where the stubs go:
  src/jbake/assets/tags/          most stubs; JBake copies them as they are.
  src/case-redirects/tags/        stubs whose name differs from a real tag page only by letter case
                                  (tags/DevOps.html -> tags/devops.html). On a case-insensitive
                                  filesystem (macOS) the two names are one file, and JBake copies assets
                                  after rendering, so the stub would replace the real page. The bake task
                                  (build.gradle) copies these only when the output folder is
                                  case-sensitive, as on Linux, where CI builds and GitHub Pages serves.

The new name of an old tag is worked out from the posts: spaces become hyphens, as JBake does, and if
no published post uses that exact tag any more, a tag that differs only by case is used. An old tag
with no match at all is an error: decide what it should point to before going on.

--check also makes sure each real tag page shows its tag as written in the posts, spaces included
(templates/tag-names.ftl, T057): JBake keeps only the hyphenated form, so a tag with a hyphen of its
own (apt-get) must be listed in that template, or it would be shown as "apt get".
Standard library only. Run from the repository root.
"""
import html, os, re, sys
from urllib.parse import quote, unquote, urljoin

SITE = "https://www.mikemcgarr.com"
BASELINE = "docs/baseline/urls.txt"
CONTENT = "src/jbake/content"
STUB_DIRS = {False: "src/jbake/assets", True: "src/case-redirects"}  # key: differs only by case?
MARKER = "<!-- tag redirect stub (scripts/tag-redirects.py) -->"


def url_path(path):
    """Encode a site path the way head-meta.ftl does (FreeMarker ?url_path), so canonicals match."""
    return quote(path, safe="/-_.!~*'()")


def published_tags():
    """Tag names as JBake publishes them: every tag of every non-draft page, spaces made hyphens."""
    return {t.replace(" ", "-") for t in written_tags()}


def written_tags():
    """Every tag of every non-draft page, as written in its front matter (spaces kept)."""
    tags = set()
    for dirpath, _, files in os.walk(CONTENT):
        for f in files:
            text = open(os.path.join(dirpath, f), encoding="utf-8", errors="replace").read()
            head = text.replace("\r\n", "\n").replace("\r", "\n").split("~~~~~~", 1)[0]
            meta = dict(line.split("=", 1) for line in head.split("\n") if "=" in line)
            if meta.get("status", "").strip() != "published":
                continue
            for tag in meta.get("tags", "").split(","):
                if tag.strip():
                    tags.add(tag.strip())
    return tags


def redirects():
    """[(old path, new path)] for every baseline tag URL that no longer has a real tag page."""
    tags = published_tags()
    by_fold = {}
    for t in tags:
        by_fold.setdefault(t.casefold(), []).append(t)
    old_urls = [u.strip() for u in open(BASELINE, encoding="utf-8") if u.startswith("tags/")]
    pairs, errors = [], []
    for old in old_urls:
        name = old[len("tags/"):-len(".html")]
        new = name.replace(" ", "-")
        if new not in tags:
            matches = by_fold.get(new.casefold(), [])
            if len(matches) != 1:
                errors.append(f"{old}: no published tag {'matches' if not matches else 'is unambiguous'}: {matches}")
                continue
            new = matches[0]
        if new != name:
            pairs.append((old, f"tags/{new}.html"))
    if errors:
        sys.exit("ERROR: can't work out where these old tag URLs should point:\n  " + "\n  ".join(errors))
    return pairs


def stub(old, new):
    target = os.path.basename(new)
    href = html.escape(url_path(target).replace("'", "%27"))
    canonical = html.escape(f"{SITE}/{url_path(new)}", quote=True)
    title = html.escape("Tag: " + target[:-len(".html")])
    return f"""<!DOCTYPE html>
{MARKER}
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title} (moved)</title>
<meta name="robots" content="noindex">
<link rel="canonical" href="{canonical}">
<meta http-equiv="refresh" content="0; url={href}">
</head>
<body>
<p>This page has moved to <a href="{href}">{canonical}</a>.</p>
</body>
</html>
"""


def stub_path(old, new):
    return os.path.join(STUB_DIRS[old.casefold() == new.casefold()], old)


def write():
    pairs = redirects()
    written = 0
    for old, new in pairs:
        path, text = stub_path(old, new), stub(old, new)
        if os.path.exists(path) and open(path, encoding="utf-8").read() == text:
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        written += 1
        print(f"wrote {path} -> {new}")
    expected = {stub_path(o, n) for o, n in pairs}
    for d in STUB_DIRS.values():
        for dirpath, _, files in os.walk(os.path.join(d, "tags")):
            for f in files:
                p = os.path.join(dirpath, f)
                if MARKER in open(p, encoding="utf-8", errors="replace").read() and p not in expected:
                    print(f"NOTE: {p} is a stub this script no longer makes. It's a published URL: keep it unless approved.")
    print(f"{len(pairs)} tag redirect stubs ({written} written, {len(pairs) - written} already up to date)")


def exists_exactly(site, rel):
    """True if site/rel exists with exactly this spelling (os.path.exists ignores case on macOS)."""
    d, name = os.path.split(os.path.join(site, rel))
    return os.path.isdir(d) and name in os.listdir(d)


def check(site):
    case_insensitive = os.path.exists(os.path.join(site, "INDEX.HTML"))
    problems, skipped, real, stubs = [], [], 0, 0
    pairs = dict(redirects())
    old_urls = [u.strip() for u in open(BASELINE, encoding="utf-8") if u.startswith("tags/")]
    for old in old_urls:
        if not exists_exactly(site, old):
            if case_insensitive and old in pairs and old.casefold() == pairs[old].casefold():
                skipped.append(old)
            else:
                problems.append(f"{old}: missing from the build")
            continue
        text = open(os.path.join(site, old), encoding="utf-8").read()
        refresh = re.search(r'<meta http-equiv="refresh" content="0; url=([^"]+)">', text)
        if not refresh:
            real += 1
            if " " in old:
                problems.append(f"{old}: a real tag page with a space in its URL (tag.sanitize is off?)")
            if old in pairs:
                problems.append(f"{old}: a real page, but expected a stub pointing to {pairs[old]}")
            continue
        stubs += 1
        target = unquote(urljoin(old, html.unescape(refresh.group(1))))
        canonical = re.search(r'<link rel="canonical" href="([^"]+)">', text)
        if target != pairs.get(old):
            problems.append(f"{old}: redirects to {target}, expected {pairs.get(old)}")
        elif not exists_exactly(site, target):
            problems.append(f"{old}: its target {target} is missing")
        elif 'http-equiv="refresh"' in open(os.path.join(site, target), encoding="utf-8").read():
            problems.append(f"{old}: its target {target} is itself a redirect")
        if " " in target:
            problems.append(f"{old}: its target {target} has a space")
        if not canonical or html.unescape(canonical.group(1)) != f"{SITE}/{url_path(target)}":
            problems.append(f"{old}: rel=canonical is not {SITE}/{url_path(target)}")
        if '<meta name="robots" content="noindex">' not in text:
            problems.append(f"{old}: no robots noindex")
    spaced = [f for f in os.listdir(os.path.join(site, "tags")) if " " in f and f"tags/{f}" not in pairs]
    problems += [f"tags/{f}: a tag page with a space in its URL that isn't a known stub" for f in spaced]
    problems += shown_names(site)
    print(f"{len(old_urls)} baseline tag URLs: {real} real tag pages, {stubs} redirect stubs, {len(skipped)} not checkable here")
    for old in skipped:
        print(f"  ~ {old} (case-insensitive filesystem: same file as {pairs[old]}; the stub is added only on Linux)")
    for p in problems:
        print(f"  ! {p}")
    if problems:
        sys.exit(f"FAIL: {len(problems)} problems in {site}")
    print(f"OK: every baseline tag URL in {site} is a tag page or a stub whose target is a tag page")


def shown_names(site):
    """Problems with the tag name a real tag page shows (its og:title, "Tag: <name>"), compared with the posts."""
    written = {t.replace(" ", "-"): t for t in written_tags()}
    problems, checked = [], 0
    for f in sorted(os.listdir(os.path.join(site, "tags"))):
        if not f.endswith(".html") or f == "index.html" or f[:-len(".html")] not in written:
            continue
        text = open(os.path.join(site, "tags", f), encoding="utf-8").read()
        if 'http-equiv="refresh"' in text:
            continue
        title = re.search(r'<meta property="og:title" content="Tag: ([^"]*)">', text)
        shown, want = title and html.unescape(title.group(1)), written[f[:-len(".html")]]
        checked += 1
        if shown != want:
            problems.append(f"tags/{f}: shows the tag as {shown!r}, the posts write {want!r} "
                            "(list it in src/jbake/templates/tag-names.ftl)")
    print(f"{checked} tag pages checked for the tag name they show: {checked - len(problems)} as written in the posts")
    return problems


if __name__ == "__main__":
    if len(sys.argv) == 3 and sys.argv[1] == "--check":
        check(sys.argv[2])
    elif len(sys.argv) == 1:
        write()
    else:
        sys.exit(__doc__)
