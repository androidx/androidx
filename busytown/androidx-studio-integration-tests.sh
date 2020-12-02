#!/bin/bash

# Script that builds androidx SNAPSHOT and runs the androidx integration
# tests from the Studio branch.

set -e

readonly SCRIPT_PATH="$(dirname $(realpath "$0"))"
readonly BASE_PATH="$(realpath "$SCRIPT_PATH/../../..")"
readonly PREBUILTS_DIR="$BASE_PATH/prebuilts"
readonly OUT_DIR="$BASE_PATH/out"
readonly BAZEL_CMD="$BASE_PATH/tools/base/bazel/bazel"
readonly M2REPO_DIR="$PREBUILTS_DIR/tools/common/androidx-integration/m2repository"
readonly ANDROIDX_INTERNAL_DIR="$PREBUILTS_DIR/androidx/internal"

echo "Using basepath $BASE_PATH"

echo "Starting $0 at $(date)"

$SCRIPT_PATH/androidx_snapshot.sh

mkdir -p $M2REPO_DIR
# Copy internal and the output to prebuilts/tools/common/androidx-integration
cp -R $ANDROIDX_INTERNAL_DIR/* $M2REPO_DIR
unzip -quo $OUT_DIR/dist/top-of-tree-m2repository-all-*.zip -d $M2REPO_DIR/..

$BAZEL_CMD test //tools/adt/idea/androidx-integration-tests:intellij.android.androidx-integration-tests

echo "Completing $0 at $(date)"

