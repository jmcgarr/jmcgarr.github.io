title=Reviving my Website with Claude
date=2026-10-25
type=post
tags=writing, claude, jbake
status=published
masthead=maui-sunset.jpg
mastheadCredit=Self, Hawaii, 2021
summary=It has been six and a half years since my last blog post. The global pandemic upended not only the world, but also my personal priorities. I found myself no longer interested in writing or public speaking. Like many of us, I was trying to make sense of the new world order, and professional investment took a backseat. This persisted for years. Over those years, so much changed. I changed jobs twice. My elementary schooler was now in high school. We fully renovated our house. Oh, and generative AI became a thing. It feels like everything has changed...and with these changes, I found that I wanted to write again.
~~~~~~

It has been six and a half years since my last blog post. The global pandemic upended not only the world, but also my personal priorities. I found myself no longer interested in writing or public speaking. Like many of us, I was trying to make sense of the new world order, and professional investment took a backseat. This persisted for years. Over those years, so much changed. I changed jobs twice. My elementary schooler was now in high school. We fully renovated our house. Oh, and generative AI became a thing. It feels like everything has changed...and with these changes, I found that I wanted to write again. 

My reasons for writing again remain largely the same as they did before. While my blog is public, I mostly wrote these posts for myself, and to myself. Writing has always brought clarity to my thoughts...forcing my ideas to solidify. My blog is also an amazing record of where I've been and what problems I was tackling. Looking back, I missed having this. The six-year gap in my blog began to gnaw away at me. 

This isn't the first time I tried to reinvigorate my blog over the years. There have been many false starts since 2020, almost all of them halting under the weight of the technical debt this site's accumulated over time. Diving into an older codebase that's been neglected requires commitment, and until recently, this is what I lacked. My compulsion for clean and correct codebases (thanks Uncle Bob) prevented me from just writing. So a few weeks back, when I decided to give it another go, I took a predictable approach...I threw [Claude Code](https://claude.com/product/claude-code) at the problem. Over the last three weeks, Claude and I made 186 commits that modernized my blog, addressed a wide swath of problems, eliminated tech debt and improved the authoring experience, without visibly changing the website. Here's what we changed. 

## Cleaning up the website

Claude was able to find a bunch of problems with my website, many of which I wasn't even aware of. These included performance improvements, fixing HTML errors and many more. Here's a list of the problems we addressed: 

