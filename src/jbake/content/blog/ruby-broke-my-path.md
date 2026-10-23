title=Ruby Broke my $PATH
date=2012-05-06
type=post
tags=bash, grails, octopress, ruby, rvm
status=published
~~~~~~
Yesterday I spent some time <a title="Octopress on OS X" href="/blog/octopress-on-os.html" target="_blank">getting Ruby up and running on my Mac</a>, with the intent of playing with [Octopress](https://octopress.org/ "Octopress").  Today, I wanted to go back to a [Grails](https://grails.org/ "Grails") app I had been working on, when I realized that Grails was no longer in my PATH.  Somehow, my PATH variable had been "hijacked" by the [Ruby](https://www.ruby-lang.org/en/ "Ruby") or [RVM](https://rvm.io/ "Ruby RVM") installs.  I immediately started to investigate the issue.

```bash
$> grails run-app
-bash: grails: command not found
```

First, I double checked my *\~/.profile* file, where I added Grails to my PATH.  This was unchanged, but Grails was stil not being added to my PATH.

```bash
export GRAILS_HOME=~/Tools/grails-1.3.7
export PATH=/opt/local/bin:/opt/local/sbin:$GRAILS_HOME/bin:$PATH
```

Ok, so *.profile* was no longer being run when I opened Terminal.  Interesting.  Now if you are a Unix sys admin, the solution will seem immediately apparent.  I had a strong suspicion that the issue was that some _\~/.bash\*_ files were being run instead of the *.profile*.  I Googled around and learned more about how about the differences between *\~/.bash_profile*, *\~/.bash_login*, *\~/.bashrc*, and my *\~/.profile*.

Turns out my suspicion was correct.  The Ruby RVM installation had created a *.bashrc*, and now when my Bash shell logged in, it only ran the _.bash\*_ files and not my *.profile*.  In order to fix this, I had to tell Bash to load both my *.profile* and my *.bashrc*.  A simple way to do this was to add the following two lines to my *\~/.bash_profile*:

```bash
. ~/.profile
. ~/.bashrc
```

Once this was added and I reloaded Terminal, my Grails path entries were back and Ruby was still working fine.  Success!

For more on how Bash shell operates, check these useful resources out:

- [http\://stefaanlippens.net/bashrc_and_others](https://www.stefaanlippens.net/bashrc_and_others/)
- [https\://www\.joshstaiger.org/archives/2005/07/bash_profile_vs.html](https://www.joshstaiger.org/archives/2005/07/bash_profile_vs.html)
