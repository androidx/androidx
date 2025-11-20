#!/bin/bash
# This script contains common logic for running Gradle tasks that involves host tests.
# It handles --continue and checks for a signal file for true build failures vs test failures.
# It is intended to be called by wrapper scripts.

set -e

# Arguments to this script:
# $1: Path to the build script to execute (e.g., "impl/build.sh" or "impl/build-studio-and-androidx.sh")
# $2: Gradle tasks to run (e.g., "test allHostTests" or "allTests")
# $3: Extra specific Gradle parameters for the build command (e.g., "-Pandroidx.useMaxDepVersions")
# Subsequent arguments ("$@") are passed through to the specified build script.

BUILD_SCRIPT_PATH="$1"
GRADLE_TASKS="$2"
EXTRA_GRADLE_PARAMS="$3"
shift 3

source "$(dirname "$0")/../setup_build_env_vars.sh"
source "$(dirname "$0")/../record_build_metrics.sh"

cd "$(dirname "$0")/.."

setup_build_env_vars
start_time=$(initialize_start_time)

BUILD_EXIT_CODE=0
if "$BUILD_SCRIPT_PATH" $GRADLE_TASKS \
    -Pandroidx.displayTestOutput=false \
    --continue \
    $EXTRA_GRADLE_PARAMS \
    "$@"; then
    echo "Gradle command completed successfully."
else
    BUILD_EXIT_CODE=$?
fi

ONLY_TEST_TASK_FAILED_SIGNAL_FILE_PATH="$OUT_DIR/androidx-settings-plugins/only_test_task_failed_signal.txt"

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    # If the build fails and the signal file does NOT exist, it's a genuine build failure.
    # The signal file is only created when test tasks are the sole cause of failure.
    if [ ! -f "$ONLY_TEST_TASK_FAILED_SIGNAL_FILE_PATH" ]; then
        echo "Gradle build failed (exit code $BUILD_EXIT_CODE)."
        exit $BUILD_EXIT_CODE
    fi
    # If the signal file DOES exist, it means only tests failed.
    # We proceed silently to allow ATP to collect and report test results.
fi

record_build_metrics "$start_time"
