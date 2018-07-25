# Contributing to cuba-cli

These instructions are for contributing code to the cuba-cli tool.

## Bug tracking

For external contributions and bug reports please use GitHub issues and pull-requests.

If you want to discuss your problem or ask something please use our forum: https://cuba-platform.com/discuss

Getting in touch with us early will also help us coordinate efforts so that not everyone ends up working on the same bug or feature at the same time.

## Getting started

## Building

**Java 10+ required**. 

1. Set JAVA_HOME variable pointing to Java 10 installation.
2. You can easily build and install project using Gradle Wrapper:

> ./gradlew assemble bundle

If there are any errors during the compilation please check our public build status at https://travis-ci.org/cuba-platform/cuba-cli Do not hesitate to report us any problems with build!

## Importing to IDE

We use IntelliJ Idea IDE for development. Just import the project as Gradle project.

See also the following guide: https://github.com/cuba-platform/cuba-cli/wiki/How-to-build-and-develop-CLI-in-IntelliJ-IDEA

## Contributing

<a href="https://gitter.im/cuba-platform/cuba"><img src="https://badges.gitter.im/Join%20Chat.svg" alt="Join the chat at https://gitter.im/cuba-platform/cuba" title=""></a>

We accept contributions as GitHub pull requests. The first time you create a pull request, you will be asked to electronically sign a contribution agreement.

https://yangsu.github.io/pull-request-tutorial/ has instructions on how to create a pull request.

Remember to check the "Allow edits from maintainers" so we can rebase the PR or make small changes if necessary.

Usually, we create an issue for the PR in our internal bug tracker (YouTrack) and add the issue number to the PR title.

## Code style

1. Source code files (Java, Kotlin, Groovy and XML) must have copyright notice with Apache 2.0 license.
2. Use default IntelliJ Idea code formatting options. You can reformat your code (reformat changed code only!) using Ctrl+Alt+L shortcut.
3. Maximum line length - 120 symbols.
4. Recommended method length - up to 50 lines.
5. All overridden methods should have @Override annotation.

## Pull requests

1. All feature branches must be named as “feature/some-feature-name”. Do not name your branch as issue number, branch name should describe the purpose of the branch.
2. If you solve an existing problem that is described in one of the issues in issues please add issue number at the start of your commit message.
3. Solve only one problem per patch.
4. Describe your changes and user-visible impact: what did you change, why did you change it, how did you change it?
5. Build project and run tests before submitting a patch.
6. Create a pull request; it will then be reviewed by the platform team. Remember to check the "Allow edits from maintainers" so we can rebase the PR or make small changes if necessary.
7. After you have submitted your change, be patient and wait. Reviewers are busy people and may not get to your patch right away. Ideally, we try to get a response within one business day.
8. Respond to review comments: review comments are meant to improve the quality of the code by pointing out defects or readability issues.
9. Most PRs take a few iterations of review before they are merged.

**We are looking forward to getting your contributions!**
