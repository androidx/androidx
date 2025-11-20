# Module root

androidx.core core-backported-fixes

# Package androidx.core.backported.fixes

The Core Backported Fixes library lets you check if a critical bug
fix has been applied to a device. This is useful because it can take a long time for bug fixes to be
rolled out to all devices, and this API provides a way for you to know when it is safe to use
features that depend on a fix.

[KnownIssues] contains a complete list of all known issues with documentation and sample usage with a
suggested workaround for each issue.

Use [BackportedFixManager] to test if a [KnownIssue] is fixed on a device:

```kotlin
val fixManager = BackportedFixManager()
if (fixManager.isFixed(KnownIssues.KI_1234)) {
    Offer_experience_that_needs_fix()
} else {
    Offer_experience_that_avoids_the_bug()
}
```

See [KnownIssues.KI_350037023] for a test-only example known issue.
