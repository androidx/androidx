# Build infrastructure

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'aurimas' reviewed: '2022-05-05' }
*-->

[TOC]

## Gradle Remote Build Cache

AndroidX build bots use
[Gradle Remote Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
to speed up build targets.
[GCP build cache Gradle plugin](https://github.com/androidx/gcp-gradle-build-cache)
is used to connect to a GCP Storage bucket `androidx-gradle-remote-cache` from
the `androidx-ge` project.
