#!/bin/bash
#
#  Copyright (C) 2020 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

set -e

function usage() {
  echo "usage: $0 <source dir> <exploded dir>"
  echo "This script splits files in <source dir> by line and puts the results in <exploded dir>"
  exit 1
}

scriptDir="$(cd $(dirname $0) && pwd)"
removeLeavesArg=""
if [ "$1" == "--remove-leaves" ]; then
  removeLeavesArg="$1"
  shift
fi
if [ "$1" == "--consolidate-leaves" ]; then
  removeLeavesArg="$1"
  shift
fi
sourceDir="$1"
explodedDir="$2"

if [ "$sourceDir" == "" ]; then
  usage
fi
sourceDir="$(cd $sourceDir && pwd)"

if [ "$explodedDir" == "" ]; then
  usage
fi
mkdir -p "$explodedDir"
explodedDir="$(cd $explodedDir && pwd)"

function explodePath() {
  sourcePath="$1"
  explodedPath="$2"

  mkdir -p "$explodedPath"

  # split $sourcePath into lines, and put each line into a file named 00001, 00002, 00003, ...
  cd "$explodedPath"
  $scriptDir/explode.py $removeLeavesArg "$sourcePath" "$explodedPath"
  touch "$explodedPath/file"
}


function main() {
  rm "$explodedDir" -rf
  mkdir -p "$explodedDir"

  cd $sourceDir
  echo splitting everything in $(pwd) into $explodedDir
  for filePath in $(find -type f); do
    explodePath "$sourceDir/$filePath" "$explodedDir/$filePath"
  done
  echo done splitting everything in $(pwd) into $explodedDir
}


main
