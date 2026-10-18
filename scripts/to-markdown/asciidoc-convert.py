#!/usr/bin/env python3
"""asciidoc-convert.py: convert the AsciiDoc posts to Markdown for JBake (T102), keeping the visible text.

Usage:
    .venv/bin/python asciidoc-convert.py [--in-place] [--stdout] FILE...

    FILE  a post with a JBake header (key=value lines, then a "~~~~~~" line) and an AsciiDoc body.
          Its name may end in .asciidoc or .md (the usual flow is `git mv x.asciidoc x.md`, then convert
          x.md in place with --in-place). Without --in-place, x.asciidoc is converted to x.md next to it.
    --in-place   overwrite FILE itself (for a FILE that was already renamed to .md)
    --stdout     print the result instead of writing a file

Then bake and run verify.py against a baseline bake (`verify.py --text-only`: Asciidoctor's wrapper
<div>s are gone, so the DOM differs by design; the visible text, the counts and the page outside
<article> must not). verify.py, not this script, is the proof.

It is a converter for exactly the AsciiDoc the 11 posts of 2014-2020, the 3 pages (About, Talks, Speaker
Bio) and the 2 AsciiDoc drafts use, not for AsciiDoc in general.
Anything outside that subset stops it with an error naming the file and line, and nothing is written for
that file. No guessing: a construct Asciidoctor would have rendered in a way this script doesn't reproduce
is refused rather than passed through.

What it does:
  - The header (everything up to and including the "~~~~~~" line) is copied byte for byte.
  - Blocks. Paragraphs stay paragraphs, line breaks included (soft breaks in both). `== T`, `=== T`,
    `==== T` and a title underlined with dashes (setext, level 1) become `## T`, `### T`, `#### T`, `## T`.
    A heading that an `<<xref>>` points at is written as raw `<h2 id="_t">T</h2>`, with the id Asciidoctor
    generates, so the link keeps working; no other heading gets an id (Markdown headings have none).
  - Listing blocks (`----`) become fenced code blocks with no language (`[source,lang]` would give the
    language; none of the posts has one). Asciidoctor emitted a bare `<pre>`; flexmark emits
    `<pre><code>`; code-highlight.ftl treats both the same at runtime (a block with no language is
    highlighted as `bash`, or `markup` if it starts with "<"), so the box looks the same.
  - Lists (`*` or `-` items, one level) become Markdown lists with the same marker, written loose (a blank
    line between items), because Asciidoctor puts each item's text in a `<p>` and a loose Markdown list
    renders `<li><p>` too; a tight list would lose the paragraph spacing between items.
  - Quote blocks (`____`, with or without `[quote]`) become `>` blockquotes. A `[quote, Author, Source]`
    attribution becomes a last paragraph in the blockquote, "— Author" then a hard line break, then the
    source (a link macro in the source is a link). That is the text Asciidoctor rendered
    ("&#8212; Author<br><cite>Source</cite>"); the look changes from a right-aligned attribution to the
    quote's own style.
  - Sidebar blocks (`****`) have no Markdown form, so they become a Bootstrap card, HTML in the
    Markdown with the content inside as Markdown: `<div class="card bg-light mb-4"><div class="card-body">`,
    `<p class="card-title">Title</p>` (when the block has a `.Title`), a blank line, the Markdown, a blank
    line, `</div></div>`. Nothing from asciidoctor.css is needed (owner's decision 2026-10-18: no
    Asciidoctor markup or styles in Markdown posts); `_content.scss` styles the title as Asciidoctor did
    and trims the card's first and last margins.
  - `image::` becomes `![alt](src)`, or `[![alt](src)](link)` with `link=`. Asciidoctor's default alt
    (the file name without its extension, hyphens and underscores as spaces) is written out when the macro
    has none. `window=_blank` on the link is dropped: the template's externalLinks() adds target="_blank"
    to every off-site link at build time (Option B, the owner's decision of 2026-10-12). An image with a
    width or an `align` has no Markdown form and is written as HTML in the Markdown, the way the converted
    WordPress posts carry their sized images, with Bootstrap's own classes and nothing from asciidoctor.css
    (owner's decision 2026-10-18): `<p class="text-center"><img src alt width="500"></p>` on one line
    (`text-center` only for `align="center"`; the `<p>` gives it a paragraph's spacing, as `![alt](src)`
    would). With a title/caption it is a Bootstrap figure on four lines: `<figure class="figure d-block
    text-center">`, the `<img class="figure-img">` (inside its `<a>` with `link=`), `<figcaption
    class="figure-caption">caption</figcaption>`, `</figure>`; `_content.scss` styles the caption as
    Asciidoctor did (bold, dark red). A `[#id]` on it is dropped unless something refers to it (nothing
    does). A third positional attribute is the `height`, kept as `height="250"`. `role="right"` or
    `role="left"` (the pages' floated photos) has no Markdown form either: the `<img>` gets Bootstrap's
    `float-right ml-3 mb-3` (or `float-left mr-3 mb-3`), inside a `<p>` (with its `<a>` when there is a
    `link=`), on one line, in place of Asciidoctor's floated `.imageblock.right` or `.image.right` span and
    its `asciidoctor.css` margins. An inline image macro (`image:`, one colon) that is a paragraph on its own,
    which Asciidoctor rendered as `<p><span class="image"><img></span></p>`, is converted like the block
    macro: `![alt](src)` when it is plain, the `<p><img>` form when it has a width, height or role. One
    inside a paragraph's text is refused. A target may be site-relative (`img/x.jpg`, as the pages have it)
    or absolute (`/img/x.jpg`); it is kept as written.
  - Passthrough blocks (`++++`) are looked at, not passed through blindly. One that holds exactly one
    `<iframe>` of a YouTube video (`youtube.com/embed/ID`) or a SlideShare deck
    (`slideshare.net/slideshow/embed_code/N`) with a `title` becomes a T115 embed: a Markdown link alone in
    a paragraph, `[title](URL)`, which embeds.ftl renders as the responsive, titled player at build time
    (owner's decision 2026-10-17; page.ftl runs embeds() too since the pages were converted). The URL is the
    iframe's src with `https:` added to a protocol-relative `//www.slideshare.net/…`. The iframe's other
    attributes (width, height, frameborder, align, marginwidth, marginheight, scrolling, style,
    allowfullscreen) are dropped: the player is sized by Bootstrap's `.embed-responsive`, so a 390×260
    player floated right becomes a full-width one (a visible change, listed for the owner). An iframe with
    no title is refused (the embed needs the title as the link text). Any other passthrough content is
    kept as the same HTML in the Markdown, on the same lines, when CommonMark would read it as an HTML
    block (its first line starts a block-level tag and it has no blank line); the script names each such
    block on stderr so it is reported. Anything else is refused.
  - A bare URL in text (`https://…` with no `[text]`), which Asciidoctor autolinks, becomes a Markdown
    autolink, `<https://…>`, with the same visible text; the URL ends where Asciidoctor's does (not at a
    trailing `,`, `.`, `?`, `!` or `)`). Asciidoctor's `class="bare"` on the link has no style and is
    dropped.
  - `---` (Markdown-style thematic break, which Asciidoctor accepts) stays `---`.
  - Inline: `*strong*`, `**strong**`, `_em_`, `__em__` (constrained and unconstrained, nested either way)
    become `**`/`*` or `__`/`_` delimiters, whichever CommonMark's flanking rules accept next to the
    neighbouring characters; `https://…[text]`, `http://…[text]` and `link:/path[text]` become
    `[text](url)`; a `^` suffix or `window=_blank` on a link is dropped (externalLinks(), as above; on an
    on-site link it is a change: the link no longer opens a new tab); `<<Title,text>>` becomes
    `[text](#_title)`. Every other `*`, `_`, `[`, `]`, `<`, `` ` ``, `~`, `|`, `\\` in text is escaped,
    as are line starts that Markdown would read as a block (`#`, `>`, `-`, `+`, `=`, `:`, "1.").
  - Asciidoctor's text replacements are applied, so the Markdown holds the characters the page showed:
    `...` becomes the ellipsis "…" (Asciidoctor adds a zero-width space, U+200B, after it; that is left
    out, it is invisible and would be an invisible character in the source), and an apostrophe between a
    letter or digit and a letter (it's, Jon's) becomes "’". The other replacements Asciidoctor would make
    (`--`, `->`, `=>`, `<-`, `<=`, `(C)`, `(R)`, `(TM)`, "`quotes`") don't occur in the posts and are
    refused, so none is silently left literal.
  - Trailing whitespace on a line is dropped (Asciidoctor ignores it; in Markdown two spaces would be a
    line break). LF line endings.

Known limits (it stops with an error naming the file and line, and writes nothing for that file):
  nested lists, ordered lists, description lists, tables, admonitions, literal (`....`), example (`====`)
  and open (`--`) blocks, a passthrough block (`++++`) that is neither a titled YouTube/SlideShare iframe
  nor an HTML block, `[[anchors]]`, `[role]` prefixes, backslash escapes, footnotes, an inline image
  inside a paragraph's text, an image `role` other than left or right, monospace (`` ` ``), hard line
  breaks (" +"), attribute references ({name}), a paragraph that starts with a space (a literal
  paragraph), a `.Title` on a block other than an image or a sidebar, an image title without a
  `caption=`, and the replacements listed above.
"""
import argparse
import html
import os
import re
import sys
import unicodedata

