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
if [ "$OUT_DIR" == "" ]; then
  OUT_DIR="../../out"
fi
mkdir -p "$OUT_DIR"
export OUT_DIR="$(cd $OUT_DIR && pwd)"
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$OUT_DIR/dist"
fi
mkdir -p "$DIST_DIR"
export DIST_DIR="$DIST_DIR"

# parse arguments
if [ "$1" == "--diagnose" ]; then
  DIAGNOSE=true
  shift
else
  DIAGNOSE=false
fi

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

# export some variables
ANDROID_HOME=../../prebuilts/fullsdk-linux

# run the build
if run ./gradlew --ci saveSystemStats "$@"; then
  echo build passed
else
  if [ "$DIAGNOSE" == "true" ]; then
    # see if diagnose-build-failure.sh can identify the root cauase
    echo "running diagnose-build-failure.sh, see build.log" >&2
    ./development/diagnose-build-failure/diagnose-build-failure.sh "--ci saveSystemStats $*"
  fi
  exit 1
fi

# check that no unexpected modifications were made to the source repository, such as new cache directories
DIST_DIR=$DIST_DIR $SCRIPT_DIR/verify_no_caches_in_source_repo.sh $BUILD_START_MARKER
