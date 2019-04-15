#!/bin/bash
set -e

stateDir="$1"

scriptPath="$(cd $(dirname $0) && pwd)"
supportRoot="$(cd $scriptPath/../../.. && pwd)"
checkoutRoot="$(cd $supportRoot/../.. && pwd)"

function usage() {
  echo "usage: $0 <statePath>"
  echo "Backs up build state into <statePath>"
  exit 1
}

if [ "$stateDir" == "" ]; then
  usage
fi

rm -rf "$stateDir"
mkdir -p "$stateDir"
stateDir="$(cd $stateDir && pwd)"

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

function backupState() {
  backupDir="$1"
  echo "Saving state into $backupDir"
  mkdir -p "$backupDir"
  copy "$checkoutRoot/out"             "$backupDir/out"
  copy "$supportRoot/.gradle"          "$backupDir/support/.gradle"
  copy "$supportRoot/buildSrc/.gradle" "$backupDir/buildSrc/.gradle"
  copy "$supportRoot/local.properties" "$backupDir/local.properties"
  copy "$GRADLE_USER_HOME"             "$backupDir/gradleUserHome"
}

backupState $stateDir

