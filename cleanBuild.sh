#!/bin/bash
set -e
cd $(dirname $0)

echo "IF THIS SCRIPT FIXES YOUR BUILD; OPEN A BUG."
echo "(in nearly all cases, it should not be necessary to run a clean build)."
# one case where it is convenient to have a clean build is for double-checking that a build failure isn't due to an incremental build failure
# another case where it is convenient to have a clean build is for performance testing
# another case where it is convenient to have a clean build is when you're modifying the build and may have introduced some errors but haven't shared your changes yet (at which point you should have fixed the errors)
echo

goals="$@"

function usage() {
  echo "Usage: $0 <tasks>"
  echo "Runs a clean build of <tasks>"
  exit 1
}

if [ "$goals" == "" ]; then
  usage
fi

export OUT_DIR=../../out
function removeCaches() {
  rm .gradle -rf
  rm buildSrc/.gradle -rf
  rm buildSrc/build -rf
  rm -f local.properties
  rm ../../out -rf
}
removeCaches

echo running build
GRADLE_USER_HOME=../../out ./gradlew --no-daemon $goals
