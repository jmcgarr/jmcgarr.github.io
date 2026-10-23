title=Exploring Test Method Names
date=2011-11-10
type=post
tags=agiledox, art of unit testing, bdd, groovy, Java, junit, testing, unit testing
status=published
summary=When coaching individuals on how to name unit test methods, I have struggled to come up with a standard that I like.  I have read numerous posts and books on the subject with a wide variety of results and standards.
~~~~~~
When coaching individuals on how to name unit test methods, I have struggled to come up with a standard that I like.  I have read numerous posts and books on the subject with a wide variety of results and standards.  In this blog I will explore the various standards for naming unit test methods and present my perspective on the subject.

Prior to discussing unit test methods, I want to mention that the naming conventions for unit test *classes* are fairly well solidified.  I will assume that everybody will using the following naming convention for their unit test classes:

> \[NameOfClassUnderTest\]Test

Some conventions specify that the 'Test' postfix be 'Tests'.  I personally don't care about the difference between the two, so long as your team uses one or the other and not both.

**JUnit**

[JUnit](https://www.junit.org/ "JUnit") is one of the first unit testing frameworks and one could argue is responsible for the explosion of unit testing over the past ten years.  In previous versions of JUnit, it was necessary that the name of the unit test method be prefixed with **test**.  This was how the JUnit runner identified unit test methods.  So a unit test method name might look like this:

<pre class="prettyprint language-groovy">
public void testAddingTwoNumbers();
</pre>

This framework limitation unit test method name was overcome in JUnit 4.  In JUnit 4, it is possible to use [Java 5's annotations](https://docs.oracle.com/javase/1.5.0/docs/guide/language/annotations.html "Java 5 Annotations") to tag a method as a test method.

<pre class="prettyprint language-groovy">
@Test public void addingTwoNumbers();
</pre>

Most modern test frameworks no longer force developers to prefix the test method name with the word test.  I prefer to avoid frameworks that impose this convention, but there are [exceptions](https://grails.apache.org/docs/latest/guide/testing.html "Grails unit testing").

**The Art of Unit Testing**

[Roy Osherove](https://osherove.com/blog "Roy Osherove") wrote a great book on unit testing called [the Art of Unit Testing](https://artofunittesting.com/ "The Art of Unit Testing").  While this book was focused on [.NET](https://en.wikipedia.org/wiki/.NET_Framework "Microsoft .NET") testing approaches, there is a lot of good recommendations there applicable to all developers, and I would recommend it.

In this book (as well as his [blog](https://osherove.com/blog/2005/4/3/naming-standards-for-unit-tests.html "Roy's Naming standards")), Roy recommends a unit test method naming convention that uses the following pattern:

> \[MethodName_StateUnderTest_ExpectedBehavior\]

While I appreciate how comprehensive this standard is, I don't personally subscribe to this approach.  I find that this approach doesn't put enough emphasis on the overall behavior of the class under test, but rather inputs and outputs of methods on the class.  When applying TDD, I find that I think more about behavior than I do inputs/outputs.

**Introducing BDD**

Dan North introduced Behavior Driven Development, or BDD in 2006 with an article in Better Software magazine (which you can read [here](https://dannorth.net/introducing-bdd/ "Dan North introducing BDD")).  In this article, Dan discusses the series of revelations he had regarding unit test naming conventions before he came across BDD's Given When Then convention.  I am not going to go over BDD here, but I definitely recommend reading this article.

While I personally love BDD and feel it is extremely valuable in defining human readable tests, I feel it aligns much better with acceptance testing than unit testing.  What peaked my interest in this article was some of the intermediate steps Dan made on his way to BDD, namely "test method names should be sentences".  He references a friend's open source project [agiledox](http://agiledox.sourceforge.net/ "AgileDox") that turned a unit test class into a document describing what the class does.  For instance, given this unit test class:

<pre class="prettyprint language-groovy">
 public class CalculatorTest {
     @Test public void addsTwoNumbers () {...}
     @Test public void subtractNumberFromAnother () {...}
     @Test public void multipliesTwoNumbers () {...}
     @Test public void dividesTwoNumbers () {...}
 }
</pre>

Agiledox would produce documentation for the class that would look like this:

> Calculator
>
> \- adds two numbers
>
> \- subtracts number from another
>
> \- multiplies two numbers
>
> \- divides two numbers

<strong></strong>This was an aha moment for me.  While I have never used agiledox, I found this convention and approach to be extremely simple and easy.  The unit test names now provide a description of the behavior of the class under test.  When I write unit test methods, I imagine these agiledox style sentences being laid out, as a sanity check.

**Focus on Behavior**

For me, the best unit test name should be focused on a particular behavior of the class under test.  How you word the test name is up to you, so long as you focus on behavior.  You should make sure to include exception cases in this definition of the behavior as well.

Another nice side effect of this focus on behavior is it makes it easier to assess whether or not the class under test is violating the [Single Responsibility Principle (SRP)](https://en.wikipedia.org/wiki/Single_responsibility_principle "SRP Wikipedia page").  By quickly looking through the feature list, provided by unit test names, you can determine whether or not the class is doing too much and should be broken up into collaborating objects.

**Side Note: Method Names in Groovy**

Since Groovy allows method names to be strings, this allows you to write a unit test method that looks like this:

<pre class="prettyprint language-groovy">
class CalculatorTest {
   @Test void &quot;adds two numbers&quot;() {...}
   @Test void &quot;subtracts a number from another&quot;() {...}
   @Test void &quot;multiplies two numbers&quot;() {...}
   @Test void &quot;divides two numbers&quot;() {...}
}
</pre>

This allows you to write true sentences without the need for a tool like agiledox to break up the camel case. Another compelling reason to use Groovy for unit testing Java.
