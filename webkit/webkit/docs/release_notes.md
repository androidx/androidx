# Writing Release Notes
CLs with API changes require a `Relnote: ` footer in the commit message, which
will be added to the release notes of the library.

## Format
The release notes footer uses markdown formatting for the notes, and should be
formatted as follows:
```
Relnote: """A release note can be
  broken over multiple lines. It should be surrounded by triple-"
  and subsequent lines should be indented 2 spaces."""
```

## What to write

The Jetpack Library generally split release notes in the following 4 headlines.
Each section below describes what to consider as part of your release notes.

Note that links to the change and fixed bugs are read automatically from the
other fields of the change description  by the system that creates the release
notes, and does not have to be manually added.

### New Feature

This heading refers to new functionality. Most new APIs are likely to fall in
this category.  For these features, please focus on the use case the feature
addresses, and should make it clear to an app developer whether this is a
feature that applies to their application.

Release notes for new features should outline:

* What is the new feature? (Classes/method names).
* What is the purpose of the new feature, and who should use it.
* Minimum WebView version required to use the feature (if applicable).

### API Change

API Changes refer to situations where an existing API behaviour or shape is
changed. This also includes deprecation of APIs or promotion of experimental
APIs to stable status.

This kind of release note should explain:

* What changed
* What was the previous behaviour/shape
* What is the new behaviour/shape
* Why was the change made
* How apps should migrate (or if they need to migrate)

### Bugfix

This heading is used for changes that are direct responses to faulty behaviour.
Release notes for a bugfix should describe:

* What buggy behavior is fixed.
* How critical this is to fix (e.g. inconvenience vs. security)
* (Optional) What the new non-buggy behavior is.


### External Contributions

Any changes contributed by external parties, i.e. not Google employees, will be
listed in the release notes under a separate "External Contributions" headline
for enhanced visibility. The format of the release note should otherwise
attempt to adhere to whichever of the other categories it falls under.
