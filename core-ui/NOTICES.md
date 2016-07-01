# Change Log

## [23.0.1](https://android.googlesource.com/platform/frameworks/support/+/refs/heads/master/v4/) (2015-09-24)

**Breakage and deprecation notices:**

- ExploreByTouchHelper
  - Several public methods that are only meant to be called by app developers (and not internally by
    the helper itself) became final. Any code that depends on overriding these methods should be
    moved elsewhere.
  - The concept of keyboard and accessibility focus have been clarified. As a result, the
    getFocusedVirtualView() method has been deprecated and will be removed in a subsequent release.
