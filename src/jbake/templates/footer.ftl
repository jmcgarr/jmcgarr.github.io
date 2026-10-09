		</div>
		<div id="push"></div>

		<!-- Footer -->
    <footer>
      <div class="container">
        <div class="row">
          <div class="col-lg-8 col-md-10 mx-auto">
            <ul class="list-inline text-center">
              <li class="list-inline-item">
                <a href="https://twitter.com/SonOfGarr">
                  <span class="fa-stack fa-lg">
                    <i class="fas fa-circle fa-stack-2x"></i>
                    <i class="fab fa-twitter fa-stack-1x fa-inverse"></i>
                  </span>
                </a>
              </li>
              <li class="list-inline-item">
                <a href="https://www.linkedin.com/in/jmcgarr/">
                  <span class="fa-stack fa-lg">
                    <i class="fas fa-circle fa-stack-2x"></i>
                    <i class="fab fa-linkedin fa-stack-1x fa-inverse"></i>
                  </span>
                </a>
              </li>
              <li class="list-inline-item">
                <a href="https://github.com/jmcgarr">
                  <span class="fa-stack fa-lg">
                    <i class="fas fa-circle fa-stack-2x"></i>
                    <i class="fab fa-github fa-stack-1x fa-inverse"></i>
                  </span>
                </a>
              </li>
            </ul>
						<p class="copyright text-muted">All posts on this blog are published with a <em>Creative Commons by-nc-sa</em> license.<a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png"/></a></p>
            <p class="copyright text-muted">Copyright &copy; Mike McGarr 2009-${published_date?string("yyyy")} | Mixed with <a href="http://getbootstrap.com/">Bootstrap</a> | Generated with <a href="http://jbake.org">JBake ${version}</a></p>
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
