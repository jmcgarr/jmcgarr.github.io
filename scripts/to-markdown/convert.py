#!/usr/bin/env python3
"""convert.py: convert legacy WordPress .html posts to Markdown for JBake (T102), without changing what
gets published.

Usage:
    .venv/bin/python convert.py [--offsite-links markdown|keep] [--in-place] [--stdout] FILE...

    FILE  a post with a JBake header (key=value lines, then a "~~~~~~" line) and an HTML body.
          Its name may end in .html or .md (the usual flow is `git mv x.html x.md`, then convert x.md
          in place with --in-place). Without --in-place, x.html is converted to x.md next to it.
    --in-place   overwrite FILE itself (for a FILE that was already renamed to .md)
    --stdout     print the result instead of writing a file
    --offsite-links markdown (default) | keep
          markdown: "Option B", the owner's decision of 2026-10-12. An off-site link (externalLinks()'s
                    definition) whose only extra attribute is target="_blank" becomes [text](url "title").
                    The page is byte-identical (the template's externalLinks() adds target="_blank" back
                    at build time), but feed.xml, which uses the raw body, loses target="_blank" on those
                    links. verify.py accepts exactly that, in the feed only, and counts it per post.
          keep:     a link with any attribute besides href/title stays raw HTML, so pages AND feed.xml
                    are unchanged (zero differences, more inline HTML).

Then bake and run verify.py against a baseline bake. verify.py, not this script, is the proof.

What it does:
  - The header (everything up to and including the "~~~~~~" line) is copied byte for byte.
  - The body becomes idiomatic Markdown where the result renders identically with this site's flexmark
    settings (jbake.properties: pegdown profile, AUTOLINKS, FENCED_CODE_BLOCKS, DEFINITIONS, TABLES,
    STRIKETHROUGH, TASKLISTITEMS, FOOTNOTES; no HARDWRAPS): paragraphs, <em>/<strong>, links, plain
    images, <h1>-<h6>, simple lists and blockquotes. One paragraph per line; LF line endings.
  - Anything Markdown can't express exactly stays raw HTML, copied from the source byte for byte: <pre>
    (Prism needs its classes), <img>/<p>/<a> with extra attributes, <span>, <div>, <table>, <iframe>,
    <object>, style=, and so on. A raw element inside a paragraph stays inline; a block that can't be
    converted stays a raw HTML block. When unsure, it keeps raw HTML.
  - Text that would trigger Markdown is escaped: \\ * _ [ ] < ` ~ | and an entity-like &, bare URLs and
    www. and e-mail addresses (AUTOLINKS), and line starts (#, >, -, +, =, :, "1.").
  - <br> followed by a newline becomes a backslash line break; any other <br> stays raw.
  - Collapsible whitespace a browser drops anyway (at the start and end of a line box) is dropped; other
    whitespace, including U+00A0, is kept (as &nbsp; at a line edge, where Markdown would strip it).
  - Entities other than &amp; &lt; &gt; &quot; are decoded to characters, except in the blocks that make
    up the page's meta description (head-meta.ftl keeps entities as written), which stay raw HTML.

Known limits (it stops with an error naming the file and line, and writes nothing for that file):
  - text or inline elements directly in the body, outside any block (Markdown would wrap a <p> round them)
  - HTML that html5lib would restructure (unclosed or misnested tags)
  - a blank line inside <script>/<style> (pegdown ends an HTML block at a blank line)

Needs html5lib (for the structure check): python3 -m venv .venv && .venv/bin/pip install html5lib
"""
import argparse
import html
import os
import re
import sys
import unicodedata
from html.parser import HTMLParser

import html5lib

VOID = {"area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source",
        "track", "wbr"}
