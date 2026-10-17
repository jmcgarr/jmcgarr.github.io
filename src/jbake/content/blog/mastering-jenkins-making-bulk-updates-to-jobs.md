title=Mastering Jenkins: Making Bulk Updates to Jobs
date=2013-03-11
type=post
tags=jenkins, groovy
status=published
~~~~~~
I recently needed to make the same update to a large number of Jenkins jobs.  Essentially, we decided to change our naming convention for our jobs.  A simple change.  In order to achieve this, I took advantage of the Jenkins Script Console.

The Jenkins Script Console allows you to run Groovy code against Jenkins to execute a task.  The Script Console provides access to Jenkins' underlying API's, which allows you to conduct almost any task.  The Script Console is available to any running Jenkins instance at '/script' or through the menus by navigating to 'Manage Jenkins > Script Console'.  In order to effectively use the Script Console, you will likely need to become familiar with [Jenkins' API's](https://javadoc.jenkins-ci.org/ "Jenkins JavaDocs").

<img class="aligncenter size-large wp-image-1402" alt="Jenkins Script Console" src="/img/jenkins-script-console.png" width="594" height="533" />

In my case, I wanted to execute a script that found all jobs matching a particular naming convention and rename them.  In our case, we had two naming conventions being used and we wanted them to all match.  We had jobs names that followed one of two formats:

- camel case, with underscores and dashes like:  'FUN-ArtifactName_Branch-Commit'
- human readable like: 'FUN-ArtifactName (Branch)'

My goal was to rename both sets of job names to look like 'fun-artifactname_branch-commit' (lower case, underscores and dashes).  Here's the script I executed:

<pre class="prettyprint language-groovy">
for (item in Hudson.instance.items) {
    name = item.name
    if (name.startsWith('FUN-')) {
      if (name.endsWith('Commit')) {
        def newName = name.toLowerCase()
        item.renameTo(newName)
      } else {
        def newName = name.replaceFirst(/\s\(/, &quot;_&quot;)
        newName = newName.replaceFirst(/\)/, &quot;-commit&quot;)
        newName = newName.toLowerCase()
        item.renameTo(newName)
      }
    }
}
</pre>

Like I said before, you need to understand Jenkins' underlying API's. In this script, I am iterating through the items on Jenkins (formerly Hudson, hence the old class name). I then grab the name of the item of only rename those items starting with the 'FUN-' prefix. For jobs in the camel case format, they all have a 'Commit' at the end of their name. For them, I just make sure the name is lowercase. For the human readable names, I need to use the replaceFirst method to reformat the names. Also note that I need to call the renameTo() method. That's it!

For more examples of Groovy scripts for the Jenkins Script Console, I recommend you check out the [Jenkins Script Console wiki page](https://www.jenkins.io/doc/book/managing/script-console/ "Jenkins Script Console Wiki Page").
