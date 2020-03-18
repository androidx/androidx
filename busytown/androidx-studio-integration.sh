#!/bin/bash
set -e

cd "$(dirname $0)/../../../"
SCRIPT_DIR="$(pwd)"
echo "Script running from $(pwd)"

# resolve DIST_DIR
if [ -z "$DIST_DIR" ]; then
  DIST_DIR="$SCRIPT_DIR/out/dist"
fi
mkdir -p "$DIST_DIR"

export OUT_DIR=out
export DIST_DIR="$DIST_DIR"

JAVA_HOME="$(pwd)/prebuilts/studio/jdk/linux" tools/gradlew -p tools/ publishLocal

export GRADLE_PLUGIN_VERSION=`grep -oP "(?<=buildVersion = ).*" tools/buildSrc/base/version.properties`
export GRADLE_PLUGIN_REPO="$(pwd)/out/repo"
export JAVA_HOME="$PWD/prebuilts/jdk/jdk8/linux-x86/"

tools/gradlew -p frameworks/support --no-daemon bOS
DIST_SUBDIR="/ui" tools/gradlew -p frameworks/support/ui --no-daemon bOS --exclude-task lintDebug
