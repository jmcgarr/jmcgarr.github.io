#!/usr/bin/env python3
"""verify.py: prove that converted posts publish exactly what they did before (T102).

Usage:
    .venv/bin/python verify.py [--report FILE] [--strict-feed] [--text-only] <baseline_build_dir> <new_build_dir> <slug>...

    <baseline_build_dir>  a copy of build/jbake baked from main before the conversion
    <new_build_dir>       build/jbake baked after it
    <slug>                a post's base name ("java-live" checks blog/java-live.html), or a path
                          relative to the build dir ending in .html (for example "about.html", or
                          "blog/drafts/<name>-draft.html" for a draft's preview page). A page from
                          page.ftl has no <article>: its body is the bare <div> inside the column div,
                          and "outside <article>" means outside that wrapper. Pages and drafts have no
                          feed item, which is shown as "not in feed" and is not a failure.

For each slug it checks four things, and the slug passes only if all four do:

 1. Page outside <article>: everything before <article> and after </article> in the page (head, meta
    description, masthead, share links, archived comments) is byte-identical.
 2. Article DOM: the post body (<article>…</article>), parsed with html5lib, has the same element tree,
    the same attributes (compared as sets: order and quoting don't matter, values do), and the same text.
 3. Feed item: the post's <item> in feed.xml (found by its <guid>) has identical title, link, pubDate and
    guid, and its <description> (the raw body) passes the same DOM comparison as 2, with ONE allowance
    (Option B, the owner's decision of 2026-10-12): an <a> whose href is off-site (externalLinks()'s
    definition) may lose target="_blank". Every other attribute (href, title, rel, …) must still match,
    and an internal link losing its target is a difference. These are counted per slug as "accepted
    (Option B)", not as differences. Pages get no allowance: externalLinks() adds the target back at
    build time, so they must be identical. --strict-feed turns the allowance off.
 4. Counts, as a cross-check, on the article and on the feed body: links, images, iframes, pre, code,
    headings, and the visible text (tags stripped, whitespace normalized) are equal.

--text-only (for the AsciiDoc posts, T102, whose Asciidoctor wrapper <div>s are gone by design): the DOM
comparisons of 2 and 3 don't decide the result; their differences are listed under "DOM differences" for
the reviewer to classify. What must still hold for a slug to pass: 1 (outside <article> byte-identical);
the article's visible text identical; the counts of links, images, iframes, headings, <pre> blocks and
inline <code> (code outside <pre>) equal (the total <code> count is shown but not required: a fenced block
is <pre><code>, a listing block was a bare <pre>); the feed item's title, link, pubDate and guid identical;
and the feed description's visible text identical. A zero-width space (U+200B, which Asciidoctor puts after
every ellipsis) is ignored in the text comparison and counted per slug as "U+200B dropped", so it is seen.

Whitespace rules for the DOM comparison (what "same text" means). They follow how a browser lays text
out (CSS Text 3, white-space: normal), so two bodies that compare equal render the same:
  - Inside <pre>, <textarea>, <listing> and <plaintext>, text must match exactly.
  - Elsewhere, runs of HTML whitespace (space, tab, CR, LF, FF; not U+00A0) collapse to one space.
  - Whitespace at the start or end of a line box is dropped: at the start and end of a block element, and
    next to a block-level child or a <br>. That includes whitespace-only text between blocks.
  - A space that follows another collapsed space across an inline tag boundary is dropped.
Differences that only these rules hide are listed as "notes" (not failures), so nothing is silent.

Output: a Markdown report (a summary table, then details per slug) on stdout, or in --report FILE.
Exit status: 0 if every slug passes, 1 if any differs, 2 on a usage error or a missing file.

Needs html5lib: python3 -m venv .venv && .venv/bin/pip install html5lib
"""
import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET

import html5lib

HTML_WS = re.compile(r"[ \t\n\r\f]+")
PRESERVE = {"pre", "textarea", "listing", "plaintext"}
BLOCK = {
    "address", "article", "aside", "blockquote", "center", "dd", "details", "dialog", "dir", "div", "dl",
    "dt", "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6",
    "header", "hgroup", "hr", "li", "listing", "main", "menu", "nav", "ol", "p", "plaintext", "pre",
    "section", "summary", "table", "caption", "thead", "tbody", "tfoot", "tr", "td", "th", "ul",
    "colgroup", "col", "legend", "noscript", "script", "style", "template",
}
# Inline elements that render as a box (not as text): they end a run of collapsible spaces.
ATOMS = {"img", "iframe", "object", "embed", "video", "audio", "input", "svg", "canvas", "select",
         "button", "textarea", "math", "picture"}
