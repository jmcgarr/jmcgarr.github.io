# Drafts triage (T053)

**Branch**: `post/T053-drafts-triage` | **Created**: 2026-10-23 | **Status**: Done

The owner's decision on each of the 12 drafts that sat under `src/jbake/content/blog/drafts/`, for task T053 in
[`04-AUTHORS-DESK.md`](../04-AUTHORS-DESK.md). What each draft contained is in [`content-2026-10-09.md`](content-2026-10-09.md),
§I.2. Drafts are never published (T009), so deleting one changes no URL; their 12 preview paths stay listed in
`docs/baseline/expected-removals.txt` as the record of T009. The deleted files remain in git history (last present
at `5752024`, the parent of this change).

| Draft (`blog/drafts/`) | Title | `date=` | Decision |
|---|---|---|---|
| `acceptance-test-driven-development.md` | Acceptance Test Driven Development | 2011-11-11 | Delete |
| `acceptance-test-frameworks.md` | DRAFT - Acceptance Test Frameworks | 2013-05-31 | Delete |
| `break-your-app-into-separate-components.md` | Break your App into Separate Components | 2011-08-16 | Delete |
| `dev-tools.md` | DRAFT - Dev Tools | 2012-09-10 | Delete |
| `evolving-features-into-acceptance-tests.md` | Evolving Features into Acceptance Tests | 2013-03-09 | Delete |
| `getting-hacked-learning-lessons-the-hard-way.md` | Getting Hacked: Learning Lessons the Hard Way | 2011-02-13 | Delete |
| `i-need-to-hire-a-devops-expert.md` | I need to hire a DevOps expert... | 2014-07-04 | Delete |
| `maven-pipelines.md` | Issues Implementing Deployment Pipelines with Maven | 2013-03-09 | Delete |
| `retrospectives-building-collective-ownership.md` | Retrospectives: Building Collective Ownership | 2012-07-11 | **Keep as a draft**, to work on |
| `specflow-selenium.md` | Guide to Setting up SpecFlow and Selenuim | 2012-03-17 | Delete |
| `the-job-dsl-plugin.md` | Mastering Jenkins: The Job DSL Plugin | 2013-05-07 | Delete |
| `unit-testing-from-zero-to-hero.md` | Unit Testing, From Zero to Hero | 2010-12-04 | Delete |

Result: 11 deleted, 1 kept. The folder now holds only `retrospectives-building-collective-ownership.md`, previewed at
`http://localhost:8080/blog/drafts/retrospectives-building-collective-ownership-draft.html` with `./gradlew bakePreview`.
