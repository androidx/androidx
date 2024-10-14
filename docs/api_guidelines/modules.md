## Modules {#module}

### Packaging and naming {#module-naming}

Java packages within Jetpack follow the format `androidx.<feature-name>`. All
classes within a feature's artifact must reside within this package, and may
further subdivide into `androidx.<feature-name>.<layer>` using standard Android
layers (app, widget, etc.) or layers specific to the feature.

Maven specifications use the groupId format `androidx.<feature-name>` and
artifactId format `<feature-name>` to match the Java package. For example,
`androidx.core.role` uses the Maven spec `androidx.core:core-role`.

Sub-features that can be separated into their own artifact are recommended to
use the following formats:

-   Java package: `androidx.<feature-name>.<sub-feature>.<layer>`
-   Maven groupId: `androidx.<feature-name>`
-   Maven artifactId: `<feature-name>-<sub-feature>`

Gradle project names and directories follow the Maven spec format, substituting
the project name separator `:` or directory separator `/` for the Maven
separators `.` or `:`. For example, `androidx.core:core-role` would use project
name `:core:core-role` and directory `/core/core-role`.

Android namespaces should be unique and match the module's root Java package. If
the root Java package is not unique, include the sub-feature name.

```
android {
    namespace "androidx.core.role"
}
```

