#!/bin/bash
set -e

echo "Starting $0 at $(date)"

BUILD_SCRIPT="impl/build.sh"
HOST_TEST_TASKS="test allHostTests zipOwnersFiles createModuleInfo"
EXTRA_PARAMS=""

"$(dirname "$0")/impl/host_test_common_test_runner.sh" "$BUILD_SCRIPT" "$HOST_TEST_TASKS" "$EXTRA_PARAMS" "$@"

echo "Completing $0 at $(date)"
