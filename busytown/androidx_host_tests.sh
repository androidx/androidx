#!/bin/bash
set -e

SCRIPT_DIR="$(cd $(dirname $0) && pwd)"
# TODO(b/141549086): move everything below into a common script once this script (androidx_host_tests.sh) is under presubmit testing
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$SCRIPT_DIR/../../../out/dist"
fi
mkdir -p "$DIST_DIR"
cd "$SCRIPT_DIR/../../.."

OUT_DIR=out DIST_DIR="$DIST_DIR" ANDROID_HOME=`pwd`/prebuilts/fullsdk-linux frameworks/support/gradlew -p frameworks/support --no-daemon test jacocoTestReport zipEcFiles --info
OUT_DIR=out/ui DIST_DIR="$DIST_DIR/ui" ANDROID_HOME=`pwd`/prebuilts/fullsdk-linux frameworks/support/ui/gradlew -p frameworks/support/ui --no-daemon test jacocoTestReport zipEcFiles --info
