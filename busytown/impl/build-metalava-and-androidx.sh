#!/bin/bash
set -e

echo "Starting $0 at $(date)"

androidxArguments="$*"

WORKING_DIR="$(pwd)"
SCRIPTS_DIR="$(cd $(dirname $0)/.. && pwd)"
cd "$SCRIPTS_DIR/../../.."
echo "Script running from $(pwd)"

# resolve dirs
export OUT_DIR=$(pwd)/out

if [ -z "$DIST_DIR" ]; then
  DIST_DIR="$OUT_DIR/dist"
fi
mkdir -p "$DIST_DIR"

export DIST_DIR="$DIST_DIR"

# resolve GRADLE_USER_HOME
export GRADLE_USER_HOME="$DIST_DIR/gradle"
mkdir -p "$GRADLE_USER_HOME"

if [ "$ROOT_DIR" == "" ]; then
  ROOT_DIR="$WORKING_DIR"
else
  ROOT_DIR="$(cd $ROOT_DIR && pwd)"
fi

METALAVA_DIR=$ROOT_DIR/tools/metalava
gw="$METALAVA_DIR/gradlew -Dorg.gradle.jvmargs=-Xmx24g"

# Use androidx prebuilt since we don't have metalava prebuilts
export ANDROID_HOME="$WORKING_DIR/../../prebuilts/fullsdk-linux/platforms/android-31/android.jar"

function buildMetalava() {
  METALAVA_BUILD_LOG="$OUT_DIR/metalava.log"
  if $gw -p $METALAVA_DIR createArchive --stacktrace --no-daemon > "$METALAVA_BUILD_LOG" 2>&1; then
    echo built metalava successfully
  else
    cat "$METALAVA_BUILD_LOG" >&2
    echo failed to build metalava
    return 1
  fi

}

buildMetalava

# Mac grep doesn't support -P, so use perl version of `grep -oP "(?<=metalavaVersion=).*"`
export METALAVA_VERSION=`perl -nle'print $& while m{(?<=metalavaVersion=).*}g' $METALAVA_DIR/src/main/resources/version.properties`
export METALAVA_REPO="$ROOT_DIR/out/dist/repo/m2repository"
export JAVA_TOOLS_JAR="$(pwd)/prebuilts/jdk/jdk8/$PREBUILT_JDK/lib/tools.jar"

function buildAndroidx() {
  LOG_PROCESSOR="$SCRIPTS_DIR/../development/build_log_processor.sh"
  properties="-Pandroidx.summarizeStderr --no-daemon"
  "$LOG_PROCESSOR" $gw $properties -p frameworks/support $androidxArguments \
    --dependency-verification=off # building against tip of tree of metalava that potentially pulls in new dependencies

}

buildAndroidx
echo "Completing $0 at $(date)"
