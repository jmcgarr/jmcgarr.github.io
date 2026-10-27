<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8"/>
<#-- Local preview only (T064): reloads the page after each rebuild. See preview-reload.ftl. Kept at column 0
     so a normal bake outputs nothing here, not even indentation. -->
<#if (config.preview_livereload!"") == "true">
<#include "preview-reload.ftl">
</#if>
    <title><#if (content.title)??><#escape x as x?xml>${content.title}</#escape><#else>Mike McGarr</#if></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <#include "embeds.ftl"><#-- embeds(): a lone link to a video or deck becomes the player (T115); head-meta.ftl and post.ftl use it -->
    <#include "head-meta.ftl"><#-- description, canonical, Open Graph and Twitter/X cards (T041, T042) -->
    <meta name="author" content="Mike McGarr">
    <meta name="keywords" content="">
    <meta name="generator" content="JBake">

    <!-- Bootstrap core CSS -->
    <link href="/vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">

    <#-- Only the Open Sans styles the CSS actually uses: 300 (header subheadings), 300 italic (post dates),
         800 (nav); owner's decision 2026-10-24 (T022) to keep this mix. Self-hosted (T073): the same font
         files Google Fonts serves, so no third-party request blocks rendering; the @font-face rules open
         clean-blog.css (src/scss/_fonts.scss, T135; they were /css/fonts.css until then). The Latin normal
         file is preloaded because every page's nav uses it, and the Latin italic on posts, whose masthead
         date is the only italic Open Sans on the site (T135). -->
    <link rel="preload" href="/fonts/open-sans/open-sans-normal-latin.woff2" as="font" type="font/woff2" crossorigin>
<#if ((content.type)!"") == "post">
    <link rel="preload" href="/fonts/open-sans/open-sans-italic-latin.woff2" as="font" type="font/woff2" crossorigin>
</#if>

    <#-- The theme and the site's own rules, compiled from src/scss/ (T043). The site-specific additions that
         were a separate extra.css are in the same file since T134, the font rules since T135. The footer and
         menu icons are inline SVGs (T047, T135), so no icon font is loaded. -->
    <link href="/css/clean-blog.css" rel="stylesheet">
<#-- Code block styles, only on pages with code (T056) -->
<#include "code-highlight.ftl">
<@codeHighlightCss/>
<#-- externalLinks(), used by post, page and index: off-site links open in a new tab (T058) -->
<#include "external-links.ftl">

    <link rel="shortcut icon" href="/favicon.ico">
  </head>
  <body>
    <#-- Hidden until a keyboard user tabs to it (T046). Every page has a <main id="main-content">. -->
    <a class="skip-link sr-only sr-only-focusable" href="#main-content">Skip to main content</a>
    <div id="wrap">