RAWTEXT = {"script", "style", "textarea", "title", "xmp", "iframe", "noembed", "noframes", "plaintext"}
PRESERVE = {"pre", "textarea", "listing", "plaintext"}
# Tags that start an HTML block in flexmark's pegdown profile (CommonMark type 6, plus pre/script/style).
BLOCK_TAGS = {
    "address", "article", "aside", "base", "basefont", "blockquote", "body", "caption", "center", "col",
    "colgroup", "dd", "details", "dialog", "dir", "div", "dl", "dt", "fieldset", "figcaption", "figure",
    "footer", "form", "frame", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hr",
    "html", "iframe", "legend", "li", "link", "main", "menu", "menuitem", "nav", "noframes", "ol",
    "optgroup", "option", "p", "param", "section", "source", "summary", "table", "tbody", "td", "tfoot",
    "th", "thead", "title", "tr", "track", "ul", "pre", "script", "style", "textarea",
}
# Block-level for CSS line boxes (an inline context ends at these).
CSS_BLOCK = BLOCK_TAGS | {"listing", "plaintext", "hgroup", "noscript", "template"}
ATOMS = {"img", "iframe", "object", "embed", "video", "audio", "input", "svg", "canvas", "select",
         "button", "textarea", "math", "picture"}
COLLAPSIBLE = " \t\n\r\f"
SAFE_ENTITIES = {"&amp;", "&lt;", "&gt;", "&quot;", "&nbsp;", "&#160;", "&#xa0;", "&#xA0;"}
META_WINDOW = 400   # characters of plain text that may feed the meta description (155 + margin)
OFFSITE = re.compile(r"^\s*(?:https?:)?//(?!(?:www\.)?mikemcgarr\.com(?![\w.-]))", re.I)


class Unsupported(Exception):
    pass


class CannotConvert(Exception):
    """This element can't be Markdown; the caller keeps it as raw HTML."""


# ---------------------------------------------------------------------------------------------------
# Source tree: elements and text with their exact source spans


class El:
    def __init__(self, tag, attrs, start, start_end, start_text):
        self.tag = tag
        self.attrs = attrs              # list of (name, value), values decoded
        self.start = start              # offset of "<tag"
        self.start_end = start_end      # offset just after the start tag
        self.start_text = start_text    # the start tag's source
        self.end_start = None           # offset of "</tag>" (None for void elements)
        self.end = None                 # offset just after the element
        self.children = []
        self.parent = None

    def attr(self, name):
        for k, v in self.attrs:
            if k == name:
                return v
        return None

    def end_text(self, src):
        return src[self.end_start:self.end] if self.end_start is not None else ""


class Text:
    tag = None

    def __init__(self, raw, start, end):
        self.raw = raw                  # source, entities as written
        self.value = html.unescape(raw)   # css_trim() may trim this (Markdown context)
        self.orig = self.value               # used inside raw HTML elements, where nothing is trimmed
        self.after_break = False             # follows a backslash line break: its leading newline is that break
        self.start = start
        self.end = end
        self.parent = None


class Comment:
    tag = "#comment"

    def __init__(self, start, end):
        self.start = start
        self.end = end
        self.parent = None


