# Stubbed Projects

### List:
- `:annotation:annotation` is stubbed by `:annotation:annotation-compatibility-stubs`, it depends on `androidx.annotation`
- `:collection:collection` is stubbed by `:collection:collection-compatibility-stubs`, it depends on `androidx.collection`
- `:compose:runtime:runtime` is stubbed by `:compose:runtime:runtime-compatibility-stubs`, it depends on `androidx.compose.runtime:runtime`
- `:compose:runtime:runtime-saveable` is stubbed by `:compose:runtime:runtime-saveable-compatibility-stubs`, it depends on `androidx.compose.runtime:runtime-saveable`
- `:lifecycle:lifecycle-common` is stubbed by `lifecycle/lifecycle-common-compatibility-stub`, it depends on `androidx.lifecycle:lifecycle-common`
- `:lifecycle:lifecycle-runtime` is stubbed by `lifecycle/lifecycle-runtime-compatibility-stub`, it depends on `androidx.lifecycle:lifecycle-runtime`
- `:lifecycle:lifecycle-runtime-compose` is stubbed by `lifecycle/lifecycle-runtime-compose-compatibility-stub`, it depends on `androidx.lifecycle:lifecycle-runtime-compose`
- `:lifecycle:lifecycle-viewmodel` is stubbed by `lifecycle/lifecycle-viewmodel-compatibility-stub`, it depends on `androidx.lifecycle:lifecycle-viewmodel`
- `:lifecycle:lifecycle-viewmodel-compose` is stubbed by `lifecycle/lifecycle-viewmodel-compose-compatibility-stub`, it depends on `androidx.lifecycle:lifecycle-viewmodel-compose`
- `:lifecycle:lifecycle-viewmodel-navigation3` is stubbed by `lifecycle/lifecycle-viewmodel-navigation3-compatibility-stub`, it depends on `androidx.lifecycle:lifecycle-viewmodel-navigation3`
- `:lifecycle:lifecycle-viewmodel-savedstate` is stubbed by `lifecycle/lifecycle-viewmodel-savedstate-compatibility-stub`, it depends on `androidx.lifecycle:lifecycle-viewmodel-savedstate`
- `:navigation:navigation-common` is stubbed by `navigation/navigation-common-compatibility-stub`, it depends on `androidx.navigation:navigation-common`
- `:navigation:navigation-runtime` is stubbed by `navigation/navigation-runtime-compatibility-stub`, it depends on `androidx.navigation:navigation-runtime`
- `:navigationevent:navigationevent-compose` is stubbed by `navigationevent/navigationevent-compose-compatibility-stub`, it depends on `androidx.navigationevent:navigationevent-compose`
- `:savedstate:savedstate` is stubbed by `savedstate/savedstate-compatibility-stub`, it depends on `androidx.savedstate:savedstate`
- `:savedstate:savedstate-compose` is stubbed by `savedstate/savedstate-compose-compatibility-stub`, it depends on `androidx.savedstate:savedstate-compose`
- `:window:window-core` is stubbed by `window/window-core-compatibility-stub`, it depends on `androidx.window:window-core`

### Purpose
The need for "stubbed projects" appeared in this PR - https://github.com/JetBrains/compose-multiplatform-core/pull/1819

YT ticket: [CMP-7183: No need to publish annotation and collection libraries anymore](https://youtrack.jetbrains.com/issue/CMP-7183)


**TL;DR;**

To maintain the compatibility with dependant 3rd party libraries versions published in the past, we have to publish a dumb klib with correct unique_name, so it would satisfy the Kotlin dependency resolver (which is independent of Gradle dependency resolver).   


**Details:**

Androidx started to publish the artefacts for all necessary Kotlin targets (jvm, native, js and wasm).
For example: https://maven.google.com/web/index.html?q=annotation#androidx.annotation:annotation

Since androidx has them now, it's redundant to publish such libraries to our repositories.
Such a publication would cause different problems:
- symbols duplication when a project depends on both: androidx library and on the same library  in `org.jetbrains.compose...` 
- unnecessary infrastructure overhead, including a need to maintain the source code synchronization and running the tests

**Conclusion:** we prefer to stop publishing the libraries available on androidx.

**Constraints:**

- [KT-61096: Drop KLIB resolve inside the Kotlin compiler](https://youtrack.jetbrains.com/issue/KT-61096)
- Need to maintain the compatibility of already published libraries (including 3rd party libs) with new Compose versions

The above constraints mean that we can't stop publishing the Klibs, because earlier publihsed 3rd party klibs expect that the dependencies (klibs with unique-names) will be supplied during compilation.
That's why we add these "stubs". 

**What makes them special:**
- their klib unique-name should be compatible (the same) with previously published klibs in `org.jetbrains.compose`
- they have `api(androidx.actualLib:version)` dependency, where androidx.actualLib:version is the library version we would redirect to.
- to avoid symbols duplication, they contain no source code, except one EmptyFile.kt with no code (the module is expected to have at least 1 kotlin file)
