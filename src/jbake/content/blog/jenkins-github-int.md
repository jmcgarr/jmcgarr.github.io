title=Jenkins and GitHub: Integration
date=2011-12-30
type=post
tags=git, github, jenkins
status=published
summary=I've been trying to setup a Jenkins build of a private GitHub project recently and found it wasn't as straight forward as I thought.  GitHub provides a feature called Deploy Keys, which are SSH keys used to authenticate servers to your GitHub repository.
~~~~~~
I've been trying to setup a [Jenkins](https://www.jenkins.io/ "Jenkins CI Server") build of a private [GitHub](https://github.com/ "GitHub") project recently and found it wasn't as straight forward as I thought.  GitHub provides a feature called [Deploy Keys](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys "Github's Deploy Keys"), which are SSH keys used to authenticate servers to your GitHub repository.  Here's how I set them up.

1. On your Jenkins server, generate a new SSH key if you don't already have one.  (Make sure you are logged in as the user that Jenkins runs as): ssh-keygen -t rsa
2. Log into your GitHub project and click the project's admin button.
3. Click the Deploy Keys link and add a new key.
4. Copy the contents of the public key you just generated (id_rsa.pub) into the GitHub Deploy Keys field.

That's it!  Now, using the [Jenkins Git plugin](https://plugins.jenkins.io/git "Jenkins Git Plugin"), you can point your Jenkins Build Job at your GitHub repository and it should authenticate just fine.

This will only work for one repository.  GitHub requires that your Deploy Keys be unique among your repositories.  Ideally, you should be able to create multiple SSH keys on your Jenkins server, but I had trouble getting this to work.  What I ended up doing instead was [adding the SSH public key](https://docs.github.com/en/get-started/git-basics/set-up-git "Git Setup") to my [GitHub user account](https://github.com/jmcgarr "Mike McGarr's GitHub account").  This allows Jenkins build access to any of the private repositories I create.

I would ideally like to get the<a title="Jenkins GitHub Plugin" href="https://plugins.jenkins.io/github" target="_blank"> Jenkins GitHub plugin</a> working, but after a 10 minute test, I stuck with my current setup so that I can move on to the next problem.
