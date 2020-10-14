#!/bin/bash
set -e

# This script runs frameworks/support/gradlew

# find script
SCRIPT_DIR="$(cd $(dirname $0) && pwd)"

# resolve DIST_DIR
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$SCRIPT_DIR/../../../../out/dist"
fi
mkdir -p "$DIST_DIR"

# cd to checkout root
cd "$SCRIPT_DIR/../../../.."

# runs a given command and prints its result if it fails
function run() {
  echo Running "$*"
  if eval "$*"; then
    return 0
  else
    echo >&2
    echo "Failed: $*" >&2
    return 1
  fi
}

# Confirm the existence of .git dirs. TODO(b/170634430) remove this
(cd frameworks/support && echo "top commit:" && git log -1)

run OUT_DIR=out    DIST_DIR=$DIST_DIR    ANDROID_HOME=./prebuilts/fullsdk-linux frameworks/support/gradlew    -p frameworks/support    --stacktrace -Pandroidx.summarizeStderr "$@"