class TreeBuilder(HTMLParser):
    def __init__(self, src):
        super().__init__(convert_charrefs=False)
        self.src = src
        self.root = El("#root", [], 0, 0, "")
        self.stack = [self.root]
        self.pos_cache = [0]
        for m in re.finditer("\n", src):
            self.pos_cache.append(m.end())
        self.last = 0           # end of the last tag/comment seen
        self.problems = []

    def src_offset(self):
        line, col = self.getpos()
        return self.pos_cache[line - 1] + col

    def add_text_until(self, pos):
        if pos > self.last:
            t = Text(self.src[self.last:pos], self.last, pos)
            t.parent = self.stack[-1]
            self.stack[-1].children.append(t)
        self.last = max(self.last, pos)

    def handle_starttag(self, tag, attrs):
        self._start(tag, attrs, self.get_starttag_text(), void=tag in VOID)

    def handle_startendtag(self, tag, attrs):
        self._start(tag, attrs, self.get_starttag_text(), void=True)

    def _start(self, tag, attrs, text, void):
        pos = self.src_offset()
        self.add_text_until(pos)
        el = El(tag, [(k, v if v is not None else "") for k, v in attrs], pos, pos + len(text), text)
        el.parent = self.stack[-1]
        self.stack[-1].children.append(el)
        self.last = el.start_end
        if void:
            el.end = el.start_end
        else:
            self.stack.append(el)

    def handle_endtag(self, tag):
        pos = self.src_offset()
        m = re.compile(r"</\s*" + re.escape(tag) + r"\s*>", re.I).match(self.src, pos)
        if not m:
            self.problems.append(f"unparsable end tag </{tag}> at line {self.getpos()[0]}")
            return
        if tag in VOID:
            self.problems.append(f"end tag </{tag}> for a void element at line {self.getpos()[0]}")
            return
        self.add_text_until(pos)
        if self.stack[-1].tag != tag:
            self.problems.append(f"</{tag}> at line {self.getpos()[0]} closes <{self.stack[-1].tag}>"
                                 f" (misnested or unclosed tags)")
            # close up to the matching element if there is one
            names = [e.tag for e in self.stack]
            if tag not in names[1:]:
                self.last = m.end()
                return
            while self.stack[-1].tag != tag:
                e = self.stack.pop()
                e.end_start = e.end = pos
        el = self.stack.pop()
        el.end_start = pos
        el.end = m.end()
        self.last = m.end()

    def handle_comment(self, data):
        pos = self.src_offset()
        self.add_text_until(pos)
        end = self.src.find("-->", pos)
        end = len(self.src) if end < 0 else end + 3
        c = Comment(pos, end)
        c.parent = self.stack[-1]
        self.stack[-1].children.append(c)
        self.last = end

    def handle_decl(self, decl):
        self.problems.append(f"declaration <!{decl}> in body")

    def handle_pi(self, data):
        self.problems.append(f"processing instruction <?{data}> in body")

    def unknown_decl(self, data):
        self.problems.append(f"unknown declaration {data!r} in body")

    def finish(self):
        self.close()
        self.add_text_until(len(self.src))
        while len(self.stack) > 1:
            e = self.stack.pop()
            self.problems.append(f"<{e.tag}> opened at offset {e.start} is never closed")
            e.end_start = e.end = len(self.src)
        self.root.end = len(self.src)
        return self.root


def preorder(el, out, depth=0):
    for c in el.children:
        if isinstance(c, El):
            out.append((depth, c.tag))
            preorder(c, out, depth + 1)
    return out


def html5_preorder(src):
    frag = html5lib.parseFragment(src, treebuilder="etree", namespaceHTMLElements=False, container="div")
    out = []

    def walk(e, d):
        for c in e:
            if isinstance(c.tag, str):
                out.append((d, c.tag.split("}")[-1]))
                walk(c, d + 1)

    walk(frag, 0)
    return out


# ---------------------------------------------------------------------------------------------------
# Inline conversion


def is_ws_char(c):
    return c == "" or c in COLLAPSIBLE or unicodedata.category(c) == "Zs"


def is_punct(c):
    if c == "":
        return False
    if c in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~":
        return True
    return unicodedata.category(c) in ("Pc", "Pd", "Pe", "Pf", "Pi", "Po", "Ps")


def left_flanking(prev, nxt):
    return not is_ws_char(nxt) and (not is_punct(nxt) or is_ws_char(prev) or is_punct(prev))


def right_flanking(prev, nxt):
    return not is_ws_char(prev) and (not is_punct(prev) or is_ws_char(nxt) or is_punct(nxt))


def delimiter_ok(d, before, inner, after):
    """Would `d inner d` parse as one emphasis span between the chars `before` and `after`?"""
    if not inner or inner[0] == d[0] or inner[-1] == d[0]:
        return False
    if before == d[0] or after == d[0]:
        return False
    first, last = inner[0], inner[-1]
    can_open = left_flanking(before, first)
    can_close = right_flanking(last, after)
    if d[0] == "_":
        can_open = can_open and (not right_flanking(before, first) or is_punct(before))
        can_close = can_close and (not left_flanking(last, after) or is_punct(after))
    # the opener must not also be able to close against something earlier, and vice versa: keep it simple
    return can_open and can_close


