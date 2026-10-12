title=Enforcing Coding Standards
date=2010-02-10
type=post
tags=checkstyle, coding standards, continuous integration, eclipse, hudson, Java
status=published
~~~~~~
While most teams recognize the importance of coding standards, I have witnessed too many development teams who define a set of coding standards, yet do not enforce them.  If you are not enforcing the coding standard, then you have no way of knowing your team's compliance with the standard and its effectiveness is negated.  In this article, I hope to share some of my experiences with enforcing coding standards on software development projects.

**Why are Coding Standards Important?**

Convincing management of the importance of  implementing coding standards is one of the first steps to successfully adoption.  You will need to justify the time you will spend defining, documenting, communicating, fostering adoption and enforcing your coding standards.  Your project manager will likely ask you the question, "So what are the benefits of implementing coding standards on our project?".  Here are a few of the benefits of coding standards:

- <span style="text-decoration:underline;">Increased maintainable</span> - developers can quickly look at another developer's code and easily read it
- <span style="text-decoration:underline;">Increased staffing flexibility</span> - developers can move between modules in the application and not have to adjust to a team members coding styles
- <span style="text-decoration:underline;">Facilitates better designs</span> - for instance, limiting all methods to 40 lines forces developers to think about the functionality of the method
- <span style="text-decoration:underline;">Coding standards can improve documentation</span> - one of my favorite rules is to enforce strict Javadocs throughout the code

Great, you management is on board and you have clearance to define and implement coding standards...what's next?\
<strong> </strong>

**Defining the Standard**

<strong> </strong>In my opinion, this is where most teams go wrong (including myself).  They want to collect all the various standards that are publicly defined, meet with the whole development team and then discuss in detail each of the standards one by one.  This process can be time consuming, taking hours or even days, and you likely walk away without a team wide consensus.

I recommend taking an Agile approach to defining your coding standards: define your standards as early as possible and evolve them often (early and often).  Instead of building a consensus within the team by deliberating for days on the standards, start with an existing publicly available standard that you mostly agree with (like [Sun's Java Coding Standards](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html)) and adopt that. [Checkstyle](http://checkstyle.sourceforge.net/), a code standard enforcement tool, can enforce this standard out of the box with a standard definition file.  Check in a copy of the Checkstyle file into your source code.  When you apply this standard against your team's current source code, members of the team will begin the complain about certain violations being reported.  This is good.  They can make changes directly to the Checkstyle configuration file to change the standard.  The team should feel like their coding standard is theirs to change and update as they wish, so give them the opportunity to own it and change it, within reason of course.

It is also important to make sure that team knows what the standard is so they can start using it.  Part of this process is making sure that the coding standard is documented.  I personally do not recommend that you go out and create a lengthy Word document detailing every coding standard (I have done this before and it is painful and useless).  If you must document the standard, use a Wiki page which is easily accessible to the team and easy to maintain.  Your wiki page should merely point to the publicly available standard you previously chose and possibly provide a link to the Checkstyle definition file in source control.  Remember, you are defining a standard for developers, who prefer something functioning (like Checkstyle) over the written word.

**Adopting the Standard**

In order to facilitate the adoption process, there are a number of tools you should provide your team that will make it easy for them to migrate to the new coding standard.  We already mentioned using Checkstyle to define the standard.  Checkstyle is a command line tool that can be integrated with common build tools such as [Ant](https://ant.apache.org/) or [Maven](https://maven.apache.org/).  While build time analysis of the coding standard is useful for reporting, developers need immediate feedback on their compliance as they write code.  To achieve this, I recommend using Checkstyle integration directly in your IDE.  Checkstyle has plugins for [Eclipse](https://www.eclipse.org/), [Netbeans](https://netbeans.apache.org/front/main/index.html), [IntelliJ](https://www.jetbrains.com/idea/) and more.  This integration allows the developers to see the violation as they type and address it immediately, preventing new violations from entering the code.

In addition to Checkstyle integration, IDE like Eclipse provide out of the box code formatters that can format your code to a given standard.  Unfortunately, Eclipse's formatter definition is decoupled from a Checkstyle definition so as your Checkstyle definition file changes, you will also have to make changes to your Eclipse formatter definition, but it is worth the effort.

Once your standard is well defined, you can use a bulk formatting tool like [Jalopy](http://jalopy.sourceforge.net/) to format all of your source code in one sweep.  I am hesitant to recommend this approach due to the possibility of introducing bugs due to formatting changes (this is possible).  If you have a lot of unit tests, then the risk of using this tool decreases significantly.  If you do want to use Jalopy, make a separate branch of your source code first, this will reduce the risk to the mainline of development, especially if you are working on legacy code.

**Enforcing the Standard**

Great, so now you have a code standard defined.  You need to start enforcing it.  After doing this on multiple projects, the most useful method of enforcing coding standards is to integrate Checkstyle into your Continuous Integration (CI) builds.  If you aren't using a CI server like [Hudson](https://projects.eclipse.org/projects/technology.hudson), consider adding this to your list of reasons why you need to add a build server to your infrastructure.  I have been using Hudson successfully for a while and I am huge fan of its simplicity.

Every night, our Hudson server runs Checkstyle against the code base to determine how many code standard violations exist in the code.  Hudson trends the number of violations over consecutive builds, which is useful for tracking the team's progress during a lengthy adoption effort.  Hudson's dashboarding capabilities allow the team easy access to the details of violations drilling down to the exact line of code.  Hudson can also be set up to define thresholds, which can mark the build as unstable or even fail the build if too many Checkstyle violations are introduced into a build.  This ability coupled with Hudson's multiple feedback mechanisms provides the team will daily reminders that the application code is in violation of the standard.

Another tool I utilize to help encourage adoption of the coding standard was a cool Hudson plugin called the [Continuous Integration game](https://plugins.jenkins.io/ci-game/).  This plugin assigns positive and negative point values to successful and failed builds, as well as Checkstyle violations.  It then provides the team with a Leaderboard showing how many points are assigned to each developer.  This may sound a bit sophomoric, but I saw a profound difference in each developer's attitude towards resolving coding standards.  This plugin taps into everybody's competitive spirit, but it should be used with some [caution](https://web.archive.org/web/20090609160326/http://www.chillisoft.co.za:80/blog/?p=244).

In summary, if you want your team to quickly adopt a coding standard,  you need define the standard early, make the it easily available to the team and integrate standard checks with your continuous integration server.  The key to making this successful is ensuring that the whole process is inexpensive to setup and maintain.  Enjoy and good luck!
