<#-- Video and slide deck embeds (T115). The owner's decision (2026-10-17): in a post, an embed is a Markdown
     link on a paragraph of its own, with the title as the link text, and the template turns it into the
     player at build time. So a post stays readable as text and needs no raw HTML:
       [Video: Ops School](https://www.youtube.com/watch?v=0gx7oQaAy6g)
       [Slides: Continuous Delivery](https://www.slideshare.net/slideshow/embed_code/8341276)
     embeds(html) returns html with every paragraph that is exactly one such link, <p><a href="URL" …>TEXT</a></p>
     (whitespace around the link allowed, a title attribute allowed), changed like this:
       - YouTube: href youtube.com/watch?v=ID, youtu.be/ID or youtube.com/embed/ID (other query parameters
         are ignored) becomes <div class="embed-responsive embed-responsive-16by9"><iframe
         class="embed-responsive-item" title="TEXT" src="https://www.youtube.com/embed/ID" allowfullscreen
         loading="lazy"></iframe></div>
       - SlideShare: href slideshare.net/slideshow/embed_code/N or …/embed_code/key/KEY becomes the same, with
         embed-responsive-4by3 (the closest to SlideShare's old 427×356 player) and that embed URL as src
     TEXT, already HTML-escaped by the Markdown renderer, becomes the iframe's title (what a screen reader
     announces). Bootstrap's .embed-responsive classes size the player to the column at every width. A link
     inside a longer paragraph, a link to any other site, and everything else in the body are left alone.
     Running it twice gives the same result: the output has no <p><a> left to match.
     post.ftl uses it on the post body, innermost, before lazyImages() and externalLinks() (which only touch
     <img> and <a>). head-meta.ftl feeds it the body before taking the meta description, which drops
     <iframe>s, so the link text can't leak into the description. The feed (feed.ftl) uses the raw body, so
     feed readers get the titled link instead of a player: that's intended. -->
<#function embeds html>
  <#-- 1. YouTube: the video ID is group 1, the link text group 2 -->
  <#local html = html?replace(r'(?i)<p>\s*<a href="https?://(?:www\.|m\.)?(?:youtube\.com/(?:watch\?(?:[^"&]*&(?:amp;)?)*v=|embed/)|youtu\.be/)([\w-]+)[^"]*"[^>]*>([^<]*)</a>\s*</p>', r'<div class="embed-responsive embed-responsive-16by9"><iframe class="embed-responsive-item" title="$2" src="https://www.youtube.com/embed/$1" allowfullscreen loading="lazy"></iframe></div>', "r")>
  <#-- 2. SlideShare: "N" or "key/KEY" is group 1, the link text group 2 -->
  <#local html = html?replace(r'(?i)<p>\s*<a href="https?://(?:www\.)?slideshare\.net/slideshow/embed_code/((?:key/)?[\w-]+)[^"]*"[^>]*>([^<]*)</a>\s*</p>', r'<div class="embed-responsive embed-responsive-4by3"><iframe class="embed-responsive-item" title="$2" src="https://www.slideshare.net/slideshow/embed_code/$1" allowfullscreen loading="lazy"></iframe></div>', "r")>
  <#return html>
</#function>
