#!/bin/bash
set -e

cd "$(dirname $0)"

# androidx.sh disables the cache unless explicitly enabled
export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp
export ANDROIDX_PROJECTS=COMPOSE

./androidx.sh -Pandroidx.compose.multiplatformEnabled=true compileDebugAndroidTestSources compileDebugSources desktopTestClasses -Pandroidx.enableAffectedModuleDetection=false "$@"
