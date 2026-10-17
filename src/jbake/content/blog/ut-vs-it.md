title=Unit vs. Integration Tests
date=2011-06-24
type=post
tags=integration testing, testing, unit testing
status=published
~~~~~~
I have been doing more and more testing recently and I wanted to start capturing some of my thoughts on testing.  An important topic is the differences between a unit test and an integration test.  I feel like this is pretty straight forward, but is still worth clarifying, since I feel like it is done incorrectly way too often.  My definition of each breaks down as follows:

**Unit Tests:**

- Focused on defining (not testing) the behavior of a particular class, and that class only.
- Fast (less than 0.1 second per test)
- No external implementation dependencies (filesystem, database, web services, etc.).  All dependencies are faked for the test context.
- Can easily be parallelized, since each test is atomic

**Integration Tests:**

- Focused on verifying the integration of one or more components together.
- May have external dependencies. (in fact, it is likely testing the integration with this dependency)
- A test that takes longer than a unit Test should (longer than 0.1 seconds per test)

Both unit and integration are focused on the internal quality of the application, whereas acceptance tests focus on the external, or business quality of the application.

**Code Coverage**\
I feel very strongly that developers should focus on achieving >80%  code coverage by unit tests only.  Unit tests should target happy case logic as well as exception logic.  With integration tests, it is not necessarily to target a high code coverage.  Integration test coverage needs to cover integration points.  For instance, write integration tests to ensure your Hibernate database mappings are wired correctly.  Exceptions that could result from a integration failure should be defined behavior in the unit test, and expressed easily through fakes.

**Summary**

Your team should have a testing strategy that focuses on using both unit and integration tests, as both have a different focus and goal.  If you have additional criteria to add to this discussion, please feel free to share it.