HEADINGS = {"h1", "h2", "h3", "h4", "h5", "h6"}
MAX_DIFFS = 40
ALLOW_TARGET_DROP = True
TEXT_ONLY = False


class Node:
    __slots__ = ("tag", "attrs", "children", "text")

    def __init__(self, tag=None, attrs=None, text=None):
        self.tag = tag            # None for a text node, "#comment" for a comment
        self.attrs = attrs or {}
        self.children = []
        self.text = text


def from_etree(el):
    """html5lib etree element -> Node tree (text and tails become text nodes)."""
    tag = el.tag
    if not isinstance(tag, str):          # comment
        return Node("#comment", text=el.text or "")
    if "}" in tag:
        tag = tag.split("}", 1)[1]
    n = Node(tag, dict(el.attrib))
    if el.text:
        n.children.append(Node(text=el.text))
    for child in el:
        n.children.append(from_etree(child))
        if child.tail:
            n.children.append(Node(text=child.tail))
    return n


def parse_fragment(html):
    frag = html5lib.parseFragment(html, treebuilder="etree", namespaceHTMLElements=False, container="div")
    root = Node("#fragment")
    if frag.text:
        root.children.append(Node(text=frag.text))
    for child in frag:
        root.children.append(from_etree(child))
        if child.tail:
            root.children.append(Node(text=child.tail))
    return root


def clone(n):
    c = Node(n.tag, dict(n.attrs), n.text)
    c.children = [clone(x) for x in n.children]
    return c


def collapse(n, in_pre=False):
    """Collapse whitespace runs in text nodes, except inside preformatted elements."""
    for c in n.children:
        if c.tag is None:
            if not in_pre:
                c.text = HTML_WS.sub(" ", c.text)
        elif c.tag != "#comment":
            collapse(c, in_pre or c.tag in PRESERVE)


def is_block(n):
    return n.tag is not None and n.tag != "#comment" and n.tag in BLOCK


def drop_interblock_ws(n):
    """Strict mode: drop whitespace-only text nodes that sit next to a block child or at a block's edges."""
    if n.tag in PRESERVE:
        return
    kids = n.children
    keep = []
    for i, c in enumerate(kids):
        if c.tag is None and c.text.strip(" ") == "":
            prev_block = i == 0 or is_block(kids[i - 1])
            next_block = i == len(kids) - 1 or is_block(kids[i + 1])
            container_block = n.tag == "#fragment" or is_block(n)
            if (prev_block and (i > 0 or container_block)) or (next_block and (i < len(kids) - 1 or container_block)):
                continue
        keep.append(c)
    n.children = keep
    for c in n.children:
        if c.tag not in (None, "#comment"):
            drop_interblock_ws(c)


def css_trim(container):
    """Render-faithful mode: drop collapsible spaces at the start/end of each line box (CSS Text 3 phase II)."""
    items = []   # ("text", node) | ("break",) | ("atom",)

    def walk(n):
        for c in n.children:
            if c.tag is None:
                items.append(("text", c))
            elif c.tag == "#comment":
                continue
            elif c.tag == "br" or is_block(c):
                items.append(("break",))
            elif c.tag in ATOMS:
                items.append(("atom",))
            else:
                walk(c)

    walk(container)
    line = []            # text nodes on the current line
    at_start = True
    prev_space = False

    def end_line():
        for node in reversed(line):
            stripped = node.text.rstrip(" ")
            node.text = stripped
            if stripped:
                break
        line.clear()

    for item in items:
        if item[0] == "break":
            end_line()
            at_start, prev_space = True, False
        elif item[0] == "atom":
            at_start, prev_space = False, False
            line.clear()   # spaces before an atom are not at a line end
        else:
            node = item[1]
            t = node.text
            if at_start or prev_space:
                t = t.lstrip(" ")
            node.text = t
            if t:
                at_start = False
                prev_space = t.endswith(" ")
                line.append(node)
    end_line()
    # recurse into block children (each is its own container)

    def recurse(n):
        for c in n.children:
            if c.tag in (None, "#comment"):
                continue
            if c.tag in PRESERVE:
                continue
            if is_block(c):
                css_trim(c)
            else:
                recurse(c)

    recurse(container)


