title=Git Crash Course
date=2012-05-03
type=post
tags=git, tutorial
status=published
~~~~~~
Our current team has decided to adopt Git and some members aren't familiar with it.  I wanted to put together a short introduction to the [Git](https://git-scm.com/ "Git") command line for these team members.  The goal is to make team members productive immediately, while learning the nuances of Git over time.  My introduction assumes that you have Git installed and setup.  For installation instructions, go to <https://git-scm.com/>.  For instructions on configuring Git, I recommend [Github](https://github.com/ "GitHub")'s tutorials for [Windows](https://docs.github.com/en/get-started/git-basics/set-up-git "Setup Github (Windows)"), [OS X](https://docs.github.com/en/get-started/git-basics/set-up-git "Setup GitHub (Mac)") and [Linux](https://docs.github.com/en/get-started/git-basics/set-up-git "Setup GitHub (Linux)").

## Cloning a Repository

First, you need to get a copy of the source code on your machine from a repository.  With Git, you do this by cloning the repository locally.  Since Git is a distributed version control system (DVCS), it will copy the whole source code repository locally.  For more on DVCS, check out [this](https://www.infoq.com/articles/dvcs-guide "Not so quick guide to DVCS").

To clone a remote repository, you will need to use the Git [clone](https://git-scm.com/docs/git-clone "Git clone Man Page") command.  The following is example of clone a public GitHub repository:

<pre>
$&gt; git clone git@github.com:jmcgarr/java-bdd-examples.git
</pre>

## Checking in Code

Now that you have the code, you can begin making changes.  Once you are ready to check in these changes, there are a few steps you need to take to get the changes into a remote repository:

<pre>
$&gt; git status -S
$&gt; git add . -A
$&gt; git commit -m &quot;Add a commit message here&quot;
$&gt; git push origin master
</pre>

Line 1 issues a [*status*](https://git-scm.com/docs/git-status "Git status man page") command, telling you which files have changed locally.  The [add](https://git-scm.com/docs/git-add "Git add man page") command on line 2 tells Git to stage all changed files so that they can be committed to your local repository.  The [commit](https://git-scm.com/docs/git-commit "Git commit Man Page") command on line 3 will commit the changes you added to your local repository.  Line 4 is where you will [push](https://git-scm.com/docs/git-push "Git push Man Page") the changes in your local repository to the remote repository.

> Note: Your fellow team members will not see your changes until you push them to the remote repository.  When you commit, you are only commit the change to your local copy of the repository

## Getting the Latest Version

Prior to pushing your changes to a remote repository, you may want to grab the latest code from the repository.  To get the latest remote changes, use the following commands:

<pre>
$&gt; git fetch origin
$&gt; git merge master
</pre>

Line 1 will sync your local Git repository with the remote Git repository.  Your working copy of the code will not be updated until you issue a merge command (line 2).  Git provides a way of combining these two commands into one, by using the [pull](https://git-scm.com/docs/git-pull "Git pull Man Page") command:

<pre>
$&gt; git pull origin master
</pre>

This command will fetch the remote repository changes, and merge them into the master branch.

## Learn More

Git is a powerful version control tool and it is extremely useful.  This tutorial should get you started immediately.  But be warned, that you should take the time to learn more about Git and how it works.  Here are some useful resources for learning more about Git:

- [https\://git-scm.com/docs](https://git-scm.com/docs "Git Reference")
- [https\://marklodato.github.io/visual-git-guide/index-en.html](https://marklodato.github.io/visual-git-guide/index-en.html "Git Visual Guide")
- [https\://docs.github.com/en/get-started/git-basics/about-remote-repositories](https://docs.github.com/en/get-started/git-basics/about-remote-repositories "GitHub Help")
- [https\://www\.youtube.com/watch?v=ZDR433b0HJY](https://www.youtube.com/watch?v=ZDR433b0HJY "Scott Chacon on Git")
