#!/bin/bash
set -e

stateDir="$1"
gradlewDir="$2"
moveArg="$3"

scriptPath="$(cd $(dirname $0) && pwd)"
supportRoot="$(cd $scriptPath/../../.. && pwd)"
checkoutRoot="$(cd $supportRoot/../.. && pwd)"

function usage() {
  echo "usage: $0 <statePath> <gradlew dir>"
  echo "Backs up build state for <gradlew dir> into <statePath>"
  exit 1
}

if [ "$stateDir" == "" ]; then
  usage
fi

if [ "$gradlewDir" == "" ]; then
  usage
fi

move=false
if [ "$moveArg" == "--move" ]; then
  move=true
  shift
fi

if [ "$3" != "" ]; then
  echo "Unrecognized argument $3"
  usage
fi

rm -rf "$stateDir"
mkdir -p "$stateDir"
stateDir="$(cd $stateDir && pwd)"

if [ "$OUT_DIR" == "" ]; then
  OUT_DIR="$checkoutDir/out"
else
  GRADLE_USER_HOME="$OUT_DIR/.gradle"
fi

if [ "$DIST_DIR" == "" ];then
  DIST_DIR="$OUT_DIR/dist"
fi

if [ "$GRADLE_USER_HOME" == "" ]; then
  GRADLE_USER_HOME="$(cd ~ && pwd)/.gradle"
fi

# makes the contents of $2 match the contents of $1
function copy() {
  from="$1"
  to="$2"
  rm "$to" -rf
  if [ -e "$from" ]; then
    mkdir -p "$(dirname $to)"
    if [ "$move" == "true" ]; then
      mv "$from" "$to"
    else
      cp --preserve=all -rT "$from" "$to"
    fi
  else
    rm "$to" -rf
  fi
}

function backupState() {
  backupDir="$1"
  echo "Saving state into $backupDir"
  mkdir -p "$backupDir"

  # back up DIST_DIR if not under OUT_DIR
  if [ "$DIST_DIR" != "$OUT_DIR/dist" ]; then
    copy "$DIST_DIR"                  "$backupDir/dist"
  fi
  # back up GRADLE_USER_HOME if not under OUT_DIR
  if [ "$GRADLE_USER_HOME" != "$OUT_DIR/.gradle" ]; then
    copy "$GRADLE_USER_HOME"          "$backupDir/gradleUserHome"
  fi
  # back up out/
  copy "$OUT_DIR"                     "$backupDir/out"

  # If DIST_DIR is under out/, then move it to where we will find it
  if [ "$DIST_DIR" == "$OUT_DIR/dist" ]; then
    mv "$backupDir/out/dist"          "$backupDir/dist" 2>/dev/null || true
  fi

  # if $GRADLE_USER_HOME is under out/ , then move it to where we will find it
  if [ "$GRADLE_USER_HOME" == "$OUT_DIR/.gradle" ]; then
    mv "$backupDir/out/.gradle" "$backupDir/gradleUserHome" 2>/dev/null || true
  fi

  copy "$gradlewDir/.gradle"          "$backupDir/support/.gradle"
  copy "$gradlewDir/buildSrc/.gradle" "$backupDir/buildSrc/.gradle"
  copy "$gradlewDir/local.properties" "$backupDir/local.properties"
}

backupState $stateDir

