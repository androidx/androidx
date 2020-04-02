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
  echo "usage: $0 <exploded dir> <source dir>"
  echo "This script concatenates files in <exploded dir> and puts the results in <source dir>"
  exit 1
}

explodedDir="$1"
sourceDir="$2"

if [ "$sourceDir" == "" ]; then
  usage
fi
mkdir -p "$sourceDir"
sourceDir="$(cd $sourceDir && pwd)"

if [ "$explodedDir" == "" ]; then
  usage
fi
mkdir -p "$explodedDir"
explodedDir="$(cd $explodedDir && pwd)"

function joinPath() {
  explodedPath="$1"
  sourcePath="$2"

  mkdir -p "$(dirname $sourcePath)"

  cd $explodedPath
  find -type f | sort | xargs cat > "$sourcePath"
}


function main() {
  rm "$sourceDir" -rf
  mkdir -p "$sourceDir"

  cd $explodedDir
  echo finding everything in $explodedDir
  filePaths="$(find -type f -name file | sed 's|/[^/]*$||' | sort | uniq)"
  echo joining all file paths under $explodedDir into $sourceDir
  for filePath in $filePaths; do
    joinPath "$explodedDir/$filePath" "$sourceDir/$filePath"
  done
  echo done joining all file paths under $explodedDir into $sourceDir
}


main
