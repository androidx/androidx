#!/bin/bash
set -e

buildId="$1"
target="$2"
sendUpToDateScan=false

function usage() {
  echo "usage: $0 <buildId> <target> [--include-up-to-date]"
  echo
  echo "Downloads build scan information for the corresponding build and uploads it to the enterprise server configured in settings.gradle"
  echo
  echo "  --include-up-to-date Also upload scan-up-to-date.zip, the scan of the second build which should be mostly UP-TO-DATE"
  exit 1
}

if [ "$buildId" == "" ]; then
  usage
fi

if [ "$target" == "" ]; then
  usage
fi

if [ "$3" != "" ]; then
  if [ "$3" == "--include-up-to-date" ]; then
    sendUpToDateScan=true
  else
    usage
  fi
fi
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

function downloadScan() {
  filename="$1"
  echo downloading build scan from $buildId $target
  if [ "$target" == "androidx_incremental" ]; then
    downloadPath="incremental/$filename"
  else
    downloadPath="$filename"
  fi
  cd /tmp
  /google/data/ro/projects/android/fetch_artifact --bid $buildId --target $target "$downloadPath"
  cd -
}

function unzipScan() {
  filename="$1"
  echo
  echo unzipping build scan
  rm -rf "$scanDir"
  unzip -q /tmp/"$filename" -d "$scanDir"
}

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

function sendScan() {
  filename="$1"
  downloadScan "$filename"
  unzipScan "$filename"
  uploadScan
}

sendScan scan.zip
echo uploaded scan
if [ "$sendUpToDateScan" == "true" ]; then
  sendScan scan-up-to-date.zip
  echo uploaded scan of second, up-to-date build
fi
