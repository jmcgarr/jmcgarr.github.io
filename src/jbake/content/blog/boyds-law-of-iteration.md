title=Boyds Law of Iteration
date=2013-02-15
type=post
tags=boyd's law, continuous delivery, devops
status=published
~~~~~~
My boss [Steve](https://x.com/PerfForensics "Steve Feldman") asked me the other day why I have never posted anything about Boyd's Law of Iteration.  "Your blog's name is 'Early and Often', right?".  He's right, I have no excuse.

Boyd's Law is the basis for most popular software development approaches.  [Lean](https://en.wikipedia.org/wiki/Lean_manufacturing "Lean"), [Agile](https://agilemanifesto.org/ "Agile"), [Continuous Delivery](https://continuousdelivery.com/ "Continuous Delivery"), [DevOps](https://en.wikipedia.org/wiki/DevOps "DevOps"), [Lean Startup](https://theleanstartup.com/ "Lean Startup")...all have their foundation in the concept that you release changes faster and more frequently.  In other words, you must iterate quickly.  Furthermore, the speed you can iterate is more important than the quality of iteration.

John Boyd was pilot and aircraft designer interested in dogfights between MiG-15s and F-86s.  From A Better Path to Enterprise Architectures (via [Coding Horror](https://blog.codinghorror.com/boyds-law-of-iteration/ "Boyd's Law of Iteration")):

> Boyd decided that the primary determinant to winning dogfights was not observing, orienting, planning, or acting better. The primary determinant to winning dogfights was observing, orienting, planning, and acting *faster*. In other words, how quickly one could iterate. *Speed of iteration*, Boyd suggested,*beats quality of iteration*.

This view is somewhat counterintuitive to how people and organizations approach quality.  If something is broken or hard or painful, the immediate reaction is to slow down, take your time, and wait until you are absolutely sure before you move forward.  This leads to long iterations and what lean practitioners would call large batches (more on [small batches](https://www.startuplessonslearned.com/2009/02/work-in-small-batches.html "Small Batches")).

Imagine you want to fire a rocket to hit a target.  You have two rockets, one has a laser guidance system and the other has no guidance system.  Which of these rockets is more likely to hit the target?  Even if you were to fire the guided missile in the completely opposite direction, I am willing to bet you would still put your money on it.  Why?  Because the guided missile is capable of making small adjustments while in flight to ensure it is always aimed at the target.  These adjustments can compensate for changes in the wind, air density and anything else that impacts flight (not an aeronautical engineer, sorry).

The missile without a guidance system has one iteration and one iteration only to hit it's target.  If your aim is off by a fraction of a degree, it can miss the target by a mile.  Guided missiles win because they are able to iterate quickly.  They adapt to changes (like a moving target).  Like the guided missile, organizations that can release software in frequently are able to adjust quickly.  Organizations that plan, do, check and act slowly often find themselves months or years into a project with little to show, other than they are aiming at the wrong target.

In the book Continuous Delivery, the authors define 8 Principles of Software Delivery, the fourth being "If it hurts, do it more frequently, and bring the pain forward".  I find this principle a great way to get people thinking about how they can iterate faster.  It takes a lot of work to implement this but it provides a tangible rule for people to apply.

It is also important to mention that when thinking of iterations, you should think about the whole value stream and not just your team's role.  An iteration is how long it takes an idea (even before it is a "requirement") to get into the hands of customers (deployed or shipped).  It is not the time it takes the development to implement the requirements and deliver to QA.  You must look at how the whole organization works to deliver value (aka ship to customers), not just a portion of the process.  You can't optimize engineering and not consider QA, for instance.  [Optimize the whole](https://web.archive.org/web/20121215054320/http://www.allaboutagile.com/lean-principle-7-optimise-the-whole/ "Optimize the whole").
