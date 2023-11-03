# Log for changes in the Navigation library
#
# `Added`: for new features
# `Changed`: for changes in existing functionality
# `Deprecated`: for soon to be removed functionality
# `Removed`: for now removed feature
# `Fixed`: for any bug fixes
# `Security`: in case of vulnerabilities
#
# Possible headings:
# API Changes
# Bug Fixes
# Dependency Updates
# Behavior Change
# External Contributions

## Unreleased

### New Features

- NavHost and NavController now supports navigating in compose with Animations.

### Bug Fixes

- Fixed an issue with Navigation in Fragments where navigating
with popUpTo and popping a fragment off the back stack without
recreating its view would cause system back to stop working.

## Version 2.6.0-rc01

### Bug Fixes

- Updated the error message and exception type when navigating on a NavController with no
  navigation graph set.

### Dependency Updates

- Changed dependency of Activity library from version 1.6.1 to version 1.7.1.


