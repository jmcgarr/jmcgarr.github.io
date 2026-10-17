title=On acceptance test architecture, lifecycles and responsibilities
date=2013-12-18
type=post
tags=acceptance test, atdd, cucumber, fitnesse, gherkin, SUT, tdd, test automation, test fixture
status=published
summary=I was recently in a conversation with testers about how acceptance tests should be structured.   As we revisit how Blackboard conducts test automation, we were looking at patterns and practices to employ.  I shared my following perspective on how to think about the architecture of your acceptance test infrastructure, the lifecycle of a specification, as well as who should conduct which step.
~~~~~~
I was recently in a conversation with testers about how acceptance tests should be structured.   As we revisit how Blackboard conducts test automation, we were looking at patterns and practices to employ.  I shared my following perspective on how to think about the architecture of your acceptance test infrastructure, the lifecycle of a specification, as well as who should conduct which step.  This post is really a teaser post, as I am merely smearing pretty pictures on the web, without much background.  Isn't that what follow-up blogs are for?

# Acceptance Test Composition

![Acceptance Test Architecture](/img/acceptance-test-arch.jpg)

A well formed acceptance test architecture is composed of five core components:

1. <b><span style="text-decoration:underline;">Specification</span></b> - This is the document that describes what the system should be doing. The requirement that will be exercised by the application.  In an automated testing framework, this is usually implemented in [Gherkin](https://cucumber.io/docs/gherkin/ "Gherkin ") ([Cucumber](https://cucumber.io/ "Cucumber")) or a wiki ([Fitnesse](https://fitnesse.org/ "Fitnesse")).  It should be easily read and understood by all members of the organization at any level and written in the business language.  I recommend collaboratively white boarding your specs (a la [Three Amigos meeting](https://www.scrumalliance.org/community/articles/2013/2013-april/introducing-the-three-amigos "Three Amigos Meeting")) and have somebody write it in the technology later.
2. <b><span style="text-decoration:underline;">Fixture</span></b> - This is the glue code.  This code understands the format that the specification is written in, and knows how to talk to either the application driver or the Automation Layer to implement the features of a test.  This is generally code.  Ideally it is written in a language similar to the SUT so everybody can update it.  In Cucumber, these are the [Step Definitions](https://cucumber.io/docs/cucumber/step-definitions/ "Step Definitions") and in Fitnesse these [Fixtures](https://fitnesse.org/FitNesse/UserGuide/FixtureGallery.html "Fitnesse Fixtures").
3. <b><span style="text-decoration:underline;">Automation (DSL)</span></b> - Don't start building this from day one.  The automation layer is the [Domain Specific Language (DSL)](https://martinfowler.com/books/dsl.html "Domain Specific Language") that will evolve as you build out your fixtures.  It is an abstraction layer over the application driver.  If you design this correctly to be a [fluent API](https://martinfowler.com/bliki/FluentInterface.html "Fluent Interface") or even a full on DSL, then you will find that non-technical members of the team can look at this an possibly even contribute here.  But don't set out to build a DSL...cause that ain't easy.
4. <b><span style="text-decoration:underline;">Application Driver</span></b> - This is the code that knows how to talk to the system under test.  For a web application, this is typically a framework like [Selenium](https://www.seleniumhq.org/ "Selenium").  If you are writing tests for a web service, then you will need to pick a tool that can send/receive SOAP or REST messages.  If your application is a batch processing system that takes a package of zipped XML files and puts them in the database, then you need to build a custom framework that knows how to build the zip file, and verify the database got updated properly.
5. <b><span style="text-decoration:underline;">System Under Test (SUT)</span></b> - This is the product you are building.  This is what you are testing.

# Acceptance Test Lifecycle

![Specification by Example Phases](/img/acceptance-test-lifecycle.jpg)\
An acceptancetest should evolve collaboratively via a cross functional team.  I think about it in three phases:

1. <b><span style="text-decoration:underline;">Collaborate and Define</span></b> - This is typically where you have a 'Three Amigos Meeting' where members of the business/analysts, testers, and engineers (and others too) will get together and write the requirement and the acceptance criteria.  This can happen on a white board or on paper.
2. <b><span style="text-decoration:underline;">Automation Phase</span></b> - Once a specification has been authored, then the specification will be captured in the automated testing tool and checked into version control.  Then the test fixture and DSL will be authored to automate the failing test.
3. <b><span style="text-decoration:underline;">Verification Phase</span></b> - This is the last phase.  The code to satisfy the test is written now.  The details of the DSL might also be filled in here (for instance, elements on the page to be called).  The test is then run and re-run until it passes.  When the test passes, the feature is "DONE".  The verification will continue via continuous integration.

You may note that you this may smell like [Test Driven Development](https://en.wikipedia.org/wiki/Test-driven_development "Test Driven Development").  It should, cause it is...or [Acceptance Test Driven Development](https://web.archive.org/web/20120327213210/http://www.manning-source.com/books/koskela/Chapter9Sample.pdf "(PDF) Chapter on ATDD").

# Responsibilities

![Specification by Example Roles](/img/acceptance-test-responsibilities.jpg)\
I would also like to comment on who should do what.

- <b><span style="text-decoration:underline;">Business and/or Analyst</span></b> - should focus on the user story and the acceptance criteria.
- <b><span style="text-decoration:underline;">QA</span></b> - should focus on the user story, acceptance criteria, the test fixture and the DSL.
- <b><span style="text-decoration:underline;">Engineer</span></b> - An engineer should be involved in all aspects of the acceptance test.  They are ultimately responsible for satisfying the requirement.

I firmly believe that an engineer (or an Engineer in Test) needs to be focused on the DSL layer and the SUT.  This is heavy coding and design and should be not be where "testers learning to code" should focus their time.
