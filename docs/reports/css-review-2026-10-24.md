# CSS review and recommendations (T022)

**Branch**: `docs/T022-css-review` | **Created**: 2026-10-24 | **Status**: Awaiting owner approval

A review of every stylesheet the site serves, with recommendations that keep the look and feel. Written for
[T022](../archive/04-AUTHORS-DESK.md) after the owner said (2026-10-24) they have no font changes in mind but are open to
suggestions, and want the CSS "cleaned up, simplified and a bit more standardized" without breaking the look.
Also covers the owner's request of the same day to modernize the code blocks. **Nothing in this PR changes the
site**: no stylesheet, template or build file is touched. The owner picks what to implement; each item says whether
it is visually neutral or a visible change, and how to prove it.

Measured on a clean bake of `main` at `469cdc3` (310 pages, 252 of them full pages with the theme; the rest are
redirect stubs).

## Summary

- **Six stylesheets, five of them on every page**, in this order: `bootstrap.min.css` (141 KB), Font Awesome
  `all.min.css` (49 KB), `fonts.css` (16 KB), the compiled `clean-blog.css` (10 KB), `extra.css` (5 KB); `code.css`
  (2 KB) joins on the 16 pages with code. 221 KB raw, **38 KB gzipped**, all render-blocking (223 / 39 KB on code
  pages). The site's own CSS (fonts + clean-blog + extra + code) is 33.6 KB raw, 7.4 KB gzipped.
- **A third of the theme CSS matches nothing.** Of the 87 rules in the compiled `clean-blog.css`, **25 match no
  element on any page** (6 more are added by JavaScript at runtime and are live). The dead rules are the whole
  contact form partial, the whole Bootstrap button partial, the masthead's `.post-heading` block (the only place
  Lora is named), two home-page rules whose selectors don't match `index.ftl`, and two unused theme classes. About
  230 of the 681 SCSS lines produce nothing visible, including a 50-line mixin nobody includes. `extra.css` has 0
  dead rules; `code.css` has none (its `.token` rules are filled by Prism).
- **Two files overlap.** `extra.css` (176 lines, 31 rules, grown over 5 revival tasks) is theme-grade CSS in a second
  file: it overrides `_post.scss` and `_masthead.scss` by selector and repeats Bootstrap's grey `#6c757d` three times
  as a literal (four times) because `_variables.scss` still carries Bootstrap 4.0's `$gray-600` (`#868e96`) under a
  4.1.3 vendor build. Two more properties are set in one partial and overridden in another.
- **All four `!important`s are in dead rules.** Live CSS has none.
- **Fonts:** Open Sans is used by exactly three things: the nav (800), the masthead subheading (300), and the post
  date inside it (300 italic). Everything else is the system stack. A page downloads one 43 KB variable-font file
  (plus a 14 KB italic on posts); `fonts.css` has 30 `@font-face` rules for 20 files because it mirrors Google's
  per-script subsets, 16 of which no English page ever fetches. No `font-display`. Lora is referenced by one dead rule
  and loaded nowhere.
- **Font Awesome costs 181 KB per first visit for four glyphs** (menu bars, three circles, LinkedIn, GitHub):
  `all.min.css` plus two woff2 files, measured in the server log, on every page. 2.5 MB of it is published.
- **Headline recommendations:** (A) delete the dead rules and fold `extra.css` into the partials: neutral, one
  request fewer, about a third less theme CSS; (B) one token set (colours, breakpoints) with Bootstrap 4's values;
  (C) inline the four icons as SVG as `footer.ftl` already does for X, and compile `fonts.css` and `code.css` from
  the same Sass tree: five stylesheets become two; (D) fonts: keep them (S1 below), optionally trim `fonts.css` to
  the Latin subsets; code blocks: keep Prism, modernize the styling and add a copy button and language label (own
  section). Verification: headless Chrome screenshots + pixel diff, proven below: 12/12 pages identical run to run.

## Inventory

### Stylesheets

| File | Lines | Raw / gzip | What it styles | Loaded by | Verdict |
|---|---|---|---|---|---|
| `src/jbake/assets/vendor/bootstrap/css/bootstrap.min.css` (4.1.3) | 7 | 140,936 / 20,969 | Reboot, grid, navbar, utilities | `header.ftl:19`, every page | Keep. The HTML uses 43 of its 1,289 classes (3%); a hand-built subset is possible later (C5) |
| `src/jbake/assets/vendor/fontawesome-free/css/all.min.css` (5.3.1) | 5 | 48,649 / 10,492 | 4 icons + stack helpers | `header.ftl:22`, every page | Replace with inline SVG (C2) |
| `src/jbake/assets/css/fonts.css` | 274 | 16,326 / 1,857 | 30 `@font-face` rules for Open Sans 300, 300 italic, 800 | `header.ftl:28`, every page | Keep the rules; trim or fold (fonts section) |
| `src/scss/` → `build/jbake/css/clean-blog.css` | 681 → 483 | 10,042 / 2,429 | The Clean Blog theme + Asciidoctor-era element rules | `header.ftl:31`, every page | Delete the dead third (A1) |
| `src/jbake/assets/css/extra.css` | 176 | 5,116 / 2,097 | Photo credit, archived comments, phone wrapping, archive/tag lists, Topics page, a11y fixes, responsive masthead | `header.ftl:34`, every page | Fold into partials (A2) |
| `src/jbake/assets/css/code.css` | 105 | 2,129 / 1,001 | `pre`, `pre > code`, Prism token colours | `code-highlight.ftl:8`, 16 pages | Keep separate (conditional); compile from Sass (B3) |

Compiled by `compileSass` in `build.gradle:148-163` (Dart Sass, expanded output, no source map). The partials and
what they amount to in the compiled file:

