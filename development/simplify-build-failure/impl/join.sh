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
  echo "usage: $0 <exploded dir> <source path>"
  echo "This script concatenates files in <exploded dir> and puts the results in <source path>"
  exit 1
}

explodedDir="$1"
sourcePath="$2"

if [ "$sourcePath" == "" ]; then
  usage
fi
sourcePath="$(readlink -f $sourcePath)"

if [ "$explodedDir" == "" ]; then
  usage
fi
mkdir -p "$explodedDir"
explodedDir="$(cd $explodedDir && pwd)"

function joinPath() {
  explodedPath="$1"
  sourceFile="$2"

  # replace ending '/.' with nothing
  sourceFile="$(echo $sourceFile | sed 's|/\.$||')"

  # if this file doesn't already exist, regenerate it
  if [ ! -e "$sourceFile" ]; then
    mkdir -p "$(dirname $sourceFile)"
    bash -c "cd $explodedPath && find -type f | sort | xargs cat > $sourceFile"
    chmod u+x "$sourceFile"
  fi
}

function deleteStaleOutputs() {
  if [ -e "$sourcePath" ]; then
    # find everything in explodedDir newer than sourcePath
    cd "$explodedDir"
    changedPaths="$(find -newer "$sourcePath" | sed 's|\([/0-9]*\)$||' | sort | uniq)"
    # for each modified inode in explodedDir, delete the corresponding inode in sourcePath
    # first check for '.' because `rm` refuses to delete '.'
    for filePath in $changedPaths; do
      if [ "$filePath" == "." ]; then
        # the source dir itself is older than the exploded dir, so we have to delete the entire source dir
        rm "$sourcePath" -rf
        return
      fi
    done
    # now we can delete any stale paths
    cd "$sourcePath"
    for filePath in $changedPaths; do
      rm $filePath -rf
    done
  fi
}

function main() {
  # Remove most files and directories under $sourcePath other than build caches (out)
  deleteStaleOutputs
  mkdir -p "$sourcePath"

  # regenerate missing files
  cd "$explodedDir"
  echo joining all file paths under $explodedDir into $sourcePath
  filePaths="$(find -type f -name file | sed 's|/[^/]*$||' | sort | uniq)"
  for filePath in $filePaths; do
    joinPath "$explodedDir/$filePath" "$sourcePath/$filePath"
  done
  for filePath in $(find -type l); do
    cp -PT "$explodedDir/$filePath" "$sourcePath/$filePath"
  done

  # record the timestamp at which we finished
  touch $sourcePath

  # announce that we're done
  echo done joining all file paths under $explodedDir into $sourcePath
}


main
