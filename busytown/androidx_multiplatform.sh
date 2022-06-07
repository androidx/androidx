#!/bin/bash
set -e

cd "$(dirname $0)"

# androidx.sh disables the cache unless explicitly enabled
export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp

./androidx.sh -Pandroidx.compose.multiplatformEnabled=true compileDebugAndroidTestSources compileDebugSources desktopTestClasses "$@"
