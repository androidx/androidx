#!/bin/bash
set -e
cd "$(dirname "$0")"

# Builds all projects that support KMP except for Compose-specific projects
# Must be run on Mac
export ANDROIDX_PROJECTS=INFRAROGUE   # TODO: Switch from `INFRAROGUE` to `KMP`

export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp

sharedArgs="--no-configuration-cache -Pandroidx.constraints=true -Pandroidx.lowMemory $*"
# Setup simulators
impl/androidx-native-mac-simulator-setup.sh

impl/build.sh buildOnServer listTaskOutputs createAllArchives "$sharedArgs"