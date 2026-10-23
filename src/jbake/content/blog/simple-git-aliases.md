title=Simple Git Aliases
date=2012-05-02
type=post
tags=git
status=published
summary=I have been using Git for a while now and in that time, I have learned the value of using aliases.  Whenever I setup a new machine with git, adding my favorite aliases is first on my list.
~~~~~~
I have been using [Git](https://git-scm.com/ "Git") for a while now and in that time, I have learned the value of using aliases.  Whenever I setup a new machine with git, adding my favorite aliases is first on my list.  I wanted to capture some of my favorite aliases here.  This will serve as not only a reference for myself, but hopefully helpful for others.

The following code snippet shows the alias portion of my \~/.gitconfig.  :

<pre>
[alias]
 st = status -s
 br = branch
 pl = pull origin master
 rm = remote -v
 lg = log --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)%Creset' --abbrev-commit --date=relative
</pre>

Here are the commands you can run to add the aliases to your global config.

<pre>
$&gt; git config --global alias.st &quot;status -s&quot;
$&gt; git config --global alias.lg &quot;log --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)&lt;%an&gt;%Creset' --abbrev-commit --date=relative&quot;
$&gt; git config --global alias.pl &quot;pull origin master&quot;
$&gt; git config --global alias.rm &quot;remote -v&quot;
</pre>

Here is a list of some other git aliases:

- <http://superuser.com/questions/169695/what-are-your-favorite-git-aliases>
- <https://www.viget.com/articles/three-magical-git-aliases>
- <https://catonmat.net/git-aliases>
