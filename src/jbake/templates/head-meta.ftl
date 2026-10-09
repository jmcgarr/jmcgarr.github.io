<#--
  Per-page description, canonical URL, and social card tags (T041, T042). header.ftl includes this in <head>.

  - Description: the page's `summary` when it has one, otherwise the opening words of the page, as plain text.
    Index, archive, and tag pages get their own fixed wording. At most 155 characters, cut at a word.
  - URLs are absolute, built from site.host without a trailing slash (the preview sets it to
    http://localhost:8080/ with one; production has none).
  - og:image is the page's header image, as its 1440px variant (scripts/masthead-variants.py makes one for
    every masthead in use). A remote masthead (http…) is used as it is, since it has no variants.
  - Every variable here starts with "meta" so it can't collide with masthead.ftl's (masthead, pageTitle, …).
-->
<#-- Text of an HTML fragment, without markup that isn't prose (code, embeds, headings, block titles). -->
<#function metaPlainText html>
  <#local t = html?replace(r"(?s)<!--.*?-->", " ", "r")>
  <#local t = t?replace(r"(?is)<(script|style|pre|iframe|object|noscript|figcaption|h[1-6])\b.*?</\1\s*>", " ", "r")>
  <#local t = t?replace(r'(?is)<div class="title">.*?</div>', " ", "r")>
  <#-- Inline tags vanish ("<a>link</a>." stays "link."); any other tag separates words. -->
  <#local t = t?replace(r"(?i)</?(a|abbr|b|cite|code|del|em|font|i|ins|kbd|mark|q|s|small|span|strike|strong|sub|sup|tt|u)\b[^>]*>", "", "r")>
  <#local t = t?replace(r"<[^>]*>", " ", "r")>
  <#local t = t?replace(r"\[/?caption[^\]]*\]", " ", "r")>
  <#local t = t?replace(r"&nbsp;|&#160;|&#xA0;", " ", "ri")>
  <#return t?replace(r"[\s ]+", " ", "r")?trim>
</#function>
<#-- Safe inside a double-quoted attribute. Entities already in the text are kept; a bare & is escaped. -->
<#function metaAttr text>
  <#return text?replace(r"&(?!#?[A-Za-z0-9]+;)", "&amp;", "r")?replace('"', "&quot;")?replace("<", "&lt;")?replace(">", "&gt;")>
</#function>
<#-- Length as a reader sees it: an entity such as &#8217; counts as one character. -->
<#function metaLength text>
  <#return text?replace(r"&#?[A-Za-z0-9]+;", "x", "r")?length>
</#function>
<#-- At most `max` characters, cut at a space (so never inside an entity), ending in an ellipsis when cut. -->
<#function metaTrim text max>
  <#if metaLength(text) lte max><#return text></#if>
  <#local out = "">
  <#list text?substring(0, [text?length, max * 8]?min)?split(" ") as word>
    <#local next = out?has_content?then(out + " " + word, word)>
    <#if metaLength(next) gt max - 1><#break></#if>
    <#local out = next>
  </#list>
  <#return out?replace(r"[\s,;:.\-–—]+$", "", "r") + "…">
</#function>
<#function metaDescriptionOf html>
  <#return metaTrim(metaAttr(metaPlainText(html)), 155)>
</#function>

<#assign metaHost = (config.site_host!"https://www.mikemcgarr.com")?remove_ending("/")>
<#assign metaKind = (content.type)!"">
<#assign metaSiteDescription = "Personal website of Mike McGarr - Passionate engineering leader and manager.">
<#assign metaDescription = "">
<#if metaKind == "masterindex">
  <#assign metaPath = "" metaTitle = "Mike McGarr" metaDescription = metaSiteDescription>
<#elseif metaKind == "archive">
  <#assign metaPath = config.archive_file!"archive.html" metaTitle = "Blog Archive">
  <#assign metaDescription = "Every post on Mike McGarr's blog, by month, newest first.">
  <#if published_posts?? && published_posts?has_content>
    <#assign metaDescription = "Every post on Mike McGarr's blog since ${published_posts?last.date?string('yyyy')}, by month, newest first.">
  </#if>
<#elseif metaKind == "tag">
  <#-- JBake's tag page path. Tag sanitizing (T039) changes the tag name itself when posts are read, so this
       stays right. (Looking the tag up in `tags` instead makes the bake about 6 times slower.) -->
  <#assign metaPath = "${config.tag_path!'tags'}/${tag}${config.output_extension!'.html'}" metaTitle = "Tag: ${tag}">
  <#assign metaCount = (tag_posts![])?size>
  <#assign metaDescription = "${metaCount} ${(metaCount == 1)?then('post', 'posts')} on Mike McGarr's blog tagged “${metaAttr(tag)}”.">
<#else>
  <#assign metaPath = (content.uri)!"" metaTitle = (content.title)!"Mike McGarr">
  <#if (content.summary)?has_content>
    <#assign metaDescription = metaDescriptionOf(content.summary)>
  <#elseif (content.body)?has_content>
    <#assign metaDescription = metaDescriptionOf(content.body)>
  </#if>
</#if>
<#if !metaDescription?has_content || metaDescription == "…">
  <#assign metaDescription = metaSiteDescription>
</#if>
<#assign metaUrl = "${metaHost}/${metaPath?url_path('UTF-8')}">

<#-- The same header image masthead.ftl shows. Keep the defaults in step with masthead.ftl and archive.ftl. -->
<#if masthead??>
  <#assign metaMasthead = masthead>
<#elseif (content.masthead)?has_content>
  <#assign metaMasthead = content.masthead>
<#elseif metaKind == "post">
  <#assign metaMasthead = "saocom-falcon-launch.jpg">
<#elseif metaKind == "archive">
  <#assign metaMasthead = "london-view.jpg">
<#else>
  <#assign metaMasthead = "lone-cypress-2.jpg">
</#if>
<#if metaMasthead?starts_with("http")>
  <#assign metaImage = metaMasthead>
<#else>
  <#-- Same forms as masthead.ftl (T037): a site path ("/img/x.jpg") is used as it is, a plain name lives in
       /img/masthead/. "/img/masthead/../x.jpg" → "/img/x.jpg", so the absolute URL has no dot segments. -->
  <#assign metaImagePath = (metaMasthead?starts_with("/"))?then(metaMasthead, "/img/masthead/${metaMasthead}")?replace(r"/[^/]+/\.\./", "/", "r")>
  <#assign metaImage = metaHost + metaImagePath?keep_before_last(".") + "-1440." + metaImagePath?keep_after_last(".")>
</#if>
    <meta name="description" content="${metaDescription}">
    <link rel="canonical" href="${metaUrl}">
    <meta property="og:site_name" content="Mike McGarr">
    <meta property="og:type" content="${(metaKind == 'post')?then('article', 'website')}">
    <meta property="og:title" content="${metaAttr(metaTitle)}">
    <meta property="og:description" content="${metaDescription}">
    <meta property="og:url" content="${metaUrl}">
    <meta property="og:image" content="${metaAttr(metaImage)}">
<#if metaKind == "post" && (content.date)??>
    <meta property="article:published_time" content="${content.date?string('yyyy-MM-dd')}">
</#if>
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:site" content="@SonOfGarr">
    <meta name="twitter:creator" content="@SonOfGarr">
