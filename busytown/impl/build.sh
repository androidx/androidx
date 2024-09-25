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
  if [ "$MANIFEST" == "" ]; then
    export MANIFEST="$DIST_DIR/manifest_${BUILD_NUMBER}.xml"
  fi
fi

# parse arguments
if [ "$1" == "--diagnose" ]; then
  DIAGNOSE=true
  shift
else
  DIAGNOSE=false
fi
if [ "$1" == "--diagnose-timeout" ]; then
  shift
  DIAGNOSE_TIMEOUT_ARG="--timeout $1"
  shift
else
  DIAGNOSE_TIMEOUT_ARG=""
fi

# record the build start time
BUILD_START_MARKER="$OUT_DIR/build.sh.start"
rm -f "$BUILD_START_MARKER"
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
    # Echo the Gradle command formatted for ease of reading.
    echo "Gradle command failed:" >&2
    echo "    $*" >&2
    return 1
  fi
}

BUILD_STATUS=0
# enable remote build cache unless explicitly disabled
if [ "$USE_ANDROIDX_REMOTE_BUILD_CACHE" == "" ]; then
  export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp
fi

# Make sure that our native dependencies are new enough for KMP/konan
# If our existing native libraries are newer, then we don't downgrade them because
# something else (like Bash) might be requiring the newer version.
function areNativeLibsNewEnoughForKonan() {
  if [[ "$(uname)" == Darwin* ]]; then
    # we don't have any Macs having native dependencies too old to build KMP/konan
    true
  elif [[ -f /etc/os-release ]]; then
    . /etc/os-release
    version=${VERSION_ID//./}  # Remove dots for comparison
    if (( version >= 2004 )); then
      true
    else
      # on Ubuntu < 20.04 we check whether we have a sufficiently new GLIBCXX
      gcc --print-file-name=libstdc++.so.6 | xargs readelf -a -W | grep GLIBCXX_3.4.21 >/dev/null
    fi
  else
    true
  fi
}

if ! areNativeLibsNewEnoughForKonan; then
  KONAN_HOST_LIBS="$OUT_DIR/konan-host-libs"
  LOG="$KONAN_HOST_LIBS.log"
  if $SCRIPT_DIR/prepare-linux-sysroot.sh "$KONAN_HOST_LIBS" > $LOG 2>$LOG; then
    export LD_LIBRARY_PATH=$KONAN_HOST_LIBS
  else
    cat $LOG >&2
    exit 1
  fi
fi

# list kotlin sessions in case there are several, b/279739438
function checkForLeftoverKotlinSessions() {
  KOTLIN_SESSIONS_DIR=$OUT_DIR/gradle-project-cache/kotlin/sessions
  NUM_KOTLIN_SESSIONS="$(ls $KOTLIN_SESSIONS_DIR 2>/dev/null | wc -l)"
  if [ "$NUM_KOTLIN_SESSIONS" -gt 0 ]; then
    echo "Found $NUM_KOTLIN_SESSIONS leftover kotlin sessions in $KOTLIN_SESSIONS_DIR"
  fi
}
checkForLeftoverKotlinSessions

# list java processes to check for any running kotlin daemons, b/282228230
function listJavaProcesses() {
  echo "All java processes:"
  ps -ef | grep /java || true
}
listJavaProcesses

# launch a process to monitor for timeouts
busytown/impl/monitor.sh 3600 busytown/impl/showJavaStacks.sh &

# run the build
if run ./gradlew --ci "$@"; then
  echo build passed
else
  if [ "$DIAGNOSE" == "true" ]; then
    # see if diagnose-build-failure.sh can identify the root cauase
    echo "running diagnose-build-failure.sh, see build.log" >&2
    # Specify a short timeout in case we're running on a remote server, so we don't take too long.
    # We probably won't have enough time to fully diagnose the problem given this timeout, but
    # we might be able to determine whether this problem is reproducible enough for a developer to
    # more easily investigate further
    ./development/diagnose-build-failure/diagnose-build-failure.sh $DIAGNOSE_TIMEOUT_ARG "--ci $*" || true
    scansPrevDir="$DIST_DIR/scans-prev"
    mkdir -p "$scansPrevDir"
    # restore any prior build scans into the dist dir
    cp ../../diagnose-build-failure/prev/dist/scan*.zip "$scansPrevDir/" || true
  fi
  BUILD_STATUS=1 # failure
fi

# check that no unexpected modifications were made to the source repository, such as new cache directories
DIST_DIR=$DIST_DIR $SCRIPT_DIR/verify_no_caches_in_source_repo.sh $BUILD_START_MARKER

# copy configuration cache reports to DIST_DIR so we can see them b/250893051
CONFIGURATION_CACHE_REPORTS_EXPORTED=$DIST_DIR/configuration-cache-reports
CONFIGURATION_CACHE_REPORTS=$OUT_DIR/androidx/build/reports/configuration-cache
if [ -d "$CONFIGURATION_CACHE_REPORTS" ]; then
    rm -rf "$CONFIGURATION_CACHE_REPORTS_EXPORTED"
    cp -r "$CONFIGURATION_CACHE_REPORTS" "$CONFIGURATION_CACHE_REPORTS_EXPORTED"
fi

exit "$BUILD_STATUS"