| Partial | Lines | Compiled rules | Live | Dead | Note |
|---|---|---|---|---|---|
| `_variables.scss` | 40 | – | – | – | 31 variables; 9 referenced by partials (`$white`, `$gray-200`, `$gray-300`, `$gray-600`, `$gray-800`, `$gray-900`, `$primary`, `$primary-hover`, `$font-family-system`), and `$gray-300` only by dead `_contact` |
| `_mixins.scss` | 70 | – | – | – | 5 mixins; `button-variant` (50 lines) never included, `serif-font` only by a dead rule |
| `_global.scss` | 90 | 13 | 11 | 2 | `.section-heading`, `.caption` |
| `_navbar.scss` | 97 | 15 | 15 | 0 | 6 of them only once `clean-blog.min.js` adds `is-fixed` / `is-visible` |
| `_masthead.scss` | 86 | 15 | 8 | 7 | the whole `.post-heading` block; `.site-heading` appears in 5 selector lists and nowhere in HTML |
| `_post.scss` | 44 | 7 | 4 | 3 | `> a > .post-subtitle`, `> .post-meta > a` (+ hover) |
| `_content.scss` | 28 | 4 | 4 | 0 | figure caption and card, used by 1 page each |
| `_contact.scss` | 63 | 8 | 0 | 8 | the contact form left with T019/T020 |
| `_footer.scss` | 13 | 3 | 3 | 0 | |
| `_bootstrap-overrides.scss` | 30 | 4 | 0 | 4 | `.btn*`: no button on the site |
| `_legacy-asciidoctor.scss` | 102 | 18 | 17 | 1 | `ul li ol` |

### What a page loads (served locally, Chrome 155, from the `http.server` log of 14 page loads)

| Request | Pages | Bytes |
|---|---|---|
| `bootstrap.min.css`, `all.min.css`, `fonts.css`, `clean-blog.css`, `extra.css` | all | 221,069 raw |
| `webfonts/fa-solid-900.woff2` + `fa-brands-400.woff2` | all | 67,400 + 65,316 |
| `fonts/open-sans/open-sans-normal-latin.woff2` (300 and 800 share this variable font) | all (preloaded) | 42,964 |
| `fonts/open-sans/open-sans-italic-latin.woff2` (post date) | posts | 14,176 |
| `code.css` + `vendor/prism/prism.min.js` | 16 pages | 2,129 + 30,475 |

### Classes the HTML uses

101 distinct class names across the 252 pages: 33 Bootstrap-only, 11 Font Awesome, 2 Bootstrap + Font Awesome
(`sr-only*`), 8 Bootstrap + theme, 6 theme, 12 `extra.css`, 5 theme + `extra.css`, and **24 defined nowhere** (`navbar-toggler-right` from Bootstrap 4.0-beta,
`share-links`, `giscus`, the WordPress `alignleft`/`alignright`/`aligncenter`/`size-*`/`wp-image-*`, prettify's
`prettyprint`/`linenums`, and `language-*`). The WordPress classes are unstyled: those images float by their
`align="left|right"` attribute, which `extra.css:61` relies on. Leave them alone; styling them would be a visible
change on 7 posts.

## Findings

Each finding says where (file:line in the source), what, the evidence, and whether fixing it is **neutral** (no
pixel changes) or **visible**. "0 pages" means the selector matched no element in any of the 310 baked pages,
checked by a selector matcher written for this review (Sources).