def prune_empty(n):
    n.children = [c for c in n.children if not (c.tag is None and c.text == "")]
    # merge adjacent text nodes
    merged = []
    for c in n.children:
        if c.tag is None and merged and merged[-1].tag is None:
            merged[-1] = Node(text=merged[-1].text + c.text)
        else:
            merged.append(c)
    n.children = merged
    for c in n.children:
        if c.tag not in (None, "#comment"):
            prune_empty(c)


def normalize(root, mode):
    r = clone(root)
    collapse(r)
    if mode == "strict":
        drop_interblock_ws(r)
    else:
        css_trim(r)
    prune_empty(r)
    return r


def short(s, n=70):
    s = s.replace("\n", "\\n")
    return s if len(s) <= n else s[: n - 1] + "…"


def text_window(a, b):
    """Short before/after excerpts around the first difference of two strings."""
    i = 0
    while i < min(len(a), len(b)) and a[i] == b[i]:
        i += 1
    lo = max(0, i - 25)
    return short(("…" if lo else "") + a[lo:i + 35]), short(("…" if lo else "") + b[lo:i + 35])


def label(n):
    if n.tag is None:
        return "text"
    if n.tag == "#comment":
        return "comment"
    return n.tag


def describe(n):
    if n.tag is None:
        return repr(short(n.text, 50))
    if n.tag == "#comment":
        return "<!--" + short(n.text, 40) + "-->"
    attrs = " ".join(f'{k}="{short(v, 40)}"' for k, v in sorted(n.attrs.items()))
    return f"<{n.tag}{' ' + attrs if attrs else ''}>"


# Off-site, as the template's externalLinks() (external-links.ftl) defines it.
OFFSITE = re.compile(r"^\s*(?:https?:)?//(?!(?:www\.)?mikemcgarr\.com(?![\w.-]))", re.I)


def accepted_target_drop(a, b):
    """Option B (owner, 2026-10-12): in feed.xml only, an off-site <a> may lose target="_blank", nothing else."""
    if a.tag != "a" or a.attrs.get("target") != "_blank" or "target" in b.attrs:
        return False
    rest = {k: v for k, v in a.attrs.items() if k != "target"}
    return rest == b.attrs and OFFSITE.match(a.attrs.get("href", "")) is not None


def compare(a, b, path, diffs, accepted=None):
    """accepted: a list that collects Option B target drops (feed only); None allows nothing."""
    if len(diffs) >= MAX_DIFFS:
        return
    if a.tag != b.tag:
        diffs.append((path, f"node type {describe(a)} → {describe(b)}"))
        return
    if a.tag is None or a.tag == "#comment":
        if a.text != b.text:
            x, y = text_window(a.text, b.text)
            diffs.append((path, f"text `{x}` → `{y}`"))
        return
    if a.attrs != b.attrs and accepted is not None and accepted_target_drop(a, b):
        accepted.append(path)
    elif a.attrs != b.attrs:
        ka, kb = set(a.attrs.items()), set(b.attrs.items())
        diffs.append((path, f"attributes − {sorted(ka - kb)} + {sorted(kb - ka)}"))
    counts = {}
    for i in range(max(len(a.children), len(b.children))):
        ca = a.children[i] if i < len(a.children) else None
        cb = b.children[i] if i < len(b.children) else None
        ref = ca or cb
        lab = label(ref)
        counts[lab] = counts.get(lab, 0) + 1
        sub = f"{path}/{lab}[{counts[lab]}]"
        if ca is None:
            diffs.append((sub, f"added {describe(cb)}"))
        elif cb is None:
            diffs.append((sub, f"removed {describe(ca)}"))
        else:
            compare(ca, cb, sub, diffs, accepted)
        if len(diffs) >= MAX_DIFFS:
            return


def visible_text(n):
    parts = []

    def walk(x):
        for c in x.children:
            if c.tag is None:
                parts.append(c.text)
            elif c.tag == "#comment":
                continue
            else:
                block = is_block(c) or c.tag == "br"
                if block:
                    parts.append(" ")
                walk(c)
                if block:
                    parts.append(" ")

    walk(n)
    return HTML_WS.sub(" ", "".join(parts)).strip()


