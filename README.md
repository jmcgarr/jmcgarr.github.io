My Blog
==================
Blog is published at http://www.mikemcgarr.com

To publish
==========
Run
```
./gradlew jbake publish
```

To preview
==========
To preview
```
./gradlew jbake jbakePreview
```

Blog To Do:
----------
- Work on overall layout/design
- Setup CI with Travis or Cloudbees?
- add favicon
- Add links to previous blog post at bottom
- internal links convert to new blog
- all links should open in new tab
- add markdown generator (removing the build error)

Gradle Plugin To Do:
--------------------
- task to create a post (optionally a branch as well called post/____)
- Add live reload type feature when preview is on (reruns jbake)