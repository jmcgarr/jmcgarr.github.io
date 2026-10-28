<!DOCTYPE html>
<html>
<head>
<title>Tag: ${tag?replace('-', ' ')}</title>
<meta property="og:title" content="Tag: ${tag?replace('-', ' ')}">
</head>
<body>
<h1>Tag: ${tag?replace('-', ' ')}</h1>
<#list tag_posts as post><p><a href="/${post.uri}">${post.title}</a></p></#list>
</body>
</html>
