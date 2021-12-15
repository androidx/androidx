# FAQ

[TOC]

## General FAQ

### What is `androidx`?

The Android Extension (`androidx`) Libraries provide functionality that extends
the capabilities of the Android platform. These libraries, which ship separately
from the Android OS, focus on improving the experience of developing apps
through broad OS- and device-level compatibility, high-level abstractions to
simplify and unify platform features, and other new features that target
developer pain points. To find out more about `androidx`, see the public
documentation on [developer.android.com](http://developer.android.com).

### Why did we move to `androidx`?

Please read our
[blog post](https://android-developers.googleblog.com/2018/05/hello-world-androidx.html)
about our migration.

### What happened to the Support Library?

As part of the Jetpack effort to improve developer experience on Android, the
Support Library team undertook a massive refactoring project. Over the course of
2017 and 2018, we streamlined and enforced consistency in our packaging,
developed new policies around vesioning and releasing, and developed tools to
make it easy for developers to migrate.

### Will there be any more updates to Support Library?

No, revision `28.0.0` of the Support Library, which launched as stable in
September 2018, was the last feature release in the `android.support` package.
There will be no further releases under Support Library packaging and they
should be considered deprecated.

### How are `androidx` and AndroidX related to Jetpack?

They are effectively the same thing! In a sentence, `androidx` is the packaging
and AndroidX is the development workflow for most components in Jetpack. Jetpack
is the external branding for libraries within the `androidx` package.

In more detail, Jetpack is the external branding for the set of components,
tools, and guidance that improve the developer experience on Android. AndroidX
is the open-source development project that defines the workflow, versioning,
and release policies for ALL libraries included in Jetpack. All libraries within
the `androidx` Java package follow a consistent set of API design guidelines,
conform to SemVer and alpha/beta revision cycles, and use the Android issue
tracker for bugs and feature requests.

### What library versions have been officially released?

You can see all publicly released versions on the interactive
[Google Maven page](https://dl.google.com/dl/android/maven2/index.html).

### How do I jetify something?

The Standalone Jetifier documentation and download link can be found
[here](https://developer.android.com/studio/command-line/jetifier), under the
Android Studio DAC.

### How do I update my library version?

See the steps specified on the version page
[here](versioning.md#how-to-update-your-version).

## Version FAQ {#version}

### When does an alpha ship?

For public releases, an alpha ships when the library lead believes it is ready.
Generally, these occur during the batched bi-weekly (every 2 weeks) release
because all tip-of-tree dependencies will need to be released too.

### Are there restrictions on when or how often an alpha can ship?

Nope.

### Can alpha work (ex. for the next Minor release) occur in the primary development branch during beta API lockdown?

No. This is by design. Focus should be spent on improving the Beta version and
adding documentation/samples/blog posts for usage!

### Is there an API freeze window between alpha and beta while API surface is reviewed and tests are added, but before the beta is released?

Yes. If any new APIs are added in this window, the beta release will be blocked
until API review is complete and addressed.

### How often can a beta release?

As often as needed, however, releases outside of the bi-weekly (every 2 weeks)
release will need to get approval from the TPM (nickanthony@).
