<#-- Code blocks (T056): styles and syntax highlighting, only on pages whose body has a <pre> block.
     Every other page gets nothing from this file (no extra bytes, no extra requests).
     header.ftl includes this file and calls <@codeHighlightCss/>; footer.ftl calls <@codeHighlightJs/>.
     The highlighter is PrismJS, self-hosted in assets/vendor/prism/ (see the header of prism.min.js). -->
<#assign codeHighlightBody = (content.body)!"">
<#assign hasCodeBlock = codeHighlightBody?contains("<pre>") || codeHighlightBody?contains("<pre ")>
<#macro codeHighlightCss><#if hasCodeBlock>
    <link href="/css/code.css" rel="stylesheet">
</#if></#macro>
<#-- The inline script runs before Prism (deferred) and puts every code block in the form Prism expects,
     <pre><code class="language-x">, without touching post content:
     - legacy WordPress posts: <pre class="prettyprint language-x"> with no <code> (and one misspelled
       "languague-groovy"); bare <pre>
     - AsciiDoc listing blocks (----): <pre> with no language
     - Markdown fenced code: already <pre><code class="language-x">, used as is
     A block with no language is treated as markup if it starts with "<", otherwise as a shell command. -->
<#macro codeHighlightJs><#if hasCodeBlock>
    <script>
      (function () {
        var pres = document.querySelectorAll("pre");
        for (var i = 0; i < pres.length; i++) {
          var pre = pres[i], code = pre.querySelector("code");
          if (!code) {
            code = document.createElement("code");
            while (pre.firstChild) code.appendChild(pre.firstChild);
            pre.appendChild(code);
          }
          var last = code.lastChild;
          if (last && last.nodeType === 3) last.nodeValue = last.nodeValue.replace(/\s+$/, "");
          if (/\blang(uage)?-/.test(code.className)) continue;
          var m = /\blanguagu?e-([\w-]+)/.exec(pre.className);
          code.className += " language-" + (m ? m[1] : /^\s*</.test(code.textContent) ? "markup" : "bash");
        }
      })();
    </script>
    <script src="/vendor/prism/prism.min.js" defer></script>
</#if></#macro>
