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
interchangeably within AndroidX, with project being the technical term used by
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

If you see an error message `No module named 'toml'` try the following steps.

*   Install necessary tools if they are not already installed
    *   (Linux) `sudo apt-get install virtualenv python3-venv`
    *   (Mac) `pip3 install virtualenv`
*   Create a virtual environment with `virtualenv androidx_project_creator` (you
    can choose another name for your virtualenv if you wish).
*   Install the `toml` library in your virtual env with
    `androidx_project_creator/bin/pip3 install toml`
*   Run the project creator script from your virtual env with
    `androidx_project_creator/bin/python3
    ../../development/project-creator/create_project.py androidx.foo foo-bar`
*   Delete your virtual env with `rm -rf ./androidx-project_creator`
    *   virtualenv will automatically .gitignore itself, but you may want to to
        remove it anyway.

#### Common sub-feature names {#module-naming-subfeature}

*   `-testing` for an artifact intended to be used while testing usages of your
    library, e.g. `androidx.room:room-testing`
*   `-core` for a low-level artifact that *may* contain public APIs but is
    primarily intended for use by other libraries in the group
*   `-ktx` for an Kotlin artifact that exposes idiomatic Kotlin APIs as an
    extension to a Java-only library (see
    [additional -ktx guidance](#module-ktx))
*   `-samples` for sample code which can be inlined in documentation (see
    [Sample code in Kotlin modules](#sample-code-in-kotlin-modules)
*   `-<third-party>` for an artifact that integrates an optional third-party API
    surface, e.g. `-proto` or `-rxjava2`. Note that a major version is included
    in the sub-feature name for third-party API surfaces where the major version
    indicates binary compatibility (only needed for post-1.x).

Artifacts **should not** use `-impl` or `-base` to indicate that a library is an
implementation detail shared within the group. Instead, use `-core`.

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
iteration until they are API-stable.

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

### Choosing a `minSdkVersion` {#module-minsdkversion}

The recommended minimum SDK version for new Jetpack libraries is currently
**19** (Android 4.4, KitKat). This SDK was chosen to represent 99% of active
devices based on Play Store check-ins (see Android Studio
[distribution metadata](https://dl.google.com/android/studio/metadata/distributions.json)
for current statistics). This maximizes potential users for external developers
while minimizing the amount of overhead necessary to support legacy versions.

However, if no explicit minimum SDK version is specified for a library, the
default is **14** (Android 4.0, Ice Cream Sandwich).

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

### Kotlin extension `-ktx` libraries {#module-ktx}

New libraries should prefer Kotlin sources with built-in Java compatibility via
`@JvmName` and other affordances of the Kotlin language; however, existing Java
sourced libraries may benefit from extending their API surface with
Kotlin-friendly APIs in a `-ktx` library.

A Kotlin extension library **may only** provide extensions for a single base
library's API surface and its name **must** match the base library exactly. For
example, `work:work-ktx` may only provide extensions for APIs exposed by
`work:work`.

Additionally, an extension library **must** specify an `api`-type dependency on
the base library and **must** be versioned and released identically to the base
library.

Kotlin extension libraries *should not* expose new functionality; they should
only provide Kotlin-friendly versions of existing Java-facing functionality.
