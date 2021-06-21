#!/bin/bash
set -e

DO_PROMPT=true
if [ "$1" == "-y" ]; then
  DO_PROMPT=false
  shift
fi

goals="$@"

function usage() {
  echo
  echo "Usage: $0 [-y] <tasks>"
  echo "Runs a clean build of <tasks>"
  echo
  echo
  echo "For example:"
  echo
  echo "  $0 assembleRelease # or any other arguments you would normally give to ./gradlew"
  echo
  echo
  echo "-y"
  echo "    Don't prompt the user to confirm that they want to run a clean build"
  exit 1
}

if [ "$goals" == "" ]; then
  usage
fi

if [ ! -e "./gradlew" ]; then
  echo "Error; ./gradlew does not exist. Must cd to a dir containing a ./gradlew first"
  # so that this script knows which gradlew to use (in frameworks/support or frameworks/support/ui)
  exit 1
fi

function confirm() {
  # Confirm whether the user wants to run this script instead of diagnose-build-failure.sh
  # Recall that we already mentioned the existence of diagnose-build-failure.sh above
  echo
  echo "Press <Enter> to run a clean build (./gradlew --clean $goals) or Ctrl-C to cancel"
  if [ "$DO_PROMPT" == "true" ]; then
    read response
  fi
}
confirm

scriptDir="$(cd $(dirname $0) && pwd)"
checkoutDir="$(cd $scriptDir/../.. && pwd)"
export OUT_DIR="$checkoutDir/out"

./gradlew --clean $goals
