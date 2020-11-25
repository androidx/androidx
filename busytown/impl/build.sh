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
(cd frameworks/support && echo "top commit:" && git --no-pager log -1)

# determine which subset of projects to include, and be sure to print it if it is specified
PROJECTS_ARG=""
if [ "$ANDROIDX_PROJECTS" != "" ]; then
  PROJECTS_ARG="ANDROIDX_PROJECTS=$ANDROIDX_PROJECTS"
fi
# --no-watch-fs disables file system watch, because it does not work on busytown
# due to our builders using OS that is too old.
run $PROJECTS_ARG OUT_DIR=out DIST_DIR=$DIST_DIR ANDROID_HOME=./prebuilts/fullsdk-linux \
    frameworks/support/gradlew -p frameworks/support \
    --stacktrace \
    -Pandroidx.summarizeStderr \
    --no-watch-fs \
    "$@"
