<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8"/>
    <title><#if (content.title)??><#escape x as x?xml>${content.title}</#escape><#else>Mike McGarr</#if></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Personal website of Mike McGarr - Passionate engineering leader and manager.">
    <meta name="author" content="https://twitter.com/SonOfGarr">
    <meta name="keywords" content="">
    <meta name="generator" content="JBake">

    <!-- Bootstrap core CSS -->
    <link href="/vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">

    <!-- Custom fonts for this template -->
    <link href="/vendor/fontawesome-free/css/all.min.css" rel="stylesheet" type="text/css">
    <#-- Only the Open Sans styles the CSS actually uses: 300 (header subheadings), 300 italic (post dates),
         800 (nav). Lora is not loaded: its only rule (.post-heading .meta) matches nothing on the site. -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Open+Sans:ital,wght@0,300;0,800;1,300" rel="stylesheet">

    <!-- Custom styles for this template -->
    <link href="/css/clean-blog.css" rel="stylesheet">

    <!-- previous styles -->
    <link href="/css/asciidoctor.css" rel="stylesheet">
    <link href="/css/extra.css" rel="stylesheet">

    <link rel="shortcut icon" href="/favicon.ico">
  </head>
  <body>
    <div id="wrap">
