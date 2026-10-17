title=Unit Testing, From Zero to Hero
date=2010-12-04
type=post
tags=junit, unit testing
status=draft
~~~~~~
**\<Is this a duplication of my previous post: /blog/practical-unit-testing.html>**

When I first decided that to learn how to write unit tests, I remember being very confused on a number of topics and stumbling a lot before I finally got the hang of it.  While this process is typical when it comes to learning, I decided that it would be nice to provide some hints and tips for a unit testing newbie.  This post is a collection of tips for somebody just jumping into unit testing.  If you familiar with unit testing, then feel free to comment on this post, adding your personal tips.

**Start with JUnit**

This is not to say that JUnit is the best framework available, cause there are others, but it is the first, and is still extremely popular.  You will be able to get the most assistance with using it.  I am not going to provide a tutorial on how to write your first unit test, mainly because there are so many out there.  ...Provide links to tutorials...

**Running the Tests**

Where most tutorials fall short is how to run the tests.  I do not recommend just straight javac and java command line calls unless you are a complete Java newb.  I am going to assume that you have some code you would like to test.  Starting out, I would try running your tests inside your IDE.  Eclipse has a great interface for this and it will help you get your feet wet.  The other advantage of IDE based execution is the speed of execution.  If I am writing a JUnit class in Eclipse, I can press Ctrl-F11 to run the unit test.  Keeping your hands on the keyboard is important for maintaining flow.

I also recommend wiring your execution up to a build tool.  You are going to want to execute your tests whenever you build your application.  If your application is built from the IDE, then stop learning JUnit and go and learn Ant or Maven.  I believe they add more critical to a project's success than unit testing, but that is just me.

**What to Test First**

...Start testing your static or isolated utility classes, avoid classes with external dependencies

**Don't Map Test Methods to Class Methods**

...Don't try mapping a test method to a class method

**Long method names are your friends**

...Don't be afraid of long test method names, in fact the longer the better

**One Assert Per Test**

The more you read about writing unit tests, you will see people talk about how you should only have a single assertion per test method.  When I first started, this seemed daunting, but the most experienced I got, I started to see the reason why you want this and how it is done.  >>>MORE>>>

**Test-First, But not Now**

Don't try test first right away...you should focus on familiarizing yourself with testing frameworks and how to write a good test.  Once you have gotten used to this, then you can try test first.  I think learning test-first development if you don't know how to write unit tests is like learning how to play hockey if you can't ice skate.  I would argue that you should try test first development at some point, even if you don't think that it is for you.  If you are interested, watching James Shore's Let Play series on Test Driven Development is one of the best ways to get started.
