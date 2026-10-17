title=How to install OpenJDK 6 on Ubuntu 10.04 (Lucid Lynx)
date=2012-09-08
type=post
tags=apt-get, debian, openjdk, puppet, ubuntu, vagrant
status=published
~~~~~~
I have been playing with [Vagrant](https://developer.hashicorp.com/vagrant "Vagrant") and [Puppet](https://www.puppet.com/community/open-source/open-source-puppet "Puppet") a lot recently and I had some trouble getting [OpenJDK 6](https://openjdk.org/projects/jdk6/ "OpenJDK 6") to install on [Ubuntu 10.04 (Lucid Lynx)](https://old-releases.ubuntu.com/releases/lucid/ "Ubuntu 10.04").  Since I had so much trouble finding the consolidated answer, I've decided to post my solution here.

The problem stems from the fact I want to install OpenJDK via a [Debian package](https://www.debian.org/distrib/packages "Debian package").  Lucid Lynx's default [apt](https://ubuntu.com/server/docs/how-to/software/package-management/ "AptGet") setup doesn't include the correct repositories needed to install OpenJDK.  To get this to work, you need to add the correct repository to apt.  I accomplished this by using the 'add-apt-repository' python library, like so:<br />

```bash
$> sudo apt-get install python-software-properties
$> sudo add-apt-repository "deb http://archives.canonical.com/ lucid partner"
```

Once this is done, you need to update the the apt-get cache.  You can do this with the following command:

```bash
$> sudo apt-get update
```

Now that apt-get is all up to date, you can finally install OpenJDK 6 JRE with the following command:

```bash
$> sudo apt-get install openjdk-6-jdk
```

I hope this helps!
