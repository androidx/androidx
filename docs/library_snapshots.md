# Library snapshots

go/androidx/library_snapshots

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'asfalcone' reviewed: '2025-09-15' }
*-->

[TOC]

## Quick how-to

Add the following snippet to your build.gradle file, replacing `buildId` with a
snapshot build ID.

```groovy {highlight=context:[buildId]}
allprojects {
    repositories {
        google()
        jcenter()
        maven { url 'https://androidx.dev/snapshots/builds/[buildId]/artifacts/repository' }
    }
}
```

You must define dependencies on artifacts using the `SNAPSHOT` version suffix,
for example:

```groovy {highlight=context:SNAPSHOT}
dependencies {
    implementation "androidx.core:core:1.2.0-SNAPSHOT"
}
```

## Where to find snapshots

If you want to use unreleased `SNAPSHOT` versions of `androidx` artifacts, you
can find them on either our public-facing build server:

`https://ci.android.com/builds/submitted/<build_id>/androidx_snapshot/latest`

or on our slightly-more-convenient [androidx.dev](https://androidx.dev) site:

`https://androidx.dev/snapshots/builds/<build-id>/artifacts` for a specific
build ID

`https://androidx.dev/snapshots/latest/artifacts` for tip-of-tree snapshots

## Obtaining a build ID

To browse build IDs, you can visit either
[androidx-main](https://ci.android.com/builds/branches/aosp-androidx-main/grid?)
on ci.android.com or [Snapshots](https://androidx.dev/snapshots/builds) on the
androidx.dev site.

Note that if you are using androidx.dev, you may substitute `latest` for a build
ID to use the last known good build.

To manually find the last known good `build-id`, you have several options.

### Snapshots on androidx.dev

[Snapshots](https://androidx.dev/snapshots/builds) on androidx.dev only lists
usable builds.

### Programmatically via `jq`

Install `jq`:

```shell
sudo apt-get install jq
```

```shell
ID=`curl -s "https://ci.android.com/builds/branches/aosp-androidx-main/status.json" | jq ".targets[] | select(.ID==\"aosp-androidx-main.androidx_snapshot\") | .last_known_good_build"` \
  && echo https://ci.android.com/builds/submitted/"${ID:1:-1}"/androidx_snapshot/latest/raw/repository/
```

### Android build server

Go to
[androidx-main](https://ci.android.com/builds/branches/aosp-androidx-main/grid?)
on ci.android.com.

For `androidx-snapshot` target, wait for the green "last known good build"
button to load and then click it to follow it to the build artifact URL.

## Using in a Gradle build

To make these artifacts visible to Gradle, you need to add it as a repository:

```groovy
allprojects {
    repositories {
        google()
        maven {
          // For all Jetpack libraries (including Compose)
          url 'https://androidx.dev/snapshots/builds/<build-id>/artifacts/repository'
        }
    }
}
```

Note that the above requires you to know the `build-id` of the snapshots you
want.

### Specifying dependencies

All artifacts in the snapshot repository are versioned as `x.y.z-SNAPSHOT`. So
to use a snapshot artifact, the version in your `build.gradle` will need to be
updated to `androidx.<groupId>:<artifactId>:X.Y.Z-SNAPSHOT`

For example, to use the `core:core:1.2.0-SNAPSHOT` snapshot, you would add the
following to your `build.gradle`:

```
dependencies {
    ...
    implementation("androidx.core:core:1.2.0-SNAPSHOT")
    ...
}
```
