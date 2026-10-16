title=Maven, Cobertura and AspectJ
date=2010-01-04
type=post
tags=aspectj, cobertura, Java, maven
status=published
~~~~~~
I recently ran into an issue running Cobertura code coverage reports on my Maven2 project.  It appears that Cobertura is not being run, since the code coverage reports always showed 0% code coverage in spite of the existence of numerous unit tests.  This problem had appeared early in the project and seemed to disappear magically, so I wrote it off.  Now that it is back, I was determined to find the answer, and I have.

Cobertura is a code coverage tool that measures the amount of code exercised by your unit tests.  Cobertura works by instrumenting your application's byte code, which occurs after compilation.  When the instrumented code is called by your unit tests, Cobertura records which packages, files, lines, methods and even conditions are covered by your unit tests.

When the problem started to occurred, I spent some time analyzing Maven build outputs from successful Cobertura executions and Cobertura executions that resulted in 0% coverage (thanks to Hudson for storing all of this historical information).  Here is the output from the failing Maven build:

<pre>
[INFO] Preparing cobertura:cobertura
[INFO] [aspectj:compile {execution: default}
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] [resources:resources {execution: default-resources}
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 16 resources
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] [compiler:compile {execution: default-compile}
[INFO] Nothing to compile - all classes are up to date
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] [cobertura:instrument {execution: default-instrument}
[INFO] Cobertura 1.9.2 - GNU GPL License (NO WARRANTY) - See COPYRIGHT file
Instrumenting 86 files to /opt/data/hudson/jobs/Trunk-Nightly/workspace/main/common/target/generated-classes/cobertura
Cobertura: Saved information on 86 classes.
Instrument time: 2161ms
[INFO] Instrumentation was successful.
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] [resources:testResources {execution: default-testResources}
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 16 resources
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] Preparing hibernate3:hbm2ddl
[WARNING Removing: hbm2ddl from forked lifecycle, to prevent recursive invocation.
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] [aspectj:compile {execution: default}
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] [resources:resources {execution: default-resources}
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 16 resources
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] [hibernate3:hbm2ddl {execution: default}
[INFO] Configuration XML file loaded: file:/opt/data/hudson/jobs/Trunk-Nightly/workspace/main/common/src/main/resources/hibernate.cfg.xml
[INFO] Configuration XML file loaded: file:/opt/data/hudson/jobs/Trunk-Nightly/workspace/main/common/src/main/resources/hibernate.cfg.xml
[INFO] Configuration Properties file loaded: /opt/data/hudson/jobs/Trunk-Nightly/workspace/main/common/target/test-classes/jdbc.properties
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] [compiler:testCompile {execution: default-testCompile}
[INFO] Nothing to compile - all classes are up to date
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] [dbunit:operation {execution: default}
[TASKS] Skipping maven reporter: there is already a result available.
[INFO] [surefire:test {execution: default-test}
[INFO] Surefire report directory: /opt/data/hudson/jobs/Trunk-Nightly/workspace/main/common/target/surefire-reports
T E S T S
</pre>

At first glance, nothing looked wrong, but then I realized that Aspectj was being run a second time, after Cobertura had already instrumented the code.  I immediately recognized that this might  be the source of the problem, but didn't know why this was happening.  I started to researched the problem and found numerous posts on the topic, but nothing that seemed to answer my question.  Then I realized that Hibernate's hbm2ddl plugin was firing during the Maven process-test-resources phase.  Once I disabled the plugin, AspectJ no longer fired a second time and Cobertura worked just fine!

Great, but why is this happening?  Why is Hibernate's attachment to the process-test-resources phase triggering AspectJ to re-compile the sources?  The answer lies in the source code for all three plugins.  All three of these Maven plugins have a default lifecycle phase that they attach themselves to.  Here are the Maven Lifecycle phases in order of execution from start to test:

> 1\.  validate\
> 2\.  initialize\
> 3\.  generate-sources\
> 4\.  process-sources (**aspectj:compile**)\
> 5\.  generate-resources\
> 6\.  process-resources (**hibernate3:hbm2ddl**)\
> 7\.  compile\
> 8\.  process-classes (**cobertura:instrument**)\
> 9\.  generate-test-sources\
> 10\. process-test-sources\
> 11\. generate-test-resources\
> 12\. process-test-resources\
> 13\. test-compile\
> 14\. process-test-classes\
> 15\. test

Hmmm, this doesn't seem right, cause according to the default phases, cobertura should have been the last to fire.  Well, it looks like the problem was in my build script, where I had hibernate3:hbm2ddl attached to the process-test-resources phase.  Ok, so now why is hibernate3:hbm2ddl triggering aspectj:compile to run a second time?  This part is still a mystery to me.  I plan on researching this topic further and when I find an answer, I will post an update to this blog entry.  If anybody has any additional insight, please feel free to leave a comment.
