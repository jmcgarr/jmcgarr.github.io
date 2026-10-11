<#-- Lazy-loaded post images (T075). Images in a post's body always sit below the full-width header, so
     they can never be the page's largest paint (LCP). Loading them lazily stops them competing with the
     header image for bandwidth on a slow connection (on three-horizons-part1, a 308 KB diagram did).
     lazyImages(html) returns html with every <img> changed like this:
       - loading="lazy" is added, unless the tag already has a loading attribute (an existing one is kept)
       - decoding="async" is added, unless the tag already has a decoding attribute
     Both go right after "<img". Running it twice gives the same result (no duplicate attributes).
     Only post.ftl uses it, on the post body. Images in the templates (header, masthead, footer) and on
     pages (About, Talks) are left alone, and the feed (feed.ftl) uses the raw body and doesn't. -->
<#function lazyImages html>
  <#local html = html?replace(r"(?i)(<img)(?=\s)(?![^>]*\sdecoding\s*=)", r'$1 decoding="async"', "r")>
  <#local html = html?replace(r"(?i)(<img)(?=\s)(?![^>]*\sloading\s*=)", r'$1 loading="lazy"', "r")>
  <#return html>
</#function>
