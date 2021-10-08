# Versioning

[TOC]

## Semantic versioning

Artifacts follow strict semantic versioning. The version for a finalized release
will follow the format `<major>.<minor>.<bugfix>` with an optional
`-<alpha|beta|rc><nn>` suffix. Internal or nightly releases should use the
`-SNAPSHOT` suffix to indicate that the release bits are subject to change.

Also check out the [Versioning FAQ](faq.md#version).

### Notation

Major (`x.0.0`)
:   An artifact's major version indicates a guaranteed forward-compatibility
    window. For example, a developer could update an artifact versioned `2.0.0`
    to `2.7.3` without taking any additional action.

Minor (`1.x.0`)
:   Minor indicates compatible public API changes. This number is incremented
    when APIs are added, including the addition of `@Deprecated` annotations.
    Binary compatibility must be preserved between minor version changes.

Bugfix (`1.0.x`)
:   Bugfix indicates internal changes to address broken behavior. Care should be
    taken to ensure that existing clients are not broken, including clients that
    may have been working around long-standing broken behavior.

#### Pre-release cycles

Alpha (`1.0.0-alphaXX`)
:   Feature development and API stabilization phase.

Beta (`1.0.0-betaXX`)
:   Functional stabilization phase.

RC (`1.0.0-rcXX`)
:   Verification phase.

Stable (no-suffix)
:   Final releases are well-tested, both by internal tests and external clients,
    and their API surface is reviewed and finalized. While APIs may be
    deprecated in future versions and removed in subsequent major version bumps,
    any APIs added at this stage should be considered semi-permanent as major
    version bumps are [strongly discouraged](#major-implications).

### Major (`x.0.0`) {#major}

An artifact's major version indicates a guaranteed forward-compatibility window.
For example, a developer could update an artifact versioned `2.0.0` to `2.7.3`
without taking any additional action; however, updating from `2.7.3` to `3.0.0`
may require a complete rewrite of their application or cause conflicts with
their dependencies.

#### When to increment {#major-when}

An artifact *must* increment its major version number in response to breaking
changes in binary or behavioral compatibility within the library itself _or_ in
response to breaking changes within a dependency.

For example, if an artifact updates a SemVer-type dependency from `1.0.0` to
`2.0.0` then the artifact must also bump its own major version number.

An artifact *may in rare cases* increment its major version number to indicate
an important but non-breaking change in the library. Note, however, that the
SemVer implications of incrementing the major version are the same as a breaking
change -- dependent projects _must_ assume the major version change is breaking
and update their dependency specifications.

#### Ecosystem implications {#major-implications}

When an artifact increases its major version, _all_ artifacts that depended on
the previous major version are no longer considered compatible and must
explicitly migrate to depend on the new major version.

As a result, if the library ecosystem is slow to adopt a new major version of an
artifact then developers may end up in a situation where they cannot update an
artifact because they depend on a library that has not yet adopted the new major
version.

For this reason, we *strongly* recommend against increasing the major version of
a “core” artifact that is depended upon by other libraries. “Leaf” artifacts --
those that apps depend upon directly and are not used by other libraries -- have
a much easier time increasing their major version.

#### Process requirements {#major-process}

If the artifact has dependencies within Jetpack, owners *must* complete the
assessment before implementing any breaking changes to binary or behavioral
compatibility.

Otherwise, owners are *strongly recommended* to complete the assessment before
implementing any breaking changes to binary or behavioral compatibility, as such
changes may negatively impact downstream clients in Android git or Google's
repository. These clients are not part of our pre-submit workflow, but filling
out the assessment will provide insight into how they will be affected by a
major version change.

### Minor (`1.x.0`) {#minor}

Minor indicates compatible public API changes. This number is incremented when
APIs are added, including the addition of `@Deprecated` annotations. Binary
compatibility must be preserved between minor version changes.

#### Moving between minor versions:

*   A change in the minor revision indicates the addition of binary-compatible
    APIs. Libraries **must** increment their minor revision when adding APIs.
    Dependent libraries are not required to update their minimum required
    version unless they depend on newly-added APIs.

### Bugfix (`1.0.x`) {#bugfix}

Bugfix indicates internal changes to address broken behavior. Care should be
taken to ensure that existing clients are not broken, including clients that may
have been working around long-standing broken behavior.

#### Moving between bugfix versions:

*   A change in the bugfix revision indicates changes in behavior to fix bugs.
    The API surface does not change. Changes to the bugfix version may *only*
    occur in a release branch. The bugfix revision must always be `.0` in a
    development branch.

### Pre-release suffixes {#pre-release-suffix}

The pre-release suffix indicates stability and feature completeness of a
release. A typical release will begin as alpha, move to beta after acting on
feedback from internal and external clients, move to release candidate for final
verification, and ultimately move to a finalized build.

Alpha, beta, and release candidate releases are versioned sequentially using a
leading zero (ex. alpha01, beta11, rc01) for compatibility with the
lexicographic ordering of versions used by SemVer.

### Snapshot {#snapshot}

Snapshot releases are whatever exists at tip-of-tree. They are only subject to
the constraints placed on the average commit. Depending on when it's cut, a
snapshot may even be binary-identical to an alpha, beta, or stable release.

Versioning policies are enforced by the following Gradle tasks:

`checkApi`: ensures that changes to public API are intentional and tracked,
asking the developer to explicitly run updateApi (see below) if any changes are
detected

`checkApiRelease`: verifies that API changes between previously released and
currently under-development versions conform to semantic versioning guarantees

`updateApi`: commits API changes to source control in such a way that they can
be reviewed in pre-submit via Gerrit API+1 and reviewed in post-submit by API
Council

`SNAPSHOT`: is automatically added to the version string for all builds that
occur outside the build server for release branches (ex. ub-androidx-release).
Local release builds may be forced by passing -Prelease to the Gradle command
line.

## Picking the right version {#picking-the-right-version}

AndroidX follows [Strict Semantic Versioning](https://semver.org), which means
that the version code is strongly tied to the API surface. A full version
consists of revision numbers for major, minor, and bugfix as well as a
pre-release stage and revision. Correct versioning is, for the most part,
automatically enforced; however, please check for the following:

### Initial version {#initial-version}

If your library is brand new, your version should start at 1.0.0, e.g.
`1.0.0-alpha01`.

The initial release within a new version always starts at `alpha01`. Note the
two-digit revision code, which allows us to do up to 99 revisions within a
pre-release stage.

### Pre-release stages

A single version will typically move through several revisions within each of
the pre-release stages: alpha, beta, rc, and stable. Subsequent revisions within
a stage (ex. alpha, beta) are incremented by 1, ex. `alpha01` is followed by
`alpha02` with no gaps.

### Moving between pre-release stages and revisions

Libraries are expected to go through a number of pre-release stages within a
version prior to release, with stricter requirements at each stage to ensure a
high-quality stable release. The owner for a library should typically submit a
CL to update the stage or revision when they are ready to perform a public
release.

Libraries are expected to allow >= 2 weeks per pre-release stage. This 'soaking
period' gives developers time to try/use each version, find bugs, and ensure a
quality stable release. Therefore, at minimum:

-   An `alpha` version must be publically available for 2 weeks before releasing
    a public `beta`
-   A `beta` version must be publically available for 2 weeks before releasing
    an public `rc`
-   A `rc` version must be publically available for 2 weeks before releasing a
    public stable version

Your library must meet the following criteria to move your public release to
each stage:

### Alpha {#alpha}

Alpha releases are expected to be functionally stable, but may have unstable API
surface or incomplete features. Typically, alphas have not gone through API
Council review but are expected to have performed a minimum level of validation.

#### Within the `alphaXX` cycle

*   API surface
    *   Prior to `alpha01` release, API tracking **must** be enabled (either
        `publish=true` or create an `api` directory) and remain enabled
    *   May add/remove APIs within `alpha` cycle, but deprecate/remove cycle is
        strongly recommended.
*   Testing
    *   All changes **should** be accompanied by a `Test:` stanza
    *   All pre-submit and post-submit tests are passing
    *   Flaky or failing tests **must** be suppressed or fixed within one day
        (if affecting pre-submit) or three days (if affecting post-submit)

### Beta {#beta}

Beta releases are ready for production use but may contain bugs. They are
expected to be functionally stable and have highly-stable, feature-complete API
surface. All APIs should have been reviewed by API Council at this stage. Tests
should have 100% coverage of public API surface and translations must be 100%
complete.

While beta represents API Freeze, it does not necessarily mean APIs are locked
down permanently. A limited number of exceptions may be granted by API Council
in cases where ship-blocking mistakes or significant user experience issues can
be addressed with minimal changes to the API surface. Exceptions **will not** be
granted for new features, non-trivial API changes, significant refactorings, or
any changes likely to introduce additional functional instability. Requests for
exceptions **must** be accompanied by a justification explaining why the change
cannot be made in a future minor version. This policy does not apply to
additions of `@Experimental` APIs or changes to `@Experimental` APIs.

#### Checklist for moving to `beta01` {#beta-checklist}

*   API surface
    *   Entire API surface has been reviewed by API Council
    *   All APIs from alpha undergoing deprecate/remove cycle must be removed
        *   The final removal of a `@Deprecated` API must occur in alpha, not in
            Beta.
*   Testing
    *   All public APIs are tested
    *   All pre-submit and post-submit tests are enabled (e.g. all suppressions
        are removed) and passing
    *   Your library passes `./gradlew library:checkReleaseReady`
*   No experimental features (e.g. `@UseExperimental`) may be used
*   All dependencies are `beta`, `rc`, or stable
*   Be able to answer the question "How will developers test their apps against
    your library?"
    *   Ideally, this is an integration app with automated tests that cover the
        main features of the library and/or a `-testing` artifact as seen in
        other Jetpack libraries

#### Within the `betaXX` cycle

*   API surface
    *   May not add, remove, or change APIs unless granted an exception by API
        Council following the beta API change exception request process
        *   Must go through the full `@Deprecate` and hard-removal cycle in
            separate `beta` releases for any exception-approved API removals or
            changes
    *   May not remove `@Experimental` from experimental APIs, as this would
        amount to an API addition
    *   **May** add new `@Experimental` APIs and change existing `@Experimental`
        APIs

### RC {#rc}

Release candidates are expected to be nearly-identical to the final release, but
may contain critical last-minute fixes for issues found during integration
testing.

#### Checklist for moving to `rc01`

*   All previous checklists still apply
*   Release branch, e.g. `androidx-<group_id>-release`, is created
*   API surface
    *   Any API changes from `beta` cycle are reviewed by API Council
*   No **known** P0 or P1 (ship-blocking) issues
*   All dependencies are `rc` or stable

#### Within the `rcXX` cycle

*   Ship-blocking bug fixes only
*   All changes must have corresponding tests
*   No API changes allowed

### Stable {#stable}

Final releases are well-tested, both by internal tests and external clients, and
their API surface is reviewed and finalized. While APIs may be deprecated and
removed in future versions, any APIs added at this stage must remain for at
least a year.

#### Checklist for moving to stable

*   Identical to a previously released `rcXX` (note that this means any bugs
    that result in a new release candidate will necessarily delay your stable
    release by a minimum of two weeks)
*   No changes of any kind allowed
*   All dependencies are stable

## Updating your version {#update}

A few notes about version updates:

-   The version of your library listed in `androidx-main` should *always* be
    higher than the version publically available on Google Maven. This allows us
    to do proper version tracking and API tracking.

-   Version increments must be done before the CL cutoff date (aka the build cut
    date).

-   **Increments to the next stability suffix** (like `alpha` to `beta`) should
    be handled by the library owner, with the Jetpack TPM (nickanthony@) CC'd
    for API+1.

-   Version increments in release branches will need to follow the guide
    [How to update your version on a release branch](release_branches.md#update-your-version)

-   When you're ready for `rc01`, the increment to `rc01` should be done in
    `androidx-main` and then your release branch should be snapped to that
    build. See the guide [Snap your release branch](release_branches.md#snap) on
    how to do this. After the release branch is snapped to that build, you will
    need to update your version in `androidx-main` to `alpha01` of the next
    minor (or major) version.


### How to update your version

1.  Update the version listed in
    `frameworks/support/buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt`
1.  If your library is a `beta` or `rc01` version, run `./gradlew
    <your-lib>:updateApi`. This will create an API txt file for the new version
    of your library. For other versions, this step is not reqired
1.  Verify changes with `./gradlew checkApi verifyDependencyVersions`.
1.  Commit these change as one commit.
1.  Upload these changes to Gerrit for review.

An example of a version bump can be found here:
[aosp/833800](https://android-review.googlesource.com/c/platform/frameworks/support/+/833800)

## `-ktx` Modules {#ktx}

Kotlin Extension modules (`-ktx`) for regular Java modules follow the same
requirements, but with one exception. They must match the version of the Java
module that they extend.

For example, let's say you are developing a java library
`androidx.foo:foo-bar:1.1.0-alpha01` and you want to add a kotlin extension
module `androidx.foo:foo-bar-ktx` module. Your new `androidx.foo:foo-bar-ktx`
module will start at version `1.1.0-alpha01` instead of `1.0.0-alpha01`.

If your `androidx.foo:foo-bar` module was in version `1.0.0-alpha06`, then the
kotlin extension module would start in version `1.0.0-alpha06`.
