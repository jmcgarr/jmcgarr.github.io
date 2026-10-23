title=Octopress on OS X
date=2012-05-05
type=post
tags=bundler, octopress, rake, ruby, rvm
status=published
summary=I have been wanting to try Octopress for a while, so I decided to spent my Friday night setting it up.  Unfortunately, I ran into a few issues installing and getting my environment setup, and I figured I would share my setup issues with everybody else.
~~~~~~
I have been wanting to try [Octopress](https://octopress.org/ "Octopress") for a while, so I decided to spent my Friday night setting it up.  Unfortunately, I ran into a few issues installing and getting my environment setup, and I figured I would share my setup issues with everybody else.  I started with the instructions from the Octopress website.

<https://octopress.org/docs/setup/>

As you run into issues, the solutions are listed below.

## RVM and Ruby Installation

First, you will need to install [RVM](https://rvm.io/ "RVM").  I followed the instructions I found on the [Pragmatic Press article](https://pragmaticstudio.com/blog/2010/9/23/install-rails-ruby-mac) for installing [Ruby](https://www.ruby-lang.org/en/ "Ruby") 1.9.  You can continue on with the [Rails](https://rubyonrails.org/ "Ruby on Rails") portion of the installation, but you don't need to for Octopress.  The only thing I would change is the version mentioned in the instructions from 1.9.3 to 1.9.2-p320, which is the version needed for Octopress.

## Bundler

Once you get Ruby installed, you will also hit issues with installing bundler with [rb-fsevent](https://github.com/guard/rb-fsevent).  I found this [bug report](https://github.com/guard/rb-fsevent/issues/26) that provides the [solution](https://github.com/guard/rb-fsevent/issues/26#issuecomment-4050279) (mad props [clippit](https://github.com/clippit)).  In short, you will need to run the following command before setting up Octopress:

<pre>
sudo xcode-select -switch /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/
</pre>

## Rake

When I ran the 'rake install' command, I got the following error:

<pre>
$ rake install rake aborted! You have already activated rake 0.9.2.2, but your Gemfile requires rake 0.9.2. Using bundle exec may solve this.
</pre>

There is also a [bug report](https://github.com/imathis/octopress/issues/250) for this on Octopress, but it was easy enough to fix by just updating the Gemfile.lock to point to rake 0.9.2.2 instead of 0.9.2.  I imagine there is a better way to do this with Ruby, but since this is my first foray into Ruby development.

That should resolve your issues.  Best of luck!
