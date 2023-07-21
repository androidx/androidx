#!/bin/bash
set -e
cd "$(dirname $0)"

# Builds all projects that support KMP except for Compose-specific projects which are already
# covered by androidx_compose_multiplatform.sh

# Must be run on Mac

export ANDROIDX_PROJECTS=INFRAROGUE   # TODO: Switch from `INFRAROGUE` to `KMP`

# disable GCP cache, these machines don't have credentials.
export USE_ANDROIDX_REMOTE_BUILD_CACHE=false

# Setup simulators
impl/androidx-native-mac-simulator-setup.sh

impl/build.sh buildOnServer :docs-kmp:zipCombinedKmpDocs --no-configuration-cache -Pandroidx.displayTestOutput=false createAllArchives -Pandroidx.constraints=true

# run a separate createAllArchives task to prepare a repository
# folder in DIST.
# This cannot be merged with the buildOnServer run because
# snapshot version is not a proper release version.
DIST_DIR=$DIST_DIR/snapshots SNAPSHOT=true impl/build.sh createAllArchives --no-configuration-cache -Pandroidx.constraints=true
