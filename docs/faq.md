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
up equivalent policies and processes.

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