ESCAPE_ALWAYS = set("\\*[]<`~|")
LINE_START = re.compile(r"^(?:[#>+\-=:]|\d+[.)](?=\s|$))")
COLLAPSIBLE = " \t\r\f"
# Asciidoctor's quoted-text regexes (substitutors.rb), translated. \w is Unicode in both.
RX_STRONG_UNC = re.compile(r"(\\)?(?:\[([^\]]+)\])?\*\*(.+?)\*\*", re.S)
RX_STRONG_CON = re.compile(r"(^|[^\w;:}])(\\)?(?:\[([^\]]+)\])?\*(\S|\S.*?\S)\*(?!\w)", re.S | re.M)
RX_EM_UNC = re.compile(r"(\\)?(?:\[([^\]]+)\])?__(.+?)__", re.S)
RX_EM_CON = re.compile(r"(^|[^\w;:}])(\\)?(?:\[([^\]]+)\])?_(\S|\S.*?\S)_(?!\w)", re.S | re.M)
RX_MONO_CON = re.compile(r"(^|[^\w;:\"'`}])(\\)?(?:\[([^\]]+)\])?`(\S|\S.*?\S)`(?![`\w])", re.S | re.M)
# Link macros: https://…[text], link:…[text] (the text may hold quoted-text markers, \x01…\x02).
RX_LINK = re.compile(r"(^|link:|[\s>\(\)\[\];\"'\x01\x02])(\\?)((?:https?|ftp)://[^\s\[\]<>]+|(?<=link:)[^\s\[\]<>]+)\[([^\]]*)\]", re.M)
# A bare URL, ending where Asciidoctor's InlineLinkRx ends it (not at a trailing , . ? ! or closing paren).
RX_BARE_URL = re.compile(r"(?:^|(?<=[\s>\(\)\[\];\"']))((?:https?|ftp)://[^\s\[\]<]*[^\s,.?!\[\]<\)])")
RX_XREF = re.compile(r"<<([^<>,]+?)(?:,\s*([^<>]*?))?>>")
# An inline image macro (one colon) on a line of its own; inside text it is refused.
RX_INLINE_IMAGE = re.compile(r"^image:([^:\s\[]\S*?)\[(.*)\]$")
RX_PASS = re.compile(r"^\+{4,}$")
# One <iframe> in a passthrough block (a space or nothing between the tags), and the embeds.ftl URL forms.
RX_IFRAME = re.compile(r"^<iframe\s+([^<>]*?)\s*>\s*</iframe>$", re.S)
RX_HTML_ATTR = re.compile(r"([\w-]+)(?:=\"([^\"]*)\")?")
RX_EMBED_SRC = re.compile(r"^(?:https?:)?//(?:www\.)?(?:(youtube\.com/embed/[\w-]+)|(slideshare\.net/slideshow/embed_code/(?:key/)?[\w-]+))$")
# CommonMark's HTML block start condition 6 (block-level tag names), for passthrough content kept as HTML.
HTML_BLOCK_TAGS = {
    "address", "article", "aside", "base", "basefont", "blockquote", "body", "caption", "center", "col",
    "colgroup", "dd", "details", "dialog", "dir", "div", "dl", "dt", "fieldset", "figcaption", "figure",
    "footer", "form", "frame", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hr",
    "html", "iframe", "legend", "li", "link", "main", "menu", "menuitem", "nav", "noframes", "ol",
    "optgroup", "option", "p", "param", "section", "source", "summary", "table", "tbody", "td", "tfoot",
    "th", "thead", "title", "tr", "track", "ul",
}
RX_REFUSED_REPL = re.compile(r"(?:\w--(?=\w)|(?:^|\s)--(?:\s|$)|->|=>|<-|<=|\((?:C|R|TM)\)|\"`|`\"|'`|`'|\\)", re.M)
RX_ELLIPSIS = re.compile(r"\.\.\.")
RX_APOS = re.compile(r"(?<=[^\W_])'(?=[^\W\d_])")
S_OPEN, S_CLOSE, E_OPEN, E_CLOSE = "\x01S\x02", "\x01/S\x02", "\x01E\x02", "\x01/E\x02"
PH = re.compile(r"\x00(\d+)\x00|\x01/?[SE]\x02")


