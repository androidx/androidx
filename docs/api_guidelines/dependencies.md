## Dependencies {#dependencies}

Artifacts may depend on other artifacts within AndroidX as well as sanctioned
third-party libraries.

### Versioned artifacts {#dependencies-versioned}

One of the most difficult aspects of independently-versioned releases is
maintaining compatibility with public artifacts. In a mono repo such as Google's
repository or Android Git at master revision, it's easy for an artifact to
accidentally gain a dependency on a feature that may not be released on the same
schedule.

-   Project `project(":core:core")` uses the tip-of-tree sources for the
    `androidx.core:core` library and requires that they be loaded in the
    workspace.
-   Playground `projectOrArtifact(":core:core")` is used for
    [Playground](/company/teams/androidx/playground.md) projects and will use
    tip-of-tree sources, if present in the workspace, or `SNAPSHOT` prebuilt
    artifacts from [androidx.dev](http://androidx.dev) otherwise.
-   Explicit `"androidx.core:core:1.4.0"` uses the prebuilt AAR and requires
    that it be checked in to the `prebuilts/androidx/internal` local Maven
    repository.

Libraries should prefer explicit dependencies with the lowest possible versions
that include the APIs or behaviors required by the library, using project or
Playground specs only in cases where tip-of-tree APIs or behaviors are required.

#### Pre-release dependencies {#dependencies-pre-release}

Pre-release suffixes **must** propagate up the dependency tree. For example, if
your artifact has API-type dependencies on pre-release artifacts, ex.
`1.1.0-alpha01`, then your artifact must also carry the `alpha` suffix. If you
only have implementation-type dependencies, your artifact may carry either the
`alpha` or `beta` suffix.

Note: This does not apply to test dependencies: suffixes of test dependencies do
*not* carry over to your artifact.

#### Pinned versions {#dependencies-prebuilt}

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

#### Tip-of-tree versions {#dependencies-project}

Below is an example of a non-pinned dependency. It ties the artifact's release
schedule to that of the dependency artifact, because the dependency will need to
be released at the same time.

```
dependencies {
   api(project(":collection"))
   ...
}
```

### Non-public APIs {#dependencies-non-public-apis}

Artifacts may depend on non-public (e.g. `@hide`) APIs exposed within their own
artifact or another artifact in the same `groupId`; however, cross-artifact
usages are subject to binary compatibility guarantees and
`@RestrictTo(Scope.LIBRARY_GROUP)` APIs must be tracked like public APIs.

```
Dependency versioning policies are enforced at build time in the createArchive task. This task will ensure that pre-release version suffixes are propagated appropriately.

Cross-artifact API usage policies are enforced by the checkApi and checkApiRelease tasks (see Life of a release).
```

### Third-party libraries {#dependencies-3p}

Artifacts may depend on libraries developed outside of AndroidX; however, they
must conform to the following guidelines:

*   Prebuilt **must** be checked into Android Git with both Maven and Make
    artifacts
    *   `prebuilts/maven_repo` is recommended if this dependency is only
        intended for use with AndroidX artifacts, otherwise please use
        `external`
*   Prebuilt directory **must** contains an `OWNERS` file identifying one or
    more individual owners (e.g. NOT a group alias)
*   Library **must** be approved by legal

Please see Jetpack's
[open-source policy page](/company/teams/androidx/open_source.md) for more
details on using third-party libraries.

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

### System health {#dependencies-health}

Generally, Jetpack libraries should avoid dependencies that negatively impact
developers without providing substantial benefit. Libraries should consider the
system health implications of their dependencies, including:

-   Large dependencies where only a small portion is needed (e.g. APK bloat)
-   Dependencies that slow down build times through annotation processing or
    compiler overhead

#### Kotlin {#dependencies-kotlin}

Kotlin is *strongly recommended* for new libraries and the Kotlin stdlib will
already be present in the transitive dependencies of any library that depends on
`androidx.annotations`.

Java-based libraries *may* migrate to Kotlin, but they must be careful to
maintain binary compatibility during the migration.

#### Kotlin coroutines {#dependencies-coroutines}

Kotlin's coroutine library adds around 100kB post-shrinking. New libraries that
are written in Kotlin should prefer coroutines over `ListenableFuture`, but
existing libraries must consider the size impact on their clients. See
[Asynchronous work with return values](/company/teams/androidx/api_guidelines#async-return)
for more details on using Kotlin coroutines in Jetpack libraries.

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
[Asynchronous work with return values](/company/teams/androidx/api_guidelines#async-return)
for more details on using `ListenableFuture` in Jetpack libraries.

#### Protobuf {#dependencies-protobuf}

[Protocol buffers](https://developers.google.com/protocol-buffers) provide a
language- and platform-neutral mechanism for serializing structured data. The
implementation enables developers to maintain protocol compatibility across
library versions, meaning that two clients can communicate regardless of the
library versions included in their APKs.

The Protobuf library itself, however, does not guarantee ABI compatibility
across minor versions and a specific version **must** be bundled with a library
to avoid conflict with other dependencies used by the developer.

Additionally, the Java API surface generated by the Protobuf compiler is not
guaranteed to be stable and **must not** be exposed to developers. Library
owners should wrap the generated API surface with well-documented public APIs
that follow an appropriate language-specific paradigm for constructing data
classes, e.g. the Java `Builder` pattern.

### Open-source compatibility {#dependencies-aosp}

Jetpack's [open-source](/company/teams/androidx/open_source.md) guidelines
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
