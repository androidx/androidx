#!/bin/bash
set -e

# This script runs frameworks/support/gradlew

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
if [ "$CHANGE_INFO" != "" ]; then
  cp "$CHANGE_INFO" "$DIST_DIR/"
fi
if [ "$MANIFEST" == "" ]; then
  export MANIFEST="$DIST_DIR/manifest_${BUILD_NUMBER}.xml"
fi

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
    return 1
  fi
}

# export some variables
ANDROID_HOME=../../prebuilts/fullsdk-linux

# run the build
if run ./gradlew --ci saveSystemStats "$@"; then
  echo build passed
else
  if [ "$DIAGNOSE" == "true" ]; then
    # see if diagnose-build-failure.sh can identify the root cauase
    echo "running diagnose-build-failure.sh, see build.log" >&2
    # Specify a short timeout in case we're running on a remote server, so we don't take too long.
    # We probably won't have enough time to fully diagnose the problem given this timeout, but
    # we might be able to determine whether this problem is reproducible enough for a developer to
    # more easily investigate further
    ./development/diagnose-build-failure/diagnose-build-failure.sh --timeout 600 "--ci saveSystemStats $*"
  fi
  if grep "/prefab" "$DIST_DIR/logs/gradle.log" >/dev/null 2>/dev/null; then
    # error looks like it might have involved prefab, copy the prefab dir to DIST where we can find it
    if [ -e "$OUT_DIR/androidx/external/libyuv/build" ]; then
      cd "$OUT_DIR/androidx/external/libyuv/build"
      echo "Zipping $PWD into $DIST_DIR/libyuv-build.zip"
      zip -qr "$DIST_DIR/libyuv-build.zip" .
      cd -
    fi
  fi
  exit 1
fi

# check that no unexpected modifications were made to the source repository, such as new cache directories
DIST_DIR=$DIST_DIR $SCRIPT_DIR/verify_no_caches_in_source_repo.sh $BUILD_START_MARKER
