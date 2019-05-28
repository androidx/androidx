#!/bin/bash
set -e

stateDir="$1"

scriptPath="$(cd $(dirname $0) && pwd)"
supportRoot="$(cd $scriptPath/../../.. && pwd)"
checkoutRoot="$(cd $supportRoot/../.. && pwd)"

function usage() {
  echo "usage: $0 <statePath>"
  echo "Restores build state from <statePath> into the places where Gradle will look for it"
  exit 1
}

if [ "$stateDir" == "" ]; then
  usage
fi
if [ "$stateDir" != "/dev/null" ]; then
  stateDir="$(cd $stateDir && pwd)"
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
    cp --preserve=all -rT "$from" "$to"
  fi
}

function restoreState() {
  backupDir="$1"
  echo "Restoring state from $backupDir"
  copy "$backupDir/out"              "$checkoutRoot/out"
  copy "$backupDir/support/.gradle"  "$supportRoot/.gradle"
  copy "$backupDir/buildSrc/.gradle" "$supportRoot/buildSrc/.gradle"
  copy "$backupDir/local.properties" "$supportRoot/local.properties"
  copy "$backupDir/gradleUserHome"   "$GRADLE_USER_HOME"
}

restoreState $stateDir

