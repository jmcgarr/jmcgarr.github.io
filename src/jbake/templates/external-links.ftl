<#-- Off-site links open in a new tab (T058, issue #6). The owner's decision (2026-10-09): do it at build
     time, in the templates, without editing posts, so every post and page (and every new one) is covered.
     externalLinks(html) returns html with every off-site <a> changed like this:
       - target="_blank" is added, unless the link already has a target (an existing target is kept)
       - rel gets "noopener noreferrer": added when there is no rel, appended when rel lacks noopener
         (rel="nofollow" becomes rel="nofollow noopener noreferrer"), left alone when it has noopener
       - a visually hidden hint is added before </a> for screen readers, only when the link opens a new tab
     Off-site means href starts with http://, https:// or // and the host isn't www.mikemcgarr.com or
     mikemcgarr.com. Relative links, /paths, #anchors and mailto: are never touched. Running it twice gives
     the same result (no duplicate attributes or hints). header.ftl includes this file; post.ftl, page.ftl
     and index.ftl use it. The feed (feed.ftl) uses the raw body and doesn't. Links written in the templates
     themselves (footer, share links) carry the same attributes and hint by hand. -->
<#-- Java regexes. \x22 is a double quote and \x27 a single quote (raw strings can't hold both).
     offsite: a lookahead placed right after "<a", true when this tag's href points off the site. -->
<#assign externalLinkOffsite = r"(?=\s)(?=[^>]*?\shref\s*=\s*[\x22\x27]?\s*(?:https?:)?//(?!(?:www\.)?mikemcgarr\.com(?![\w.-])))">
<#assign externalLinkHint = r'<span class="sr-only"> (opens in a new tab)</span>'>
<#function externalLinks html>
  <#-- 1. target="_blank" where the tag has no target -->
  <#local html = html?replace(r"(?i)(<a" + externalLinkOffsite + r"(?![^>]*\starget\s*=)[^>]*?)(\s*>)", r'$1 target="_blank"$2', "r")>
  <#-- 2. rel without noopener: append "noopener noreferrer" to its value -->
  <#local html = html?replace(r"(?i)(<a" + externalLinkOffsite + r"[^>]*?\srel\s*=\s*([\x22\x27]))(?![^\x22\x27>]*\bnoopener\b)([^\x22\x27>]*?)(?=\2)", r"$1$3 noopener noreferrer", "r")>
  <#-- 3. rel="noopener noreferrer" where the tag has no rel -->
  <#local html = html?replace(r"(?i)(<a" + externalLinkOffsite + r"(?![^>]*\srel\s*=)[^>]*?)(\s*>)", r'$1 rel="noopener noreferrer"$2', "r")>
  <#-- 4. the screen reader hint before </a>, on links with target="_blank" that don't have it yet -->
  <#local html = html?replace(r"(?is)(<a" + externalLinkOffsite + r"(?=[^>]*\starget\s*=\s*[\x22\x27]?_blank)[^>]*>)((?:(?!<a[\s>]|</a>|\(opens in a new tab\)).)*)(</a>)", r"$1$2" + externalLinkHint + "$3", "r")>
  <#return html>
</#function>
<#-- The same for template output: <@externalLinksIn><#include "masthead.ftl"></@externalLinksIn> -->
<#macro externalLinksIn><#local html><#nested></#local>${externalLinks(html)}</#macro>
