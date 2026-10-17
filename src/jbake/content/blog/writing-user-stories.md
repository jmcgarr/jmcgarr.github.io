title=Writing User Stories
date=2012-05-04
type=post
tags=agile, INVEST, requirements, testing, user stories
status=published
~~~~~~
A friend recently asked me if I had any advise on how to write [user stories](https://en.wikipedia.org/wiki/User_story "User Story (Wikipedia)").  While I don't claim to be an expert on writing user stories, I knew enough to guide him.  Here's what I came up with.

## Why User Stories?

User stories are a terse requirements format that puts emphasis on conversations rather than comprehensive documentation.  User stories are the preferred requirements format for [Agile](https://agilemanifesto.org/ "Manifesto of Agile Software Development") software development projects.  Other requirements formats, like [use cases](https://en.wikipedia.org/wiki/Use_case "Use Cases"), attempt to provide a definitive requirements definition.  Unfortunately, due to their verbose nature, they do not allow for changes to the requirements that are discovered over time.  User stories excel at this.

## The 3 C's

[Ron Jeffries](https://ronjeffries.com/ "Ron Jeffries") defines [3 C's](https://ronjeffries.com/xprog/articles/expcardconversationconfirmation/ "Ron Jeffries 3 C's of User Stories") which are the critical aspects of a user story:

- *__Card__* - user stories should be written on a card.  This helps keep them small.  The card serves a placeholder for the conversation.  The card is what is used during [release planning](http://www.extremeprogramming.org/rules/planninggame.html "Release Planning").
- *__Conversation__* - as previously stated, a user story puts emphasis on having a conversation between the customer and the programmers.  Ron points out that these conversations can be supplemented by documents, and the best documents at examples.
- *__Confirmation__* - this refers to the acceptance criteria or test for each user story.  The customer should communicate the acceptance tests prior to development.  The best type of confirmation for a user story is an example, and preferably automated.

## User Story Format

User stories traditionally conform to the following format:

> As a \<type of user>,\
> I want \<some feature>,\
> So that \<some reason or business value>

Most agile experts would agree that the most important part of a user story is the last line, which defines the business value.  Since this the most important line, I tend to prefer [Elizabeth Keogh](https://lizkeogh.com/ "Elizabeth Keogh")'s [user story format](https://sirenian.livejournal.com/47679.html "Elizabeth Koening's User Story Format"), that looks like this:

> In order to \<some reason or business value>\
> As a \<type of user>,\
> I want \<some feature>

This format places the business value statement first.  When writing a ne story, you are forced to think about the business value before the feature.

## INVEST

The [INVEST](https://xp123.com/invest-in-good-stories-and-smart-tasks/ "INVEST in good stories") model is a way of evaluating the quality of a user story.  Here's what it stands for:

- *__Independent__* - a user story should not be dependent upon another story.
- *__Negotiable__* - the details of the story will be worked out during the conversation.  Talk about the story before working on it.
- *__Valuable__* - the user story needs to be valuable to the customer or user.
- *__Estimable__* - the estimation need not be perfect, but good enough so you can rank and schedule a story.
- *__Small__* - small is good.  Small is easier to estimate.
- *__Testable__* - make sure that you can validate the user story is done.  Write tests!

## Further Reading

For more information on user stories, I highly recommend these resources.  I especially recommend [Fadi Stephan](https://x.com/FadiStephan "Fadi Stephan")'s (a fellow [Excellain](https://excella.com/ "Excella Consutling")) great presentation, [the Art of Storytelling](http://www.agilejourneyman.com/2010/10/art-of-storytelling.html "The Art of Storytelling").

- [Michael Cohn on User Stories](https://www.mountaingoatsoftware.com/agile/user-stories "User Stories")
- [Wikipedia on User Stories](https://en.wikipedia.org/wiki/User_story "Wikipedia on User Stories")
- [Extreme Programming on User Stories](http://www.extremeprogramming.org/rules/userstories.html "User Stories at ExtremeProgramming.com")
- [Ron Jeffries on the 3 C's](https://ronjeffries.com/xprog/articles/expcardconversationconfirmation/ "Ron Jeffries on the 3C's")
- [Elizabeth Keogh on User Story Format](https://sirenian.livejournal.com/47679.html "Elizabeth Keogh on User Story Format")
- [Dan North on User Stories](https://dannorth.net/whats-in-a-story/ "Dan North on User Stories")