ESCAPE_ALWAYS = set("\\*[]<`~|")


def escape_text(s):
    out = []
    n = len(s)
    for i, c in enumerate(s):
        if c in ESCAPE_ALWAYS:
            out.append("\\" + c)
        elif c == "_":
            prev = s[i - 1] if i else ""
            nxt = s[i + 1] if i + 1 < n else ""
            if prev.isalnum() and nxt.isalnum():
                out.append(c)
            else:
                out.append("\\_")
        elif c == "&" and re.match(r"&#?[A-Za-z0-9]+;", s[i:]):
            out.append("\\&")
        else:
            out.append(c)
    t = "".join(out)
    # AUTOLINKS: scheme://, www., e-mail addresses
    t = re.sub(r"(?i)\b([a-z][a-z0-9+.\-]*):(?=//)", r"\1\\:", t)
    t = re.sub(r"(?i)\b(www)\.", r"\1\\.", t)
    t = re.sub(r"(?<=[^\s\\])@(?=[^\s])", r"\\@", t)
    return t


LINE_START = re.compile(r"^(?:[#>+\-=:]|\d+[.)](?=\s|$))")


def escape_line_start(line):
    """Escape a Markdown block marker at the start of a paragraph line."""
    m = LINE_START.match(line)
    if not m:
        return line
    tok = m.group(0)
    if tok[0].isdigit():
        return tok[:-1] + "\\" + tok[-1] + line[len(tok):]
    return "\\" + line


def fix_line_edges(line):
    """Markdown strips whitespace at line edges, U+00A0 included; write such characters as entities."""
    def ent(c):
        return "&nbsp;" if c == " " else f"&#x{ord(c):X};"
    i = 0
    while i < len(line) and line[i] not in COLLAPSIBLE and is_ws_char(line[i]):
        i += 1
    head = "".join(ent(c) for c in line[:i])
    rest = line[i:]
    j = len(rest)
    hard = rest.endswith("\\") and not rest.endswith("\\\\")
    end = j - 1 if hard else j
    k = end
    while k > 0 and rest[k - 1] not in COLLAPSIBLE and is_ws_char(rest[k - 1]):
        k -= 1
    tail = "".join(ent(c) for c in rest[k:end])
    return head + rest[:k] + tail + rest[end:]