| # | Where | What | Evidence | Fix is |
|---|---|---|---|---|
| C1 | `src/scss/_contact.scss` (whole file, 63 lines) | Contact-form styles for a form the site no longer has | `.floating-label-form-group*`: 0 pages. `contact_me*.js` went in T020. Holds 2 of the site's 4 `!important`s | Neutral |
| C2 | `src/scss/_bootstrap-overrides.scss` (whole file, 30 lines) | `.btn`, `.btn-primary`, `.btn-lg` | 0 pages; no `<button class="btn">` or `.btn` link anywhere. Holds the other 2 `!important`s | Neutral (a future post using `.btn` would get Bootstrap's default button instead of the theme's) |
| C3 | `src/scss/_masthead.scss:53-85` | The `.post-heading` block (7 compiled rules), and `.site-heading` in 5 selector lists | `masthead.ftl:53` renders `.page-heading` for every page type; posts put the date in `.subheading em`. `.post-heading`, `.site-heading`: 0 pages. Line 72 is the only reference to Lora | Neutral |
| C4 | `src/scss/_mixins.scss:5-53, 59-64` | `button-variant` mixin never included; `serif-font` only used by C3; `background-cover` wraps one declaration | `grep -rn button-variant src/scss` finds only the definition. `button-variant` uses the global `darken()` Dart Sass deprecated, so including it would start warning | Neutral |
| C5 | `src/scss/_global.scss:53-68` | `.section-heading`, `.caption` | 0 pages | Neutral |
| C6 | `src/scss/_post.scss:17-20, 27-35` | `.post-preview > a > .post-subtitle` and `.post-preview > .post-meta > a` | `index.ftl:19-21` puts `p.post-subtitle` *beside* the `<a>`, not inside it, and the date has no link. 0 pages. The home page's summaries have never had the theme's weight 300 | Neutral to delete. Making the summary rule match would be **visible** (D1) |
| C7 | `src/scss/_legacy-asciidoctor.scss:72-74` and the `ul li ol`, `ol li ul` parts of lines 62-65 | Nested-list rules for combinations no page has | 0 pages (`ul li ul`: 175 pages, `ol li ol`: 5) | Neutral |
| C8 | `src/scss/_variables.scss` | 22 of 31 variables unused (the theme palette `$blue`…`$cyan`, `$secondary`…`$dark`, five greys, `$black`). `$gray-600` is `#868e96` (Bootstrap 4.0) while the vendored Bootstrap is 4.1.3 (`#6c757d`) | `#6c757d` appears 4 times in `extra.css` (lines 37, 43, 106, 146), plus `#e9ecef` (= `$gray-200`), `#212529` and `rgba(33,37,41,.3)` (= `$gray-900`), `#0085a1` (= `$primary`) as literals. T046 overrode `.post-meta` with `#6c757d` because `$gray-600` failed contrast | Neutral to add tokens with today's values. Changing `$gray-600` itself is **visible** (D3) |
| C9 | `_global.scss:50` vs `_legacy-asciidoctor.scss:90`; `_post.scss:26` vs `extra.css:146`; `_masthead.scss:5` vs `extra.css:71,164`; `_masthead.scss:26` vs `:28-30` | Same selector + property declared twice, later wins | Normalized CSS shows `blockquote color: #868e96` and `#6f6f6f`; `.post-preview > .post-meta color: #868e96` and `#6c757d`. `header.masthead` is opened three times. The masthead padding media query repeats the identical `150px 0` | Neutral: keep the winning value, delete the loser |
| C10 | `src/jbake/assets/css/extra.css` (176 lines) | Theme-grade rules outside the theme: photo credit (T046), comments (T045), phone wrapping (T103), archive lists (T104), Topics (T057), a11y (T046), responsive masthead (T071). The README calls it "small site-specific additions" | `git log` shows 6 commits since 2026-10-05 and 5 from 2014-2018. It loads last, so folding it must keep its rules after `_legacy-asciidoctor` | Neutral if order is preserved |
| C11 | `src/jbake/assets/css/code.css` | Plain CSS with its own colour set, outside the Sass tree; conditional load is right | Its 11 token rules are live only after Prism runs | Neutral to compile from Sass; keep conditional |
| C12 | `src/jbake/assets/css/fonts.css` | 30 rules, 20 files (336 KB published) mirroring Google's subsets; no `font-display`; latin normal is preloaded (`header.ftl:27`), latin italic (every post) is not | Server log: only the two latin files are ever requested | Trimming rules is neutral for English text; removing files needs Rule 1 approval; `font-display` is **visible during load** |
| C13 | `_navbar.scss:31,55`, `_masthead.scss:28,47,77`, `_post.scss:37`, `_legacy-asciidoctor.scss:30`, `extra.css:74,109,115,127,167,172` | 13 media queries with literal breakpoints: `768px` ×6, `992px` ×3, `576px` ×2, `1280px`, `374.98px`; 9 of them with the IE-era `only screen and` prefix | Bootstrap 4's are 576/768/992/1200. `1280px` is not one, but it is paired with the `<link rel=preload media>` queries in `masthead.ftl:43-45`, so it must stay (or both move) | Tokens: neutral. Moving 1280 to 1200: **visible** between 1200 and 1279 px (D6) |
| C14 | `_navbar.scss:56-69,93`, `_global.scss:41,70,87`, `_contact.scss:21,25,38`, `code.css:11,23,25`, `extra.css:141` | Vendor prefixes: `-webkit-transition/transform/backface-visibility` (2013 GPU hack), `::-moz-selection`, `-webkit-overflow-scrolling: touch` (no effect since iOS 13), `-moz-tab-size`, `-webkit-hyphens`, `-webkit-box-shadow` (dead) | Dart Sass adds no prefixes; the comment at `_mixins.scss:54` says they were kept from the 2018 autoprefixer run | Neutral for the ones listed. Keep `-webkit-font-smoothing` (non-standard, no unprefixed form) and `-webkit-box-decoration-break` (Safari; check caniuse before dropping) |
| C15 | `_contact.scss:21-22`, `_bootstrap-overrides.scss:22-23` | The 4 `!important`s | All in C1/C2 | Neutral (gone with C1/C2) |
| C16 | `footer.ftl:16`, `footer.ftl:37`, `masthead.ftl:48` | Inline styles: the X icon's SVG sizing; `style="border-width:0"` on the CC badge; the masthead's three custom properties | Bootstrap's reboot already sets `img { border-style: none }`, so the CC style does nothing. The custom properties are per-page data and belong inline | Neutral: move the SVG rule to `_footer.scss`, drop the CC attribute, keep the custom properties |
| C17 | `_navbar.scss:65`, `_masthead.scss:26`, `_global.scss:9,14,54`, `_post.scss:13,23,40` | Magic numbers (`top: -67px` is the nav height; 150px masthead padding; a px/em/rem mix) | Theme inheritance | Neutral to name them; changing any value is **visible** |
| C18 | `src/scss/*.scss` | Only `_content` and `_legacy-asciidoctor` say what they style and why; the theme partials carry Start Bootstrap's one-liners. `clean-blog.scss` doesn't say why the order matters beyond the last line | | Neutral |
| C19 | `header.ftl:22`, `menu.ftl:7`, `footer.ftl:15-32` | Font Awesome 5.3.1 for `fa-bars`, `fa-circle` ×3, `fa-linkedin`, `fa-github`; `footer.ftl:16` already inlines the X logo as SVG (T047) | 48.6 KB CSS + 132.7 KB of woff2 per first visit, every page; 2.5 MB published (`eot`/`svg`/`ttf`/`woff` copies no modern browser requests) | Replacing with inline SVG: **low visible risk** (a glyph rendered from a font vs the same path as SVG can differ by a pixel; the diff will flag the footer and the menu button) |
| C20 | `header.ftl:19-37` | 5 render-blocking stylesheets on every page, 6 on code pages | 223 KB raw / 38 KB gz | Fewer files: neutral |
| C21 | `src/jbake/assets/js/clean-blog.min.js` | Half the theme script binds jQuery handlers for the dead contact form (C1) | | Neutral (JS, not CSS; same cleanup) |
| C22 | `extra.css:58` | `a.bare` (an Asciidoctor class) in the wrapping rule | 0 pages | Neutral |
| C23 | `_global.scss:29-37`, `_legacy-asciidoctor.scss:31-36` | `h5`, `h6` in selector lists | 0 pages (harmless element selectors) | Leave |
| C24 | `README.md:246` | Documents `extra.css` as the place for additions; after A2 it should point at the partials | | Docs |

## Recommendations

Ordered by safety. Effort: S (an hour), M (half a day), L (more). "Proof" is how to show the look did not change
(see "How to verify").

### A. Visually neutral cleanups (one PR)

