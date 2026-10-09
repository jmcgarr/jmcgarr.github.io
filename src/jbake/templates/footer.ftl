		</div>
		<div id="push"></div>

		<!-- Footer -->
    <footer>
      <div class="container">
        <div class="row">
          <div class="col-lg-8 col-md-10 mx-auto">
            <ul class="list-inline text-center">
              <li class="list-inline-item">
                <#-- X (T047). Font Awesome 5.3 has no X logo, so it's inlined: the "x-twitter" icon from Font Awesome Free 6
                     (https://fontawesome.com, CC BY 4.0), sized and centered like the other fa-stack-1x icons. -->
                <a href="https://x.com/SonOfGarr" target="_blank" rel="noopener noreferrer">
                  <span class="fa-stack fa-lg" aria-hidden="true">
                    <i class="fas fa-circle fa-stack-2x"></i>
                    <svg class="fa-stack-1x fa-inverse" viewBox="0 0 512 512" focusable="false" style="width:1em;height:1em;left:50%;top:50%;transform:translate(-50%,-50%)"><path fill="currentColor" d="M389.2 48h70.6L305.6 224.2 487 464H345L233.7 318.6 106.5 464H35.8L200.7 275.5 26.8 48H172.4L272.9 180.9 389.2 48zM364.4 421.8h39.1L151.1 88h-42L364.4 421.8z"/></svg>
                  </span><span class="sr-only">Mike McGarr on X (opens in a new tab)</span>
                </a>
              </li>
              <li class="list-inline-item">
                <a href="https://www.linkedin.com/in/jmcgarr/" target="_blank" rel="noopener noreferrer">
                  <span class="fa-stack fa-lg" aria-hidden="true">
                    <i class="fas fa-circle fa-stack-2x"></i>
                    <i class="fab fa-linkedin fa-stack-1x fa-inverse"></i>
                  </span><span class="sr-only">Mike McGarr on LinkedIn (opens in a new tab)</span>
                </a>
              </li>
              <li class="list-inline-item">
                <a href="https://github.com/jmcgarr" target="_blank" rel="noopener noreferrer">
                  <span class="fa-stack fa-lg" aria-hidden="true">
                    <i class="fas fa-circle fa-stack-2x"></i>
                    <i class="fab fa-github fa-stack-1x fa-inverse"></i>
                  </span><span class="sr-only">Mike McGarr on GitHub (opens in a new tab)</span>
                </a>
              </li>
            </ul>
						<p class="copyright text-muted">All posts on this blog are published with a <em>Creative Commons by-nc-sa</em> license.<a rel="license noopener noreferrer" href="http://creativecommons.org/licenses/by-nc-sa/4.0/" target="_blank"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png"/><span class="sr-only"> (opens in a new tab)</span></a></p>
            <p class="copyright text-muted">Copyright &copy; Mike McGarr 2009-${published_date?string("yyyy")} | Mixed with <a href="http://getbootstrap.com/" target="_blank" rel="noopener noreferrer">Bootstrap<span class="sr-only"> (opens in a new tab)</span></a> | Generated with <a href="http://jbake.org" target="_blank" rel="noopener noreferrer">JBake ${version}<span class="sr-only"> (opens in a new tab)</span></a></p>
          </div>
        </div>
      </div>
    </footer>


    <!-- Bootstrap core JavaScript -->
    <script src="/vendor/jquery/jquery.min.js"></script>
    <script src="/vendor/bootstrap/js/bootstrap.bundle.min.js"></script>

    <!-- Custom scripts for the clean blog look and feel -->
    <script src="/js/clean-blog.min.js"></script>

    <!-- Privacy-friendly analytics, no cookies: https://mikemcgarr.goatcounter.com (doesn't count localhost) -->
    <script data-goatcounter="https://mikemcgarr.goatcounter.com/count" async src="https://gc.zgo.at/count.js"></script>
<#-- Code highlighting, only on pages with code (T056, code-highlight.ftl) -->
<@codeHighlightJs/>
  </body>
</html>
