      <!-- Navigation -->
      <nav class="navbar navbar-expand-lg navbar-light fixed-top" id="mainNav">
        <div class="container">
          <a class="navbar-brand" href="/">Mike McGarr</a>
<#-- The button's "bars" icon is from Font Awesome Free 5.3.1 (https://fontawesome.com, CC BY 4.0), inlined as SVG
     since T135 so no icon font is loaded; sized by `#mainNav .navbar-toggler svg` in src/scss/_navbar.scss. Kept at
     column 0 so the bake outputs nothing here, not even indentation. -->
          <button class="navbar-toggler navbar-toggler-right" type="button" data-toggle="collapse" data-target="#navbarResponsive" aria-controls="navbarResponsive" aria-expanded="false" aria-label="Toggle navigation">
            Menu
            <svg viewBox="0 0 448 512" aria-hidden="true" focusable="false"><path fill="currentColor" d="M16 132h416c8.837 0 16-7.163 16-16V76c0-8.837-7.163-16-16-16H16C7.163 60 0 67.163 0 76v40c0 8.837 7.163 16 16 16zm0 160h416c8.837 0 16-7.163 16-16v-40c0-8.837-7.163-16-16-16H16c-8.837 0-16 7.163-16 16v40c0 8.837 7.163 16 16 16zm0 160h416c8.837 0 16-7.163 16-16v-40c0-8.837-7.163-16-16-16H16c-8.837 0-16 7.163-16 16v40c0 8.837 7.163 16 16 16z"/></svg>
          </button>
          <div class="collapse navbar-collapse" id="navbarResponsive">
            <ul class="navbar-nav ml-auto">
              <li class="nav-item">
                <a class="nav-link" href="/index.html">Home</a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="/about.html">About</a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="/archive.html">Blog</a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="/tags/">Topics</a>
              </li>

              <li class="nav-item">
                <a class="nav-link" href="/talks.html">Talks</a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="/${config.feed_file}">Subscribe</a>
              </li>
            </ul>
          </div>
        </div>
      </nav>
