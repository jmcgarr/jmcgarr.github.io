title=Gotcha when changing TeamCity's default port
date=2011-11-17
type=post
tags=continuous integration, teamcity
status=published
summary=I ran into an issue today where I changed my TeamCity default port (from 8080) and found that my build agents were suddenly disconnected.  After some fiddling around, I discovered that I needed to manually change each Build Agent's configuration to point to the new master server port.
~~~~~~
I ran into an issue today where I changed my [TeamCity default port (from 8080)](http://stackoverflow.com/questions/2387375/teamcity-change-port-for-web-server "StackOverflow on changing TeamCity's default port") and found that my build agents were suddenly disconnected.  After some fiddling around, I discovered that I needed to manually change each Build Agent's configuration to point to the new master server port.

To make this change, open up the $TEAMCITY_HOME/buildAgent/conf/buildAgent.properties file and update the following value:

<pre>
serverUrl=http:\://localhost\:8080
</pre>

In this case, the agent is running on the same server as the master server.  Once you restart the build agent, the master server recognizes the agent as back online.  Hope this helps.
