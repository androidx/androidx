#!/bin/bash
set -e

function usage() {
  echo "Usage: $0 <buildId> <target> [<numBuilds>]"
  echo "Fetches the build number log for <buildId> <target> and reruns the last <numBuilds> of that target that happened on the corresponding server"
  exit 1
}

buildId="$1"
target="$2"
if [ "$buildId" == "" ]; then
  usage
fi
if [ "$target" == "" ]; then
  usage
fi
numBuilds="$3"
if [ "$numBuilds" == "" ]; then
  numBuilds="2"
fi

cd "$(dirname $0)/.."

function cleanup() {
  ./gradlew --stop || true
  rm ../../out -rf
  mkdir -p ../../out
}
cleanup

function echoAndDo() {
  echo "$*"
  eval "$*"
}

function fetch_artifact() {
  fetchBuildId="$1"
  remotePath="$2"
  localDir="$3"
  cd "$localDir"
  echoAndDo /google/data/ro/projects/android/fetch_artifact --bid "$fetchBuildId" --target "$target" "$remotePath"
  cd -
}
function downloadBuildNumberLog() {
  if [ "$target" == "androidx_incremental" ]; then
    buildNumberLog=incremental/build_number.log
  else
    buildNumberLog=build_number.log
  fi
  echo Downloading build number history for build $buildId
  fetch_artifact "$buildId" "$buildNumberLog" ../../out
}
downloadBuildNumberLog

buildNumberHistory="$(cat ../../out/build_number.log | tail -n "$numBuilds")"
echo Will run $target for sources from these builds: $buildNumberHistory .

function checkoutSources() {
  checkoutBuildId="$1"
  echo checking out sources for build $checkoutBuildId

  fetch_artifact "$checkoutBuildId" BUILD_INFO ../../out/
  superprojectRevision="$(jq -r ".parsed_manifest.superproject.revision" ../../out/BUILD_INFO)"

  echo checking out superproject revision $superprojectRevision

  cd ../../
  git fetch origin "$superprojectRevision"
  git checkout "$superprojectRevision"
  git submodule update
  cd -
  echo done checking out sources for build $checkoutBuildId
}
function runBuild() {
  bash "./busytown/$target.sh"
}

for previousBuildId in $buildNumberHistory; do
  checkoutSources $previousBuildId
  if runBuild; then
    echo build of $previousBuildId succeeded
  else
    echo build of $previousBuildId failed
    exit 1
  fi
done
