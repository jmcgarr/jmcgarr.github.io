My Blog
==================
Blog is published at http://www.mikemcgarr.com

To publish
==========
Run
```
./gradlew bake publish
```

To preview
==========
To preview
```
./gradlew bake bakePreview
```

Blog To Do:
----------
- Add links to previous blog post at bottom
- internal links convert to new blog
- all links should open in new tab
- Update instructions to README for "live editing" mode (window 1 `gw -t bake` and window 2 `gw bakePreview`)
- Tweak look and feel of masthead to take up less vertical space
- Build the CSS using SCSS (or something like that)

Gradle Plugin To Do:
--------------------
- task to create a post (optionally a branch as well called post/____)
- Add live reload type feature when preview is on (reruns jbake) - see script in build.gradle
- Improve the incremental bake time
- Enable a single command that will incrementally bake, serve content via jetty and live reload on edits
- toast message when bake it complete, especially in live reload mode

Redesign NICE-TO-HAVES
----------------------
- restore tags to each post
- redesign the layout of the blog archive page
- redesign the layout of the talks page
- Find a better standard masthead for old posts
- Update the header for posts to match the example
- reevaluate the default fonts
- Update the about me content
- code snippet look and feel
- Update build so that scss generates the css (like in the example: https://startbootstrap.com/template-overviews/clean-blog/)

Optional mastheads
==================
- https://unsplash.com/photos/tGTVxeOr_Rs
