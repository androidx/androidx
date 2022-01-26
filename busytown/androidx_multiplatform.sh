#!/bin/bash
set -e

cd "$(dirname $0)"

./androidx.sh -Pandroidx.compose.multiplatformEnabled=true compileDebugAndroidTestSources compileDebugSources desktopTestClasses "$@"
