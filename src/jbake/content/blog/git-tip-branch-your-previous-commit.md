title=Git Tip: Branch your previous commit
date=2012-08-21
type=post
tags=git
status=published
summary=I just made a commit (6452234) to master, but realized that is should have been committed to a branch and I want master pointing at a previous commit (bb5331e).  How do I resolve this?
~~~~~~
I just made a commit (6452234) to master, but realized that is should have been committed to a branch and I want master pointing at a previous commit (bb5331e).  How do I resolve this?

<pre>
C:\code\project&gt;git lg
* 6452234 - (HEAD, master) reformat test variables
* bb5331e - (origin/master) more unit tests
</pre>

First, create the new branch where you would like your commit to live:

<pre>
git checkout -b cool-feature
</pre>

Now your master and the new branch are pointing to the same commit, like this:

<pre>
C:\code\project&gt;git lg
* 6452234 - (HEAD, master, cool-feature) reformat test variables
* bb5331e - (origin/master) more unit tests
</pre>

Go back to the master branch.

<pre>
git checkout master
</pre>

Now, reset your master branch to point the revision that you want to go back to:

<pre>
git reset --hard bb5331e
</pre>

Master is now pointing to the old revision and the new revision is on the branch only.  Checking the log, we can confirm this:

<pre>
C:\code\project&gt;git lg
* bb5331e - (HEAD, master, origin/master) more unit tests
</pre>

To confirm that the commit has been moved to your branch, just checkout the branch and run the log:

<pre>
C:\code\project&gt;git checkout cool-feature
Switched to a new branch 'cool-feature'
C:\code\project&gt;git lg
* 6452234 - (HEAD, cool-feature) reformat test variables
* bb5331e - (master, origin/master) more unit tests
</pre>

Special thanks to [Mark's great blog entry](https://longair.net/blog/2009/04/16/git-fetch-and-merge/) on not using Git pull. Also if you want to know how I got my Git logs to show up using 'git lg', check out my <a href="/blog/simple-git-aliases.html" target="_blank">previous entry on git alias</a>.