- The size of the website went from 125MB over 1,824 files to just 22MB in 433 files. 
- The home page went from 1.3MB to 0.39MB.
- The site's [Lighthouse](https://developer.chrome.com/docs/lighthouse/) performance scores went from the low 60s and 70s to the high 90s. The performance improvement for mobile phones was even better. 
- We eliminated all the ad trackers, reducing external links from 46 to just 8. 
- We eliminated all 1,574 HTML errors across the website. 
- We found and fixed 275 broken links. 
- My old publishing approach, which relied on [GitHub Personal Access Tokens (PAT)](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens), was broken. We migrated to a [GitHub Actions based publishing model](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site#publishing-with-a-custom-github-actions-workflow) and removed the PAT. This model is much easier to reason about.
- There were a dozen-unpublished draft blog posts that were accessible online, if you know how to find them. Only one of them was worth preserving. 
- Accessing this site without the `www` showed a browser security warning. Claude guided me through the process of updating my DNS records to remove these issues. 
- Most of my social media share links across the site were broken or out of date (RIP Twitter). We updated them all. 
- Blog posts no longer scroll sideways on mobile phones. 
- There was some leftover debris from a previous WordPress migration that we cleaned up. 
- We removed unused code such as 1,400 icon fonts, JavaScript and CSS. 
- External links now consistently open in a new browser tab.
- All image alt tags were added or expanded upon, vastly improving accessibility. 

## Website Improvements

In addition to cleaning up bugs and improving the performance of the site, Claude and I were able to make a number of improvements.

- **Committing to Markdown:** One of the biggest and most meaningful changes we made was migrating all the source content pages to Markdown. Previously, all of my blog posts were written in either Markdown, AsciiDoc or HTML pages. The HTML pages were the blog posts ported from my original WordPress site. AsciiDoc was the first Markdown format I used when authoring content. Claude was able to migrate all the posts to Markdown without impacting the content.  
- **Disqus -> Giscus:** [Disqus](https://disqus.com/) recently introduced an ad based monetization. While Disqus served me well for years, this change didn't match my vision for the website. Claude recommended I switch to [Giscus](https://github.com/giscus/giscus), which is a GitHub Discussion based commenting platform. I'd never heard of Giscus before this effort. Claude easily switched over, and even preserved all the previous Disqus comments before making the switch. This was one of the ad tracking sites we eliminated. 
- **Google Analytics -> Goatcounter:** Claude helped me migrate from [Google Analytics](https://marketingplatform.google.com/) to [GoatCounter](https://www.goatcounter.com/), a much smaller and more privacy-friendly web traffic service. Goatcounter was another site I'd never heard anything about. Another ad tracker eliminated. 
- **Simplified Publishing:** The previous publishing model was a complicated Jekyll based approach of authoring content on the `source` branch, building the static site and pushing the changes to the `main` branch. We switched to a model where `main` is the only branch I care about, and leveraging GitHub's Pages Deployment model. So much simpler. 
- **Improved CI:** Previous versions of the website published via [TravisCI](https://www.travis-ci.com/). I'd already switched to GitHub Actions a while ago, but hadn't cleaned up my TravisCI configs. 
- **Tooling Upgrades:** We upgraded JBake from `2.6` -> `2.7`, Gradle from `5.6` -> `9.8` and Java from `1.8` -> `25`. That last one looks shocking, but I am fairly certain there was a good technical reason I was still on 1.8. 
- **Better Gradle Integration:** Claude did a great job creating [Gradle Tasks](https://docs.gradle.org/current/userguide/more_about_tasks.html) to automate the creation of new blog posts. I can now run `./gradlew newPost -Ptitle=...` to generate a blog post. 
- **🥰 Live Reload:** One feature I've been pining for was a better live reload feature while working locally. Claude was able to build custom Gradle tasks to 
- **Dependabot:** Clade added dependabot to my site to help manage dependencies. 
- **Syntax Highlighting:** Code snippets now include syntax highlighting. 

## Committed to JBake, for now

Given the scale of changes I was embarking on, I thought it prudent to also consider a switch off JBake. I've loved JBake, but it is rapidly becoming a dying open source project. The last release, `v2.7.0` was from 2021. Claude was more than happy to build a decision matrix for me, comparing [Hugo](https://gohugo.io/), [Astro](https://astro.build/), [Eleventy](https://www.11ty.dev/) and [Roq](https://iamroq.dev/) to JBake. I remained irrationally interested in supporting Java based platforms, but was not opposed to Golang based platforms either. After reviewing Claude's evaluation of the platforms, I've decided to stick with JBake for now. The upgrades we made definitely improved JBake, but over time it may not be enough. 

## Cleaning up after Claude

The amount of work I was able to get done, with limited involvement from me was amazing. However, Claude's work product was far from perfect. I've noticed that Claude could have done a better job of building reusable code, rather than focusing on scripts. For instance, it added a ton of functionality to my `build.gradle`, but it's kind of a mess now. I plan on migrating all of that functionality into a Gradle Plugin. There are also a myriad of scripts Claude did a great job storing, but likely also need to be cleaned up. I spent much of my three weeks focused on the results and not the quality of the code and project. That is what remains. 

## What's Next?

Now that I've revived my blog, I plan on writing more. I don't have a planned cadence or schedule, but I do plan on incorporating blogging into the remainder of 2026 and 2027. I also plan on building and talking about a bunch of side projects I plan on working on, in addition to the various fun professional adventures I might have. 