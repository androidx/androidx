#!/bin/bash
set -e

function usage() {
  echo "Usage: $0 [--gc] [--target <target>] <buildId> [<buildId>...]"
  echo
  echo "Downloads logs from the given build Ids, and then"
  echo "Updates the exemptions file (messages.ignore) to include a suppression for each of those messages."
  echo
  echo "  [--gc] Also remove any suppressions that don't match any existent messages"
  echo "  [--target] Only download from logs from <target>, not from all targets"
  exit 1
}

targets=""
gc=false
while true; do
  if [ "$1" == "--gc" ]; then
    gc=true
    shift
    continue
  fi
  if [ "$1" == "--target" ]; then
    shift
    targets="$1"
    shift
    continue
  fi
  break
done

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
  for i in 0 $(seq 20); do
    if [ "$i" == "0" ]; then
      logName="gradle.log"
    else
      logName="gradle.${i}.log"
    fi
    filepath="logs/$logName"
    # incremental build uses a subdirectory
    if [ "$target" == "androidx_incremental" ]; then
      filepath="incremental/$filepath"
    fi
    if fetch_artifact --bid "$buildId" --target "$target" "$filepath"; then
      echo "downloaded log ${i} in build $buildId target $target"
    else
      echo
      echo "$logName does not exist in build $buildId target ${target}; continuing"
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
target_scripts="$(cd $BUILD_SCRIPTS_DIR && find -maxdepth 1 -name "*.sh" -type f | grep -v androidx-studio-integration | sed 's|^./||')"

# find the target names that enable build log validation
if [ "$targets" == "" ]; then
  targets="$(echo $target_scripts | sed 's/\.sh//g')"
fi

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
if [ "$gc" == "true" ]; then
  gcArg="--gc"
else
  gcArg=""
fi
$SCRIPT_DIR/build_log_simplifier.py --update $gcArg $logs
echo
echo succeeded
