# KGP Integration

go/androidx-kgp-integration

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'aurimas', owner: 'juliamcclellan', owner: 'fsladkey' reviewed: '2025-04-24' }
*-->

[TOC]

To catch Kotlin upgrade issues before upgrade time, AndroidX team has set up an
integration test with the latest version of Kotlin gradle plugin available on
the
[JetBrains dev repository](https://packages.jetbrains.team/maven/p/kt/dev/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml).

The integration tests run on GitHub and can be found
[here](https://github.com/androidx/androidx/actions/workflows/kgp-nightly-integration.yml).
Currently they build the `collection` project.

Because some code cannot be made compatible with both the current version and
the latest version, a
[patch file](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:.github/integration-patches/kgp-nightly.patch)
is applied before the test runs.

## Fixing failing integration tests

The test can fail for a few reasons.

#### Changes in KGP which can be made compatible in androidx-main

Simply make the change (e.g. suppression) in aosp and merge the change.

#### Changes in KGP which cannot be made in androidx-main, and/or failure to apply changes

*   Upgrade to the latest Kotlin version used by the integration test with
    `./development/update_kotlin.sh <KOTLIN_VERSION>`
*   If there is an existing patch file, apply it with `git apply
    .github/integration-patches/kgp-nightly.patch`
    *   If the patch file cannot be applied due to merge conflicts, see the
        instructions
        [here](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:.github/integration-patches/).
*   Update the Kotlin version in `gradle/libs.versions.toml`
*   Make any necessary changes
*   Ensure `collection` builds locally
*   Run the following (or something similar) to update the patch file

```
# copy new patch file to a temporary file or directly to clipboard
git diff HEAD~2  -- :^.github :^development :^gradle/libs.versions.toml > tmp.patch
# start a new branch and replace the contents of the patch file
repo start update-patch-file
cat tmp.patch > .github/integration-patches/kgp-nightly.patch
```

*   Commit the changes to the new branch
*   Validate that patch applies by running `git apply --stat
    .github/integration-patches/kgp-nightly.patch`
*   Upload and merge the change
*   Trigger the
    [integration branch](https://github.com/androidx/androidx/actions/workflows/kgp-nightly-integration.yml)
    to validate the changes

#### Bugs in KGP

If a change appears to be an unintentional breakage by KGP, the failure should
be reported to Jetbrains. If possible, try to work around the failure with one
of the above methods so that the integration test can continue catching
additional regressions.
