title=Improving my Shell-fu: Oh My Zsh
date=2013-03-19
type=post
tags=bash, oh-my-zsh, shell-fu, zsh
status=published
summary=I have been recently diving head first into the wonderful world of the shell.  Having spent a fair amount of my career on Windows machines at work (even though I have been using Mac's at home for 6+ years), it's only in the past year that I have made the full switch to 100% Mac for work and home.
~~~~~~
I have been recently diving head first into the wonderful world of the shell.  Having spent a fair amount of my career on Windows machines at work (even though I have been using Mac's at home for 6+ years), it's only in the past year that I have made the full switch to 100% Mac for work and home.  I have found that this has enabled me to focus on learning more about the shell.  (no more cmd for me!)

As part of this experience, I have also picked up a bunch of tips and tools from a bunch of my co-workers here at [Blackboard](https://www.blackboard.com/ "Blackboard").  I also have been picking up some tips from various other sources, most notably the [Changelog podcast](https://changelog.com/podcast "Changelog podcast").  I wanted to start sharing of these tools, setups and experiences since I am excited about them.  Rather than one giant blog, I will release them in a series of blogs called <a title="Shell-fu series of blogs" href="/tags/shell-fu.html" target="_blank">Shell-fu</a>.  The first Shell-fu blog is on [Oh My Zsh](https://github.com/ohmyzsh/ohmyzsh "Oh my Zsh") (OMZ).

## Oh My Zsh

I have heard about [zsh](https://www.zsh.org/ "Zsh") for a while and I knew people were switching over in droves.  It wasn't until I heard the Changelog podcast [episode on Oh My Zsh](https://changelog.com/podcast/61 "Changelog podcast on Oh My Zsh") that I decided to give it a try.  I was impressed with how simple it was to install and setup (Note: make sure you backup all your dot files from your home directory first, just in case):

<pre class="prettyprint linenums language-bash">
curl -L https://github.com/robbyrussell/oh-my-zsh/raw/master/tools/install.sh | sh
</pre>

These are the OMZ killer features.

## Zsh

The best feature of Oh My Zsh is Zsh itself.  I am just learning Zsh, but so far there are immediately two things it does much better than bash.

### Autocomplete

Zsh is built with an autocomplete feature that is awesome.  When I type 'vi \<tab>', zsh will present me with a list of the files in my current directory.  If I press \<tab> a second time though, it highlights the first one in the list.  At which point I can just cycle through the files with my arrow keys and select the one I want.  Pretty cool huh?  the autocomplete can also help you see what commands are in your path.  Type 'v \<tab>' and I can see all the commands that start with v.

The autocomplete is very much command aware as well.  [Mike Grouchy's](https://mikegrouchy.com/ "Mike Grouchy") [Zsh blog](https://mikegrouchy.com/blog/zsh-is-your-friend/ "Zsh is your friend") shows how when you type 'kill \<tab>', you get a list of the available processes you can kill.  Now that is awesome!

I also stumbled upon this feature recently.  The autocomplete works with your history to combine into an awesome feature.  If I type 'ssh \<up arrow>' it will go back into my history to the last ssh command I ran.  Wow!

### Autocorrect

If you type a command, Zsh will help you out.  For instance, if I type 'vargant up \<enter>', Zsh stops me and says are you sure this command is right.  Looks like you are trying to type 'vagrant'.  It gives you four options: (n)o, (y)es, (a)bort, and (e)dit.  (n)o will run the uncorrected command, (y) runs the autocorrected command, (a) exits to a blank prompt and (e) exits to a prompt with the command you wrote written out.

<pre>
mmcgarr@mmcgarrmbp:~/Code » vargant up
zsh: correct 'vargant' to 'vagrant' [nyae]?
mmcgarr@mmcgarrmbp:~/Code » vargant up
</pre>

## Themes

At first, I wanted to try OMZ because I wanted to checkout all of the themes.  This sounds dumb, but I was curious how if there were any good shell themes out there.  I had been previously using [dotfiles](https://github.com/jmcgarr/dotfiles "dotfiles") on Bash and aware of some the things you can add to your prompt.  OMZ has over 60 themes you can try out and are easily configured via the \~/.zshrc file.  I tried the 'random' feature at first which shows a new theme for every prompt you open.  This is a great way to explore the themes.  I have currently settled on 'dpoggi' theme (see screenshot below).  Check out all the themes [here](https://github.com/ohmyzsh/ohmyzsh/wiki/themes "Oh my Zsh themes").

Additionally, you will note in the below screenshot, that my prompt says '(master)' with a lightning bolt.  Most of the themes have built in git awareness.  Which means when you are in the directory of a git project, it will show you which branch you are in, as well as whether or not you uncommitted changes (the lightning bolt).  Each of them does this a bit different.  Worth exploring.

## Plugins

The next thing I checked out were the plugins.  OMZ comes with a bunch of plugins.  These plugins add shell magic for specific frameworks, like auto-complete and additional context information.  Of course you can see that these plugins are building upon Zsh's awesome auto-complete features.  For example, I added the vagrant OMZ plugin and when I type 'vagrant \<tab>', it presents me with a list of the available commands for vagrant.

<img class="aligncenter size-large" alt="Terminal: typing 'vagrant' and pressing Tab lists Vagrant's commands (box, destroy, halt, init, up and more) with a description of each" src="/img/ohmyzsh-vagrant.png" width="594" height="256" />

Enabling/disabling a plugin is as simple as editing the \~/.zshrc file and updating the following line.  As you can see, I enabled the vagrant, knife and gradle plugins (git enabled by default).

<pre class="prettyprint linenums">
plugins=(git vagrant knife gradle)
</pre>

For a list of all the plugins, check out [this wiki page](https://github.com/ohmyzsh/ohmyzsh/wiki/Plugins "Oh My Zsh Plugins").

## Conclusion

OMZ and Zsh have a ton of other features that I will not get into in this blog post.  You should definitely check out their [cheatsheet](https://github.com/ohmyzsh/ohmyzsh/wiki/Cheatsheet "Oh My Zsh Cheatsheet") as well.  If you are on a Mac and just using bash, I highly recommend trying Oh My Zsh.  It is awesome and worth it!
