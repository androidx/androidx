# FAQ

[TOC]

## General FAQ

### What is `androidx`?

Artifacts within the `androidx` package comprise the libraries of
[Android Jetpack](https://developer.android.com/jetpack).

Libraries in the `androidx` package provide functionality that extends the
capabilities of the Android platform. These libraries, which ship separately
from the Android OS, focus on improving the experience of developing apps
through broad OS- and device-level compatibility, high-level abstractions to
simplify and unify platform features, and other new features that target
developer pain points.

### How are `androidx` and AndroidX related to Jetpack?

They are effectively the same thing!

**Jetpack** is the external branding for the set of components, tools, and
guidance that improve the developer experience on Android.

Libraries within Jetpack use the **`androidx`** Java package and Maven group ID.
Developers expect these libraries to follow a consistent set of API design
guidelines, conform to SemVer and alpha/beta revision cycles, and use the public
Android issue tracker for bugs and feature requests.

**AndroidX** is the open-source project where the majority\* of Jetpack
libraries are developed. The project's tooling and infrastructure enforce the
policies associated with Jetback branding and `androidx` packaging, allowing
library developers to focus on writing and releasing high-quality code.

<sup>* Except a small number of libraries that were historically developed using
a different workflow, such as ExoPlayer/Media or AndroidX Test, and have built
up equivalent policies and processes.</sup>

### Why did we move to `androidx`?

Please read our
[blog post](https://android-developers.googleblog.com/2018/05/hello-world-androidx.html)
about our migration.

### What happened to the Support Library?

As part of the Jetpack effort to improve developer experience on Android, the
Support Library team undertook a massive refactoring project. Over the course of
2017 and 2018, we streamlined and enforced consistency in our packaging,
developed new policies around versioning and releasing, and developed tools to
make it easy for developers to migrate.

### Will there be any more updates to Support Library?

No, revision `28.0.0` of the Support Library, which launched as stable in
September 2018, was the last feature release in the `android.support` package.
There will be no further releases under Support Library packaging and they
should be considered deprecated.

### What library versions have been officially released?

You can see all publicly released versions on the interactive
[Google Maven page](https://maven.google.com).

### How do I test my change in a separate Android Studio project? {#faq-test-change-studio}

If you're working on a new feature or bug fix in AndroidX, you may want to test
your changes against another project to verify that the change makes sense in a
real-world context or that a bug's specific repro case has been fixed.

If you need to be absolutely sure that your test will exactly emulate the
developer's experience, you can repeatedly build the AndroidX archive and
rebuild your application. In this case, you will need to create a local build of
AndroidX's local Maven repository artifact and install it in your Android SDK
path.

First, use the `createArchive` Gradle task to generate the local Maven
repository artifact:

```shell
# Creates <path-to-checkout>/out/repository/
./gradlew createArchive
```

Using your alternate (non-AndroidX) version of Android Studio open the project's
`settings.gradle.kts` and add the following within
`dependencyResolutionManagement` to make your project look for binaries in the
newly built repository:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Add this
        maven {
            setUrl("<absolute-path-to-checkout>/out/repository/")
        }
    }
}
```

NOTE Gradle resolves dependencies in the order that the repositories are defined
(if 2 repositories can resolve the same dependency, the first listed will do so
and the second will not). Therefore, if the library you are testing has the same
group, artifact, and version as one already published, you will want to list
your custom maven repo first.

Finally, in the dependencies section of your standalone project's `build.gradle`
file, add or update the `implementation` entries to reflect the AndroidX modules
that you would like to test. Example:

```
dependencies {
    ...
    implementation "androidx.appcompat:appcompat:1.0.0-alpha02"
}
```

If you are testing your changes in the Android Platform code, you can replace
the module you are testing
`YOUR_ANDROID_PATH/prebuilts/sdk/current/androidx/m2repository` with your own
module. We recommend only replacing the module you are modifying instead of the
full m2repository to avoid version issues of other modules. You can either take
the unzipped directory from
`<path-to-checkout>/out/dist/top-of-tree-m2repository-##.zip`, or from
`<path-to-checkout>/out/repository/` after building `androidx`. Here is an
example of replacing the RecyclerView module:

```shell
$TARGET=YOUR_ANDROID_PATH/prebuilts/sdk/current/androidx/m2repository/androidx/recyclerview/recyclerview/1.1.0-alpha07;
rm -rf $TARGET;
cp -a <path-to-sdk>/extras/m2repository/androidx/recyclerview/recyclerview/1.1.0-alpha07 $TARGET
```

Make sure the library versions are the same before and after replacement. Then
you can build the Android platform code with the new `androidx` code.

### How do I add content to a library's Overview reference doc page?

Put content in a markdown file that ends with `-documentation.md` in the
directory that corresponds to the Overview page that you'd like to document.

For example, the `androidx.compose.runtime`
[Overview page](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary)
includes content from
[compose-runtime-documentation.md](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/compose-runtime-documentation.md).

### How do I enable MultiDex for my library?

It is enabled automatically as androidx minSdkVersion is API >=21.

### Common build errors

#### Diagnosing build failures

If you've encountered a build failure and you're not sure what is triggering it,
then please run
`./development/diagnose-build-failure/diagnose-build-failure.sh`.

This script can categorize your build failure into one of the following
categories:

*   The Gradle Daemon is saving state in memory and triggering a failure
*   Your source files have been changed and/or incompatible git commits have
    been checked out
*   Some file in the out/ dir is triggering an error
    *   If this happens, diagnose-build-failure.sh should also identify which
        file(s) specifically
*   The build is nondeterministic and/or affected by timestamps
*   The build via gradlew actually passes and this build failure is specific to
    Android Studio

Some more-specific build failures are listed below in this page.

#### Out-of-date platform prebuilts

Like a normal Android library developed in Android Studio, libraries within
`androidx` are built against prebuilts of the platform SDK. These are checked in
to the `prebuilts/fullsdk-darwin/platforms/<android-version>` directory.

If you are developing against pre-release platform APIs in the internal
`androidx-platform-dev` branch, you may need to update these prebuilts to obtain
the latest API changes.

#### Missing external dependency

If Gradle cannot resolve a dependency listed in your `build.gradle`:

*   You will probably want to import the missing artifact via
    [importMaven.sh](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:development/importMaven/README.md)

    *   We store artifacts in the prebuilts repositories under
        `prebuilts/androidx` to facilitate reproducible builds even if remote
        artifacts are changed.

*   You may need to [establish trust for](#dependency-verification) the new
    artifact

##### Importing dependencies in `libs.versions.toml`

Libraries typically reference dependencies using constants defined in
[`libs.versions.toml`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:gradle/libs.versions.toml).
Update this file to include a constant for the version of the library that you
want to depend on. You will reference this constant in your library's
`build.gradle` dependencies.

**After** you update the `libs.versions.toml` file with new dependencies, you
can download them by running:

```shell
cd frameworks/support &&\
development/importMaven/importMaven.sh import-toml
```

This command will resolve everything declared in the `libs.versions.toml` file
and download missing artifacts into `prebuilts/androidx/external` or
`prebuilts/androidx/internal`.

Make sure to upload these changes before or concurrently (ex. in the same Gerrit
topic) with the dependent library code.

##### Downloading a dependency without changing `libs.versions.toml`

You can also download a dependency without changing `libs.versions.toml` file by
directly invoking:

```shell
cd frameworks/support &&\
./development/importMaven/importMaven.sh someGroupId:someArtifactId:someVersion
```

##### Missing konan dependencies

Kotlin Multiplatform projects need prebuilts to compile native code, which are
located under `prebuilts/androidx/konan`. **After** you update the kotlin
version of AndroidX, you should also download necessary prebuilts via:

```shell
cd frameworks/support &&\
development/importMaven/importMaven.sh import-konan-binaries --konan-compiler-version <new-kotlin-version>
```

Please remember to commit changes in the `prebuilts/androidx/konan` repository.

#### Dependency verification

If you import a new dependency that is either unsigned or is signed with a new,
unrecognized key, then you will need to add new dependency verification metadata
to indicate to Gradle that this new dependency is trusted. See the instructions
[here](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/gradle/README.md)

#### Updating an existing dependency

If an older version of a dependency prebuilt was already checked in, please
manually remove it within the same CL that adds the new prebuilt. You will also
need to update `Dependencies.kt` to reflect the version change.

#### My gradle build fails with "error: cannot find symbol" after making framework-dependent changes.

You probably need to update the prebuilt SDK used by the gradle build. If you
are referencing new framework APIs, you will need to wait for the framework
changes to land in an SDK build (or build it yourself) and then land in both
prebuilts/fullsdk and prebuilts/sdk. See
[Updating SDK prebuilts](/docs/playbook.md#prebuilts-fullsdk)
for more information.

#### How do I handle refactoring a framework API referenced from a library?

Because AndroidX must compile against both the current framework and the latest
SDK prebuilt, and because compiling the SDK prebuilt depends on AndroidX, you
will need to refactor in stages:

1.  Remove references to the target APIs from AndroidX
2.  Perform the refactoring in the framework
3.  Update the framework prebuilt SDK to incorporate changes in (2)
4.  Add references to the refactored APIs in AndroidX
5.  Update AndroidX prebuilts to incorporate changes in (4)
