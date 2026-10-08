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
    <meta name="description" content="Personal website of Mike McGarr - Passionate engineering leader and manager.">
    <meta name="author" content="Mike McGarr">
    <meta name="keywords" content="">
    <meta name="generator" content="JBake">

    <!-- Bootstrap core CSS -->
    <link href="/vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">

    <!-- Custom fonts for this template -->
    <link href="/vendor/fontawesome-free/css/all.min.css" rel="stylesheet" type="text/css">
    <#-- Only the Open Sans styles the CSS actually uses: 300 (header subheadings), 300 italic (post dates),
         800 (nav). Lora is not loaded: its only rule (.post-heading .meta) matches nothing on the site.
         Self-hosted (T073): the same font files Google Fonts serves, so no third-party request blocks
         rendering. The Latin normal file is preloaded because every page's nav uses it. -->
    <link rel="preload" href="/fonts/open-sans/open-sans-normal-latin.woff2" as="font" type="font/woff2" crossorigin>
    <link href="/css/fonts.css" rel="stylesheet">

    <!-- Custom styles for this template -->
    <link href="/css/clean-blog.css" rel="stylesheet">

    <!-- previous styles -->
    <link href="/css/asciidoctor.css" rel="stylesheet">
    <link href="/css/extra.css" rel="stylesheet">

    <link rel="shortcut icon" href="/favicon.ico">
  </head>
  <body>
    <div id="wrap">
