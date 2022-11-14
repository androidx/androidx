#!/bin/bash
set -e

export ANDROIDX_PROJECTS=KMP

# disable GCP cache, these machines don't have credentials.
export USE_ANDROIDX_REMOTE_BUILD_CACHE=false

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

# Setup simulators
impl/androidx-native-mac-simulator-setup.sh

impl/build.sh darwinBenchmarkResults allTests \
    --no-configuration-cache \
    -Pandroidx.ignoreTestFailures \
    -Pandroidx.displayTestOutput=false \
    "$@"

echo "Completing $0 at $(date)"