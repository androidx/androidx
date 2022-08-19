## Deprecation and removal

While SemVer's binary compatibility guarantees restrict the types of changes
that may be made within a library revision and make it difficult to remove an
API, there are many other ways to influence how developers interact with your
library.

### Deprecation (`@deprecated`)

Deprecation lets a developer know that they should stop using an API or class.
All deprecations must be marked with a `@Deprecated` Java annotation as well as
a `@deprecated <migration-docs>` docs annotation explaining how the developer
should migrate away from the API.

Deprecation is an non-breaking API change that must occur in a **major** or
**minor** release.

APIs that are added during a pre-release cycle and marked as `@Deprecated`
within the same cycle, e.g. added in `alpha01` and deprecated in `alpha06`,
[must be removed](versioning.md#beta-checklist) before moving to `beta01`.

### Soft removal (@removed)

Soft removal preserves binary compatibility while preventing source code from
compiling against an API. It is a *source-breaking change* and not recommended.

Soft removals **must** do the following:

*   Mark the API as deprecated for at least one stable release prior to removal.
*   Mark the API with a `@RestrictTo(LIBRARY)` Java annotation as well as a
    `@removed <reason>` docs annotation explaining why the API was removed.
*   Maintain binary compatibility, as the API may still be called by existing
    dependent libraries.
*   Maintain behavioral compatibility and existing tests.

This is a disruptive change and should be avoided when possible.

Soft removal is a source-breaking API change that must occur in a **major** or
**minor** release.

### Hard removal

Hard removal entails removing the entire implementation of an API that was
exposed in a public release. Prior to removal, an API must be marked as
`@deprecated` for a full **minor** version (`alpha`->`beta`->`rc`->stable),
prior to being hard removed.

This is a disruptive change and should be avoided when possible.

Hard removal is a binary-breaking API change that must occur in a **major**
release.

### For entire artifacts

We do not typically deprecate or remove entire artifacts; however, it may be
useful in cases where we want to halt development and focus elsewhere or
strongly discourage developers from using a library.

Halting development, either because of staffing or prioritization issues, leaves
the door open for future bug fixes or continued development. This quite simply
means we stop releasing updates but retain the source in our tree.

Deprecating an artifact provides developers with a migration path and strongly
encourages them -- through Lint warnings -- to migrate elsewhere. This is
accomplished by adding a `@Deprecated` and `@deprecated` (with migration
comment) annotation pair to *every* class and interface in the artifact.

To deprecate an entire artifact:

1.  Mark every top-level API (class, interface, extension function, etc.) in the
    artifact as `@Deprecated` and update the API files
    ([example CL](https://android-review.googlesource.com/c/platform/frameworks/support/+/1938773))
1.  Schedule a release of the artifact as a new minor version. When you populate
    the release notes, explain that the entire artifact has been deprecated.
    Include the reason for deprecation and the migration strategy.
1.  After the artifact has been released, remove the artifact from the source
    tree, versions file, and tip-of-tree docs configuration
    ([example CL](https://android-review.googlesource.com/c/platform/frameworks/support/+/2061731/))

The fully-deprecated artifact will be released as a deprecation release -- it
will ship normally with accompanying release notes indicating the reason for
deprecation and migration strategy, and it will be the last version of the
artifact that ships. It will ship as a new minor stable release. For example, if
`1.0.0` was the last stable release, then the deprecation release will be
`1.1.0`. This is so Android Studio users will get a suggestion to update to a
new stable version, which will contain the `@deprecated` annotations.

After an artifact has been released as fully-deprecated, it can be removed from
the source tree.
