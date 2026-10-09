
      <#if content.title??>
        <#assign pageTitle = content.title>
      </#if>
      <#if content.subtitle??>
        <#assign pageSubtitle = content.subtitle>
      </#if>

<!-- Grab masthead data -->
      <#if masthead??>
        <!-- do nothing...we are set -->
      <#elseif content.masthead??>
        <#assign masthead = content.masthead>
        <#if content.mastheadCredit??>
          <#assign mastheadCredit = content.mastheadCredit>
        </#if>
      <#elseif content.type == "post">
        <#assign masthead = "saocom-falcon-launch.jpg">
        <#assign mastheadCredit = "https://www.flickr.com/photos/spacex/44451125454/">
      <#else>
        <#assign masthead = "lone-cypress-2.jpg">
        <#assign mastheadCredit = "https://flic.kr/p/hADZKt">
      </#if>

      <#-- masthead= can be a full URL (a remote image, used as-is), a site path starting with "/" (any
           image under src/jbake/assets), or a file name in /img/masthead/ (T037) -->
      <#if masthead?starts_with("http") || masthead?starts_with("/")>
        <#assign mastheadURL = masthead>
      <#else>
        <#assign mastheadURL = "/img/masthead/${masthead}">
      </#if>

      <#-- Responsive header image (T071): phones get the 960px variant, tablets and small laptops 1440px,
           larger screens the full image. Variants come from scripts/masthead-variants.py. The CSS
           (extra.css) picks one by width, and the preloads use the same media queries so each browser
           fetches exactly one image, early. Remote (http) mastheads have no variants. -->
      <#if mastheadURL?starts_with("http")>
        <#assign mastheadSm = mastheadURL mastheadMd = mastheadURL>
      <#else>
        <#assign mastheadBase = mastheadURL?keep_before_last(".") mastheadExt = mastheadURL?keep_after_last(".")>
        <#assign mastheadSm = "${mastheadBase}-960.${mastheadExt}" mastheadMd = "${mastheadBase}-1440.${mastheadExt}">
      </#if>
      <link rel="preload" as="image" href="${mastheadSm}" media="(max-width: 575.98px)">
      <link rel="preload" as="image" href="${mastheadMd}" media="(min-width: 576px) and (max-width: 1279.98px)">
      <link rel="preload" as="image" href="${mastheadURL}" media="(min-width: 1280px)">

      <!-- Page Header -->
      <header class="masthead" style="--masthead-sm: url('${mastheadSm}'); --masthead-md: url('${mastheadMd}'); --masthead-lg: url('${mastheadURL}')">
        <div class="overlay"></div>
        <div class="container">
          <div class="row">
            <div class="col-lg-8 col-md-10 mx-auto">
              <div class="page-heading">
                <#-- The home page shows no title, but screen readers still get its heading (T046) -->
                <h1><#if pageTitle?has_content>${pageTitle}<#else><span class="sr-only">Mike McGarr</span></#if></h1>
                <#if pageSubtitle?has_content>
                  <span class="subheading">${pageSubtitle}</span>
                </#if>
                <#if content.type == "post" && content.date??>
                  <span class="subheading"><em>${content.date?string("MMMM dd, yyyy")}</em></span>
                </#if>
              </div>
            </div>
          </div>
          <div class="row">
            <div class="">
            </div>
          </div>
        </div>
      <#if mastheadCredit??>
        <div class="container">
          <div class="row justify-content-between">
            <div class="col-4"></div>
            <div class="col-4 text-right">
              <#-- The span gets a faint dark backdrop (extra.css), so the credit stays readable on bright photos (T046) -->
              <p class="photo-credit"><span class="photo-credit-text">Photo credit:
                <#if mastheadCredit?starts_with("http")>
                  <a href="${mastheadCredit}" target="_blank">${mastheadCredit}</a>
                <#else>
                  ${mastheadCredit}
                </#if></span>
              </p>
            </div>
          </div>
        </div>
      </#if>
      </header>
