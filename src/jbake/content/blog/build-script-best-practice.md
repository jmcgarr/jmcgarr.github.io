title=Build Scripts and External Dependencies
date=2013-02-15
type=post
tags=build automation, build scripts, gradle, maven
status=published
~~~~~~
[<img align="right" alt="xkcd comic 'RTFM': a man calls 911 after his toaster stabs him, and the operator asks whether he read the toaster's man page first" src="https://imgs.xkcd.com/comics/rtfm.png" width="245" height="304" />](https://xkcd.com/293/) I recently ran across some project code that when checked out of version control, failed with an obscure warning.  After some digging around in the code, I realized the script made a reference to an external plugin library.  I had no clue where to acquire this library or setup this build.  I had to reach out to other developers to find out what was causing the failure.  The dependency turned out that a second project needed to be checked out from source control and added to the PATH.  I was also told that I should have [RTFM](https://en.wikipedia.org/wiki/RTFM "Read the Fucking Manual").

**No External Dependencies**

I strongly believe that a project checked out from version control should be as simple and independent as possible.  I should just grab the code and run the build.  I try to avoid at all costs having environment dependencies, save the bare minimum (like a JDK and build tool installed).  This will allow teams to quickly switch between projects and be productive quickly.

> *__Note__: [Gradle](https://www.gradle.org/ "Gradle") provides an awesome way to minimize external dependencies even further.  The [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html "Gradle Wrapper") is a small bit of code you check-into your project that downloads and runs Gradle.  All you need is Java installed on the machine.  The [Grails](https://grails.org/ "Grails") project has also adopted this approach with the [Grails Wrapper](https://grails.apache.org/docs/2.1.0/guide/single.html#wrapper "Grails Wrapper").    I think this is an awesome step in the right direction.*

Modern build tools do a great job of managing external dependencies such as libraries, but usually provide a facility to customize build execution through a personal file in your home directory.  I generally avoid these types of customizations.

**If you must...**

If however, your build script must rely on an external dependency, then it should check and warn the user explicitly.  The error message I see when I run the build should tell me exactly what I need to do to fix the problem (like where the "f\*cking manual" is and what page to read).  This makes debugging build scripts for new users straightforward.

**...and fail fast**

Additionally, if your build script is going to fail because of a missing external dependency, do it quickly.  If your build script's *deploy* phase depends on a password file being in your home directory, then ideally it should fail as soon as I call the build script with an appropriate warning.  I shouldn't have to wait on the *compile*, *unit tests* and *package* phases to finish before it fails.  This may be hard to do with some build tools (looking at you Maven), but others make this very easy (love you Gradle).

<b> Fail intelligently</b>

Lastly, if I call the *compile* phase, don't fail the build if I am missing a dependency needed for the *deploy* phase.  I should only see a warning if I am trying to execute the task.  I may want to write a build script that requires secure access to a username and password on the continuous integration server, but developers will never execute this phase.  I don't want them to have to setup a username and password for something they will never execute.

Gradle makes this very easy to do.  Gradle has three phases of build execution: initialization, configuration and execution.  The initialization phase is to build a project graph (for multi-module projects), the configuration phase builds a task graph and the execute phase executes the project tasks.  You can write code that checks the contents of the task graph and conditionally executes logic.  For information on how to do this in Gradle, [read the f\*cking manual](https://docs.gradle.org/current/userguide/build_lifecycle.html#task_graph "Gradle: Configuring the DAG").
