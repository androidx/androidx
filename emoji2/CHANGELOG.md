# Log for changes in the emoji2 library
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

### Bug Fixes

- Fixed a bug introduced in 1.3 that would cause MetricsAffectingSpans such as RelativeSizeSpan to
  apply twice. Once during text layout, and again inside of EmojiSpan.draw. The result was
  incorrectly sized draw, visible if any of the text size parameters were changed by the
  span. (b/283208650)