class Unsupported(Exception):
    pass


# ---------------------------------------------------------------------------------------------------
# Markdown text escaping (as convert.py) and CommonMark delimiter rules


def is_ws_char(c):
    return c == "" or c in COLLAPSIBLE or c == "\n" or unicodedata.category(c) == "Zs"


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
    return can_open and can_close


def escape_text(s):
    out = []
    n = len(s)
    for i, c in enumerate(s):
        if c in ESCAPE_ALWAYS:
            out.append("\\" + c)
        elif c == "_":
            prev = s[i - 1] if i else ""
            nxt = s[i + 1] if i + 1 < n else ""
            out.append(c if prev.isalnum() and nxt.isalnum() else "\\_")
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


def escape_line_start(line):
    m = LINE_START.match(line)
    if not m:
        return line
    tok = m.group(0)
    if tok[0].isdigit():
        return tok[:-1] + "\\" + tok[-1] + line[len(tok):]
    return "\\" + line


def replacements(s):
    """Asciidoctor's text replacements that occur in the posts, as the characters the page showed."""
    m = RX_REFUSED_REPL.search(s)
    if m:
        raise Unsupported(f"text Asciidoctor would replace (not in the subset): {m.group(0)!r}")
    s = RX_ELLIPSIS.sub("…", s)
    s = RX_APOS.sub("’", s)
    return s


def section_id(title):
    """Asciidoctor's auto-generated section id (idprefix "_", idseparator "_")."""
    t = re.sub(r"<[^>]*>", "", title)
    t = re.sub(r"&#?\w+;", "", t)
    t = re.sub(r"[^\w]+", "_", t.lower()).strip("_")
    return "_" + t


# ---------------------------------------------------------------------------------------------------
# Inline markup → a small node tree → Markdown


