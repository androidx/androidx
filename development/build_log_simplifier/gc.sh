#!/bin/bash
set -e

function usage() {
  echo "Usage: $0 <buildId> [<buildId>...]"
  echo
  echo "Downloads logs from the given build Ids and then garbage collects any messages in"
  echo "messages.ignore that don't match any of the downloaded logs"
  exit 1
}

if [ "$1" == "" ]; then
  usage
fi

# fetch_artifact --bid <buildId> --target <target> <filepath>
function fetch_artifact() {
  fetch_script=/google/data/ro/projects/android/fetch_artifact
  echo $fetch_script "$@"
  $fetch_script "$@"
}

# fetch_log <target>
function fetch_logs() {
  build_id="$1"
  target="$2"
  newDir="${build_id}_${target}"
  mkdir $newDir
  cd $newDir
  # download as many logs as exist
  for i in $(seq 20); do
    if fetch_artifact --bid "$buildId" --target "$target" "logs/gradle.${i}.log"; then
      echo "downloaded log ${i} in build $buildId target $target"
    else
      echo "log ${i} does not exist in build $buildId target ${target}; continuing"
      echo
      break
    fi
  done
  cd ..
}

function setup_temp_dir() {
  tempDir="/tmp/build_log_gc"
  rm -rf "$tempDir"
  mkdir -p "$tempDir"
  echo cd "$tempDir"
  cd "$tempDir"
}

# get some paths
SCRIPT_DIR="$(cd $(dirname $0) && pwd)"
SUPPORT_ROOT="$(cd $SCRIPT_DIR/../.. && pwd)"
BUILD_SCRIPTS_DIR="$SUPPORT_ROOT/busytown"

# find the .sh files that enable build log validation
target_scripts="$(cd $BUILD_SCRIPTS_DIR && find -name "*.sh" -type f | xargs grep -l "impl/build.sh" | sed 's|^./||')"

# find the target names that enable build log validation
targets="$(echo $target_scripts | sed 's/\.sh//g')"

# download log for each target
setup_temp_dir
while [ "$1" != "" ]; do
  buildId="$1"
  for target in $targets; do
    fetch_logs $buildId $target
  done
  shift
done


# process all of the logs
logs="$(echo */*.log)"
echo
echo $SCRIPT_DIR/build_log_simplifier.py --update --gc $logs
$SCRIPT_DIR/build_log_simplifier.py --update --gc $logs
