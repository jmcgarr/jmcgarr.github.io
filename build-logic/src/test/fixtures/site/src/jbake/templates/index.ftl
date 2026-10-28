<!DOCTYPE html>
<html>
<head><title>Fixture</title><link rel="stylesheet" href="css/clean-blog.css"></head>
<body>
<#list published_posts as post><p><a href="${post.uri}">${post.title}</a></p></#list>
</body>
</html>
