#!/bin/bash
set -e

# Runs host tests for all projects that support KMP except for Compose-specific projects which are
# already covered by androidx_compose_multiplatform.sh

# Must be run on Mac

export ANDROIDX_PROJECTS=KMP

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

# Setup simulators
impl/androidx-native-mac-simulator-setup.sh

# simulator tests disabled due to b/350735930
impl/build.sh allHostTests \
    --no-configuration-cache \
    -Pandroidx.ignoreTestFailures \
    -Pandroidx.displayTestOutput=false \
    -Pandroidx.lowMemory \
    -x tvosSimulatorArm64Test \
    -x watchosSimulatorArm64Test \
    "$@"

echo "Completing $0 at $(date)"
