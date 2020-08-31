#!/bin/bash
set -e

# This script runs frameworks/support/gradlew and frameworks/support/ui/gradlew , each with the same arguments

# find script
SCRIPT_DIR="$(cd $(dirname $0) && pwd)"

# resolve DIST_DIR
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$SCRIPT_DIR/../../../../out/dist"
fi
mkdir -p "$DIST_DIR"

# cd to checkout root
cd "$SCRIPT_DIR/../../../.."

LOG_SIMPLIFIER="$SCRIPT_DIR/../../development/build_log_simplifier.sh"

"$LOG_SIMPLIFIER" OUT_DIR=out/ui DIST_DIR=$DIST_DIR/ui ANDROID_HOME=./prebuilts/fullsdk-linux frameworks/support/ui/gradlew -p frameworks/support/ui --stacktrace "$@"
"$LOG_SIMPLIFIER" OUT_DIR=out    DIST_DIR=$DIST_DIR    ANDROID_HOME=./prebuilts/fullsdk-linux frameworks/support/gradlew    -p frameworks/support    --stacktrace "$@"