class Converter:
    def __init__(self, src, offsite_links):
        self.src = src
        self.offsite_links = offsite_links
        self.inline_raw = 0
        self.orig_mode = 0

    # -- whitespace a browser drops (CSS Text 3 phase II), applied to a block's inline content ----------
    def css_trim(self, container):
        items = []

        def walk(n):
            for c in n.children:
                if isinstance(c, Text):
                    items.append(("text", c))
                elif isinstance(c, Comment):
                    continue
                elif c.tag == "br" or c.tag in CSS_BLOCK:
                    items.append(("break",))
                elif c.tag in ATOMS or c.tag in RAWTEXT:
                    items.append(("atom",))
                else:
                    walk(c)

        walk(container)
        line = []
        state = {"start": True, "space": False}

        def end_line():
            for t in reversed(line):
                s = t.value.rstrip(COLLAPSIBLE)
                t.value = s
                if s:
                    break
            line.clear()

        for it in items:
            if it[0] == "break":
                end_line()
                state.update(start=True, space=False)
            elif it[0] == "atom":
                line.clear()
                state.update(start=False, space=False)
            else:
                t = it[1]
                v = t.value
                if state["start"] or state["space"]:
                    v = v.lstrip(COLLAPSIBLE)
                t.value = v
                if v:
                    state["start"] = False
                    state["space"] = v[-1] in COLLAPSIBLE
                    line.append(t)
        end_line()

    def raw(self, node):
        return self.src[node.start:node.end]

    def inline(self, nodes, before, after):
        """Markdown for a list of inline nodes. `before`/`after`: the characters around it."""
        parts = []
        i = 0
        nodes = list(nodes)
        while i < len(nodes):
            n = nodes[i]
            joined = "".join(parts)
            prev_char = joined[-1] if joined else before
            if isinstance(n, Text):
                v = n.orig if self.orig_mode else n.value
                if n.after_break:
                    v = v.lstrip(COLLAPSIBLE)
                if v == "":
                    i += 1
                    continue
                v = re.sub(r"[ \t\r\f]*\n[ \t\r\f\n]*", " ", v)
                parts.append(escape_text(v))
            elif isinstance(n, Comment):
                r = self.raw(n)
                if "\n" in r:
                    raise CannotConvert("multi-line comment")
                parts.append(r)
            elif n.tag == "br":
                nxt = nodes[i + 1] if i + 1 < len(nodes) else None
                rest_blank = all(isinstance(x, Text) and (x.orig if self.orig_mode else x.value).strip(COLLAPSIBLE) == ""
                                 for x in nodes[i + 1:])
                if isinstance(nxt, Text) and nxt.raw.startswith("\n") and not rest_blank:
                    # a backslash line break, as in the source: "<br />\n" + text
                    parts.append("\\\n")
                    nxt.after_break = True
                    if self.src[n.start:n.end] not in ("<br />", "<br/>", "<br>"):
                        parts[-1] = self.raw(n) + "\n"
                        self.inline_raw += 1
                else:
                    parts.append(self.raw(n))
                    self.inline_raw += 1
            else:
                parts.append(self.element(n, prev_char, self.peek(nodes[i + 1:], after)))
            i += 1
        return "".join(parts)

    def peek(self, rest, after):
        """First output character of the nodes that follow (approximate: Markdown form)."""
        for n in rest:
            if isinstance(n, Text):
                v = re.sub(r"[ \t\r\f]*\n[ \t\r\f\n]*", " ", n.value)
                if v:
                    return escape_text(v)[0]
            elif isinstance(n, Comment):
                return "<"
            elif n.tag in ("strong", "em"):
                return "*"
            elif n.tag == "a":
                return "["
            elif n.tag == "img":
                return "!"
            else:
                return "<"
        return after

    def raw_inline(self, el, before, after):
        """Raw tags around converted content: <span style="…">Markdown</span>."""
        if "\n" in el.start_text:
            raise CannotConvert(f"<{el.tag}> start tag spans lines")
        self.inline_raw += 1
        if el.end_start is None:          # void element
            return el.start_text
        if el.tag in RAWTEXT or el.tag in ATOMS and el.tag != "img" or el.tag in PRESERVE:
            r = self.raw(el)
            if "\n" in r:
                raise CannotConvert(f"<{el.tag}> spans lines")
            return r
        self.orig_mode += 1
        try:
            inner = self.inline(el.children, ">", "<")
        finally:
            self.orig_mode -= 1
        return el.start_text + inner + el.end_text(self.src)

    def edge_ws(self, el):
        """Does the element's content start or end with collapsible whitespace (as written), or is it empty?"""
        parts = []

        def walk(e):
            for c in e.children:
                if isinstance(c, Text):
                    parts.append(c.orig)
                elif isinstance(c, El):
                    if c.tag in VOID or c.tag in ATOMS:
                        parts.append("x")
                    else:
                        walk(c)

        walk(el)
        s = "".join(parts)
        return s.strip(COLLAPSIBLE) != s or s == ""

    def element(self, el, before, after):
        tag = el.tag
        if tag in CSS_BLOCK and tag not in ("iframe",):
            raise CannotConvert(f"block <{tag}> inside inline content")
        names = {k for k, _ in el.attrs}
        if tag in ("strong", "em") and not el.attrs:
            if self.edge_ws(el):
                return self.raw_inline(el, before, after)
            inner = self.inline(el.children, "*", "*")
            if inner == "" or inner.strip(COLLAPSIBLE) != inner:
                return self.raw_inline(el, before, after)
            for d in (("**", "__") if tag == "strong" else ("*", "_")):
                if delimiter_ok(d, before, inner, after):
                    return d + inner + d
            return self.raw_inline(el, before, after)
        if tag == "a":
            href = el.attr("href")
            md_ok = href is not None and names <= {"href", "title"}
            if (not md_ok and self.offsite_links == "markdown" and href is not None
                    and names <= {"href", "title", "target"} and el.attr("target") == "_blank"
                    and OFFSITE.match(href)):
                md_ok = True
            if md_ok:
                md = self.link(el, href)
                if md is not None:
                    return md
            return self.raw_inline(el, before, after)
        if tag == "img" and names <= {"src", "alt", "title"} and "src" in names and "alt" in names:
            dest = self.dest(el.attr("src"))
            if dest is not None:
                alt = escape_text(el.attr("alt"))
                if "\n" not in alt and alt == alt.strip(COLLAPSIBLE):
                    return f"![{alt}]({dest}{self.title(el.attr('title'))})"
            return self.raw_inline(el, before, after)
        if tag == "code" and not el.attrs and all(isinstance(c, Text) for c in el.children):
            text = "".join(c.value for c in el.children)
            raw_text = "".join(c.raw for c in el.children)
            if (text and "\n" not in text and text.strip(" ") == text and "`" not in text
                    and "&" not in raw_text):
                return "`" + text + "`"
            return self.raw_inline(el, before, after)
        return self.raw_inline(el, before, after)

    def dest(self, url):
        if url is None or url == "" or re.search(r"[\s<>\\\x00-\x1f]", url):
            return None
        depth = 0
        for c in url:
            depth += c == "("
            depth -= c == ")"
            if depth < 0:
                return None
        if depth:
            return None
        return re.sub(r"&(?=#?[A-Za-z0-9]+;)", "&amp;", url)

    def title(self, t):
        if t is None:
            return ""
        if "\n" in t:
            raise CannotConvert("title with a newline")
        t = t.replace("\\", "\\\\").replace('"', '\\"')
        t = re.sub(r"&(?=#?[A-Za-z0-9]+;)", "&amp;", t)
        return f' "{t}"'

    def link(self, el, href):
        dest = self.dest(href)
        if dest is None or self.edge_ws(el):
            return None
        title = self.title(el.attr("title"))
        kids = el.children
        text = "".join(c.value for c in kids if isinstance(c, Text)) if all(isinstance(c, Text) for c in kids) else None
        if (text is not None and text == href and not title and re.fullmatch(r"(?i)(https?|ftp)://[^\s<>()\[\]\\`*_]+", href)
                and "&" not in href):
            return f"<{href}>"
        inner = self.inline(kids, "[", "]")
        if inner == "" or "\n" in inner or inner.strip(COLLAPSIBLE) != inner:
            return None
        return f"[{inner}]({dest}{title})"

    # -- blocks -----------------------------------------------------------------------------------
    def para_lines(self, el):
        """Markdown lines for the inline content of a block element (p, li, h*)."""
        self.css_trim(el)
        before_raw = self.inline_raw
        md = self.inline(el.children, "", "")
        if md.strip(COLLAPSIBLE) == "":
            raise CannotConvert("empty")
        lines = md.split("\n")
        out = []
        for ln in lines:
            ln = ln.strip(COLLAPSIBLE) if not ln.endswith("\\") else ln.lstrip(COLLAPSIBLE)
            out.append(fix_line_edges(escape_line_start(ln)))
        return out, self.inline_raw - before_raw

    def raw_block(self, el, why=""):
        """The element's source, with blank lines removed (they would end the HTML block)."""
        s = self.raw(el)
        if el.tag not in BLOCK_TAGS and not isinstance(el, Comment):
            raise Unsupported(f"<{el.tag}> at the top level can't be a raw HTML block ({why})")
        pres = []

        def find(e):
            for c in getattr(e, "children", []):
                if isinstance(c, El):
                    if c.tag in PRESERVE or c.tag in ("script", "style"):
                        pres.append((c.start_end - el.start, (c.end_start or c.end) - el.start, c.tag))
                    else:
                        find(c)

        if isinstance(el, El) and (el.tag in PRESERVE or el.tag in ("script", "style")):
            pres.append((el.start_end - el.start, (el.end_start or el.end) - el.start, el.tag))
        elif isinstance(el, El):
            find(el)
        lines = s.split("\n")
        out = []
        pos = 0
        carry = ""
        for idx, ln in enumerate(lines):
            line_start = pos
            pos += len(ln) + 1
            if ln.strip(COLLAPSIBLE) == "" and idx > 0:
                inside = [p for p in pres if p[0] <= line_start <= p[1]]
                if inside:
                    if inside[0][2] in ("script", "style"):
                        raise Unsupported(f"blank line inside <{inside[0][2]}>")
                    carry += ln + "&#10;"
                    continue
                continue      # drop a blank line outside <pre>: whitespace between tags
            out.append(carry + ln)
            carry = ""
        if carry:
            out.append(carry)
        return "\n".join(out)

    def block(self, el, meta_raw):
        """Markdown for one top-level (or container-level) node: a string, or None to skip."""
        if isinstance(el, Text):
            if el.value.strip(COLLAPSIBLE) == "":
                return None
            line = self.src.count("\n", 0, el.start) + 1
            raise Unsupported(f"text outside a block at body line {line}: {el.value.strip()[:40]!r}")
        if isinstance(el, Comment):
            r = self.raw(el)
            if "\n\n" in r:
                raise Unsupported("comment with a blank line")
            return ("raw", r)
        tag = el.tag
        if el in meta_raw:
            return ("raw", self.raw_block(el, "meta description"))
        try:
            if tag == "p" and not el.attrs:
                lines, nraw = self.para_lines(el)
                return ("md", "\n".join(lines))
            if tag in ("h1", "h2", "h3", "h4", "h5", "h6") and not el.attrs:
                lines, _ = self.para_lines(el)
                if len(lines) != 1:
                    raise CannotConvert("heading with a line break")
                text = lines[0]
                if text.endswith("#"):
                    text = text[:-1] + "\\#"
                return ("md", "#" * int(tag[1]) + " " + text)
            if tag in ("ul", "ol") and not el.attrs:
                return ("md", self.list_block(el))
            if tag == "blockquote" and not el.attrs:
                return ("md", self.quote_block(el))
            if tag == "div" and "\n" not in el.start_text:
                kids = [c for c in el.children if not (isinstance(c, Text) and c.value.strip(COLLAPSIBLE) == "")]
                if kids and all(isinstance(c, El) and c.tag in BLOCK_TAGS for c in kids):
                    inner = self.blocks(el.children, meta_raw)
                    return ("md", el.start_text + "\n\n" + inner + "\n\n" + el.end_text(self.src))
        except CannotConvert:
            pass
        return ("raw", self.raw_block(el))

    def list_block(self, el):
        items = []
        n = 0
        for c in el.children:
            if isinstance(c, Text) and c.value.strip(COLLAPSIBLE) == "":
                continue
            if not (isinstance(c, El) and c.tag == "li" and not c.attrs):
                raise CannotConvert("list with something other than plain <li>")
            if any(isinstance(k, El) and k.tag in CSS_BLOCK for k in c.children):
                raise CannotConvert("list item with block content")
            n += 1
            marker = "-" if el.tag == "ul" else f"{n}."
            lines, _ = self.para_lines(c)
            pad = " " * (len(marker) + 1)
            items.append(marker + " " + lines[0] + "".join("\n" + pad + ln for ln in lines[1:]))
        if not items:
            raise CannotConvert("empty list")
        return "\n".join(items)

    def quote_block(self, el):
        paras = []
        for c in el.children:
            if isinstance(c, Text) and c.value.strip(COLLAPSIBLE) == "":
                continue
            if not (isinstance(c, El) and c.tag == "p" and not c.attrs):
                raise CannotConvert("blockquote with something other than <p>")
            lines, _ = self.para_lines(c)
            paras.append("\n".join("> " + ln for ln in lines))
        if not paras:
            raise CannotConvert("empty blockquote")
        return "\n>\n".join(paras)

    def blocks(self, nodes, meta_raw):
        out = []
        for n in nodes:
            b = self.block(n, meta_raw)
            if b is not None:
                out.append(b)
        self.kinds = getattr(self, "kinds", []) + out
        return "\n\n".join(b[1] for b in out)


