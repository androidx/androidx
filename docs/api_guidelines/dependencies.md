## Dependencies {#dependencies}

Artifacts may depend on other artifacts within AndroidX as well as sanctioned
third-party libraries. Additionally, artifacts may have toolchain dependencies
that are not explicitly specified in their `dependencies` build configuration or
don't appear in their Maven publications (`pom` or `module` files).

### Versioned artifacts {#dependencies-versioned}

One of the most difficult aspects of independently-versioned releases is
maintaining compatibility with public artifacts. In a monorepo such as Google's
repository or Android Git at `main` revision, it's easy for an artifact to
accidentally gain a dependency on a feature that may not be released on the same
schedule.

To address this problem, library owners in AndroidX can choose from several
types of dependencies:

-   Project `project(":core:core")` uses the tip-of-tree sources for the
    `androidx.core:core` library and requires that they be loaded in the
    workspace and released at the same time.
-   Playground `projectOrArtifact(":core:core")` is used for the
    [Playground](/docs/playground.md) workflow and will use
    tip-of-tree sources, if present in the workspace, or `SNAPSHOT` prebuilt
    artifacts from [androidx.dev](http://androidx.dev) otherwise.
-   Pinned `"androidx.core:core:1.4.0"` uses the prebuilt AAR and requires that
    it be checked in to the `prebuilts/androidx/internal` local Maven repository
    or, when using the Playground workflow, the remote Google Maven repository.

Libraries should prefer pinned dependencies with the lowest possible versions
that include the APIs or behaviors required by the library, and should only use
project or Playground specs in cases where tip-of-tree APIs or behaviors are
required.

**Do not** upgrade the version of a library's dependency to artificially boost
adoption of that version.

#### Pre-release dependencies {#dependencies-pre-release}

Pre-release suffixes **must** propagate up the dependency tree. For example, if
your artifact has a dependency on an artifact versioned `1.1.0-alpha01` then
your artifact must also carry the `alpha` suffix.

NOTE This does not apply to test dependencies: suffixes of test dependencies do
*not* carry over to your artifact.

#### Pinned versions {#dependencies-prebuilt}

To avoid issues with dependency versioning, pin your dependencies to the oldest
stable version of an artifact that includes the necessary APIs. This will ensure
that the artifact's release schedule is not accidentally tied to that of another
artifact and will allow developers to use older libraries if desired.

```
dependencies {
   api("androidx.collection:collection:1.0.0")
   ...
}
```

Artifacts are built and tested against both pinned and tip-of-tree versions of
their dependencies to ensure behavioral compatibility.

#### Tip-of-tree versions {#dependencies-project}

Below is an example of a project dependency, which uses tip-of-tree sources for
the dependency rather than a prebuilt `JAR` or `AAR`. It ties the artifact's
release schedule to that of the dependency artifact because the dependency will
need to be released at the same time.

```
dependencies {
   api(project(":collection"))
   ...
}
```

### Non-public APIs {#dependencies-non-public-apis}

Artifacts may depend on non-public or restricted APIs exposed within their own
artifact or another artifact in the same `groupId`; however, cross-artifact
usages are subject to binary compatibility guarantees. See
[`@RestrictTo` APIs](/docs/api_guidelines#restricted-api) for
more details.

Dependency versioning policies are enforced at build time in the `createArchive`
task, which ensures that pre-release version suffixes are propagated
appropriately. Cross-artifact API usage policies are enforced by the `checkApi`
and `checkApiRelease` tasks.

### Third-party libraries {#dependencies-3p}

Artifacts may depend on libraries developed outside of AndroidX; however, they
must conform to the following guidelines:

*   Prebuilt **must** be checked into Android Git
    *   `prebuilts/maven_repo` is recommended if this dependency is only
        intended for use with AndroidX artifacts, otherwise please use
        `external`
*   Prebuilt directory **must** contains an `OWNERS` file identifying one or
    more individual owners (e.g. NOT a group alias)
*   Library **must** be approved by legal

Please see Jetpack's
[open-source policy page](/docs/open_source.md) for more
details on using third-party libraries.

### Platform extension (sidecar JAR) dependencies {#dependencies-sidecar}

Platform extension or "sidecar JAR" libraries ship as part of the Android system
image and are made available to developers through the `<uses-library>` manifest
tag.

Examples include Camera OEM extensions (`androidx.camera.extensions.impl`) and
Window OEM extensions (`androidx.window.extensions`).

Extension libraries may be defined in AndroidX library projects (see
`androidx.window.extensions`) or externally, ex. in AOSP alongside the platform.
In either case, we recommend that libraries use extensions as pinned, rather
than project-type, dependencies to facilitate versioning across repositories.

*Do not* ship extension interfaces to Google Maven. Teams may choose to ship
stub JARs publicly, but that is not covered by AndroidX workflows.

Project dependencies on extension libraries **must** use `compileOnly`:

`build.gradle`:

```
dependencies {
    // Extension interfaces defined in Jetpack
    compileOnly(project(":window:extensions:extensions"))

    // Extension interfaces defined in a stub JAR
    compileOnly(
        fileTree(
            dir: "../wear_stubs",
            include: ["com.google.android.wearable-stubs.jar"]
        )
    )
}
```

Documentation dependencies **must** use the `stubs` configuration:

`docs-public/build.gradle`:

```
dependencies {
  stubs("androidx.window:window-extensions:1.0.0-alpha01")
  stubs(
    fileTree(
        dir: "../wear/wear_stubs/",
        include: ["com.google.android.wearable-stubs.jar"]
    )
  )
}
```

See [Packaging and naming](/docs/api_guidelines#module-naming)
for details about defining extension interfaces in Jetpack projects.

### Types of dependencies {#dependencies-types}

AndroidX allows dependencies to be specified as `api` or `implementation` with a
"pinned" Maven spec (ex. `androidx.core:core:1.0.0`) or a "tip-of-tree" project
spec (ex. `project(":core:core")`).

Projects used in Playground, the experimental GitHub workflow, should use a
"recent" project or artifact spec (ex. `projectOrArtifact(":core:core")`) which
will default to tip-of-tree when used outside of the Playground workflow or a
pinned `SNAPSHOT` artifact otherwise.

Regardless of which dependency spec is used, all projects are built against
tip-of-tree dependencies in CI to prevent regressions and enforce Jetpack's
compatible-at-head policy.

#### `api` versus `implementation` {#dependencies-api-vs-impl}

`api`-type dependencies will appear in clients' auto-complete as though they had
added the dependency directly to their project, and Studio will run any lint
checks bundled with `api`-type dependencies.

Dependencies whose APIs are exposed in a library's API surface **must** be
included as `api`-type. For example, if your library's API surface includes
`AccessibilityNodeInfoCompat` then you will use an `api`-type dependency on the
`androidx.core:core` library.

NOTE Libraries that provide client-facing lint checks, including
`annotation-experimental`, **must** be included as `api`-type to ensure that
lint checks are run in the clients' dependent projects.

`implementation`-type dependencies will be included in the classpath, but will
not be made available at design time (ex. in auto-complete) unless the client
explicitly adds them.

#### Constraints {#dependencies-constraints}

Dependency constraints ensure that when certain libraries are used together,
they meet certain requirements. Defining a constraint on a library *does not*
pull that library into the classpath.

In Jetpack, we use constraints to ensure that atomically-grouped libraries all
resolve to the same version
([example](https://android-review.googlesource.com/c/platform/frameworks/support/+/1973425))
and, in rare cases, to avoid conflicts resulting from classes being moved
between artifacts
([example](https://android-review.googlesource.com/c/platform/frameworks/support/+/2086029)).

`core/core-ktx/build.gradle`:

```
dependencies {
    // Atomic group
    constraints {
        implementation(project(":core:core"))
    }
}
```

In *extremely* rare cases, libraries may need to define a constraint on a
project that is not in its `studiow` project set, ex. a constraint between the
Paging and Compose libraries. As a workaround, libraries may hard-code the Maven
coordinate using a version variable
([example](https://android-review.googlesource.com/c/platform/frameworks/support/+/2160202))
to indicate the tip-of-tree version.

`paging/paging-common/build.gradle`:

```
dependencies {
    // Atomic group
    constraints {
        implementation("androidx.paging:paging-compose:${LibraryVersions.PAGING_COMPOSE}")
    }
}
```

### Dependency considerations {#dependencies-health}

Generally, Jetpack libraries should avoid dependencies that negatively impact
developers without providing substantial benefit. Libraries should consider the
implications of their dependencies, including:

-   Large dependencies where only a small portion is needed (e.g. APK bloat)
-   Dependencies that slow down build times through annotation processing or
    compiler overhead
-   Dependencies which do not maintain binary compatibility and conflict with
    semantic versioning guarantees
-   Dependencies that are intended for server environments and don't interact
    well with the Android build toolchain (e.g. R8) or runtime (e.g. ART)

#### Kotlin {#dependencies-kotlin}

Kotlin is *strongly recommended* for new libraries and the Kotlin stdlib will
already be present in the transitive dependencies of any library that depends on
`androidx.annotations`.

```
plugins {
    id("AndroidXPlugin")
    id("kotlin-android")
}

dependencies {
    implementation(libs.kotlinStdlib)
}
```

Java-based libraries *may* migrate to Kotlin, but they must be careful to
maintain binary compatibility during the migration. Metalava does not cover all
possible aspects of migration, so some manual work will be required.

#### Kotlin coroutines {#dependencies-coroutines}

The Kotlin coroutines library adds around 100kB post-shrinking. New libraries
that are written in Kotlin should prefer coroutines over `ListenableFuture`, but
existing libraries must consider the size impact on their clients. See
[Asynchronous work with return values](/docs/api_guidelines#async-return)
for more details on using Kotlin coroutines in Jetpack libraries.

```
dependencies {
    implementation(libs.kotlinCoroutinesAndroid)
}
```

#### GSON {#dependencies-gson}

GSON relies heavily on reflection and interacts poorly with app optimization
tools like R8. Instead, consider using `org.json` which is included in the
Android platform SDK.

#### Guava {#dependencies-guava}

The full Guava library is very large and should only be used in cases where
there is a reasonable assumption that clients already depend on full Guava.

For example, consider a library `androidx.foo:foo` implemented using Kotlin
`suspend fun`s and an optional `androidx.foo:foo-guava` library that provides
`ListenableFuture` interop wrappers with a direct dependency on
`kotlinx.coroutines:kotlinx-coroutines-guava` and a transitive dependency on
Guava.

Libraries that only need `ListenableFuture` may instead depend on the standalone
`com.google.guava:listenablefuture` artifact. See
[Asynchronous work with return values](/docs/api_guidelines#async-return)
for more details on using `ListenableFuture` in Jetpack libraries.

#### Protobuf {#dependencies-protobuf}

**Note**: It is preferred to use the [`wire`](https://github.com/square/wire)
library for handling protocol buffers in Android libraries as it has a binary
stable runtime. An example of its usage can be found
[here](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:benchmark/benchmark-common/build.gradle?q=wireRuntime%20file:gradle&ss=androidx%2Fplatform%2Fframeworks%2Fsupport).

[Protocol buffers](https://developers.google.com/protocol-buffers) provide a
language- and platform-neutral mechanism for serializing structured data. The
implementation enables developers to maintain protocol compatibility across
library versions, meaning that two clients can communicate regardless of the
library versions included in their APKs.

The Protobuf library itself, however, does not guarantee ABI compatibility
across minor versions and a specific version **must** be used with a library to
avoid conflict with other dependencies used by the developer. To do this, you
must first create a new project to repackage the protobuf runtime classes, and
then have it as a dependency in the project you generate protos in. In the
project that generates protos, you must also relocate any import statements
containing `com.google.protobuf` to your target package name. The
[AndroidXRepackagePlugin](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/private/src/main/kotlin/androidx/build/AndroidXRepackageImplPlugin.kt)
abstracts this for you. An example of its use to repackage the protobuf runtime
library can be found
[here](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:wear/protolayout/protolayout-external-protobuf/build.gradle)
and its associated use in the library that generates protos can be found
[here](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:wear/protolayout/protolayout-proto/build.gradle).

Additionally, the Java API surface generated by the Protobuf compiler is not
guaranteed to be stable and **must not** be exposed to developers. Library
owners should wrap the generated API surface with well-documented public APIs
that follow an appropriate language-specific paradigm for constructing data
classes, e.g. the Java `Builder` pattern.

### Open-source compatibility {#dependencies-aosp}

Jetpack's [open-source](/docs/open_source.md) guidelines
require that libraries consider the open-source compatibility implications of
their dependencies, including:

-   Closed-source or proprietary libraries or services that may not be available
    on AOSP devices
-   Dependencies that may prevent developers from effectively isolating their
    tests from third-party libraries or services

Primary artifacts, e.g. `workmanager`, **must not** depend on closed-source
components including libraries and hard-coded references to packages,
permissions, or IPC mechanisms that may only be fulfilled by closed-source
components.

Optional artifacts, e.g. `workmanager-gcm`, *may* depend on closed-source
components or configure a primary artifact to be backed by a closed-source
component via service discovery or initialization.

Some examples of safely depending on closed-source components include:

-   WorkManager's GCM Network Manager integration, which uses
    [manifest metadata](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:work/work-gcm/src/main/AndroidManifest.xml)
    for service discovery and provides an optional artifact exposing the
    service.
-   Downloadable Fonts integration with Play Services, which plugs in via a
    [`ContentProvider`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:core/core/src/androidTest/java/androidx/core/provider/MockFontProvider.java)
    as a service discovery mechanism with developer-specified
    [signature verification](https://developer.android.com/guide/topics/ui/look-and-feel/downloadable-fonts#adding-certificates)
    for additional security.

Note that in all cases, the developer is not *required* to use GCM or Play
Services and may instead use another compatible service implementing the same
publicly-defined protocols.

### Toolchain dependencies

Toolchain dependencies are typically specified by the AndroidX build system and
are often limited, if any, configuration on behalf of library owners.

#### Kotlin language

Several projects within AndroidX depend on aspects of the Kotlin compiler that
do not guarantee binary compatibility, which means (1) Kotlin updates within
AndroidX may be more complicated and (2) Kotlin updates may be more complicated
for external clients.

For this reason, we try to separate (1) and (2) by pinning the Kotlin language
and API versions until the new compiler has been in use in AndroidX for at least
three months.

Library owners *may* in limited cases update their Kotlin language version early
by specifying the `kotlinVersion` DSL property:

```
androidx {
    kotlinVersion KOTLIN_1_9
}
```

Note that this propagates the version requirement to all dependencies and is not
appropriate for low-level libraries.

#### Java language

The Java language level determines the minimum version of the Java runtime
required for lint checks and other host-side libraries like compilers.

To avoid causing issues for clients, we try to separate Java compiler or runtime
updates from language level by pinning the Java language level to the second
most-recent stable LTS version. In extreme cases, however, we may be required to
move to a more recent version because of a dependency like AGP or Gradle.

Library owners *may*, in cases where clients are unable to update their Java
version, temporarily pin their Java language version to a lower value by
specifying the compatibility DSL properties:

```
javaExtension.apply {
    // TODO(b/12345678) Remove this once clients are able to update.
    sourceCompatibility = VERSION_17
    targetCompatibility = VERSION_17
}
```

When doing so, library owners **must** file a bug and establish a timeline to
un-pin and rejoin the rest of AndroidX.

#### Desugaring and R8/D8

Currently, the highest Java language level supported for Android libraries is
Java 1.8 (`VERSION_1_8``) via D8/R8 desugaring. See Use Java 8 language features
and APIs for more details.

AndroidX **does not** currently support library API desugaring, so the use of
Java 8 APIs requires increasing the library's `minSdk`.

#### Android SDK

The AndroidX Core & Tooling team automatically updates the `compileSdk` value
following the first public release of a stable SDK, e.g. following SDK
finalization during the Beta stage of platform SDK development.

Library owners **must not** attempt to pin their `compileSdk` to a lower value.

Libraries that are developed against extension SDKs *may* pin their `compileSdk`
to a higher value, e.g. `34-ext5` when the rest of AndroidX is using `34`.
