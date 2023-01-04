#!/bin/bash
set -e

# Runs host tests for all projects that support KMP except for Compose-specific projects which are
# already covered by androidx_compose_multiplatform.sh

# Must be run on Mac

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