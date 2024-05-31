#!/bin/bash
set -e
cd "$(dirname "$0")"

export ANDROIDX_PROJECTS=KMP

export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp

sharedArgs="--no-configuration-cache -Pandroidx.lowMemory $*"
# Setup simulators
impl/androidx-native-mac-simulator-setup.sh

impl/build.sh buildOnServer listTaskOutputs checkExternalLicenses "$sharedArgs"