class Inline:
    """Converts one block's text. Asciidoctor's order: quoted text first, then macros, then replacements."""

    def __init__(self, conv):
        self.conv = conv
        self.nodes = []

    def ph(self, node):
        self.nodes.append(node)
        return f"\x00{len(self.nodes) - 1}\x00"

    def mark(self, s):
        """Replace quoted text with open/close markers, leaving the content in place (nesting works)."""
        def quoted(open_m, close_m):
            def sub(m):
                g = m.groups()
                lead = g[0] if m.re in (RX_STRONG_CON, RX_EM_CON, RX_MONO_CON) else ""
                esc = g[1] if m.re in (RX_STRONG_CON, RX_EM_CON, RX_MONO_CON) else g[0]
                role = g[2] if m.re in (RX_STRONG_CON, RX_EM_CON, RX_MONO_CON) else g[1]
                inner = g[3] if m.re in (RX_STRONG_CON, RX_EM_CON, RX_MONO_CON) else g[2]
                if esc:
                    raise Unsupported(f"backslash-escaped quoted text {m.group(0)!r}")
                if role:
                    raise Unsupported(f"[role] on quoted text {m.group(0)!r}")
                return lead + open_m + inner + close_m
            return sub
        s = RX_STRONG_UNC.sub(quoted(S_OPEN, S_CLOSE), s)
        s = RX_STRONG_CON.sub(quoted(S_OPEN, S_CLOSE), s)
        s = RX_EM_UNC.sub(quoted(E_OPEN, E_CLOSE), s)
        s = RX_EM_CON.sub(quoted(E_OPEN, E_CLOSE), s)
        if RX_MONO_CON.search(s):
            raise Unsupported(f"monospace text {RX_MONO_CON.search(s).group(0)!r}")
        return s

    def macros(self, s):
        def link(m):
            lead, esc, url, text = m.groups()
            if esc:
                raise Unsupported(f"backslash-escaped link {m.group(0)!r}")
            if lead == "link:":
                lead = ""
            text, attrs = split_link_text(text)
            for k in attrs:
                if k != "window":
                    raise Unsupported(f"link attribute {k}= on {url}")
            if text == "":
                raise Unsupported(f"link with empty text: {url}")
            return lead + self.ph(("link", url, text))
        s = RX_LINK.sub(link, s)
        if re.search(r"(?<![\w:])image:[^:\s]", s):
            raise Unsupported("inline image inside a paragraph's text")
        # a bare URL (Asciidoctor autolinks it) becomes a Markdown autolink with the same text
        s = RX_BARE_URL.sub(lambda m: self.ph(("autolink", m.group(1), "")), s)

        def xref(m):
            target, text = m.group(1).strip(), m.group(2)
            if text is None or text.strip() == "":
                raise Unsupported(f"xref without text <<{target}>>")
            if target in self.conv.heading_ids:
                return self.ph(("link", "#" + self.conv.heading_ids[target], text.strip()))
            raise Unsupported(f"xref to something that isn't a section title: <<{target}>>")
        s = RX_XREF.sub(xref, s)
        if "<<" in s or ">>" in s:
            raise Unsupported("unparsed << or >>")
        return s

    def tree(self, s):
        """Markers and placeholders → nested nodes: ("text", s) | ("strong"|"em", kids) | ("link", url, kids)."""
        root = []
        stack = [("root", root)]
        pos = 0
        for m in PH.finditer(s):
            if m.start() > pos:
                stack[-1][1].append(("text", s[pos:m.start()]))
            tok = m.group(0)
            if m.group(1) is not None:
                node = self.nodes[int(m.group(1))]
                kind, url, text = node
                stack[-1][1].append((kind, url, self.tree(text)))
            elif tok in (S_OPEN, E_OPEN):
                kids = []
                stack[-1][1].append(("strong" if tok == S_OPEN else "em", kids))
                stack.append(("strong" if tok == S_OPEN else "em", kids))
            else:
                want = "strong" if tok == S_CLOSE else "em"
                if stack[-1][0] != want:
                    raise Unsupported("overlapping emphasis (strong/em not nested)")
                stack.pop()
            pos = m.end()
        if pos < len(s):
            stack[-1][1].append(("text", s[pos:]))
        if len(stack) != 1:
            raise Unsupported("unbalanced emphasis markers")
        return root

    def render(self, nodes, before, after):
        parts = []
        for i, n in enumerate(nodes):
            joined = "".join(parts)
            prev_char = joined[-1] if joined else before
            nxt = self.peek(nodes[i + 1:], after)
            if n[0] == "text":
                parts.append(escape_text(replacements(n[1])))
            elif n[0] in ("strong", "em"):
                inner = self.render(n[1], "*", "*")
                if inner == "" or inner.strip(COLLAPSIBLE + "\n") != inner:
                    raise Unsupported(f"emphasis with whitespace at its edge: {inner!r}")
                for d in (("**", "__") if n[0] == "strong" else ("*", "_")):
                    if delimiter_ok(d, prev_char, inner, nxt):
                        parts.append(d + inner + d)
                        break
                else:
                    raise Unsupported(f"no Markdown delimiter fits around {inner!r} (between {prev_char!r} and {nxt!r})")
            elif n[0] == "link":
                url = n[1]
                if re.search(r"[\s<>()\\\x00-\x1f]", url):
                    raise Unsupported(f"link URL that Markdown can't hold: {url}")
                inner = self.render(n[2], "[", "]")
                if inner == "" or "\n" in inner or inner.strip(COLLAPSIBLE) != inner:
                    raise Unsupported(f"link text Markdown can't hold: {inner!r}")
                parts.append(f"[{inner}]({url})")
            elif n[0] == "autolink":
                if re.search(r"[\s<>\\\x00-\x1f]", n[1]):
                    raise Unsupported(f"bare URL that a Markdown autolink can't hold: {n[1]}")
                parts.append(f"<{n[1]}>")
            else:
                raise Unsupported(f"node {n[0]}")
        return "".join(parts)

    def peek(self, rest, after):
        for n in rest:
            if n[0] == "text":
                v = escape_text(replacements(n[1]))
                if v:
                    return v[0]
            elif n[0] in ("strong", "em"):
                return "*"
            elif n[0] == "autolink":
                return "<"
            else:
                return "["
        return after

    def convert(self, text):
        """AsciiDoc inline text (may span lines) → Markdown text, lines kept."""
        s = self.mark(text)
        s = self.macros(s)
        md = self.render(self.tree(s), "", "")
        return "\n".join(escape_line_start(ln.rstrip(COLLAPSIBLE)) for ln in md.split("\n"))

    def plain(self, text):
        """Inline text that must have no markup (a heading that needs an id, a title): the text with
        Asciidoctor's replacements, HTML-escaped."""
        s = self.macros(self.mark(text))
        if "\x00" in s or "\x01" in s:
            raise Unsupported(f"markup where plain text is needed: {text!r}")
        return html.escape(replacements(s), quote=False)


def split_link_text(text):
    """`[text, window=_blank]` → ("text", {"window": "_blank"}); `[text^]` → ("text", {"window": "_blank"})."""
    attrs = {}
    parts = text.split(",")
    body = [parts[0]]
    for p in parts[1:]:
        m = re.fullmatch(r"\s*(\w+)=(.*)", p)
        if m:
            attrs[m.group(1)] = m.group(2).strip()
        else:
            body.append(p)
    t = ",".join(body)
    if t.endswith("^"):
        t = t[:-1]
        attrs["window"] = "_blank"
    return t, attrs