def plain_text(src):
    """Roughly head-meta.ftl's metaPlainText, to find the blocks that feed the meta description."""
    t = re.sub(r"(?s)<!--.*?-->", " ", src)
    t = re.sub(r"(?is)<(script|style|pre|iframe|object|noscript|figcaption|h[1-6])\b.*?</\1\s*>", " ", t)
    t = re.sub(r"<[^>]*>", " ", t)
    return re.sub(r"\s+", " ", t)


def meta_raw_blocks(root, src):
    """Top-level blocks in the meta description's window whose text has entities flexmark would decode."""
    keep = set()
    seen = 0
    for c in root.children:
        if seen > META_WINDOW:
            break
        if isinstance(c, El):
            s = src[c.start:c.end]
            seen += len(plain_text(s).strip())
            ents = set(re.findall(r"&#?[A-Za-z0-9]+;", plain_text(s)))
            if ents - SAFE_ENTITIES:
                keep.add(c)
        elif isinstance(c, Text):
            seen += len(c.value.strip())
    return keep


def convert(text, offsite_links="markdown"):
    m = re.search(r"^~{6}[ \t]*\n", text, re.M)
    if not m:
        raise Unsupported("no ~~~~~~ header separator")
    header, body = text[:m.end()], text[m.end():]
    if "\r" in body:
        raise Unsupported("CR line endings in the body (convert to LF first)")
    tb = TreeBuilder(body)
    tb.feed(body)
    root = tb.finish()
    if tb.problems:
        raise Unsupported("; ".join(tb.problems))
    mine, theirs = preorder(root, []), html5_preorder(body)
    if mine != theirs:
        for i, (a, b) in enumerate(zip(mine, theirs)):
            if a != b:
                raise Unsupported(f"html5lib would restructure the HTML (element {i}: {a} vs {b})")
        raise Unsupported(f"html5lib would restructure the HTML ({len(mine)} vs {len(theirs)} elements)")
    conv = Converter(body, offsite_links)
    md = conv.blocks(root.children, meta_raw_blocks(root, body))
    out = header + md + "\n"
    stats = line_stats(conv.kinds)
    return out, stats


