<#-- Code blocks (T056, T136): styles, syntax highlighting, a language label and a copy button, only on pages
     whose body has a <pre> block. Every other page gets nothing from this file (no extra bytes, no extra requests).
     header.ftl includes this file and calls <@codeHighlightCss/>; footer.ftl calls <@codeHighlightJs/>.
     The highlighter is PrismJS, self-hosted in assets/vendor/prism/ (see the header of prism.min.js). -->
<#assign codeHighlightBody = (content.body)!"">
<#assign hasCodeBlock = codeHighlightBody?contains("<pre>") || codeHighlightBody?contains("<pre ")>
<#macro codeHighlightCss><#if hasCodeBlock>
    <link href="/css/code.css" rel="stylesheet">
</#if></#macro>
<#-- The inline script runs before Prism (deferred) and puts every code block in the form Prism expects,
     <pre><code class="language-x">, without touching post content:
     - the converted WordPress posts' raw HTML: <pre class="prettyprint language-x"> with no <code>; bare <pre>
     - Markdown fenced code: already <pre><code class="language-x">, used as is
     A block with no language is treated as markup if it starts with "<", otherwise as a shell command.
     Then (T136) it labels each block with its language (data-lang, which code.css shows top right; a block
     whose language was only guessed gets no label) and adds a copy button, when the page is served over
     https or from localhost (the Clipboard API exists only there). Without JavaScript the blocks are styled
     by code.css as plain <pre>, with no colours, label or button. -->
<#macro codeHighlightJs><#if hasCodeBlock>
    <script>
      (function () {
        var canCopy = !!(navigator.clipboard && navigator.clipboard.writeText);
        var icon = '<svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true" focusable="false">' +
          '<rect x="5.5" y="5.5" width="9" height="9" rx="1.5"/>' +
          '<path d="M10.5 5.5V3a1.5 1.5 0 0 0-1.5-1.5H3A1.5 1.5 0 0 0 1.5 3v6A1.5 1.5 0 0 0 3 10.5h2.5"/></svg>';
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
          var m = /\blang(?:uage)?-([\w-]+)/.exec(code.className) || /\blanguage-([\w-]+)/.exec(pre.className);
          if (m) {
            if (!/\blang(uage)?-/.test(code.className)) code.className += " language-" + m[1];
            pre.setAttribute("data-lang", m[1]);
          } else {
            code.className += " language-" + (/^\s*</.test(code.textContent) ? "markup" : "bash");
          }
          if (canCopy) addCopyButton(pre, code);
        }
        function addCopyButton(pre, code) {
          var button = document.createElement("button");
          button.type = "button";
          button.className = "copy-code";
          button.setAttribute("aria-label", "Copy code to clipboard");
          button.setAttribute("aria-live", "polite");
          button.title = "Copy";
          button.innerHTML = icon;
          button.addEventListener("click", function () {
            navigator.clipboard.writeText(code.textContent).then(function () {
              button.classList.add("is-copied");
              button.textContent = "Copied";
              button.setAttribute("aria-label", "Copied");
              setTimeout(function () {
                button.classList.remove("is-copied");
                button.innerHTML = icon;
                button.setAttribute("aria-label", "Copy code to clipboard");
              }, 1200);
            });
          });
          pre.appendChild(button);
          pre.className += " has-copy";
        }
      })();
    </script>
    <script src="/vendor/prism/prism.min.js" defer></script>
</#if></#macro>
