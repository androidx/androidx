#!/bin/bash
set -e

echo "Starting $0 at $(date)"

androidxArguments="$*"

SCRIPTS_DIR="$(cd $(dirname $0)/.. && pwd)"
cd "$SCRIPTS_DIR/../../.."
echo "Script running from $(pwd)"
ANDROIDX_DIR="$(pwd)"

# Resolve JDK folders for host OS
STUDIO_JDK="linux"
PREBUILT_JDK="linux-x86"
if [[ $OSTYPE == darwin* ]]; then
  STUDIO_JDK="mac/Contents/Home"
  PREBUILT_JDK="darwin-x86"
fi

# resolve dirs
export OUT_DIR=$(pwd)/out

if [ -z "$DIST_DIR" ]; then
  DIST_DIR="$OUT_DIR/dist"
fi
mkdir -p "$DIST_DIR"

# resolve GRADLE_USER_HOME
export GRADLE_USER_HOME="$OUT_DIR/gradle"
mkdir -p "$GRADLE_USER_HOME"

if [ "$STUDIO_DIR" == "" ]; then
  STUDIO_DIR="$ANDROIDX_DIR"
else
  STUDIO_DIR="$(cd $STUDIO_DIR && pwd)"
fi

TOOLS_DIR=$STUDIO_DIR/tools
gw="$TOOLS_DIR/gradlew -Dorg.gradle.jvmargs=-Xmx24g"

plat="linux"
case "`uname`" in
  Darwin* )
    plat="darwin"
    ;;
esac

export ANDROID_HOME="$ANDROIDX_DIR/prebuilts/fullsdk-$plat"

function buildStudio() {
  STUDIO_BUILD_LOG="$OUT_DIR/studio.log"
  if JAVA_HOME="$STUDIO_DIR/prebuilts/studio/jdk/jdk17/$STUDIO_JDK" $gw -p $TOOLS_DIR publishLocal --stacktrace --no-daemon > "$STUDIO_BUILD_LOG" 2>&1; then
    echo built studio successfully
  else
    cat "$STUDIO_BUILD_LOG" >&2
    echo failed to build studio
    return 1
  fi

  # stop any remaining Gradle daemons, b/205883835
  JAVA_HOME="$STUDIO_DIR/prebuilts/studio/jdk/jdk17/$STUDIO_JDK" $gw -p $TOOLS_DIR --stop
}

function zipStudio() {
  cd "$STUDIO_DIR/out/"
  zip -qr "$DIST_DIR/tools.zip" repo
  cd -
}

buildStudio
zipStudio

# list java processes to check for any running kotlin daemons, b/201504768
function listJavaProcesses() {
  echo "All java processes:"
  ps -ef | grep /java || true
}
listJavaProcesses

# kill kotlin compile daemons in hopes of addressing memory problems, b/201504768
function killKotlinDaemons() {
  ps -ef | grep -i java.*kotlin-daemon-embeddable.*org.jetbrains.kotlin.daemon.KotlinCompileDaemon | grep -v grep | awk '{print $2}' | xargs --no-run-if-empty kill || true
}
killKotlinDaemons

listJavaProcesses

# Depend on the generated version.properties file, as the version depends on
# the release flag
versionProperties="$STUDIO_DIR/out/build/base/builder-model/build/resources/main/com/android/builder/model/version.properties"
# Mac grep doesn't support -P, so use perl version of `grep -oP "(?<=buildVersion = ).*"`
export GRADLE_PLUGIN_VERSION=$(perl -nle'print $& while m{(?<=buildVersion=).*}g' "$versionProperties")
echo "GRADLE_PLUGIN_VERSION=$GRADLE_PLUGIN_VERSION"
export LINT_VERSION=$(perl -nle'print $& while m{(?<=baseVersion=).*}g' "$versionProperties")
echo "LINT_VERSION=$LINT_VERSION"
export GRADLE_PLUGIN_REPO="$STUDIO_DIR/out/repo:$STUDIO_DIR/prebuilts/tools/common/m2/repository"
if [ "$USE_ANDROIDX_REMOTE_BUILD_CACHE" == "" ]; then
  export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp
fi

$SCRIPTS_DIR/impl/build.sh $androidxArguments --profile --dependency-verification=off -Pandroidx.validateNoUnrecognizedMessages=false
echo "Completing $0 at $(date)"