def line_stats(kinds):
    md_lines = raw_lines = mixed = 0
    for kind, s in kinds:
        for ln in s.split("\n"):
            if not ln.strip() or ln.strip() == ">":
                continue
            if kind == "raw" or re.fullmatch(r"</?div\b[^>]*>", ln.strip()):
                raw_lines += 1
            elif re.search(r"<[A-Za-z/!]", re.sub(r"<[a-z][a-z0-9+.-]*://[^\s<>]*>", "", ln.replace("\\<", ""))):
                mixed += 1
            else:
                md_lines += 1
    return {"markdown": md_lines, "markdown_with_inline_html": mixed, "raw_html": raw_lines}


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0],
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("files", nargs="+")
    ap.add_argument("--offsite-links", choices=("markdown", "keep"), default="markdown")
    ap.add_argument("--in-place", action="store_true")
    ap.add_argument("--stdout", action="store_true")
    args = ap.parse_args()
    failed = 0
    print("file\tmarkdown\tmarkdown+inline-html\traw-html-block", file=sys.stderr)
    for path in args.files:
        with open(path, encoding="utf-8", newline="") as f:
            text = f.read()
        try:
            out, stats = convert(text, args.offsite_links)
        except Unsupported as e:
            print(f"{path}: NOT CONVERTED: {e}", file=sys.stderr)
            failed += 1
            continue
        if args.stdout:
            sys.stdout.write(out)
        else:
            dest = path if args.in_place else os.path.splitext(path)[0] + ".md"
            with open(dest, "w", encoding="utf-8", newline="") as f:
                f.write(out)
        print(f"{os.path.basename(path)}\t{stats['markdown']}\t{stats['markdown_with_inline_html']}\t{stats['raw_html']}",
              file=sys.stderr)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