def counts_of(n):
    c = {"links": 0, "images": 0, "iframes": 0, "pre": 0, "code": 0, "inline code": 0, "headings": 0}

    def walk(x, in_pre):
        for k in x.children:
            if k.tag in (None, "#comment"):
                continue
            if k.tag == "a":
                c["links"] += 1
            elif k.tag == "img":
                c["images"] += 1
            elif k.tag == "iframe":
                c["iframes"] += 1
            elif k.tag == "pre":
                c["pre"] += 1
            elif k.tag == "code":
                c["code"] += 1
                if not in_pre:
                    c["inline code"] += 1
            elif k.tag in HEADINGS:
                c["headings"] += 1
            walk(k, in_pre or k.tag == "pre")

    walk(n, False)
    c["text chars"] = len(text_for_compare(visible_text(n)))
    return c


ZWSP = "\u200b"


def text_for_compare(t):
    """--text-only: U+200B is invisible; it is counted separately, not compared."""
    return t.replace(ZWSP, "") if TEXT_ONLY else t


def counts_equal(a, b):
    """--text-only: the total <code> count may differ (bare <pre> → <pre><code>); everything else must match."""
    skip = {"code"} if TEXT_ONLY else set()
    return all(a[k] == b[k] for k in a if k not in skip)


def compare_bodies(base_html, new_html, where, allow_target_drop=False):
    """Returns (diffs, notes, base_counts, new_counts, base_text, new_text, accepted)."""
    a, b = parse_fragment(base_html), parse_fragment(new_html)
    na, nb = normalize(a, "css"), normalize(b, "css")
    diffs = []
    accepted = [] if allow_target_drop else None
    compare(na, nb, where, diffs, accepted)
    notes = []
    if not diffs:
        sa, sb = normalize(a, "strict"), normalize(b, "strict")
        compare(sa, sb, where, notes, [] if allow_target_drop else None)
    return diffs, notes, counts_of(na), counts_of(nb), visible_text(na), visible_text(nb), accepted or []


PAGE_WRAPPER = re.compile(r'<div class="col-lg-8 col-md-10 mx-auto">\s*<div>')
DIV_TAG = re.compile(r"<(/?)div\b")


def split_article(page):
    """(before, body, after). A post's body is <article>…</article>. A page (page.ftl, no <article>) has
    its body in a bare <div> right inside the column div; its end is found by counting div nesting."""
    s = page.find("<article>")
    e = page.rfind("</article>")
    if s >= 0 and e >= 0:
        return page[:s], page[s + len("<article>"):e], page[e:]
    m = PAGE_WRAPPER.search(page)
    if not m:
        return None
    depth = 1
    for t in DIV_TAG.finditer(page, m.end()):
        depth += -1 if t.group(1) else 1
        if depth == 0:
            return page[:m.end()], page[m.end():t.start()], page[t.start():]
    return None


def first_line_diff(a, b):
    la, lb = a.split("\n"), b.split("\n")
    for i in range(max(len(la), len(lb))):
        x = la[i] if i < len(la) else "<missing>"
        y = lb[i] if i < len(lb) else "<missing>"
        if x != y:
            return i + 1, short(x.strip(), 90), short(y.strip(), 90)
    return None


def feed_items(path):
    tree = ET.parse(path)
    items = {}
    for item in tree.getroot().iter("item"):
        guid = (item.findtext("guid") or "").strip()
        items[guid] = item
    return items


def read(path):
    with open(path, encoding="utf-8", newline="") as f:
        return f.read()


