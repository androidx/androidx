# FAQ

[TOC]

## General FAQ

### What is AndroidX?

The Android Extension (AndroidX) Libraries provide functionality that extends
the capabilities of the Android platform. These libraries, which ship separately
from the Android OS, focus on improving the experience of developing apps
through broad OS- and device-level compatibility, high-level abstractions to
simplify and unify platform features, and other new features that target
developer pain points. To find out more about AndroidX, see the public
documentation on developer.android.com.

### Why did we move to AndroidX?

Please read our
[blog post](https://android-developers.googleblog.com/2018/05/hello-world-androidx.html)
about our migration to AndroidX.

### What happened to the Support Library?

As part of the Jetpack effort to improve developer experience on Android, the
Support Library team undertook a massive refactoring project. Over the course of
2017 and 2018, we streamlined and enforced consistency in our packaging,
developed new policies around vesioning and releasing, and developed tools to
make it easy for developers to migrate.

### Will there be any more updates to Support Library?

No, Revision 28.0.0 of the Support Library, which launched as stable in
September 2018, was the last feature release in the android.support package.
There will be no further releases under Support Library packaging.

### How is AndroidX related to Jetpack?

They are the same thing! In a sentence, AndroidX is the packaging and
internally-facing development project for all components in Jetpack. Jetpack is
the external branding for libraries within AndroidX.

In more detail, Jetpack is the external branding for the set of components,
tools, and guidance that improve the developer experience on Android. AndroidX
is the open-source development project that defines the workflow, versioning,
and release policies for ALL libraries included in Jetpack. All libraries within
the androidx Java package follow a consistent set of API design guidelines,
conform to SemVer and alpha/beta revision cycles, and use the Android issue
tracker for bugs and feature requests.

### What AndroidX library versions have been officially released?

You can see all publicly released versions on the interactive
[Google Maven page](https://dl.google.com/dl/android/maven2/index.html).

### How do I jetify something?

The Standalone Jetifier documentation and download link can be found
[here](https://developer.android.com/studio/command-line/jetifier), under the
Android Studio DAC.

### How do I update my library version?

See the steps specified on the version page
[here](versioning.md#how-to-update-your-version).

### How do I test my change in a separate Android Studio project?

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
# Creates <path-to-checkout>/out/dist/sdk-repo-linux-m2repository-##.zip
./gradlew createArchive
```

Next, take the ZIP output from this task and extract the contents to the Android
SDK path that you are using for your alternate (non-AndroidX) version of Android
Studio. For example, you may be using `~/Android/SDK/extras` if you are using
the default Android Studio SDK for app development or
`prebuilts/fullsdk-linux/extras` if you are using fullsdk for platform
development.

```shell
# Creates or overwrites android/m2repository
cd <path-to-sdk>/extras
unzip <path-to-checkout>/out/dist/top-of-tree-m2repository-##.zip
```

Finally, in the dependencies section of your standalone project's `build.gradle`
file, add or update the `compile` entries to reflect the AndroidX modules that
you would like to test:

```
dependencies {
    ...
    compile "com.android.support:appcompat-v7:26.0.0-SNAPSHOT"
}
```

## Version FAQ {#version}

### How are changes in dependency versions propagated?

If you declare `api(project(":depGroupId"))` in your `build.gradle`, then the
version change will occur automatically. While convienent, be intentional when
doing so because this causes your library to have a direct dependency on the
version in development.

If you declare `api("androidx.depGroupId:depArtifactId:1.0.0")`, then the
version change will need to be done manually and intentionally. This is
considered best practice.

### How does a library begin work on a new Minor version?

Set the version to the next minor version, as an alpha.

### How does a library ship an API reference documentation bugfix?

Developers obtain API reference documentation from two sources -- HTML docs on
[d.android.com](https://d.android.com), which are generated from library release
artifacts, and Javadoc from source JARs on Google Maven.

As a result, documentation bug fixes should be held with other fixes until they
can go through a normal release cycle. Critical (e.g. P0) documentation issues
**may** result in a [bugfix](loaf.md#bugfix) release independent of other fixes.

### When does an alpha ship?

For public releases, an alpha ships when the library lead believes it is ready.
Generally, these occur during the batched bi-weekly (every 2 weeks) release
because all tip-of-tree dependencies will need to be released too.

### Are there restrictions on when or how often an alpha can ship?

Nope.

### Can Alpha work (ex. for the next Minor release) occur in the primary development branch during Beta API lockdown?

No. This is by design. Focus should be spent on improving the Beta version and
adding documentation/samples/blog posts for usage!

### Is there an API freeze window between Alpha and Beta while API surface is reviewed and tests are added, but before the Beta is released?

Yes. If any new APIs are added in this window, the beta release will be blocked
until API review is complete and addressed.

### How often can a Beta release?

As often as needed, however, releases outside of the bi-weekly (every 2 weeks)
release will need to get approval from the TPM (nickanthony@).

### What are the requirements for moving from Alpha to Beta?

See the [Beta section of Versioning guidelines](versioning.md?#beta) for
pre-release cycle transition requirements.

### What are the requirements for a Beta launch?

See the [Beta section of Versioning guidelines](versioning.md?#beta) for
pre-release cycle transition requirements.