def parse_attrlist(s):
    """`alt,500,align="center"` → (["alt", "500"], {"align": "center"}). Quotes around a value are removed."""
    pos, named = [], {}
    buf, quote, items = "", None, []
    for c in s:
        if quote:
            buf += c
            if c == quote:
                quote = None
        elif c in "\"'":
            quote = c
            buf += c
        elif c == ",":
            items.append(buf)
            buf = ""
        else:
            buf += c
    items.append(buf)
    for it in items:
        it = it.strip()
        if it == "":
            continue
        m = re.fullmatch(r"(\w+)=(.*)", it, re.S)
        if m:
            v = m.group(2).strip()
            if len(v) >= 2 and v[0] == v[-1] and v[0] in "\"'":
                v = v[1:-1]
            named[m.group(1)] = v
        else:
            if len(it) >= 2 and it[0] == it[-1] and it[0] in "\"'":
                it = it[1:-1]
            pos.append(it)
    return pos, named


# ---------------------------------------------------------------------------------------------------
# Blocks


RX_HEADING = re.compile(r"^(={1,5}) (\S.*)$")
RX_LISTING = re.compile(r"^-{4,}$")
RX_QUOTE = re.compile(r"^_{4,}$")
RX_SIDEBAR = re.compile(r"^\*{4,}$")
RX_LIST_ITEM = re.compile(r"^([*-]) (\S.*)$")
RX_HR = re.compile(r"^(---|\*\*\*|''')$")
RX_IMAGE = re.compile(r"^image::(\S+?)\[(.*)\]$")
RX_ATTR = re.compile(r"^\[(.*)\]$")
RX_TITLE = re.compile(r"^\.([^\s.].*)$")
RX_REFUSE = [
    (re.compile(r"^\.{4,}$"), "literal block"),
    (re.compile(r"^={4,}$"), "example block"), (re.compile(r"^--$"), "open block"),
    (re.compile(r"^\|==="), "table"), (re.compile(r"^(NOTE|TIP|IMPORTANT|WARNING|CAUTION):"), "admonition"),
    (re.compile(r"^//"), "comment"), (re.compile(r"^:\w[\w-]*:"), "document attribute"),
    (re.compile(r"^include::"), "include"), (re.compile(r"^\[\["), "anchor [[...]]"),
    (re.compile(r"^(\*{2,}|-{2,}|\.{1,5}) \S"), "nested or ordered list item"),
    (re.compile(r"^\d+\. "), "numbered list item"), (re.compile(r"^\s+\S"), "indented line (literal paragraph)"),
    (re.compile(r"^\+$"), "list continuation"),
    (re.compile(r"footnote:"), "footnote"), (re.compile(r"\{\w[\w-]*\}"), "attribute reference"),
    (re.compile(r" \+$"), "hard line break"), (re.compile(r"\S::(\s|$)"), "description list"),
]


def is_block_start(line):
    """A line that ends the paragraph before it (delimiters, headings, items, macros, attribute lines)."""
    return bool(RX_LISTING.match(line) or RX_QUOTE.match(line) or RX_SIDEBAR.match(line) or RX_HEADING.match(line)
                or RX_LIST_ITEM.match(line) or RX_IMAGE.match(line) or RX_ATTR.match(line) or RX_HR.match(line)
                or RX_TITLE.match(line) or RX_PASS.match(line))


class Block:
    def __init__(self, kind, md, line, blank_before):
        self.kind = kind            # "md" | "raw" | "fence" | "list" | "quote" | "hr" | "heading"
        self.md = md
        self.line = line
        self.blank_before = blank_before


