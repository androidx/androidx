#!/bin/bash
set -e

buildId="$1"
target="$2"

function usage() {
  echo "usage: $0 <buildId> <target>"
  echo
  echo "Downloads build scan information for the corresponding build and uploads it to the enterprise server configured in settings.gradle"
  exit 1
}

if [ "$buildId" == "" ]; then
  usage
fi

if [ "$target" == "" ]; then
  usage
fi

function downloadScan() {
  echo downloading build scan from $buildId $target
  if [ "$target" == "androidx_incremental" ]; then
    downloadPath="incremental/scan.zip"
  else
    downloadPath="scan.zip"
  fi
  cd /tmp
  /google/data/ro/projects/android/fetch_artifact --bid $buildId --target $target "$downloadPath"
  cd -
}
downloadScan

# find scan dir
if [ "$OUT_DIR" != "" ]; then
  effectiveGradleUserHome="$OUT_DIR/.gradle"
else
  if [ "$GRADLE_USER_HOME" != "" ]; then
    effectiveGradleUserHome="$GRADLE_USER_HOME"
  else
    effectiveGradleUserHome="$HOME/.gradle"
  fi
fi
scanDir="$effectiveGradleUserHome/build-scan-data"

function unzipScan() {
  echo
  echo unzipping build scan
  rm -rf "$scanDir"
  unzip /tmp/scan.zip -d "$scanDir"
}
unzipScan

function uploadScan() {
  log="$scanDir/upload-failure.log"
  rm -f "$log"
  echo
  echo uploading build scan
  ./gradlew buildScanPublishPrevious
  sleep 2
  if cat "$log" 2>/dev/null; then
    echo upload failed
  fi
}
uploadScan
