# Policies and processes

This document is intended to describe release policies that affect the workflow
of an engineer developing within the AndroidX libraries. It also describes the
process followed by a release engineer or TPM to take a development branch and
publish it as a release on Google Maven.

Policies and processes automated via tooling are noted in
<span style="color:#bf9000;">yellow</span>.

[TOC]

## Project directory structure {#directory-structure}

Libraries developed in AndroidX follow a consistent project naming and directory
structure.

Library groups should organize their projects into directories and project names
(in brackets) as:

```
<feature-name>/
  <feature-name>-<sub-feature>/ [<feature-name>:<feature-name>-<sub-feature>]
    samples/ [<feature-name>:<feature-name>-<sub-feature>:samples]
  integration-tests/
    testapp/ [<feature-name>:testapp]
    testlib/ [<feature-name>:testlib]
```

For example, the `navigation` library group's directory structure is:

```
navigation/
  navigation-benchmark/ [navigation:navigation-benchmark]
  ...
  navigation-ui/ [navigation:navigation-ui]
  navigation-ui-ktx/ [navigation:navigation-ui-ktx]
  integration-tests/
    testapp/ [navigation:integration-tests:testapp]
```

### Project creator script {#project-creator}

Note: The terms _project_, _module_, and _library_ are often used
interchangeably within AndroidX, with project being the technical term used by
Gradle to describe a build target, e.g. a library that maps to a single AAR.

New projects can be created using our
[project creation script](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:development/project-creator/?q=project-creator&ss=androidx%2Fplatform%2Fframeworks%2Fsupport)
available in our repo.

It will create a new project with the proper structure and configuration based
on your project needs!

To use it:

```sh
cd ~/androidx-main/framework/support && \
cd development/project-creator && \
./create_project.py androidx.foo foo-bar
```

## Terminology {#terminology}

**Artifact**
:   Previously referred to as "a Support Library library." A library --
    typically Java or Android -- that maps to a single Maven artifact, ex.
    `androidx.recyclerview:recyclerview`. An artifact is associated with a
    single Android Studio module and a directory containing a `build.gradle`
    configuration, resources, and source code.

**API Council**
:   A committee that reviews Android APIs, both platform and library, to ensure
    they are consistent and follow the best-practices defined in our API
    guidelines.

**Semantic Versioning (SemVer)**
:   A versioning standard developed by one of the co-founders of GitHub that is
    understood by common dependency management systems, including Maven. In this
    document, we are referring specifically to
    [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html).

## Managing versions {#managing-versions}

This section outlines the steps for a variety of common versioning-related
tasks. Artifact versions should **only** be modified by their owners as
specified in the artifact directory's `OWNERS` file.

Artifact versions are specified in
[`LibraryVersions.kt`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt).
Versions are bound to your artifact in the `supportLibrary` block in your
artifact's `build.gradle` file. The `Version` class validates the version string
at build time.

In the
[`LibraryVersions.kt`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt)
file:

```
object LibraryVersions {
    val SNAZZY_ARTIFACT = Version("1.1.0-alpha03")
}
```

In your artifact's `build.gradle` file:

```
import androidx.build.LibraryVersions

supportLibrary {
   mavenVersion = LibraryVersions.SNAZZY_ARTIFACT
}
```

## Dependencies {#dependencies}

Artifacts may depend on other artifacts within AndroidX as well as sanctioned
third-party libraries.

### Versioned artifacts {#versioned-artifacts}

One of the most difficult aspects of independently-versioned releases is
maintaining compatibility with public artifacts. In a mono repo such as Google's
repository or Android Git at master revision, it's easy for an artifact to
accidentally gain a dependency on a feature that may not be released on the same
schedule.

#### Pre-release dependencies {#pre-release-dependencies}

Pre-release suffixes **must** propagate up the dependency tree. For example, if
your artifact has API-type dependencies on pre-release artifacts, ex.
`1.1.0-alpha01`, then your artifact must also carry the `alpha` suffix. If you
only have implementation-type dependencies, your artifact may carry either the
`alpha` or `beta` suffix.

Note: This does not apply to test dependencies: suffixes of test dependencies do
_not_ carry over to your artifact.

#### Pinned versions {#pinned-versions}

To avoid issues with dependency versioning, consider pinning your artifact's
dependencies to the oldest version (available via local `maven_repo` or Google
Maven) that satisfies the artifact's API requirements. This will ensure that the
artifact's release schedule is not accidentally tied to that of another artifact
and will allow developers to use older libraries if desired.

```
dependencies {
   api("androidx.collection:collection:1.0.0")
   ...
}
```

Artifacts should be built and tested against both pinned and tip-of-tree
versions of their dependencies to ensure behavioral compatibility.

#### Non-Pinned versions {#nonpinned-versions}

Below is an example of a non-pinned dependency. It ties the artifact's release
schedule to that of the dependency artifact, because the dependency will need to
be released at the same time.

```
dependencies {
   api(project(":collection"))
   ...
}
```

### Non-public APIs {#non-public-apis}

Artifacts may depend on non-public (e.g. `@hide`) APIs exposed within their own
artifact or another artifact in the same `groupId`; however, cross-artifact
usages are subject to binary compatibility guarantees and
`@RestrictTo(Scope.LIBRARY_GROUP)` APIs must be tracked like public APIs.

```
Dependency versioning policies are enforced at build time in the createArchive task. This task will ensure that pre-release version suffixes are propagated appropriately.

Cross-artifact API usage policies are enforced by the checkApi and checkApiRelease tasks (see Life of a release).
```

### Third-party libraries {#third-party-libraries}

Artifacts may depend on libraries developed outside of AndroidX; however, they
must conform to the following guidelines:

*   Prebuilt **must** be checked into Android Git with both Maven and Make
    artifacts
    *   `prebuilts/maven_repo` is recommended if this dependency is only
        intended for use with AndroidX artifacts, otherwise please use
        `external`
*   Prebuilt directory **must** contains an OWNERS file identifying one or more
    individual owners (e.g. NOT a group alias)
*   Library **must** be approved by legal