New modules in androidx can be created using the
[project creator script](#module-creation).

NOTE Modules for OEM-implemented shared libraries (also known as extensions or
sidecars) that ship on-device and are referenced via the `<uses-library>` tag
should follow the naming convention `com.android.extensions.<feature-name>` to
avoid placing `androidx`-packaged code in the platform's boot classpath.

#### Maven name and description

The `name` and `description` fields of the `androidx` configuration block are
used to generate Maven artifact metadata, which is displayed on the artifact's
maven.google.com entry and d.android.com landing page.

```
androidx {
    name = "WorkManager Kotlin Extensions"
    description = "Kotlin-friendly extensions for WorkManager."
}
```

The name should be a human-readable, title-cased representation of the
artifact's Maven coordinate. All components of the name **must** appear in the
artifact's Maven group or artifact ID, with some exceptions:

-   Marketing names may be shortened when used in the Maven group or artifact
    ID, ex. "WorkManager" as `work`, "Android for Cars" as `car`, or "Kotlin
    Extensions" as `ktx`
-   Long (>10 character) words may be truncated to a short (>5 character) prefix
-   Pluralization may be changed, ex. "Views" as `view`
-   The following descriptive terms may appear in the name:
    -   "extension(s)"
    -   "for"
    -   "integration"
    -   "with"

**Do not** use the following terms in the name:

-   "AndroidX"
-   "Library"
-   "Implementation"

The description should be a single phrase that completes the sentence, "This
library provides ...". This phrase should provide enough description that a
developer can decide whether they might want to learn more about using your
library. **Do not** simply repeat the name of the library.

#### Project directory structure {#module-structure}

Libraries developed in AndroidX follow a consistent project naming and directory
structure.

Library groups should organize their projects into directories and project names
(in brackets) as:

```
<feature-name>/
  <feature-name>-<sub-feature>/ [<feature-name>:<feature-name>-<sub-feature>]
    samples/ [<feature-name>:<feature-name>-<sub-feature>-samples]
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

#### Project creator script {#module-creation}

Note: The terms *project*, *module*, and *library* are often used
interchangeably within AndroidX, with *project* being the technical term used by
Gradle to describe a build target, e.g. a library that maps to a single AAR.

New projects can be created using our
[project creation script](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:development/project-creator/?q=project-creator&ss=androidx%2Fplatform%2Fframeworks%2Fsupport)
available in our repo.

It will create a new project with the proper structure and configuration based
on your project needs!

To use it:

```sh
cd ~/androidx-main/frameworks/support && \
cd development/project-creator && \
./create_project.py androidx.foo foo-bar
```

If you are creating an unpublished module such as an integration test app with \
the project creator script, it may not make sense to follow the same naming \
conventions as published libraries. In this situation it is safe to comment out
\
the `artifact_id` validation from the script or rename the module after it has \
been created.

If you see an error message `No module named 'toml'`, `'setuptools'`, or
similar, try the following steps:

*   Install necessary tools if they are not already installed
    *   (Linux) `sudo apt-get install virtualenv python3-venv`
    *   (Mac) `pip3 install virtualenv`
    *   (Mac homebrew) `brew install virtualenv`
*   Create a virtual environment with `virtualenv androidx_project_creator` (you
    can choose another name for your virtualenv if you wish).
*   Install the missing module(s) in your virtual environment with
    `androidx_project_creator/bin/pip3 install setuptools toml`
*   Run the project creator script from your virtual env with
    `androidx_project_creator/bin/python3
    ../../development/project-creator/create_project.py androidx.foo foo-bar`
*   Delete your virtual env with `rm -rf ./androidx-project_creator`
    *   virtualenv will automatically .gitignore itself, but you may want to to
        remove it anyway.

Note: if the module you are creating is an application (not a library), such as
you might want for integration-tests, edit the project's `build.gradle` file and
replace the plugin `id("com.android.library")` with
`id("com.android.application")`. This allows you to run activities in that
module from within Android Studio.

#### Common sub-feature names {#module-naming-subfeature}

*   `-testing` for an artifact intended to be used while testing usages of your
    library, e.g. `androidx.room:room-testing`
*   `-core` for a low-level artifact that *may* contain public APIs but is
    primarily intended for use by other libraries in the group
*   `-common` for a low-level, platform-agnostic Kotlin multi-platform artifact
    intended for both client use and use by other libraries in the group
*   `-ktx` for a Kotlin artifact that exposes idiomatic Kotlin APIs as an
    extension to a Java-only library. Note that new modules should be written in
    Kotlin rather than using `-ktx` artifacts.
*   `-samples` for sample code which can be inlined in documentation (see
    [Sample code in Kotlin modules](#sample-code-in-kotlin-modules)
*   `-<third-party>` for an artifact that integrates an optional third-party API
    surface, e.g. `-proto`, `-guava`, or `-rxjava2`. This is common for Kotlin
    libraries adapting their async APIs for Java clients. Note that a major
    version is included in the sub-feature name (ex. `rxjava3`) for third-party
    API surfaces where the major version indicates binary compatibility (only
    needed for post-1.x).

Artifacts **should not** use `-impl` or `-base` to indicate that a library is an
implementation detail shared within the group. Instead, use `-core` or `-common`
as appropriate.

#### Splitting existing modules

Existing modules *should not* be split into smaller modules; doing so creates
the potential for class duplication issues when a developer depends on a new
sub-module alongside the older top-level module. Consider the following
scenario:

*   `androidx.library:1.0.0`
    *   contains class `androidx.library.A`
    *   contains class `androidx.library.util.B`

This module is split, moving `androidx.library.util.B` to a new module:

*   `androidx.library:1.1.0`
    *   contains class `androidx.library.A`
    *   depends on `androidx.library.util:1.1.0`
*   `androidx.library.util:1.1.0`
    *   contains class `androidx.library.util.B`

A developer writes an app that depends directly on `androidx.library.util:1.1.0`
and also transitively pulls in `androidx.library:1.0.0`. Their app will no
longer compile due to class duplication of `androidx.library.util.B`.

While it is possible for the developer to fix this by manually specifying a
dependency on `androidx.library:1.1.0`, there is no easy way for the developer
to discover this solution from the class duplication error raised at compile
time.

Same-version groups are a special case for this rule. Existing modules that are
already in a same-version group may be split into sub-modules provided that (a)
the sub-modules are also in the same-version group and (b) the full API surface
of the existing module is preserved through transitive dependencies, e.g. the
sub-modules are added as dependencies of the existing module.

#### Same-version (atomic) groups {#modules-atomic}

Library groups are encouraged to opt-in to a same-version policy whereby all
libraries in the group use the same version and express exact-match dependencies
on libraries within the group. Such groups must increment the version of every
library at the same time and release all libraries at the same time.

Atomic groups are specified in
[libraryversions.toml](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:libraryversions.toml):

```
// Non-atomic library group
APPCOMPAT = { group = "androidx.appcompat" }
// Atomic library group
APPSEARCH = { group = "androidx.appsearch", atomicGroupVersion = "versions.APPSEARCH" }
```

Libraries within an atomic group should not specify a version in their
`build.gradle`:

```groovy
androidx {
    name = 'AppSearch'
    publish = Publish.SNAPSHOT_AND_RELEASE
    mavenGroup = LibraryGroups.APPSEARCH
    inceptionYear = '2019'
    description = 'Provides local and centralized app indexing'
}
```

The benefits of using an atomic group are:

-   Easier for developers to understand dependency versioning
-   `@RestrictTo(LIBRARY_GROUP)` APIs are treated as private APIs and not
    tracked for binary compatibility
-   `@RequiresOptIn` APIs defined within the group may be used without any
    restrictions between libraries in the group

Potential drawbacks include:

-   All libraries within the group must be versioned identically at head
-   All libraries within the group must release at the same time

#### Early-stage development {#modules-atomic-alpha}

There is one exception to the same-version policy: newly-added libraries within
an atomic group may be "quarantined" from other libraries to allow for rapid
iteration until they are API-stable. For example:

```groovy
androidx {
    name = "androidx.emoji2:emoji2-emojipicker"
    mavenVersion = LibraryVersions.EMOJI2_QUARANTINE
}
```

```groovy
EMOJI2_QUARANTINE = "1.0.0-alpha01"
```

A quarantined library must stay within the `1.0.0-alphaXX` cycle until it is
ready to conform to the same-version policy. While in quarantime, a library is
treated at though it is in a separate group from its nomical same-version group:

-   Must stay in `1.0.0-alphaXX`, e.g. same-version policy is not enforced
-   May use `project` or pinned version dependencies, e.g. strict-match
    dependencies are not enforced
-   May release on a separate cadence from other libraries within group
-   Must not reference restricted `LIBRARY-GROUP`-scoped APIs

When the library would like to leave quarantine, it must wait for its atomic
group to be within a `beta` cycle and then match the version. It is okay for a
library in this situation to skip versions, e.g. move directly from
`1.0.0-alpha02` to `2.1.3-beta06`.

#### Kotlin Multiplatform library versions {#modules-kmp-versioning}

When a library adds [Kotlin Multiplatform](/docs/kmp.md)
support, it is permitted to have different versions for the multiplatform
artifacts until they reach alpha quality.

##### Atomic Kotlin Multiplatform versions

To specify an atomic version group for the Kotlin Multiplatform artifacts, use
the `multiplatformGroupVersion` property in the `libraryversions.toml` file.

```
[versions]
DATASTORE = "1.2.3"
DATASTORE_KMP = "1.2.3-dev05"
[groups]
DATASTORE = { group = "androidx.datastore", atomicGroupVersion = "versions.DATASTORE", multiplatformGroupVersion = "versions.DATASTORE_KMP" }
```

Note that you can specify a `multiplatformGroupVersion` if and only if you are
also specifying a `atomicGroupVersion`.

##### Non-atomic Kotlin Multiplatform versions

If your Kotlin Multiplatform Library does not have atomic version groups, you
can specify a KMP specifc version in the `build gradle` file:

```groovy
import androidx.build.KmpPlatformsKt
...

androidx {
    name = "Collection"
    type = LibraryType.KMP_LIBRARY
    mavenGroup = LibraryGroups.COLLECTION
    mavenVersion = KmpPlatformsKt.enableNative(project) ? LibraryVersions.COLLECTION_KMP : LibraryVersions.KMP
    inceptionYear = "2018"
    description = "Standalone efficient collections."
}
```

### Choosing a `minSdkVersion` {#module-minsdkversion}

The recommended minimum SDK version for new Jetpack libraries is currently
**23** (Android 6.0, Marshmallow). This SDK was chosen to represent 99% of
active devices based on Play Store check-ins (see Android Studio
[distribution metadata](https://dl.google.com/android/studio/metadata/distributions.json)
for current statistics). This maximizes potential users for external developers
while minimizing the amount of overhead necessary to support legacy versions.

However, if no explicit minimum SDK version is specified for a library, the
default is **21** (Android 5.0, Lollipop).

Note that a library **must not** depend on another library with a higher
`minSdkVersion` that its own, so it may be necessary for a new library to match
its dependent libraries' `minSdkVersion`.

Individual modules may choose a higher minimum SDK version for business or
technical reasons. This is common for device-specific modules such as Auto or
Wear.

Individual classes or methods may be annotated with the
[@RequiresApi](https://developer.android.com/reference/android/annotation/RequiresApi.html)
annotation to indicate divergence from the overall module's minimum SDK version.
Note that this pattern is *not recommended* because it leads to confusion for
external developers and should be considered a last-resort when backporting
behavior is not feasible.

### Platform extension (sidecar JAR) libraries {#module-extension}

Platform extension or "sidecar JAR" libraries ship as part of the Android system
image and are made available to developers through the `<uses-library>` manifest
tag.

Interfaces for platform extension libraries *may* be defined in Jetpack, like
`androidx.window.extensions`, but must be implemented in the Android platform
via AOSP or by device manufacturers. See
[WindowManager Extensions](https://source.android.com/docs/core/display/windowmanager-extensions)
for more details on the platform-side implementation of extension libraries,
including motivations for their use.

See
[Platform extension (sidecar JAR) dependencies](/docs/api_guidelines#dependencies-sidecar)
for guidelines on depending on extension libraries defined externally or within
Jetpack.

### Framework- and language-specific libraries (`-ktx`, `-guava`, etc.) {#module-ktx}

New libraries should prefer Kotlin sources with built-in Java compatibility via
`@JvmName` and other affordances of the Kotlin language. They may optionally
expose framework- or language-specific extension libraries like `-guava` or
`-rxjava3`.

Existing Java-sourced libraries may benefit from extending their API surface
with Kotlin-friendly APIs in a `-ktx` extension library.

Extension libraries **may only** provide extensions for a single base library's
API surface and its name **must** match the base library exactly. For example,
`work:work-ktx` may only provide extensions for APIs exposed by `work:work`.

Additionally, an extension library **must** specify an `api`-type dependency on
the base library and **must** be versioned and released identically to the base
library.

Extension libraries *should not* expose new functionality; they should only
provide language- or framework-friendly versions of existing library
functionality.
