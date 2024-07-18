## Deprecation and removal

While SemVer's binary compatibility guarantees restrict the types of changes
that may be made within a library revision and make it difficult to remove an
API, there are many other ways to influence how developers interact with your
library.

### Deprecation (`@Deprecated`)

Deprecation lets a developer know that they should stop using an API or class.
All deprecations must be marked with a `@Deprecated` code annotation as well as
a `@deprecated <explanation>` docs annotation (for Java) or
[`@Deprecated(message = <explanation>)`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-deprecated/)
(for Kotlin) explaining the rationale and how the developer should migrate away
from the API.

Deprecations in Kotlin are encouraged to provide an automatic migration by
specifying the
[`replaceWith = ReplaceWith(<replacement>)`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-replace-with/)
parameter to `@Deprecated` in cases where the migration is a straightforward
replacement and *may* specify
[`level = DeprecationLevel.ERROR`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-deprecation-level/)
(source-breaking) in cases where the API on track to be fully removed.

Deprecation is an non-breaking API change that must occur in a **major** or
**minor** release.

APIs that are added during a pre-release cycle and marked as `@Deprecated`
within the same cycle, e.g. added in `alpha01` and deprecated in `alpha06`,
[must be removed](/company/teams/androidx/versioning.md#beta-checklist) before
moving to `beta01`.

NOTE While some APIs can safely be removed without a deprecation cycle, a full
cycle of deprecate (with replacement) and release prior to removal is *strongly
recommended* for APIs that are likely to have clients, including APIs referenced
by the Android platform build and `@RequiresOptIn` APIs that have shipped
in a public beta.

### Soft removal (`@removed` or `DeprecationLevel.HIDDEN`)

Soft removal preserves binary compatibility while preventing source code from
compiling against an API. It is a *source-breaking change* and not recommended.

Soft removals **must** do the following:

*   Mark the API as deprecated for at least one stable release prior to removal.
*   In Java sources, mark the API with a `@RestrictTo(LIBRARY)` Java annotation
    as well as a `@removed <reason>` docs annotation explaining why the API was
    removed.
*   In Kotlin sources, mark the API with `@Deprecated(message = <reason>,
    level = DeprecationLevel.HIDDEN)` explaining why the API was removed.
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
    the release notes, explain that the entire artifact has been deprecated and
    will no longer receive new features or bug fixes. Include the reason for
    deprecation and the migration strategy.
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

#### Long-term support

Artifacts which have been fully deprecated and removed are not required to fix
any bugs -- including security issues -- which are reported after the library
has been removed from source control; however, library owners *may* utilize
release branches to provide long-term support.

When working on long-term support in a release branch, you may encounter the
following issues:

-   Release metadata produced by the build system is not compatible with the
    release scheduling tool
-   Build targets associated with the release branch do not match targets used
    by the snapped build ID
-   Delta between last snapped build ID and proposed snap build ID is too large
    and cannot be processed by the release branch management tool

### Discouraging usage in Play Store

[Google Play SDK Console](https://play.google.com/sdk-console/) allows library
owners to annotate specific library versions with notes, which are shown to app
developers in the Play Store Console, or permanently mark them as outdated,
which shows a warning in Play Store Console asking app developers to upgrade.

In both cases, library owners have the option to prevent app developers from
releasing apps to Play Store that have been built against specific library
versions.

Generally, Jetpack discourages the use of either notes or marking versions as
outdated. There are few cases that warrant pushing notifications to app
developers, and it is easy to abuse notes as advertising to drive adoption. As a
rule, upgrades to Jetpack libraries should be driven by the needs of app
developers.

Cases where notes may be used include:

1.  The library is used directly, rather than transitively, and contains `P0` or
    `P1` (ship-blocking, from the app's perspective) issues
    -   Transitively-included libraries should instead urge their dependent
        libraries to bump their pinned dependency versions
1.  The library contains ship-blocking security issues. In this case, we
    recommend preventing app releases since developers may be less aware of
    security issues.
1.  The library was built against a pre-release SDK which has been superseded by
    a finalized SDK. In this case, we recommend preventing app releases since
    the library may crash or show unexpected behavior.

Cases where marking a version as outdated maybe used:

1.  The library has security implications and the version is no longer receiving
    security updates, e.g. the release branch has moved to the next version.

In all cases, there must be a newer stable or bugfix release of the library that
app developers can use to replace the outdated version.
