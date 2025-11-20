#!/bin/bash
set -e

# Runs host tests for all projects that support KMP except for Compose-specific projects which are
# already covered by androidx_compose_multiplatform.sh

# Must be run on Mac

export ANDROIDX_PROJECTS=INFRAROGUE   # TODO: Switch from `INFRAROGUE` to `KMP`

echo "Starting $0 at $(date)"

BUILD_SCRIPT="impl/build.sh"
HOST_TEST_TASKS="darwinBenchmarkResults allHostTests"
EXTRA_PARAMS="--no-configuration-cache"

"$(dirname "$0")/impl/androidx-native-mac-simulator-setup.sh"
"$(dirname "$0")/impl/host_test_common_test_runner.sh" "$BUILD_SCRIPT" "$HOST_TEST_TASKS" "$EXTRA_PARAMS" "$@"

echo "Completing $0 at $(date)"
