<#--
  A tag as the author wrote it (T057), for showing to readers. head-meta.ftl includes this, so every template has it.

  With tag.sanitize=true (T039) JBake turns each space in a tag into a hyphen while it reads the posts, and keeps only
  that form: templates get "acceptance-test", never "acceptance test". The hyphenated form stays in URLs (it's the tag
  page's file name); headings, descriptions, and the Topics page show the tag with its spaces again.

  JBake doesn't keep the original, so a hyphen is turned back into a space unless the tag is listed below: tags whose
  hyphens were written by the author (key: the tag as JBake gives it; value: the tag as written in the posts).
  A new tag with a hyphen of its own goes here. `scripts/tag-redirects.py --check` (run by CI) compares every tag
  page with the posts' tags and names any tag that is shown wrong.
-->
<#assign tagNamesAsWritten = {"apt-get": "apt-get", "oh-my-zsh": "oh-my-zsh", "shell-fu": "shell-fu"}>
<#function tagName tag>
  <#return tagNamesAsWritten[tag]!tag?replace("-", " ")>
</#function>
