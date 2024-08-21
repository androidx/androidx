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

function die() {
  echo "$@" >&2
  exit 1
}

function usage() {
  violation="$1"
  die "
  Usage: $0 <git treeish>
         $0 <path>:<git treeish> <path>:<git treeish>

  Validates that libraries built from the given versions are the same as
  the build outputs built at HEAD. This can be used to validate that a refactor
  did not change the outputs.
  If a git treeish is given with no path, the path is considered to be frameworks/support

  Example: $0 HEAD^
  Example: $0 prebuilts/androidx/external:HEAD^ frameworks/support:work^

    * A git treeish is what you type when you run 'git checkout <git treeish>'
    See also https://git-scm.com/docs/gitglossary#Documentation/gitglossary.txt-aiddeftree-ishatree-ishalsotreeish .

  You can also supply additional arguments that will be passed through to validateRefactorHelper.py, using -P
  For example, the baseline arguments that validateRefactorHelper.py accepts.
  Example: $0 HEAD^ -p agpKmp

  validateRefactor also accepts git treeishes as named arguments using -g
  Example: $0 -g HEAD^ -p agpKmp
  "
  return 1
}

# Fills in a default repository path of "frameworks/support:" for any args that don't specify
# their repository. Given an input of: "work^ prebuilts/androidx/external:HEAD^", should return
# "frameworks/support:work^ prebuilts/androidx/external:HEAD^".
function expandCommitArgs() {
  inputSpecs="$@"
  outputSpecs=""
  for spec in $inputSpecs; do
    if echo "$spec" | grep -v ":" >/dev/null; then
      spec="frameworks/support:$spec"
    fi
    outputSpecs="$outputSpecs $spec"
  done
  echo $outputSpecs
}

# Given a list of paths like "frameworks/support prebuilts/androidx/external",
# runs `git checkout -` in each
function uncheckout() {
  repositoryPaths="$@"
  for repositoryPath in $repositoryPaths; do
    echoAndDo git -C "$checkoutRoot/$repositoryPath" checkout -
  done
}
# Given a list of version specs like "a/b:c d/e:f", returns just the paths: "a/b d/e"
function getParticipatingProjectPaths() {
  specs="$@"
  result=""
  for arg in $specs; do
    echo parsing $arg >&2
    repositoryPath="$(echo $arg | sed 's|\([^:]*\):\([^:]*\)|\1|')"
    otherVersion="$(echo $arg | sed 's|\([^:]*\):\([^:]*\)|\2|')"
    if [ "$otherVersion" != "HEAD" ]; then
      result="$result $repositoryPath"
    fi
  done
  echo $result
}
# Given a list of paths, returns a string containing the currently checked-out version of each
function getCurrentCommits() {
  repositoryPaths="$@"
  result=""
  for repositoryPath in $repositoryPaths; do
    currentVersion="$(cd $checkoutRoot/$repositoryPath && git log -1 --format=%H)"
    result="$result $repositoryPath:$currentVersion"
  done
  echo $result
}
function echoAndDo() {
  echo "$*"
  eval "$*"
}
# Given a list of version specs like "a/b:c d/e:f", checks out the appropriate version in each
# In this example it would be `cd a/b && git checkout e` and `cd e/e && git checkout f`
function checkout() {
  versionSpecs="$1"
  for versionSpec in $versionSpecs; do
    project="$(echo $versionSpec | sed 's|\([^:]*\):\([^:]*\)|\1|')"
    ref="$(echo     $versionSpec | sed 's|\([^:]*\):\([^:]*\)|\2|')"
    echo "checking out $ref in project $project"
    echoAndDo git -C "$checkoutRoot/$project" checkout "$ref"
  done
}
function unzipInPlace() {
  archiveName="$1"
  echoAndDo unzip -q "$archiveName" -d "${archiveName}.unzipped"
}
function doBuild() {
  # build androidx
  echoAndDo ./gradlew createArchive zipDocs --no-daemon --rerun-tasks --offline -Pandroidx.highMemory
  archiveName="top-of-tree-m2repository-all-0.zip"
  unzipInPlace "${tempOutPath}/dist/top-of-tree-m2repository-all-0.zip"
  unzipInPlace "${tempOutPath}/dist/docs-tip-of-tree-0.zip"
  unzipInPlace "${tempOutPath}/dist/docs-public-0.zip"
}

nonNamedArgs=()
oldCommits=()
passThruArgs=()
while [ $OPTIND -le "$#" ]; do
  if getopts ":p:g:" opt; then
    case $opt in
      \? ) usage;;
      g ) oldCommits+="$(expandCommitArgs $OPTARG)";;
      p ) passThruArgs+="$OPTARG";;
    esac
    case $OPTARG in
      -*) usage;;
    esac
  else
    nonNamedArgs+=("${!OPTIND}")
    ((OPTIND++))
  fi
done

oldCommits+="$(expandCommitArgs $nonNamedArgs)"

projectPaths="$(getParticipatingProjectPaths $oldCommits)"
if [ "$oldCommits" == "" ]; then
  usage
fi

newCommits="$(getCurrentCommits $projectPaths)"
cd "$supportRoot"
if [[ $(git update-index --refresh) ]]; then echo "You have local changes; stash or commit them or this script won't work"; exit 1; fi
if [[ $(git diff-index --quiet HEAD) ]]; then echo "You have local changes; stash or commit them or this script won't work"; exit 1; fi
echo old commits: $oldCommits
echo new commits: $newCommits
cd "$supportRoot"
oldOutPath="${checkoutRoot}/out-old"
newOutPath="${checkoutRoot}/out-new"
tempOutPath="${checkoutRoot}/out"

rm -rf "$oldOutPath" "$newOutPath" "$tempOutPath"

echo building new commit
doBuild
mv "$tempOutPath" "$newOutPath"

echo building previous commit

checkout "$oldCommits"
if doBuild; then
  echo previous build succeeded
else
  echo previous build failed
  uncheckout "$projectPaths"
  exit 1
fi
uncheckout "$projectPaths"
mv "$tempOutPath" "$oldOutPath"


echo
echo diffing results
# This script performs the diff, and filters out known issues and non-issues with baselines
python development/validateRefactorHelper.py "$passThruArgs"
echo end of difference
