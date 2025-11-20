#!/bin/bash
set -e
cd "$(dirname "$0")"

export ANDROIDX_PROJECTS=KMP

export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp

sharedArgs="-Pandroidx.lowMemory $*"

impl/build.sh buildOnServer createAllArchives listTaskOutputs checkExternalLicenses "$sharedArgs"