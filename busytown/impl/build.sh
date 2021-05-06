#!/bin/bash
set -e

# This script runs frameworks/support/gradlew

function showDiskStats() {
  echo "df -h"
  df -h
}
showDiskStats

# find script
SCRIPT_DIR="$(cd $(dirname $0) && pwd)"

# resolve directories
cd "$SCRIPT_DIR/../.."
OUT_DIR="../../out"
mkdir -p "$OUT_DIR"
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$OUT_DIR/dist"
fi
mkdir -p "$DIST_DIR"

# record the build start time
BUILD_START_MARKER="$OUT_DIR/build.sh.start"
touch $BUILD_START_MARKER
# record the build number
echo "$BUILD_NUMBER" >> "$OUT_DIR/build_number.log"
# only keep the last 10 build numbers
tail -n 10 "$OUT_DIR/build_number.log" > "$OUT_DIR/build_number.log.tail"
mv "$OUT_DIR/build_number.log.tail" "$OUT_DIR/build_number.log"
cp "$OUT_DIR/build_number.log" "$DIST_DIR/build_number.log"

# runs a given command and prints its result if it fails
function run() {
  echo Running "$*"
  if eval "$*"; then
    return 0
  else
    echo >&2
    echo "Gradle command failed:" >&2
    echo >&2
    # Echo the Gradle command formatted for ease of reading.
    # Put each argument on its own line because some arguments may be long.
    # Also put "\" at the end of non-final lines so the command can be copy-pasted
    echo "$*" | sed 's/ / \\\n/g' | sed 's/^/    /' >&2
    showDiskStats
    return 1
  fi
}

# Confirm the existence of .git dirs. TODO(b/170634430) remove this
(echo "top commit:" && git --no-pager log -1)

# determine which subset of projects to include, and be sure to print it if it is specified
PROJECTS_ARG=""
if [ "$ANDROIDX_PROJECTS" != "" ]; then
  PROJECTS_ARG="ANDROIDX_PROJECTS=$ANDROIDX_PROJECTS"
fi
# --no-watch-fs disables file system watch, because it does not work on busytown
# due to our builders using OS that is too old.
run $PROJECTS_ARG OUT_DIR=$OUT_DIR DIST_DIR=$DIST_DIR ANDROID_HOME=../../prebuilts/fullsdk-linux \
    ./gradlew --ci saveSystemStats "$@"

# check that no unexpected modifications were made to the source repository, such as new cache directories
DIST_DIR=$DIST_DIR $SCRIPT_DIR/verify_no_caches_in_source_repo.sh $BUILD_START_MARKER
