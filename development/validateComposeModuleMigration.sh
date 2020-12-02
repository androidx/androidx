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

# Temporary script to aid migration from androidx.ui to androidx.compose, by validating artifacts
# are identical / compatible during the migration
# Most of the logic here is taken from validateRefactor.sh, and cut down into a less generic script
# This can be removed after b/160233169 is finished

supportRoot="$(cd $(dirname $0)/.. && pwd)"
checkoutRoot="$(cd ${supportRoot}/../.. && pwd)"

function die() {
  echo "$@" >&2
  exit 1
}

function usage() {
  violation="$1"
  die "
  Usage: $0 androidx.ui:ui-material androidx.compose.material:material --build

  Builds artifacts for the current commit, and the previous commit, and diffs the output between
  the old artifact and new artifact, to validate that refactoring a library in Compose has
  expected changes (such as pom file changes). Also unzips and prints the location of the class
  files inside the .aar, to facilitate comparing the classes with a tool such as
  japi-compliance-checker.

  Running without --build will just diff the expected outputs, for cases when you wish to verify
  multiple artifacts in the same commits.
  "
  return 1
}

function echoAndDo() {
  echo "$*"
  eval "$*"
}

function doBuild() {
  echoAndDo ./gradlew createArchive --no-daemon --rerun-tasks --offline
  archiveName="top-of-tree-m2repository-all-0.zip"
  echoAndDo unzip -q "${tempOutPath}/dist/ui/${archiveName}" -d "${tempOutPath}/dist/ui/${archiveName}.unzipped"
}

# Ensure at least 2 args
if (( $# < 2 )); then
    usage
fi

oldLibrary=$1
newLibrary=$2
shouldBuild=false
if [[ $3 == "--build" ]]; then
  shouldBuild=true
fi

echo old library = $oldLibrary
echo new library = $newLibrary
echo building = $shouldBuild

# Replace . and : with / to build the output path from the artifact ID
oldLibraryRelativePath="${oldLibrary//.//}"
oldLibraryRelativePath="${oldLibraryRelativePath//://}"

newLibraryRelativePath="${newLibrary//.//}"
newLibraryRelativePath="${newLibraryRelativePath//://}"

oldOutPath="${checkoutRoot}/out-old"
newOutPath="${checkoutRoot}/out-new"
tempOutPath="${checkoutRoot}/out"

if $shouldBuild ; then
  echo removing out/
  rm -rf "$oldOutPath" "$newOutPath" "$tempOutPath"

  echo building new commit
  doBuild
  mv "$tempOutPath" "$newOutPath"

  echo building previous commit

  echoAndDo git -C "${checkoutRoot}/frameworks/support/ui" checkout "HEAD^"
  doBuild
  echoAndDo git -C "${checkoutRoot}/frameworks/support/ui" checkout -
  mv "$tempOutPath" "$oldOutPath"
fi

m2repositoryRelativePath="dist/ui/top-of-tree-m2repository-all-0.zip.unzipped/m2repository"
oldLibraryOutput="${oldOutPath}/${m2repositoryRelativePath}/${oldLibraryRelativePath}"
newLibraryOutput="${newOutPath}/${m2repositoryRelativePath}/${newLibraryRelativePath}"

echo unzipping aars
find ${oldLibraryOutput} -name "*.aar" -exec sh -c 'unzip -o -d `dirname {}` {}' ';'
find ${newLibraryOutput} -name "*.aar" -exec sh -c 'unzip -o -d `dirname {}` {}' ';'

oldClasses="$(find ${oldLibraryOutput} -name "classes.jar")"
newClasses="$(find ${newLibraryOutput} -name "classes.jar")"

echo
echo old classes.jar
echo ${oldClasses}
echo
echo new classes.jar
echo ${newClasses}
echo
echo example japi-compliance-checker command
echo ./japi-compliance-checker.pl -lib ${newLibrary} ${oldClasses} ${newClasses}

echo
echo diffing .module file
oldModuleFile="$(find ${oldLibraryOutput} -name "*.module")"
newModuleFile="$(find ${newLibraryOutput} -name "*.module")"
diff ${oldModuleFile} ${newModuleFile}

echo
echo diffing .pom file
oldPomFile="$(find ${oldLibraryOutput} -name "*.pom")"
newPomFile="$(find ${newLibraryOutput} -name "*.pom")"
diff ${oldPomFile} ${newPomFile}

echo
echo diffing overall directory
# Ignore checksums and classes.jar file
diff -r -x "*.md5*" -x "*.sha*" -x "classes.jar" ${oldLibraryOutput} ${newLibraryOutput}
