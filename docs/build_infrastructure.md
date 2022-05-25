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

AndroidX build bots use the
[Gradle Remote Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
to speed up build targets. We
[configure](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/remoteBuildCache.gradle;drc=dd99f75742c18a499110b979c7c25bf822113e3e;l=49)
the
[GCP build cache Gradle plugin](https://github.com/androidx/gcp-gradle-build-cache)
to connect to the GCP Storage bucket `androidx-gradle-remote-cache` in the
`androidx-ge` project.
