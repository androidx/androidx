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

JAVA_HOME="$(pwd)/prebuilts/studio/jdk/linux" tools/gradlew -p tools/ publishLocal --stacktrace

export GRADLE_PLUGIN_VERSION=`grep -oP "(?<=buildVersion = ).*" tools/buildSrc/base/version.properties`
export GRADLE_PLUGIN_REPO="$(pwd)/out/repo:$(pwd)/prebuilts/tools/common/m2/repository"
export JAVA_HOME="$PWD/prebuilts/jdk/jdk11/linux-x86/"
export JAVA_TOOLS_JAR="$PWD/prebuilts/jdk/jdk11/linux-x86/lib/tools.jar"
export LINT_PRINT_STACKTRACE=true

tools/gradlew -p frameworks/support --no-daemon bOS --stacktrace -Pandroidx.allWarningsAsErrors
DIST_SUBDIR="/ui" tools/gradlew -p frameworks/support/ui --no-daemon bOS --stacktrace -Pandroidx.allWarningsAsErrors
