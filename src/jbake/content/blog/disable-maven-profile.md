title=Maven Tip: Disabling Profiles when a Property Does Not Exist
date=2011-02-09
type=post
tags=build automation, Java, liquibase, maven, maven profiles, unit testing
status=published
summary=I wanted to share a method for enabling a Maven Build Profile in the absense of a system property.  I did not see this approach documented in the current version of the Maven Build Profile documentation, so I thought I would share it.
~~~~~~
I wanted to share a method for enabling a [Maven](https://maven.apache.org/ "Maven") [Build Profile](https://maven.apache.org/guides/introduction/introduction-to-profiles.html "Maven Profiles") in the absense of a system property.  I did not see this approach documented in the current version of the Maven Build Profile documentation, so I thought I would share it.

I find this approach particularly useful for turning off build features whenever you are disabling tests.  In Maven, there are two properties that will [skip the execution of all tests](https://maven.apache.org/surefire/maven-surefire-plugin/examples/skipping-tests.html "Skipping tests"), but I prefer the "skipTests" property.

As an example, let's take a Maven [POM](https://maven.apache.org/pom.html#What_is_the_POM "Maven Project Object Model") file that runs <a title="Managing Database Changes with Liquibase" href="/blog/intro-to-liquibase.html" target="_blank">Liquibase </a>to update the database.   If you want to run a Maven install without executing the tests, then you would run the following command (Note: I am not advocating that tests be skipped, but I think this is a useful example):

<pre>
mvn clean install -DskipTests
</pre>

If I wanted to also disable Liquibase whenever the tests were skipped, I would configure Liquibase is a profile.  This profile would be triggered in absence of the  the "skipTests" property.  This means that when the property is not declared, Liquibase will run.  Here is how you would declare this profile:

<pre class="prettyprint language-xml">
&lt;profiles&gt;
   &lt;profile&gt;
      &lt;id&gt;update-database&lt;/id&gt;
      &lt;activation&gt;
         &lt;property&gt;
            &lt;name&gt;!skipTests&lt;/name&gt;
         &lt;/property&gt;
      &lt;/activation&gt;
      ...
      &lt;!-- Liquibase Plugin Configuration Here --&gt;
      ...
   &lt;/profile&gt;
&lt;/profiles&gt;
</pre>

An alternative to this approach is to turn off the profile explicitly:

<pre>
mvn clean install -DskipTests -P !update-database
</pre>

Using this approach does make it clear what you are trying to do, which is also an advantage.  There is an advantage in turning features on and off based on a related trigger, like a single system property.

In regards to the [Liquibase Maven plugin](https://docs.liquibase.com/community/integration-guide-5-0-4/maven-goals "Liquibase Maven Plugin"), a feature was added to [allow property based skipping](https://liquibase.jira.com/browse/CORE-495 "Skip Liquibase Feature").  This is only available as of [Liquibase 2.0](https://web.archive.org/web/20110224023221/http://blog.liquibase.org:80/2011/01/liquibase-2-0-officially-released.html "Liquibase 2.0 Released").
