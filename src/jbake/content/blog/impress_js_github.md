title=Interactive presentations with impress.js + GitHub Pages
date=2012-01-16
type=post
tags=continuous delivery, css3, github, github pages, html5, impress.js, mcjug, prezi, ruby, showoff, sinatra
status=published
summary=Next month, I am giving a follow-up of my Continuous Delivery talk to the Montgomery County Java User's Group.  In preparing for this talk, I decided that challenge myself by using a presentation tool other than PowerPoint.
~~~~~~
Next month, I am giving a follow-up of my <a title="Continuous Delivery Talk" href="/blog/cd-at-mcjug.html" target="_blank">Continuous Delivery talk</a> to the [Montgomery County Java User's Group](https://sites.google.com/site/mcjugmd/home "MCJUG").  In preparing for this talk, I decided that challenge myself by using a presentation tool other than PowerPoint.  In fact, I decided to avoid any desktop variant and try some of the next generation web-based presentation tools.

I first considered [<img class="alignright" src="/img/ruby-logo.gif" alt="Ruby logo" width="186" height="67" />Showoff](https://www.ruby-lang.org/ "Ruby Lang"), a nice framework for building presentations in HTML (running on [Ruby](https://www.ruby-lang.org/ "Ruby")/[Sinatra](https://sinatrarb.com/ "Sinatra")).  This tool is hot in the developer community right now.  Since it is a running application, it depends on deploying to hosting provider like [Heroku](https://www.heroku.com/ "Heroku").  It does have a nice simple markdown for capturing content and I would consider using it in the future.

<img class="alignleft" src="/img/HTML5_Logo_512.png" alt="HTML5 logo" width="88" height="88" />[HTML5 Slides](https://github.com/html5rocks/slides.html5rocks.com "HTML5 Slides") is more of a demonstration on how you can use [HTML5](https://en.wikipedia.org/wiki/HTML5 "HTML5")/[CSS3](https://www.css3.info/ "CSS3")'s features to build web-based slide shows.  I considered this option as a way to learn more about HTML5.  The pages still need to hosted somewhere, and [AWS S3](https://aws.amazon.com/s3/ "AWS S3") would be the perfect location.

[<img class="alignright" src="/img/prezi_vertical.png" alt="Prezi logo" width="169" height="68" />Prezi.com](https://prezi.com/ "Prezi") is a service for building Flash-based presentations that break the convention of slides.  With Prezi.com, you can build dynamic visual presentations where the camera moves about on a page.  Public presentations are hosted for free, but you would have to pay for private presentations.  While Prezi <del datetime="2012-01-17T02:33:17+00:00">does</del> did offer a unique presentation style, I didn't feel the platform (Flash-based) fit my needs.

In the end, I chose [impress.js](https://github.com/impress/impress.js "impress.js"), as it seemed to be the best of all solutions.  Impress.js is essentially an HTML5 presentation framework that enables you to build Prezi.com like presentations. (thanks to [Matt Makai](https://www.mattmakai.com/ "MattMakai.com") for pointing this tool out)  You build your presentations in html (div = slide) and using tags and css, you can manipulate size, position, scale and even animation within your slide.  It is also the newest tool on the scene and seems promising.  By building my presentation in impress.js, I will be forced to beef up my front-end skills, which have been lacking as of late.

I also discovered that impress.js presentations can easily be hosted on [GitHub](https://github.com/ "GitHub") using [GitHub Pages](https://pages.github.com/ "GitHub Pages").  Since my presentation is just a web page, all I have to do is push my changes to the *gh-pages* branch of my GitHub repository and wait for the site to be displayed.  GitHub will deploy the website using a url following this convention:

> http\://${username}.github.com/${repositoryname}

Github Pages works for private repositories as well, and all you need to do to enable it is create the *gh-pages* branch.  Having my presentation displayed directly from version control with a simple url makes the process simple to manage.  Updates to the latest version are a git add, commit, push away.

Since I am just starting down this road, I may have some further feedback on how these tools work out, but so far, the presentation is looking great!  If interested in any or all of these presentation technologies, here are some sample presentations:

- [TDD your CLI](https://web.archive.org/web/20130302060935/http://tdd-ruby-cli.heroku.com/#1 "TDD your CLI presentation") by [Dave Copeland](https://www.naildrivin5.com/ "Dave Copeland's website") (showoff)
- [HTML5 Demo Slides](https://github.com/html5rocks/slides.html5rocks.com "HTML5 Slides") at [HTML5Rocks](https://web.archive.org/web/20120115102741/http://www.html5rocks.com:80/en/ "HTML5 Rocks ") (HTML5 slides)
- [Coca Cola Company](https://prezi.com/ftv9hvziwqi2/coca-cola-company/ "Coca Cola Company Presentation") (Prezi.com)
- [12412.org Presentation](https://extra.12412.org/digibury/#/title "12412.org Presentation") (impress.js)
