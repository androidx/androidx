#!/bin/bash
set -e
cd "$(dirname $0)"

# Builds all projects that support KMP except for Compose-specific projects which are already
# covered by androidx_compose_multiplatform.sh

# Must be run on Mac

export ANDROIDX_PROJECTS=INFRAROGUE   # TODO: Switch from `INFRAROGUE` to `KMP`

# This target is for testing that clean builds work correctly
# We disable the remote cache for this target unless it was already enabled
if [ "$USE_ANDROIDX_REMOTE_BUILD_CACHE" == "" ]; then
  export USE_ANDROIDX_REMOTE_BUILD_CACHE=false
fi

sharedArgs="--no-configuration-cache -Pandroidx.constraints=true $*"
# Setup simulators
impl/androidx-native-mac-simulator-setup.sh

impl/build.sh buildOnServer listTaskOutputs createAllArchives $sharedArgs

# run a separate createAllArchives task to prepare a repository
# folder in DIST.
# This cannot be merged with the buildOnServer run because
# snapshot version is not a proper release version.
DIST_DIR=$DIST_DIR/snapshots SNAPSHOT=true impl/build.sh createAllArchives $sharedArgs
