title=A Practical Approach to Unit Testing
date=2010-05-07
type=post
tags=bdd, easyb, eclipse, Java, jbehave, junit, maven, tdd, unit testing
status=published
summary=Last year I was asked to give a one day workshop on how to write unit tests.  I put together a presentation and workshop format and it was a huge success.
~~~~~~
Last year I was asked to give a one day workshop on how to write unit tests.  I put together a presentation and workshop format and it was a huge success.  The team quickly took to not only writing unit tests but was able to quickly utilize mock frameworks and other testing tools.  The team's code coverage for the application quickly increased and management was pleased.  I recently took another look at this presentation to see if I wanted to revise anything and I thought I would share some of the thoughts here.  These guidelines are intended to get somebody started with unit testing quickly, but could be a welcome refresher for somebody who has been writing tests for a while.

<strong> Benefits of Unit Testing</strong>

I won't belabor this point, simply because there is too much literature floating around the internet on why to unit test your code.  In fact, I think that you would be hard pressed to find anybody who would disagree on this point.  I have found however that a surprisingly high percentage of developers I have encountered agree that writing unit tests is good, but still don't know how to do it.  Regardless, here is a list of some (but not all) of the benefits of conducting unit testing:

- Reduces New Bugs
- Reduces Bugs from Reappearing
- Enables Refactoring
- Improves Design
- Forces you to Think
- Reduces Fear
- Speeds the Development Process
- Can Provide a Form of Documentation

I am certain that there are more than this, so feel free to leave me a comment and let me know your thoughts.

**Where to Start Testing**

Projects that are trying to start unit testing usually stop when they try to determine which part of their code they should test first.  I decided that my guidance on where to start depends on the developer's level of proficiency with writing unit tests.

If you are a unit testing newbie, then you should start by writing unit tests against a utility class.  Utility classes tend to have very little external dependencies and have a simple input/output approach.  You will find that a basic lesson in using [JUnit](https://www.junit.org/ "JUnit's Website") will provide you all the information you need to test these methods.

For developers experienced in unit testing, then your goal should be to focus on the most critical components in your application.  This code should meet at least one of the following criteria:

- most commonly used functions
- most critical functions (security, DAOs, business/service classes)
- most complex functions (high cyclomatic complexity)

My preferred IDE, [Eclipse](https://www.eclipse.org/ "Eclipse"), provides some simple wizards for creating unit tests quickly.  I also prescribe the to [Maven](https://maven.apache.org/ "Maven") [project structure convention](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html "Maven Standard Directory Layout") of storing all of my unit tests in the project's src/test/java directory.  If this is your first time writing unit tests, then I recommend that you follow the [JUnit cookbook](http://junit.sourceforge.net/doc/cookbook/cookbook.htm "JUnit Cookbook") for a guide on how to write unit tests with JUnit.

**Writing Meaningful Tests**

It is important to understand how to write meaningful unit tests, which are tests that focus on testing the behavior of the Class Under Test (CUT).  This focus on behavior is the central motivation of [Behavior Driven Development (BDD)](https://behaviour-driven.org/ "BDD"), which teaches that unit tests should be structure in a ["Given, When, Then"](https://web.archive.org/web/20100503031651/http://blog.objectmentor.com:80/articles/2008/11/27/the-truth-about-bdd "Given When Then") structure.  While I would support any team moving to a full BDD implementation using frameworks like [easyb](https://www.easyb.org/ "easyb") or [JBehave](https://jbehave.org/ "JBehave"), it isn't necessary.  If you read Dan North's article on the [evolution of BDD](https://dannorth.net/blog/introducing-bdd/ "Introducing BDD"), he discusses how he struggled to teach developers how to write good unit tests, and the key was semantics.  He first wanted developers to think of their test names as sentences rather following the existing convention of prefixing the word test to the method under test. I really liked this concept and I felt that the focus on behavior coupled with the meaningful test names helps developers significantly.  So came up with these guidelines:

- Each test method should map to a behavior and/or condition of the CUT
- Each test method's name should be descriptive enough to define the behavior/conditions being tested
- Each test method should have assertions that validate the behavior/conditions being tested

Sounds great, but I think that an example might be more meaningful.  Let's imagine we are testing a utility class that will convert Celsius to Fahrenheit and back again.  When we stub out a unit test class, this is what it looks like.

<pre class="prettyprint language-java">
public class TemperatureUtilTest
{
  @Test public void testCanConvertCelsiusToFahrenheit () {...}
  @Test public void testCanHandleNullFahrenheit() {...}
  @Test public void testCanHandleInvalidFahrenheit() {...}
  @Test public void testCanConvertFahrenheitToCelsius() {...}
  @Test public void testCanHandleNullCelsius() {...}
  @Test public void testCanHandleInvalidCelsius() {...}
}
</pre>

What is great about this format, as Dan North points out, is if you remove the Java syntax and the word "test" from the code, you are left with:

> TemperatureUtil...
>
> ...can convert Celsius to Fahrenheit.
>
> ...can handle null Fahrenheit.
>
> ...can handle invalid Fahrenheit.
>
> ...can convert Fahrenheit to Celsius.
>
> ...can handle null Celsius.
>
> ...can handle invalid Celsius.

This serves as a good depiction of the behavior of the CUT.   This is a good example of how unit tests can serve as documentation.  One of the problems with this documentation is that it is deeply embedded in the code and not easily extracted, a problem which is further addressed with BDD and [Acceptance Test Driven Development (ATDD)](https://www.methodsandtools.com/archive/archive.php?id=72 "Acceptance Test Driven Development") frameworks and tools.

**Write Simple Assertions**

Now that you have your test methods stubbed out, start writing your tests.  Your tests should be short and sweet, passing in the control input data to the method under test and then asserting the results of the test.  Many sites state that a unit test should only have one assertion and I agree with the philosophy of this approach, but I don't enforce it in practice.  Rather, your assertions should be testing the behavior, which may mean more than one assertion.  Let's look at a quick example of a test method.

<pre class="prettyprint language-java">
@Test public void testCanConvertCelsiusToFarhenheit()
{
  Fahrenheit fTemp = new Fahrenheit(32);
  Celsius cTemp = TemperatureUtil.convert(fTemp);
  assertEquals(0, celsius.getTempAsInt());
}
</pre>

Pretty straight forward.

**Some final thoughts**

I definitely do not consider this the most comprehensive approach to unit testing, but I think that some of this information could be helpful to somebody starting out.  Good luck and remember that unit testing can be fun!
