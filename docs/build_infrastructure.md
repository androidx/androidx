# Build infrastructure

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'aurimas' reviewed: '2022-05-05' }
*-->

[TOC]

## Build invocation scripts

AndroidX uses
[`busytown/`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:busytown/)
directory to wrap build bot invocations to a single command per build target.
Each one of these scripts receives `DIST_DIR`, `OUT_DIR`, and `CHANGE_INFO`
enviroment variables. `DIST_DIR` is a directory for putting build artifacts that
should be saved. `OUT_DIR` is a directory to write temporary build outputs.
`CHANGE_INFO` points to a file that allows build system to know what changed for
a given change that is being built.

## Gradle Remote Build Cache

AndroidX build bots use
[Gradle Remote Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
to speed up build targets.
[GCP build cache Gradle plugin](https://github.com/androidx/gcp-gradle-build-cache)
is used to connect to a GCP Storage bucket `androidx-gradle-remote-cache` from
the `androidx-ge` project.
