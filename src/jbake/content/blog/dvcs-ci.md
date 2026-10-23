title=GitFlow and Continuous Integration
date=2011-09-14
type=post
tags=artifactory, branching, continuous integration, dvcs, git, gitflow, github, gradle, Java, jenkins
status=published
summary=I came across an interesting blog post by James Carr discussing his workflow when building Java applications.  In general, I think that the stack and process he mentions is top notch.
~~~~~~
I came across an [interesting blog post](https://web.archive.org/web/20110926011039/http://blog.james-carr.org:80/2011/09/09/my-current-java-workflow/ "James Carr - My Current Java Workflow") by James Carr discussing his workflow when building Java applications.  In general, I think that the stack and process he mentions is top notch.  I love [Gradle](https://www.gradle.org/ "Gradle"), [Jenkins](https://www.jenkins.io/ "Jenkins"), [Artifactory](https://jfrog.com/artifactory/ "Artifactory") and [Git](https://git-scm.com/ "Git")/[GitHub](https://github.com/ "GitHub").  I have been playing with or using in production all of these tools and they are amazing.  I especially like how the article provides a tutorial on how to setup your project to look just like his, something I will definitely try.

Where the article raised questions for me was when James started talking about [GitFlow](https://github.com/nvie/gitflow "GitFlow") and Vincent Driessen's [Git Branching Model](https://nvie.com/posts/a-successful-git-branching-model/ "Vincent Driessen's Git Branching Model").  If you haven't read this article, I recommend you stop and go read it now.

Vincent's approach to using Git takes full advantage of a major feature provided in DVCS, [cheap local branching](https://web.archive.org/web/20110920132555/http://whygitisbetterthanx.com/#cheap-local-branching "Why git is better than X - Cheap Local Branching").  As an advocate of [continuous integration](https://martinfowler.com/articles/continuousIntegration.html "Continuous Integration") however, I have always recommended that one should avoid the approach of excessive branching.  One of the principle practices of continuous integration is:

> Everyone commits to the mainline everyday

Martin Fowler goes on to say,

> "In practice it's often useful if developers commit more frequently than that. The more frequently you commit, the less places you have to look for conflict errors, and the more rapidly you fix conflicts."

So taking this a basic rule of thumb, does Vincent's branching model conflict with continuous integration?  The short answer is no, so long as the developers are only branching locally and are pushing their changes to *origin* (the centralized git repository) daily.

In Vincent's approach however, he says

> "But besides the centralized push-pull relationships, each developer may also pull changes from other peers to form sub teams. For example, this might be useful to work together with two or more developers on a big new feature, before pushing the work in progress to `origin` prematurely."

This is an approach to feature branching Martin Fowler calls [Promiscuous Integration](https://martinfowler.com/bliki/FeatureBranch.html "Martin Fowler on Feature Branching ").  Martin Fowler attempts to assess whether the concept of promiscuous integration is a good idea or not.  I recommend you read the article yourself, but he ends up saying he thinks it's not the best idea (but doesn't outright say don't do it).  He recommends using [Feature Toggles](https://martinfowler.com/bliki/FeatureFlag.html "Feature Toggles") or [Branch by Abstraction](https://martinfowler.com/bliki/BranchByAbstraction.html "Branch by Abstraction") as approaches to avoiding Feature Branches in long running development cycles.  This same advice is reinforced in the book [Continuous Delivery](https://www.amazon.com/Continuous-Delivery-Deployment-Automation-Addison-Wesley/dp/0321601912 "Continuous Delivery") by [Jez Humble](https://jezhumble.net/ "Jez Humble") and [Dave Farley](https://www.davefarley.net "Dave Farley").  I highly recommend this book!

So, what does one do with this information?  Is use of GitFlow or promiscuous integration a bad idea?  I think that it can work very well for some teams and could be very dangerous in others.  In general, I like it when the VCS stays out of the way and the team gets in the habit of pushing changes and looking to the CI server validation that everything is ok.  Introducing promiscuous integration could interrupt this cycle and allow code changes to circumvention the mainline longer than they should.  This branching scheme feels complex, even with the addition of GitFlow.

I plan on trying GitFlow and further investigating it's merits, hopefully writting a more in depth analysis of how it works.  I think that it is an interesting idea (worth blogging about) and has a lot of potential.  I would love to hear what you think about GitFlow, promiscuous integration, feature branching and how they are good or bad, so please comment below!
