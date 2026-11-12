#!/bin/bash
set -e

echo "Starting $0 at $(date)"

BUILD_SCRIPT="impl/build.sh"
TASKS="test allHostTests zipOwnersFiles createModuleInfo assembleRelease assembleAndroidTest zipTestConfigsWithApks"
EXTRA_PARAMS="-Pandroid.experimental.disableCompileSdkChecks=true -Pandroidx.useMaxDepVersions"

"$(dirname "$0")/impl/host_test_common_test_runner.sh" "$BUILD_SCRIPT" "$TASKS" "$EXTRA_PARAMS" "$@"

echo "Completing $0 at $(date)"
