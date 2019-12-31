
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

<!--  TODO fix this -->
      <#assign mastheadURL = "/img/masthead/${masthead}">

      <!--#if masthead.starts_with("http")-->
        <!--#assign mastheadURL = masthead-->
      <!--#else-->
        <!--#assign mastheadURL = "/img/masthead/${masthead}"-->
      <!--/#if-->

      <!-- Page Header -->
      <header class="masthead" style="background-image: url('${mastheadURL}')">
        <div class="overlay"></div>
        <div class="container">
          <div class="row">
            <div class="col-lg-8 col-md-10 mx-auto">
              <div class="page-heading">
                <h1>${pageTitle}</h1>
                <#if pageSubtitle??>
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
              <p class="photo-credit">Photo credit:
                <#if mastheadCredit?starts_with("http")>
                  <a href="${mastheadCredit}" target="_blank">${mastheadCredit}</a>
                <#else>
                  ${mastheadCredit}
                </#if>
              </p>
            </div>
          </div>
        </div>
      </#if>
      </header>