class Converter:
    def __init__(self, body):
        self.lines = body.split("\n")
        self.heading_ids = {}
        self.needs_id = set()
        self.stats = {"markdown": 0, "markdown_with_inline_html": 0, "raw_html": 0, "html_kept": []}
        self.scan_headings()

    # -- pre-scan: which section titles do xrefs point at? --------------------------------------------
    def scan_headings(self):
        titles = []
        prev = None
        for ln in self.lines:
            m = RX_HEADING.match(ln)
            if m:
                titles.append(m.group(2).strip())
            elif re.fullmatch(r"-{3,}", ln) and prev is not None and abs(len(ln) - len(prev)) <= 1 and not RX_LISTING.match(prev):
                titles.append(prev.strip())
            prev = ln if ln.strip() else None
        for t in titles:
            self.heading_ids[t] = section_id(t)
        for m in RX_XREF.finditer("\n".join(self.lines)):
            if m.group(1).strip() in self.heading_ids:
                self.needs_id.add(m.group(1).strip())

    # -- the block parser ---------------------------------------------------------------------------
    def convert(self):
        self.blocks = []
        self.parse(0, len(self.lines), top=True)
        return self.join(self.blocks)

    def err(self, i, msg):
        raise Unsupported(f"body line {i + 1}: {msg}")

    def inline(self, text):
        return Inline(self).convert(text)

    def parse(self, start, end, top, container=None):
        """Parse lines[start:end] into self.blocks (or `container`)."""
        out = self.blocks if container is None else container
        i = start
        blank_before = 0
        pending = {"title": None, "attrs": {}, "attr_line": None, "id": None, "quote": None, "source": None}
        while i < end:
            ln = self.lines[i]
            if ln.strip() == "":
                if ln != "":
                    self.err(i, "whitespace-only line")
                blank_before += 1
                i += 1
                continue
            for rx, what in RX_REFUSE:
                if rx.search(ln):
                    self.err(i, f"{what}: {ln.strip()[:60]!r}")
            lead = blank_before
            blank_before = 0
            # block title / attribute lines apply to the next block
            m = RX_TITLE.match(ln)
            if m and not RX_HR.match(ln):
                if pending["title"] is not None:
                    self.err(i, "two block titles")
                pending["title"] = m.group(1).rstrip()
                pending["lead"] = lead
                i += 1
                continue
            m = RX_ATTR.match(ln)
            if m:
                self.attr_line(i, m.group(1), pending)
                if "lead" not in pending:
                    pending["lead"] = lead
                i += 1
                continue
            lead = pending.pop("lead", lead)
            # headings
            m = RX_HEADING.match(ln)
            if m:
                self.check_no_pending(i, pending)
                out.append(Block(*self.heading(i, len(m.group(1)), m.group(2).strip()), i, lead))
                i += 1
                continue
            if i + 1 < end and re.fullmatch(r"-{3,}", self.lines[i + 1]) and abs(len(self.lines[i + 1]) - len(ln)) <= 1 \
                    and not is_block_start(ln):
                self.check_no_pending(i, pending)
                out.append(Block(*self.heading(i, 2, ln.strip()), i, lead))
                i += 2
                continue
            if RX_HR.match(ln):
                self.check_no_pending(i, pending)
                out.append(Block("hr", "---", i, lead))
                i += 1
                continue
            # delimited blocks
            if RX_LISTING.match(ln):
                j = self.find_close(i, end, ln)
                lang = pending["source"]
                self.check_no_pending(i, pending, allow=("source",))
                out.append(Block("fence", self.fence(self.lines[i + 1:j], lang), i, lead))
                i = j + 1
                continue
            if RX_QUOTE.match(ln):
                j = self.find_close(i, end, ln)
                attribution = pending["quote"]
                self.check_no_pending(i, pending, allow=("quote",))
                out.append(Block("quote", self.quote(i, self.lines[i + 1:j], attribution), i, lead))
                i = j + 1
                continue
            if RX_SIDEBAR.match(ln):
                j = self.find_close(i, end, ln)
                title = pending["title"]
                self.check_no_pending(i, pending, allow=("title",))
                out.append(Block("raw", self.sidebar(i, self.lines[i + 1:j], title), i, lead))
                i = j + 1
                continue
            if RX_PASS.match(ln):
                j = self.find_close(i, end, ln)
                self.check_no_pending(i, pending)
                out.append(Block(*self.passthrough(i, self.lines[i + 1:j]), i, lead))
                i = j + 1
                continue
            m = RX_IMAGE.match(ln)
            if m:
                kind, md = self.image(i, m.group(1), m.group(2), pending)
                pending.update(title=None, attrs={}, attr_line=None, id=None, quote=None, source=None)
                out.append(Block(kind, md, i, lead))
                i += 1
                continue
            # an inline image macro that is a whole paragraph (the pages' photos): converted like a block one
            m = RX_INLINE_IMAGE.match(ln)
            if m and (i + 1 >= end or self.lines[i + 1].strip() == "" or is_block_start(self.lines[i + 1])):
                kind, md = self.image(i, m.group(1), m.group(2), pending, inline=True)
                pending.update(title=None, attrs={}, attr_line=None, id=None, quote=None, source=None)
                out.append(Block(kind, md, i, lead))
                i += 1
                continue
            m = RX_LIST_ITEM.match(ln)
            if m:
                self.check_no_pending(i, pending)
                j, md = self.list(i, end, m.group(1))
                out.append(Block("list", md, i, lead))
                i = j
                continue
            # paragraph: up to a blank line or a block start
            self.check_no_pending(i, pending)
            j = i + 1
            while j < end and self.lines[j].strip() != "" and not is_block_start(self.lines[j]) \
                    and not (j + 1 < end and re.fullmatch(r"-{3,}", self.lines[j + 1]) and abs(len(self.lines[j + 1]) - len(self.lines[j])) <= 1):
                j += 1
            text = "\n".join(self.lines[i:j])
            try:
                md = self.inline(text)
            except Unsupported as e:
                self.err(i, str(e))
            out.append(Block("md", md, i, lead))
            i = j
        if any(v for k, v in pending.items() if k != "lead" and v not in (None, {})):
            self.err(end - 1, "block title or attribute line with no block after it")

    def check_no_pending(self, i, pending, allow=()):
        for k in ("title", "attrs", "id", "quote", "source"):
            if k in allow:
                continue
            if pending[k] not in (None, {}):
                self.err(i, f"{k} ({pending['attr_line'] or pending[k]!r}) on a block that can't take it")
        pending.update(title=None, attrs={}, attr_line=None, id=None, quote=None, source=None)

    def attr_line(self, i, inner, pending):
        pending["attr_line"] = "[" + inner + "]"
        pos, named = parse_attrlist(inner)
        if pos and pos[0].startswith("#") and not named and len(pos) == 1:
            pending["id"] = pos[0][1:]
            return
        if pos and pos[0] == "quote":
            if named:
                self.err(i, f"named attribute on [quote]: {inner}")
            if len(pos) > 3:
                self.err(i, f"[quote] with more than two attributes: {inner}")
            pending["quote"] = (pos[1] if len(pos) > 1 else None, pos[2] if len(pos) > 2 else None)
            return
        if pos and pos[0] == "source":
            if len(pos) != 2 or named or not re.fullmatch(r"[\w-]+", pos[1]):
                self.err(i, f"[source] without a single language: {inner}")
            pending["source"] = pos[1]
            return
        if not pos and named and set(named) <= {"caption", "link", "window"}:
            pending["attrs"].update(named)
            return
        self.err(i, f"attribute line not in the subset: [{inner}]")

    def find_close(self, i, end, delim):
        for j in range(i + 1, end):
            if self.lines[j] == delim:
                return j
        self.err(i, f"unclosed {delim} block")

    def heading(self, i, level, title):
        """level: the number of "=" (2 for a setext title underlined with dashes). Returns (kind, md)."""
        if level < 2 or level > 5:
            self.err(i, f"heading level {level} (= is the document title)")
        if title.endswith("#"):
            self.err(i, "heading ending in #")
        if title in self.needs_id:
            try:
                text = Inline(self).plain(title)
            except Unsupported as e:
                self.err(i, str(e))
            return "raw", f'<h{level} id="{self.heading_ids[title]}">{text}</h{level}>'
        try:
            md = self.inline(title)
        except Unsupported as e:
            self.err(i, str(e))
        if "\n" in md:
            self.err(i, "heading spanning lines")
        return "heading", "#" * level + " " + md

    def fence(self, lines, lang):
        text = "\n".join(lines)
        ticks = "```"
        while re.search("^" + ticks, text, re.M):
            ticks += "`"
        return ticks + (lang or "") + "\n" + text + "\n" + ticks

    def quote(self, i, lines, attribution):
        paras = []
        k = 0
        while k < len(lines):
            if lines[k].strip() == "":
                k += 1
                continue
            if is_block_start(lines[k]):
                self.err(i + 1 + k, f"only paragraphs are supported in a quote block: {lines[k][:40]!r}")
            j = k
            while j < len(lines) and lines[j].strip() != "":
                if is_block_start(lines[j]):
                    self.err(i + 1 + j, f"only paragraphs are supported in a quote block: {lines[j][:40]!r}")
                j += 1
            try:
                paras.append(self.inline("\n".join(lines[k:j])))
            except Unsupported as e:
                self.err(i + 1 + k, str(e))
            k = j
        if not paras:
            self.err(i, "empty quote block")
        if attribution and any(attribution):
            author, source = attribution
            if not author:
                self.err(i, "[quote] with a source but no author")
            try:
                a = "— " + self.inline(author)
                if source:
                    a += "\\\n" + self.inline(source)
            except Unsupported as e:
                self.err(i, str(e))
            paras.append(a)
        return "\n>\n".join("\n".join("> " + ln for ln in p.split("\n")) for p in paras)

    def sidebar(self, i, lines, title):
        inner = []
        self.parse(i + 1, i + 1 + len(lines), top=False, container=inner)
        for b in inner:
            if b.kind not in ("md", "list"):
                self.err(b.line, "only paragraphs and lists are supported in a sidebar")
        # a Bootstrap card (owner's decision 2026-10-18), not Asciidoctor's .sidebarblock
        head = '<div class="card bg-light mb-4"><div class="card-body">'
        if title is not None:
            try:
                head += '\n<p class="card-title">' + Inline(self).plain(title) + "</p>"
            except Unsupported as e:
                self.err(i - 1, str(e))
        return head + "\n\n" + self.join(inner).rstrip("\n") + "\n\n</div></div>"

    def passthrough(self, i, lines):
        """A `++++` block: a titled YouTube/SlideShare iframe becomes a T115 embed link; other content that
        CommonMark reads as an HTML block is kept as it is (and named on stderr); the rest is refused."""
        text = "\n".join(lines).strip("\n")
        if text.strip() == "":
            self.err(i, "empty passthrough block")
        m = RX_IFRAME.match(text.strip())
        if m:
            attrs = {}
            for am in RX_HTML_ATTR.finditer(m.group(1)):
                attrs[am.group(1).lower()] = html.unescape(am.group(2)) if am.group(2) is not None else ""
            src = attrs.get("src", "")
            em = RX_EMBED_SRC.match(src)
            if not em:
                self.err(i, f"iframe that isn't a YouTube or SlideShare embed: src={src!r}")
            title = attrs.get("title", "").strip()
            if not title:
                self.err(i, "iframe without a title (the embed link needs it as its text)")
            if re.search(r"[\\\[\]\n<>]", title) or RX_REFUSED_REPL.search(title):
                self.err(i, f"iframe title Markdown can't hold as link text: {title!r}")
            url = "https://www." + (em.group(1) or em.group(2))
            return "embed", f"[{escape_text(title)}]({url})"
        first = text.lstrip().split("\n", 1)[0]
        tm = re.match(r"^<(/?)([A-Za-z][A-Za-z0-9-]*)(?=[\s/>]|$)", first)
        if not tm or tm.group(2).lower() not in HTML_BLOCK_TAGS:
            self.err(i, f"passthrough content that isn't an HTML block: {first[:60]!r}")
        if re.search(r"\n[ \t]*\n", text):
            self.err(i, "passthrough HTML with a blank line (it would end the HTML block)")
        self.stats["html_kept"].append(f"body line {i + 1}: {first[:60]}")
        return "raw", text

    def image(self, i, target, attrlist, pending, inline=False):
        """A block image macro, or (inline=True) an inline one that is a paragraph of its own."""
        pos, named = parse_attrlist(attrlist)
        named = {**pending["attrs"], **named}
        if pending["quote"] or pending["source"]:
            self.err(i, "quote/source attribute on an image")
        for k in named:
            if k not in ("alt", "link", "window", "align", "caption", "width", "height", "role"):
                self.err(i, f"image attribute {k}=")
        if len(pos) > 3:
            self.err(i, f"image with more than three positional attributes: [{attrlist}]")
        alt = named.get("alt", pos[0] if pos else None)
        if alt is None:
            alt = re.sub(r"[-_]", " ", re.sub(r"\.[^.]*$", "", target.rsplit("/", 1)[-1]))
        width = named.get("width", pos[1] if len(pos) > 1 else None)
        height = named.get("height", pos[2] if len(pos) > 2 else None)
        for k, v in (("width", width), ("height", height)):
            if v is not None and not re.fullmatch(r"\d+", v):
                self.err(i, f"image {k} that isn't a number: {v!r}")
        align = named.get("align")
        if align not in (None, "center"):
            self.err(i, f"image align={align}")
        role = named.get("role")
        if role not in (None, "left", "right"):
            self.err(i, f"image role={role}")
        if role and align:
            self.err(i, "image with both a role and an align")
        link = named.get("link")
        title = pending["title"]
        caption = named.get("caption")
        if title is not None and caption is None:
            self.err(i, "image title without caption= (Asciidoctor would number it)")
        if caption is not None and title is None:
            self.err(i, "caption= without a title")
        if inline and (title is not None or pending["id"]):
            self.err(i, "title or id on an inline image")
        if not re.match(r"^/?img/[\w./-]+$", target):
            self.err(i, f"image target outside /img/: {target}")
        if "window" in named and not link:
            self.err(i, "window= without link=")
        if pending["id"] and pending["id"] in self.needs_id:
            self.err(i, "image id that an xref points at")
        if width is None and height is None and align is None and role is None and title is None:
            if re.search(r"[\\\[\]\n]", alt):
                self.err(i, f"alt text Markdown can't hold: {alt!r}")
            md = f"![{escape_text(alt)}]({target})"
            if link:
                if re.search(r"[\s<>()\\]", link):
                    self.err(i, f"link URL Markdown can't hold: {link}")
                md = f"[{md}]({link})"
            return "md", md
        # HTML in the Markdown with Bootstrap's classes (owner's decision 2026-10-18; lazy-images.ftl
        # adds loading/decoding and externalLinks() the target at build time). A floated image (role=)
        # gets Bootstrap's float and margin utilities on the <img>, in place of asciidoctor.css's
        # .imageblock.right / .image.right.
        center = ' class="text-center"' if align == "center" else ""
        attrs = f'src="{html.escape(target)}" alt="{html.escape(alt)}"' + (f' width="{width}"' if width else "") \
            + (f' height="{height}"' if height else "")
        if title is None:
            cls = {"right": ' class="float-right ml-3 mb-3"', "left": ' class="float-left mr-3 mb-3"', None: ""}[role]
            img = f"<img {attrs}{cls}>"
            if link:
                img = f'<a href="{html.escape(link)}">{img}</a>'
            return "raw", f"<p{center}>{img}</p>"
        if role:
            self.err(i, "floated image with a caption")
        img = f'<img {attrs} class="figure-img">'
        if link:
            img = f'<a href="{html.escape(link)}">{img}</a>'
        try:
            cap = html.escape(replacements(caption), quote=False) + Inline(self).plain(title)
        except Unsupported as e:
            self.err(i, str(e))
        return "raw", (f'<figure class="figure d-block{" text-center" if align == "center" else ""}">\n{img}\n'
                       f'<figcaption class="figure-caption">{cap}</figcaption>\n</figure>')

    def list(self, i, end, marker):
        items = []
        j = i
        while j < end:
            ln = self.lines[j]
            if ln.strip() == "":
                # a blank line, then another item of the same list: still this list
                k = j
                while k < end and self.lines[k].strip() == "":
                    k += 1
                m = RX_LIST_ITEM.match(self.lines[k]) if k < end else None
                if m and m.group(1) == marker:
                    j = k
                    continue
                break
            m = RX_LIST_ITEM.match(ln)
            if m:
                if m.group(1) != marker:
                    self.err(j, f"list marker changes from {marker} to {m.group(1)}")
                items.append([j, m.group(2)])
                j += 1
                continue
            if is_block_start(ln):
                self.err(j, f"a block inside a list item: {ln[:40]!r}")
            if not items:
                self.err(j, "list without an item")
            items[-1][1] += "\n" + ln
            j += 1
        md = []
        pad = " " * (len(marker) + 1)
        for line_no, text in items:
            try:
                lines = self.inline(text).split("\n")
            except Unsupported as e:
                self.err(line_no, str(e))
            md.append(marker + " " + lines[0] + "".join("\n" + pad + ln for ln in lines[1:]))
        return j, "\n\n".join(md)

    def join(self, blocks):
        out = []
        prev = None
        for b in blocks:
            if prev is not None:
                n = b.blank_before
                spaced = ("fence", "raw", "list", "quote", "hr", "embed")
                if n == 0 and (b.kind in spaced or prev.kind in spaced):
                    n = 1
                out.append("\n" * n)
            elif b.blank_before:
                out.append("\n" * b.blank_before)
            out.append(b.md + "\n")
            prev = b
            for ln in b.md.split("\n"):
                if not ln.strip() or ln.strip() == ">":
                    continue
                if b.kind == "fence":
                    self.stats["markdown"] += 1
                elif re.match(r"^\s*<[A-Za-z/]", ln):
                    self.stats["raw_html"] += 1
                elif re.search(r"<[A-Za-z/!]", ln.replace("\\<", "")):
                    self.stats["markdown_with_inline_html"] += 1
                else:
                    self.stats["markdown"] += 1
        return "".join(out)


def convert(text):
    m = re.search(r"^~{6}[ \t]*\n", text, re.M)
    if not m:
        raise Unsupported("no ~~~~~~ header separator")
    header, body = text[:m.end()], text[m.end():]
    if "\r" in body:
        raise Unsupported("CR line endings in the body (convert to LF first)")
    if "\t" in body:
        raise Unsupported("tab character in the body")
    if "\x00" in body or "\x01" in body or "\x02" in body:
        raise Unsupported("control character in the body")
    body = body.rstrip("\n")
    conv = Converter(body)
    md = conv.convert()
    return header + md.rstrip("\n") + "\n", conv.stats


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0],
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("files", nargs="+")
    ap.add_argument("--in-place", action="store_true")
    ap.add_argument("--stdout", action="store_true")
    args = ap.parse_args()
    failed = 0
    print("file\tmarkdown\tmarkdown+inline-html\traw-html-lines", file=sys.stderr)
    for path in args.files:
        with open(path, encoding="utf-8", newline="") as f:
            text = f.read()
        try:
            out, stats = convert(text)
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
        for note in stats["html_kept"]:
            print(f"  {os.path.basename(path)}: passthrough kept as HTML, {note}", file=sys.stderr)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
