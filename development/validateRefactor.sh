#!/bin/bash
#
#  Copyright (C) 2019 The Android Open Source Project
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

supportRoot="$(cd $(dirname $0)/.. && pwd)"
checkoutRoot="$(cd ${supportRoot}/../.. && pwd)"

function usage() {
  echo "usage: $0 <git treeish>"
  echo
  echo "For example, $0 HEAD^"
  echo
  echo "Validates that libraries built from <git treeish>* are the same as the build outputs at HEAD."
  echo "This can be used to validate that a refactor did not change the outputs."
  echo
  echo "* A git treeish is what you type when you run 'git checkout <git treeish>'"
  echo "  See also https://git-scm.com/docs/gitglossary#Documentation/gitglossary.txt-aiddeftree-ishatree-ishalsotreeish ."
  return 1
}

oldCommit="$1"
if [ "$oldCommit" == "" ]; then
  usage
fi
newCommit="$(git log -1 --format=%H)"

oldOutPath="${checkoutRoot}/out-old"
newOutPath="${checkoutRoot}/out-new"
tempOutPath="${checkoutRoot}/out"

function echoAndDo() {
  echo "$*"
  eval "$*"
}

function doBuild() {
  ./gradlew createArchive
  unzip "${tempOutPath}/dist/top-of-tree-m2repository-all-0.zip" -d "${tempOutPath}/dist/top-of-tree-m2repository-all-0.unzipped"
}

rm -rf "$oldOutPath" "$newOutPath" "$tempOutPath"

echo building new commit
doBuild
mv "$tempOutPath" "$newOutPath"


echo building previous commit
echoAndDo git checkout "$oldCommit"
if doBuild; then
  echo previous build succeeded
else
  echo previous build failed
  git checkout -
  exit 1
fi
git checkout -
mv "$tempOutPath" "$oldOutPath"

echo
echo diffing results
# Don't care about maven-metadata files because they have timestamps in them
# We might care to know whether .sha1 or .md5 files have changed, but changes in those files will always be accompanied by more meaningful changes in other files, so we don't need to show changes in .sha1 or .md5 files
echoAndDo diff -r -x "maven-metadata*" -x "*.sha1" -x "*.md5" "$oldOutPath/dist/top-of-tree-m2repository-all-0.unzipped" "$newOutPath/dist/top-of-tree-m2repository-all-0.unzipped"
echo end of difference
