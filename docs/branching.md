# Branch workflow

[TOC]

## Single Development Branch [`aosp/androidx-main`]

All feature development occurs in the public main development branch of the
Android Open Source Project: `androidx-main`. This branch serves as the central
location and source of truth for all AndroidX library source code. All `alpha`
and `beta` version work -- development, builds, and releases -- will be done
ONLY in this branch.

## Release Branches [`aosp/androidx-\<feature\>-release`]

Release branches are used for stabilitization of a library and support of a
previous stable release. With one development branch, this is how AndroidX
provides support for the previous `rc` or stable version.

When a library updates to rc (release-candidate) or stable, that library version
will be snapped over to that library’s release branch. If that release branch
doesn’t exist, then a release branch will be created for that library, snapped
from androidx-main at the commit that changed the library to an rc or stable
version.

Release branches have the following properties:

*   A release branch will contain `rc` or `stable` versions of libraries
*   Release branches can **ONLY** be changed through cherry-picks
*   Bug fixes and updates to `rc` or stable versions must be cherry-picked
*   No `alpha` or `beta` versions will exist in a release branch.
*   Toolchain and other library wide changes to `androidx-main` will be synced
    to each release branch.
*   Release branches will have the naming format
    `androidx-<feature-name>-release`
*   Release branches will be re-snapped from `androidx-main` for each new minor
    version release (for example, releasing `2.2.0-rc01` after `2.1.0`)
