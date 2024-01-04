#!/bin/bash
set -e

echo "Starting $0 at $(date)"

androidxArguments="$*"

SCRIPTS_DIR="$(cd $(dirname $0)/.. && pwd)"
cd "$SCRIPTS_DIR/../../.."
CHECKOUT_ROOT="$(pwd)"
echo "Script running from $(pwd)"

# resolve dirs
export OUT_DIR=$(pwd)/out

if [ -z "$DIST_DIR" ]; then
  DIST_DIR="$OUT_DIR/dist"
fi
mkdir -p "$DIST_DIR"

export DIST_DIR="$DIST_DIR"

# resolve GRADLE_USER_HOME
export GRADLE_USER_HOME="$OUT_DIR/gradle"
mkdir -p "$GRADLE_USER_HOME"

METALAVA_DIR=$CHECKOUT_ROOT/tools/metalava
gw="$METALAVA_DIR/gradlew -Dorg.gradle.jvmargs=-Xmx24g"

# Use androidx prebuilt since we don't have metalava prebuilts
export ANDROID_HOME="$CHECKOUT_ROOT/prebuilts/fullsdk-linux/"

function buildMetalava() {
  METALAVA_BUILD_LOG="$OUT_DIR/metalava.log"
  if $gw -p $METALAVA_DIR publish --stacktrace --no-daemon > "$METALAVA_BUILD_LOG" 2>&1; then
    echo built metalava successfully
  else
    cat "$METALAVA_BUILD_LOG" >&2
    echo failed to build metalava
    return 1
  fi

}

buildMetalava

# Mac grep doesn't support -P, so use perl version of `grep -oP "(?<=metalavaVersion=).*"`
METALAVA_VERSION_FILE="$METALAVA_DIR/version.properties"
export METALAVA_VERSION=`perl -nle'print $& while m{(?<=metalavaVersion=).*}g' $METALAVA_VERSION_FILE`
if [ -z "$METALAVA_VERSION" ]; then
  echo Failed to retrieve version from $METALAVA_VERSION_FILE >&2
  exit 1
fi
export METALAVA_REPO="$DIST_DIR/repo/m2repository"

function buildAndroidx() {
  ./frameworks/support/busytown/impl/build.sh $androidxArguments \
    --dependency-verification=off # building against tip of tree of metalava that potentially pulls in new dependencies

}

buildAndroidx
echo "Completing $0 at $(date)"
