title=Vagrant Cheatsheat
date=2012-09-06
type=post
tags=cheatsheet, vagrant
status=published
summary=I have been using Vagrant on and off for a while and I find myself forgetting some of the commands.  Instead of looking through Vagrant documentation to find the specific commands that I was looking for, I decided to capture the commands here.
~~~~~~
I have been using Vagrant on and off for a while and I find myself forgetting some of the commands.  Instead of looking through Vagrant documentation to find the specific commands that I was looking for, I decided to capture the commands here.  I hope that this reference helps somebody out.

- [vagrant box add](https://developer.hashicorp.com/vagrant/docs/cli/box#box-add "Installing a Box") - allows you to install a box (or VM) to the local machine
- [vagrant box remove](https://developer.hashicorp.com/vagrant/docs/cli/box#box-remove "Removing Boxes") - removes a box from the local machine
- [vagrant box list](https://developer.hashicorp.com/vagrant/docs/cli/box#box-list "List Vagrant Boxes") - lists the locally installed Vagrant boxes
- [vagrant init](https://developer.hashicorp.com/vagrant/docs/cli/init "Vagrant Project Setup") - initializes a project to use Vagrant
- [vagrant up](https://developer.hashicorp.com/vagrant/docs/cli/up "Your First Vagrant Environment") - starts up the vagrant VM
- [vagrant suspend](https://developer.hashicorp.com/vagrant/docs/cli/suspend "Suspend Vagrant") - saves the state of the current VM.
- [vagrant resume](https://developer.hashicorp.com/vagrant/docs/cli/suspend "Resume the VM") - will load up the suspended VM.
- [vagrant halt](https://developer.hashicorp.com/vagrant/docs/cli/halt "Halting a Vagrant VM") - will shut down the VM, saving configuration. (restart with 'up' command)
- [vagrant destroy](https://developer.hashicorp.com/vagrant/docs/cli/destroy "Destroy the VM") - will destroy the VM with all config changes.
- [vagrant reload](https://developer.hashicorp.com/vagrant/docs/cli/reload "Apply Port Forwarding") - apply Vagrant configuration changes (like port forwarding) without rebuilding the VM.
- vagrant status - tells you the current state of the Vagrant project's VM
- vagrant gem - install Vagrant plugins via RubyGems
- [vagrant ssh](https://developer.hashicorp.com/vagrant/docs/cli/ssh "Vagrant SSH") - short cut to SSH into the running VM
- [vagrant package](https://developer.hashicorp.com/vagrant/docs/cli/package "Packaging a Vagrant Project") - create a distribution of the VM you have running.
- [vagrant \<command> --help](https://developer.hashicorp.com/vagrant/docs/cli "Vagrant Help") - Command that will provide man pages for a vagrant command.

There is extensive documentation on the Vagrant website.  I hope this helps!
