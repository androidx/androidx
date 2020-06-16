#!/bin/bash
set -e

usage() {
  echo "usage: $0 <command> <arguments>"
  echo
  echo "executes <command> <arguments> and then runs build_log_simplifier.py against its output"
  exit 1
}

if [[ "$1" == "" ]]; then
  usage
fi

# run Gradle and save stdout and stderr into $logFile
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"
if [ -n "$DIST_DIR" ]; then
  LOG_DIR="$DIST_DIR"
else
  LOG_DIR="$SCRIPT_PATH/../../../out/dist"
fi

mkdir -p "$LOG_DIR"
logFile="$LOG_DIR/gradle.log"
rm -f "$logFile"
echo "Running $@"
if bash -c "$*" > >(tee -a "$logFile") 2> >(tee -a "$logFile" >&2); then
  echo "Succeeded: $*"
else
  echo >&2
  echo "Failed: $*" >&2
  echo Attempting to locate the relevant error messages via build_log_simplifier.py >&2
  echo >&2
  # Try to identify the most relevant lines of output, and put them at the bottom of the
  # output where they will also be placed into the build failure email.
  # TODO: We may be able to stop cleaning up Gradle's output after Gradle can do this on its own:
  # https://github.com/gradle/gradle/issues/1005
  # and https://github.com/gradle/gradle/issues/13090
  summaryLog="$LOG_DIR/error_summary.log"
  $SCRIPT_PATH/build_log_simplifier.py $logFile | tail -n 100 | tee "$summaryLog" >&2
  exit 1
fi
