# The Manual Prebuilts Dance™

NOTE There is also a [script](releasing_detailed.md#the-prebuilts-dance™) that
automates this step.

Public-facing Jetpack library docs are built from prebuilts to reconcile our
monolithic docs update process with our independently-versioned library release
process.

Submit the following changes in the same Gerrit topic so that they merge in the
same build ID:

1.  Commit your release artifact to the AndroidX AOSP checkout's local Maven
    repository under `prebuilts/androidx/internal`.

2.  Update the version for your library in the public docs configuration
    ([docs-public/build.gradle](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:docs-public/build.gradle)).
    If this is the first time that your library is being published, you will
    need to add a new entry.

Once both changes are, make sure to note the build ID where they landed. You
will need to put this in your release request bug for Docs team.