def check(slug, base_dir, new_dir, base_feed, new_feed):
    rel = slug if slug.endswith(".html") else f"blog/{slug}.html"
    res = {"slug": slug, "rel": rel, "outside": "?", "article": "?", "feed": "?", "counts": "?",
           "details": [], "notes": [], "count_rows": [], "accepted": 0, "accepted_paths": [], "dom": [],
           "zwsp": 0, "text": "?"}
    ok = True
    bp, np_ = os.path.join(base_dir, rel), os.path.join(new_dir, rel)
    if not os.path.exists(bp) or not os.path.exists(np_):
        res["details"].append(f"missing page: {'baseline' if not os.path.exists(bp) else 'new'} {rel}")
        res.update(outside="missing", article="missing", feed="missing", counts="missing", ok=False)
        return res
    pa, pb = split_article(read(bp)), split_article(read(np_))
    if pa is None or pb is None:
        res["details"].append("no <article> element or page body wrapper in the page")
        res.update(outside="no body", ok=False)
        return res
    # 1. outside the article
    if pa[0] == pb[0] and pa[2] == pb[2]:
        res["outside"] = "identical"
    else:
        ok = False
        res["outside"] = "**differs**"
        for name, x, y in (("before <article>", pa[0], pb[0]), ("after </article>", pa[2], pb[2])):
            if x != y:
                ln, bx, by = first_line_diff(x, y)
                res["details"].append(f"{name}, line {ln}: `{bx}` → `{by}`")
    # 2. article DOM
    diffs, notes, ca, cb, ta, tb, _ = compare_bodies(pa[1], pb[1], "article")
    res["zwsp"] += ta.count(ZWSP) - tb.count(ZWSP)
    ta, tb = text_for_compare(ta), text_for_compare(tb)
    if TEXT_ONLY:
        res["article"] = "identical" if not diffs else f"{len(diffs)}{'+' if len(diffs) >= MAX_DIFFS else ''} (listed)"
        res["dom"] += [f"`{p}`: {d}" for p, d in diffs]
        res["text"] = "identical" if ta == tb else "**differs**"
    else:
        res["article"] = "identical" if not diffs else f"**{len(diffs)}{'+' if len(diffs) >= MAX_DIFFS else ''} differences**"
        ok &= not diffs
        res["details"] += [f"`{p}`: {d}" for p, d in diffs]
        res["notes"] += [f"`{p}`: {d}" for p, d in notes]
    count_ok = counts_equal(ca, cb) and ta == tb
    for k in ca:
        res["count_rows"].append(("article", k, ca[k], cb[k]))
    if ta != tb:
        x, y = text_window(ta, tb)
        res["details"].append(f"article visible text: `{x}` → `{y}`")
    # 3. feed item
    ia, ib = base_feed.get(rel), new_feed.get(rel)
    if ia is None or ib is None:
        res["feed"] = "not in feed" if ia is None and ib is None else "**missing in one feed**"
        if not (ia is None and ib is None):
            ok = False
    else:
        fok = True
        for field in ("title", "link", "pubDate", "guid"):
            if (ia.findtext(field) or "") != (ib.findtext(field) or ""):
                fok = False
                res["details"].append(f"feed `{field}`: `{short(ia.findtext(field) or '')}` → `{short(ib.findtext(field) or '')}`")
        fd, fn, fa, fb, fta, ftb, acc = compare_bodies((ia.findtext("description") or "").strip(),
                                                       (ib.findtext("description") or "").strip(), "description",
                                                       allow_target_drop=ALLOW_TARGET_DROP)
        res["accepted"] = len(acc)
        res["accepted_paths"] = acc
        res["zwsp"] += fta.count(ZWSP) - ftb.count(ZWSP)
        fta, ftb = text_for_compare(fta), text_for_compare(ftb)
        if TEXT_ONLY:
            res["dom"] += [f"feed `{p}`: {d}" for p, d in fd]
        else:
            fok &= not fd
            res["details"] += [f"feed `{p}`: {d}" for p, d in fd]
            res["notes"] += [f"feed `{p}`: {d}" for p, d in fn]
        for k in fa:
            res["count_rows"].append(("feed", k, fa[k], fb[k]))
        if fta != ftb:
            x, y = text_window(fta, ftb)
            res["details"].append(f"feed visible text: `{x}` → `{y}`")
            if TEXT_ONLY:
                res["text"] = "**differs**"
        count_ok &= counts_equal(fa, fb) and fta == ftb
        res["feed"] = "identical" if fok else "**differs**"
        ok &= fok
    res["counts"] = "equal" if count_ok else "**differ**"
    ok &= count_ok
    res["ok"] = ok
    return res


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0],
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("base")
    ap.add_argument("new")
    ap.add_argument("slugs", nargs="+")
    ap.add_argument("--report", help="write the Markdown report here (default: stdout)")
    ap.add_argument("--strict-feed", action="store_true",
                    help="no Option B allowance: a dropped target=\"_blank\" in feed.xml is a difference")
    ap.add_argument("--text-only", action="store_true",
                    help="DOM differences are listed, not failures; visible text, counts, the page outside "
                         "<article> and the feed item's fields still must match (see the header)")
    args = ap.parse_args()
    global ALLOW_TARGET_DROP, TEXT_ONLY
    ALLOW_TARGET_DROP = not args.strict_feed
    TEXT_ONLY = args.text_only
    for d in (args.base, args.new):
        if not os.path.isdir(d):
            print(f"not a directory: {d}", file=sys.stderr)
            return 2
    try:
        base_feed = feed_items(os.path.join(args.base, "feed.xml"))
        new_feed = feed_items(os.path.join(args.new, "feed.xml"))
    except (OSError, ET.ParseError) as e:
        print(f"cannot read feed.xml: {e}", file=sys.stderr)
        return 2
    results = [check(s, args.base, args.new, base_feed, new_feed) for s in args.slugs]
    out = []
    passed = sum(r["ok"] for r in results)
    total_accepted = sum(r["accepted"] for r in results)
    if TEXT_ONLY:
        out.append(f"# verify.py --text-only: {passed} of {len(results)} pass (visible text, counts, page outside "
                   f"<article>, feed item fields){f' ({total_accepted} accepted Option B feed differences)' if total_accepted else ''}\n")
        out.append(f"Baseline `{args.base}`, new `{args.new}`. DOM differences are listed per slug, not judged.\n")
        out.append("| Slug | Outside `<article>` | Visible text | Counts | Feed item | DOM differences (article + feed) | U+200B dropped | Result |")
        out.append("|---|---|---|---|---|---|---|---|")
        for r in results:
            out.append(f"| `{r['slug']}` | {r['outside']} | {r['text']} | {r['counts']} | {r['feed']} | {len(r['dom'])} | "
                       f"{r['zwsp']} | {'pass' if r['ok'] else '**FAILS**'} |")
    else:
        out.append(f"# verify.py: {passed} of {len(results)} identical"
                   f"{f' ({total_accepted} accepted Option B feed differences)' if total_accepted else ''}\n")
        out.append(f"Baseline `{args.base}`, new `{args.new}`.\n")
        out.append("| Slug | Outside `<article>` | Article DOM | Feed item | Accepted (Option B) | Counts | Result |")
        out.append("|---|---|---|---|---|---|---|")
        for r in results:
            out.append(f"| `{r['slug']}` | {r['outside']} | {r['article']} | {r['feed']} | {r['accepted']} | {r['counts']} | "
                       f"{'identical' if r['ok'] else '**DIFFERS**'} |")
    for r in results:
        verdict = ("pass" if r["ok"] else "FAILS") if TEXT_ONLY else ("identical" if r["ok"] else "DIFFERS")
        out.append(f"\n## `{r['rel']}`: {verdict}\n")
        if r["count_rows"]:
            out.append("| Where | Count | Before | After |")
            out.append("|---|---|---|---|")
            for where, k, x, y in r["count_rows"]:
                out.append(f"| {where} | {k} | {x} | {y}{'' if x == y else ' **≠**'} |")
        if r["accepted_paths"]:
            out.append(f"\nAccepted (Option B): {len(r['accepted_paths'])} off-site links lost target=\"_blank\" in "
                       "feed.xml, nothing else: " + ", ".join(f"`{x}`" for x in r["accepted_paths"]))
        if r["details"]:
            out.append("\nDifferences:\n")
            out += [f"- {d}" for d in r["details"]]
        if r["notes"]:
            out.append("\nNotes (whitespace a browser drops when it lays out the line; not a difference):\n")
            out += [f"- {d}" for d in r["notes"]]
        if r["dom"]:
            out.append(f"\nDOM differences ({len(r['dom'])}, listed for review; --text-only doesn't judge them):\n")
            out += [f"- {d}" for d in r["dom"]]
    text = "\n".join(out) + "\n"
    if args.report:
        with open(args.report, "w", encoding="utf-8") as f:
            f.write(text)
        print(f"{passed} of {len(results)} {'pass' if TEXT_ONLY else 'identical'}; {total_accepted} accepted (Option B); report: {args.report}")
        for r in results:
            if not r["ok"]:
                print(f"  DIFFERS: {r['slug']}")
    else:
        sys.stdout.write(text)
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