| # | Item | Findings | Effort | Risk | Proof |
|---|---|---|---|---|---|
| A1 | Delete the dead rules: `_contact.scss` and `_bootstrap-overrides.scss` (whole files), the `.post-heading` block and the `.site-heading` selectors, `.section-heading`, `.caption`, the two `.post-preview` rules, `ul li ol`, `a.bare`, the `button-variant` and `serif-font` mixins, the unused variables. About 25 rules, 109 declarations, 230 SCSS lines, ~3.5 KB raw of CSS | C1-C7, C15, C22 | S | None: nothing matches them | Pixel diff 0 on the page set; normalized CSS diff shows only deletions |
| A2 | Fold `extra.css` into partials, keeping each block's order after `_legacy-asciidoctor`: masthead rules (photo credit, responsive image, overflow) into `_masthead.scss`; archive/tag and Topics rules into a new `_lists.scss`; archived comments into `_comments.scss`; skip link and wrapping into `_accessibility.scss` or `_global.scss`. Delete `extra.css` from `header.ftl` (its URL `/css/extra.css` is a published asset: leave a one-line file or add it to `docs/baseline/expected-removals.txt` with the owner's OK, Rule 1) | C10, C20, C24 | M | Low: cascade order is the only trap | Normalized diff: same (media, selector, declaration) set minus the C9 duplicates; pixel diff 0 |
| A3 | Collapse the double declarations to the winning value: `blockquote` colour `#6f6f6f`, `.post-meta` colour `#6c757d`, one `header.masthead` block, drop the repeated masthead padding | C9 | S | None | Normalized diff shows only the losing lines removed |
| A4 | Tokens for the literals with today's values: `$text-muted: #6c757d`, `$asciidoctor-red: #7a2518`, `$overlay-30: rgba(33,37,41,.3)`, `$nav-height: 67px`, `$masthead-padding: 150px`; reuse `$gray-200`, `$gray-900`, `$primary` where the literal equals them. Do **not** change `$gray-600` (D3) | C8, C17 | S | None if values are copied exactly | Normalized diff: identical |
| A5 | Drop the prefixes no supported browser needs (C14 list); keep `-webkit-font-smoothing` and `-webkit-box-decoration-break` | C14 | S | None in current browsers | Normalized diff: only prefixed lines removed; pixel diff 0 |
| A6 | Inline styles to CSS: `.fa-stack svg { width:1em; height:1em; left:50%; top:50%; transform:translate(-50%,-50%) }` in `_footer.scss`; delete the CC badge's `style` | C16 | S | None | Pixel diff 0 |
| A7 | Structure and naming: a header comment per partial (what it styles, which template, which task added it); `clean-blog.scss` lists the cascade order and why; `_mixins.scss` shrinks to `transition-all` or goes (replace `sans-serif-font` with a `$font-family-open-sans` variable next to `$font-family-system`); README "To change the site's styles" updated | C18, C24 | S | None | `./gradlew clean bake` passes; diff is comments and names |
| A8 | Trim `clean-blog.min.js` to the navbar part, or keep the file (it is 1.1 KB) | C21 | S | Low: test the scroll-up nav reveal (T020's test) | Manual |

### B. Standardization, neutral by construction

| # | Item | Effort | Risk | Proof |
|---|---|---|---|---|
| B1 | One breakpoint map and mixin: `$grid-breakpoints: (sm: 576px, md: 768px, lg: 992px, xl: 1200px)` and `@mixin media-up($name)` in `_mixins.scss` (Bootstrap 4's own SCSS isn't in the repo; the compiled vendor CSS is). Replace the 13 literal queries; drop `only screen and`. Keep `1280px` and `374.98px` as named tokens (`$masthead-lg`, `$phone-narrow`) with a comment pointing at `masthead.ftl:43-45` | S | None (same numbers) | Normalized diff: media strings change only by the `only screen and` prefix |
| B2 | One colour set: Bootstrap 4.1.3's greys as the names (`$gray-600: #6c757d`) is the clean end state, but it moves the masthead's fallback colour (D3). Neutral path: keep `$gray-600` for the masthead only, as `$masthead-fallback: #868e96`, and use the 4.1.3 values everywhere else | S | None on the neutral path | Pixel diff 0 |
| B3 | Compile `code.css` and `fonts.css` from `src/scss/` as extra entries (`code.scss`, `fonts.scss`) so all CSS has one source and one token set; `compileSass` already compiles every non-partial in `src/scss`. Same URLs, same bytes | M | Low | `diff` of the compiled files against today's: whitespace only |
| B4 | A guard in `reviewChanges` or CI: fail if `src/jbake/assets/css/` gains a hand-written file other than the ones listed (like the T043 `clean-blog.css` check). Optional | S | None | |

### C. Delivery improvements

| # | Item | Effort | Visible risk | Proof |
|---|---|---|---|---|
| C1 | Fewer stylesheets: after A2 and B3 a page loads `bootstrap.min.css` + `clean-blog.css` (fonts folded in as a partial at the top), plus `code.css` on 16 pages: 5 requests become 2 (3 with code) | included above | None | Pixel diff 0; `grep -c 'rel="stylesheet"' build/jbake/index.html` = 2 |
| C2 | Font Awesome → inline SVG for the 4 icons, as `footer.ftl:16` already does for X (paths from Font Awesome Free 5.3.1's `svgs/`, CC BY 4.0, keep the attribution comment). Saves 181 KB per first visit and a render-blocking file. The `vendor/fontawesome-free/` files are published URLs: keep them published (nothing links them) or remove with Rule 1's steps | S | **Low**: same shapes, but font hinting vs SVG can move a pixel. The diff will flag the footer and menu button; check them by eye at 1280 and 375 | Pixel diff: differences only inside the icon boxes |
| C3 | `font-display`: today the browser blocks text up to 3 s if a font is slow. `swap` shows the system font first (a flash on slow connections), `optional` may skip Open Sans entirely on slow connections. The latin file is small and preloaded, so the gain is small | S | **Visible during load** (FOUT), not in a final screenshot | Owner's call; not part of the cleanup |
| C4 | Preload `open-sans-italic-latin.woff2` on posts (every post date uses it) | S | None | Network tab: the italic file starts with the HTML |
| C5 | A Bootstrap subset built from Bootstrap 4.1.3's SCSS (reboot, grid, navbar, the 43 classes and the JS-added `collapsing`/`show`), 21 KB gz → roughly 6 KB. PurgeCSS needs Node, which the owner ruled out (T043), so this is a hand-kept import list | L | **Medium**: easy to miss a utility; every page must be diffed | Pixel diff 0 on all 252 pages (the scripts take a slug list; `find build/jbake -name '*.html'` gives it) |
| C6 | Inline `code.css` (1 KB gz) in a `<style>` on the 16 code pages instead of a request | S | None | Pixel diff 0 on the 3 code sample pages |

### D. Visible changes the owner might choose (separate PRs, each its own decision)

| # | Item | What changes | Recommendation |
|---|---|---|---|
| D1 | Make the home page summaries match the theme's intent (`.post-subtitle` at weight 300) | Lighter summaries on the home page | No: the current look has been live for years; delete the rule (A1) |
| D2 | Drop Open Sans for the system stack everywhere | Nav, masthead subheading, post dates (fonts section, S2) | Owner's call; see fonts section |
| D3 | `$gray-600` → Bootstrap 4.1.3's `#6c757d` | The masthead's background before the photo loads (and if it fails) gets slightly darker | Fine to do, but it is a visible change: say so in the PR |
| D4 | Modernize code blocks | See the "Code blocks" section | Yes, as its own PR |
| D5 | Align the masthead's `1280px` breakpoint to Bootstrap's `1200px` | Laptops 1200-1279 px wide load the full image instead of the 1440 variant | No: the 1280 split was chosen in T071 for image sizes, not layout |

## Fonts (T022)

### Current state

| Text | Family | Weight / style | Where set | Pages |
|---|---|---|---|---|
| Body, headings, lists, footer, archive, Topics | System stack (`-apple-system, BlinkMacSystemFont, "Avenir Next", Avenir, "Nimbus Sans L", Roboto, "Noto Sans", "Segoe UI", Arial, Helvetica, "Helvetica Neue", sans-serif`) | 400; headings 800 | `_variables.scss:40`, `_global.scss:8,35` | all |
| Nav brand and links, "Menu" toggler | Open Sans | 800 (toggler 800, links 800 uppercase 12px) | `_navbar.scss:9-28` | all |
| Masthead subheading (page subtitle) | Open Sans | 300 | `_masthead.scss:39-46` | 75 (About, Talks, posts with a subtitle) |
| Post date in the masthead | Open Sans | 300 italic (`<em>` inside `.subheading`) | `masthead.ftl:60` | 74 posts |
| Code | `SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace` | 400 | `code.css:12` | 16 |
| `.post-heading .meta` | Lora | 300 italic | `_masthead.scss:68-72` | **0** (dead, C3) |

Files: 20 woff2 (336 KB, SIL OFL, `src/jbake/assets/fonts/open-sans/`), one variable font per style and script
subset; the 300 and 800 upright weights are the same file. A page fetches `open-sans-normal-latin.woff2` (43 KB,
preloaded) and, on posts, `open-sans-italic-latin.woff2` (14 KB). No request leaves the origin (T073). `fonts.css`
is 274 lines because it reproduces Google's 30 subset rules; browsers fetch a subset only when a page contains its
characters, which for the nav, subheadings and dates means Latin only.

### Suggestions

| | Suggestion | Bytes / requests | Visible? | Trade-off |
|---|---|---|---|---|
| S1 | **Keep the fonts as they are.** Remove the Lora mixin and the dead `.meta` rule, replace the `sans-serif-font` mixin with a `$font-family-open-sans` variable, fold `fonts.css` into the compiled sheet (B3/C1), preload the italic on posts (C4) | −1 request; same bytes | No | None. The nav/subheading contrast with the system body text is part of the Clean Blog look |
| S2 | **System stack everywhere.** Delete the three Open Sans uses; `fonts.css`, the preload and the 20 files go (Rule 1: keep the files published or list them in `expected-removals.txt`) | −2 requests, −57 KB per first visit, −16 KB CSS; 388 KB less published | **Yes, on every page**: the nav (on macOS SF Pro Heavy instead of Open Sans ExtraBold, on Windows Segoe UI), the subheading on 75 pages, the italic date on 74 posts. Open Sans is wider and rounder; side by side it is obvious, to a visitor it reads as "the same site, slightly different nav" | One family throughout (the body already moved to the system stack in 2024). Gives up the one typographic contrast the theme has left |
| S3 | **Trim to the Latin subsets.** Keep 6 of the 30 rules (latin and latin-ext for the three styles) and 4 of the 20 files | `fonts.css` 16 KB → ~3 KB (1.9 → ~0.6 KB gz); 237 KB less published | No for today's content (a nav, subtitle or date with Greek, Cyrillic, Hebrew or Vietnamese characters would fall back to the system font). Prove with a scan of `.subheading` and nav text for code points outside U+0000-024F | Removing the 16 files removes published URLs (Rule 1); the neutral form keeps the files and drops only the rules |

**Recommendation:** S1, with S3 if the owner approves the 16 file removals (or S3's neutral form). Not S2: the
saving is one cached 43 KB file, and the cost is the site's remaining typographic contrast. If the owner wants a
single family, S2 is clean and simple, and the verification below will show exactly which regions change. No font
changes in this PR; T022 stays open until the owner picks.

## Code blocks

The owner (2026-10-24): "I would like to modernize the code snippet blocks I show on my page. I suspect there are
some modern libraries that can make this easier."

### Current state

- **How a block is produced.** A Markdown fenced block is rendered by flexmark in the bake as
  `<pre><code class="language-x">`. The 13 legacy WordPress posts keep raw HTML in their Markdown: 22 bare `<pre>`
  and 11 `<pre class="prettyprint language-…">` (Google code-prettify classes from the WordPress era; `linenums` on
  2 of them is ignored, nothing renders line numbers). No `[sourcecode]` or `brush:` remnants are left. Inline code
  (`*:not(pre) > code`, `_legacy-asciidoctor.scss:94`) is rare: one post has inline code and no block.
- **Blast radius:** 16 pages with 46 blocks: 3 posts with fenced code (13 blocks; `git-for-fork-pullrequest`,
  `openjdk-6-install-lucid`, `ruby-broke-my-path`, all 2012-2014) and 13 legacy posts with 33 raw `<pre>` blocks.
  Whatever changes must keep the 33 legacy blocks readable without editing the posts (Rule 3).
- **Highlighting** is client-side only (T056, issue #15, done 2026-10-08/09, see
  [`archive/00-REVIVAL.md`](../archive/00-REVIVAL.md)): `src/jbake/templates/code-highlight.ftl` adds, only when the
  page body contains `<pre`, `code.css` in the head and, at the end of the body, a 15-line inline script that wraps
  bare `<pre>` content in `<code>`, maps the prettify classes, guesses `markup` or `bash` for blocks with no language,
  and then loads `vendor/prism/prism.min.js` with `defer`. Without JavaScript the blocks are styled but not coloured.
- **Prism** is 1.30.0 (March 2025), self-hosted as core + 11 language components (markup, clike, javascript, java,
  groovy, bash, ini, properties, json, yaml, diff), no plugins: 30.5 KB, 11.1 KB gz. Upstream accepts only security
  PRs while v2 is developed, and no v2 has been released, so 1.30 is a frozen line: it works, but it will not get new
  grammars or features.
- **`code.css`** (105 lines) already does the basics well: GitHub-like light palette, every token colour ≥ 4.5:1
  (T056), `overflow-x: auto` so long lines scroll inside the block, 0.875/0.9375 rem monospace, 6px radius, no
  wrapping. What is missing against a 2026 blog: a language label, a copy button, a `ui-monospace` entry in the
  font stack, a dark variant, and the inline code box (`#f2f2f2` with a `#cccccc` border, from Asciidoctor) looks
  dated next to the blocks.

### Options

| Option | How it fits the JBake + Gradle build | Cost | Build-time or client | Maintenance | Visual impact |
|---|---|---|---|---|---|
| O1 **Keep Prism 1.30, add its plugins** (Toolbar + Copy to Clipboard, Show Language, Line Numbers) and retune the theme | Append the plugin files to `prism.min.js` as T056 did; their CSS into `code.css` | +4-6 KB JS, +1 KB CSS | Client | Frozen upstream; plugins are stable | Visible by design: buttons and labels appear |
| O2 **highlight.js 11** | Same conditional load; swap `prism.min.js` for `highlight.min.js` + a language pack; token classes are `hljs-*`, so the token section of `code.css` is rewritten | Core ~8 KB gz + languages; similar to Prism | Client | Active | Colours change unless re-themed; auto-detection is a mixed blessing on the 22 bare blocks (the inline script's guess is more predictable) |
| O3a **Build-time: Shiki** (TextMate grammars, VS Code themes, dual light/dark through CSS variables, no client JS) | Needs Node at build time. Options: the `com.github.node-gradle.node` plugin downloads a Node for the build (works in CI and locally) and a Gradle task runs a script over `build/jbake/**/*.html` after `bake`, replacing each `<pre>` with Shiki's output; or a pre-commit step. Either way it adds Node to a build the owner deliberately kept Java-only (T043, 2026-10-09: "no Node or Ruby") and a post-processing step JBake has no hook for | 0 KB client JS, +~2 KB CSS; build +Node download | Build | Active; a second toolchain to keep current | Best-looking output; a new theme, so visible by design |
| O3b **Build-time on the JVM** | No mature option. flexmark has no highlighter (T056 note). `org.jetbrains:markdown` is a Markdown parser, not a highlighter. JHighlight (codelibs fork, 2.0.0 in 2026) covers Java, HTML, XML and a few more but not bash, yaml or diff, which this site needs. Lumis4J (`io.roastedroot:lumis4j`, Tree-sitter grammars via WASM, 70+ languages) is the one modern JVM project, but pre-1.0 and marked work-in-progress. Running highlight.js or Shiki inside GraalJS from Gradle is possible but heavy (tens of MB) and slow | | Build | Pre-1.0 or narrow | |
| O4 **CSS-only modernization + 30 lines of our own JS**: no library change. Clean `code.css` (tokens from `_variables`, `ui-monospace` stack, padding, radius, label, scrollbar), a copy button and a language label added by the inline script that already knows each block's language, inline code restyled, a `prefers-color-scheme: dark` palette for the blocks only | Edits to `code-highlight.ftl` and `code.css` (or `code.scss` after B3) | +~1 KB CSS, +~1 KB inline JS | Client (label and button need JS, as the highlighting already does) | Ours; nothing new to update | Visible by design, and contained to the 16 pages |

### Recommendation

**O4, keeping Prism 1.30 as the highlighter** (O1 without its plugins: the copy button and label are simpler to own
than Prism's Toolbar plugin, and the plugin line is frozen anyway). It fits a static, low-traffic site with 16 code
pages: no new toolchain, no Node, no build hook, nothing changes for the 33 legacy blocks except their styling, and
it is reversible. Revisit Shiki if the build ever gains Node for another reason, or Lumis4J when it reaches 1.0.

What "modern" would look like here, consistent with Clean Blog:

- **Font:** `ui-monospace, SFMono-Regular, Menlo, Consolas, "Liberation Mono", monospace`, 0.875 rem (0.9375 from
  768px, as now), line-height 1.6, `tab-size: 4`.
- **Block:** 1 rem / 1.25 rem padding, 8px radius, 1px `#e1e4e8` border (as now) or none, `overflow-x: auto`
  with no wrapping (commands and diffs stay exact), a thin scrollbar via `scrollbar-width: thin`, 1.5 rem below.
- **Language label:** small uppercase grey text top-right (`pre[data-lang]::before { content: attr(data-lang) }`),
  set by the inline script from the class it already computes; hidden for blocks it only guessed.
- **Copy button:** top-right, visible on hover/focus (always on touch), `navigator.clipboard.writeText`, "Copied"
  for a second, `aria-label`. About 20 lines of vanilla JS in `code-highlight.ftl`, no dependency.
- **Inline code:** `#f2f2f2` background, no border, `0.1em 0.3em` padding, 4px radius, 0.9em; same text colour.
- **Dark mode for the blocks only:** `@media (prefers-color-scheme: dark)` with a GitHub-dark-dimmed palette for
  `pre` and the tokens. The site has no dark mode; dark blocks on a light page are a common, deliberate look. If the
  owner prefers blocks that stay light, drop this item.
- **Line numbers:** skip. The 2 `linenums` blocks never had them on this site, and numbers break copy-paste.

### Verification for the code-block change

A code-block change is visible by design, so the test is not pixel equality:

1. Before/after screenshots at 1280 and 375 of: `git-for-fork-pullrequest` (fenced Markdown, 7 blocks),
   `git-tip-branch-your-previous-commit` (bare `<pre>`, 7 blocks), `mastering-jenkins-making-bulk-updates-to-jobs`
   (prettify `language-groovy`), `improving-my-shell-fu-oh-my-zsh` (the issue #15 page), and `dvcs-ci` (inline code,
   no block); the owner approves the after images.
2. **Every `<pre>` on the site still renders as a block with the same text**: zero differences in this script's output
   between the before and after builds (it strips tags inside each `<pre>`, so Prism's `<span>`s don't matter):

   ```sh
   # before: ./gradlew clean bake on main, then
   python3 -I -c 'import glob,html,re,sys
   for f in sorted(glob.glob("build/jbake/**/*.html",recursive=True)):
     for i,m in enumerate(re.findall(r"<pre\b[^>]*>(.*?)</pre>",open(f,encoding="utf-8").read(),re.S)):
       print(f, i, repr(html.unescape(re.sub(r"<[^>]+>","",m)).strip()))' > /tmp/pre-before.txt
   # after: same command to /tmp/pre-after.txt, then
   diff /tmp/pre-before.txt /tmp/pre-after.txt && echo "pre text unchanged"
   ```
3. The 16 code pages and no others load `code.css` and Prism:
   `grep -l 'css/code.css' build/jbake/**/*.html | wc -l` = 16, as today.
4. On the 375 shots, no page scrolls sideways (`phone-shots.mjs` prints "fits" per page).

## How to verify "no change in look and feel"

Available on this machine (checked 2026-10-24): Google Chrome 155 at `/Applications/Google Chrome.app`, Node 21.6.2,
`gtimeout` (coreutils) and `jwebserver`; system Python 3.9 and Homebrew 3.11/3.12 have **no** Pillow, Playwright,
bs4 or lxml; no ImageMagick. Pillow installs cleanly into a venv. The repo already has the tools from T102 in
`scripts/to-markdown/`: `screenshots.sh` (headless Chrome, 1280px), `phone-shots.mjs` (Chrome over the DevTools
protocol at a real 375px viewport; `--window-size=375` would only crop a wider layout on macOS) and `pixeldiff.py`
(Pillow). **Never use port 8080**: it is the owner's preview.

### Method 1 (preferred, proven): screenshots + pixel diff

Run this on `main` into `before/`, then on the branch into `after/`, from the repo root. The page set covers every
template and the content shapes that must not change: home, About, Talks, Archive, Topics, a tag page, a 2009 post,
a 2013 post with floated images (`align=` attributes), a post with archived comments, headings and a blockquote, the
newest post, a fenced-code post and a legacy `<pre>` post.

```sh
./gradlew clean bake
python3 -m http.server 8765 --directory build/jbake &     # not 8080
PAGES="index.html about.html talks.html archive.html tags/index.html tags/devops.html \
  justifying-dual-monitors the-phoenix-project understanding-culture three-horizons-part1 \
  git-for-fork-pullrequest git-tip-branch-your-previous-commit"
PORT=8765 sh scripts/to-markdown/screenshots.sh /tmp/css-before $PAGES                       # 1280px
node --experimental-websocket scripts/to-markdown/phone-shots.mjs /tmp/css-before http://localhost:8765 $PAGES  # 375px
# optional third width (tablet): the same Chrome command screenshots.sh runs, with --window-size=820,9000
# ... switch to the branch, ./gradlew clean bake, repeat into /tmp/css-after, then:
python3 -m venv /tmp/pdv && /tmp/pdv/bin/pip install -q Pillow
/tmp/pdv/bin/python scripts/to-markdown/pixeldiff.py /tmp/css-before /tmp/css-after /tmp/css-diff
kill %1
```

`pixeldiff.py` prints `identical (0 pixels differ)` per page and exits 1 on any difference, writing a red-on-grey
image per differing page into `/tmp/css-diff` so the change can be located. **Proven on 2026-10-24**: two runs of
these exact commands over 6 pages × 2 widths (home, About, Archive, Topics, `the-phoenix-project`,
`git-for-fork-pullrequest`) gave 12/12 `identical (0 pixels differ)`, so the method is deterministic and a
nonzero result means a real change. `screenshots.sh` shoots a 1280×9000 window; `phone-shots.mjs` shoots the full
page height. Note the archived comments (`<details>`) are closed in a screenshot: for a change to `.archived-*` rules
also check one open by eye.

### Method 2: computed-style dump (optional, for a change that moves no pixels but you want proof per element)

`phone-shots.mjs` shows how to drive Chrome over the DevTools protocol; a sibling script can call
`Runtime.evaluate` with
`[...document.querySelectorAll('*')].map(e => [e.tagName, e.className, JSON.stringify(getComputedStyle(e))])` and
write one JSON file per page, to `diff` before/after. Not needed when Method 1 is clean; useful to confirm a
suspected `:hover`/`:focus` difference, which screenshots don't exercise.

### Method 3 (minimum): normalized diff of the compiled CSS

For a refactor that should only delete or move rules, show that the set of (media, selector, declaration) triples
only lost the intended lines. The script below (stdlib only, kept in the scratchpad for this review) prints one
sorted line per triple; run it on both builds and `diff`:

```sh
python3 -I cssnorm.py build/jbake/css/clean-blog.css build/jbake/css/extra.css build/jbake/css/code.css > /tmp/css-norm-before.txt
# ... after the change (same three files, or fewer):
diff /tmp/css-norm-before.txt /tmp/css-norm-after.txt
```

```python
# cssnorm.py
import re, sys
def block(t, i):
    d = 0
    for j in range(i, len(t)):
        d += (t[j] == "{") - (t[j] == "}")
        if d == 0: return t[i + 1:j], j + 1
def rules(css, media=""):
    css = re.sub(r"/\*.*?\*/", "", css, flags=re.S); i = 0
    while (j := css.find("{", i)) >= 0:
        head = " ".join(css[i:j].split()); body, i = block(css, j)
        if head.startswith("@media"): yield from rules(body, head[6:].strip())
        elif head.startswith("@"): yield (media, head, " ".join(body.split()))
        else:
            for s in head.split(","):
                for d in body.split(";"):
                    if d.strip(): yield (media, s.strip(), " ".join(d.split()))
out = {f"{m or '-'} | {s} | {d}" for f in sys.argv[1:] for m, s, d in rules(open(f, encoding="utf-8").read())}
print("\n".join(sorted(out)))
```

Today's three files normalize to 431 lines; 109 of them belong to the dead rules in A1 (48 to `_contact.scss`
alone), 16 to the JS-added `#mainNav.is-*` rules and 39 to Prism's `.token` rules, which must stay. A1's diff should
show exactly those 109 lines (plus the C9 losers) removed and nothing added; A2's should show no change at all.

## Proposed tasks

Next free ID across `04-AUTHORS-DESK.md`, `05-REVIEW-FLOW.md` and `06-GRADLE-PLUGIN.md` is **T134** (T133 is the
highest in use; checked with `grep -rhoE '\bT[0-9]{3}\b' docs | sort -u | tail -1`). Fonts need no new task: T022 is
that decision.

```
- [ ] T134 [IMP] CSS cleanup, visually neutral: delete the dead rules (A1), fold `extra.css` into the partials (A2),
      collapse the double declarations (A3), name the literal colours, sizes and breakpoints (A4, B1, B2 neutral
      path), drop the obsolete prefixes (A5), move the two inline styles (A6), header comment per partial and README
      (A7) (`src/scss/`, `src/jbake/assets/css/extra.css`, `src/jbake/templates/header.ftl`, `footer.ftl`, `README.md`)
  - **Test:** Method 1 on the 12-page set at 1280 and 375: `pixeldiff.py` exits 0 with `identical` for every page.
    Method 3: the normalized diff removes only the A1/A3 lines and adds none. `grep -c '!important'
    build/jbake/css/*.css` is 0 for every file. `./gradlew clean bake` prints no Sass warning. `/css/extra.css`
    still answers (stub) or is listed in `docs/baseline/expected-removals.txt` with the owner's approval.
- [ ] T135 [IMP] CSS delivery: compile `fonts.css` and `code.css` from `src/scss/` (B3), fold the font rules into
      `clean-blog.css` so every page loads two stylesheets (C1), replace Font Awesome with the four inline SVGs (C2),
      preload the italic on posts (C4), inline `code.css` on code pages (C6) (`src/scss/`,
      `src/jbake/templates/header.ftl`, `menu.ftl`, `footer.ftl`, `code-highlight.ftl`)
  - **Test:** `grep -c 'rel="stylesheet"' build/jbake/index.html` = 2 and `build/jbake/blog/three-horizons-part1.html`
    = 2; no page references `fontawesome` (`grep -rl fontawesome build/jbake --include='*.html'` is empty). Method 1:
    every page identical except inside the footer icons and the menu button, which the owner checks by eye in
    `/tmp/css-diff`. The nav collapse and the scroll-up reveal still work at 375 and 1280 (T020's test).
- [ ] T136 [IMP] Modernize code blocks (option O4): keep Prism 1.30, restyle `code.css` from the site's tokens
      (`ui-monospace` stack, padding, radius, scrollbar, inline code), add a language label and a copy button in
      `code-highlight.ftl`, and a `prefers-color-scheme: dark` palette for the blocks if the owner wants it
      (`src/jbake/assets/css/code.css` or `src/scss/code.scss`, `src/jbake/templates/code-highlight.ftl`)
  - **Test:** The five sample pages above at 1280 and 375, approved by the owner. The `<pre>` text script reports
    zero differences across the whole build. `grep -l 'css/code.css' build/jbake/**/*.html | wc -l` = 16. On the 375
    shots every page "fits". The copy button copies the block's text in Chrome and Safari; without JavaScript the
    block is still a styled, readable `<pre>`.
```

## Sources

Files read: `src/scss/*.scss`, `src/jbake/assets/css/{fonts,extra,code}.css`, `src/jbake/templates/{header,menu,
masthead,footer,code-highlight,index,post,page,archive,tags}.ftl`, `src/jbake/assets/js/clean-blog.min.js`,
`build.gradle` (compileSass, bake, reviewChanges), `README.md` ("To change the site's styles"),
`docs/04-AUTHORS-DESK.md` (T022), `docs/archive/00-REVIVAL.md` (T016, T019, T020, T043, T045, T046, T047, T056, T057,
T071, T073, T102, T103, T104, issue #15), `scripts/to-markdown/{screenshots.sh,phone-shots.mjs,pixeldiff.py}`.

Commands run (from the worktree, all on the bake of `469cdc3`):

- `./gradlew clean bake` (5 s, no warnings); sizes and gzip via a short Python script over `build/jbake`.
- Selector inventory: a stdlib Python matcher (tag, class, id, attribute, descendant/child/sibling combinators,
  `:first-child`, `:last-child`, `:not()`, `:has()`; pseudo-elements and interaction pseudo-classes stripped) run over
  the 310 baked pages for every rule in the three compiled stylesheets. JS-added classes (`is-fixed`, `is-visible`,
  `token`, `collapsing`, `show`) were identified from `clean-blog.min.js`, Prism and Bootstrap's JS and counted as
  live.
- Class usage: `class="…"` attributes across the build against the selectors of each stylesheet.
- Request log: `python3 -m http.server 8765 --directory build/jbake`, 14 page loads by headless Chrome.
- Screenshots: `screenshots.sh` and `phone-shots.mjs` twice over 6 pages, `pixeldiff.py` between the runs (12/12
  identical); the images stayed in the scratchpad.
- Code blocks: `grep -l '^```' src/jbake/content/blog/*.md` (3), `grep -ohE '<pre[^>]*>' …` (33 in 13 posts),
  `grep -l '<pre' build/jbake/blog/*.html` (16), Prism's header comment and `languages.*=` names in `prism.min.js`.
- Tooling: `which node npx chromium google-chrome`, `ls /Applications`, `python3 -c "import playwright"` and the
  same for Pillow, bs4, lxml under the system and Homebrew Pythons.
- Web (2026-10-24): PrismJS GitHub README (security-only PRs while v2 is developed; 1.30.0 is the latest npm release,
  March 2025); Maven Central listings for `org.codelibs:jhighlight` and `io.roastedroot:lumis4j`.
