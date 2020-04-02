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
  echo 'NAME'
  echo '  simplify-build-failure.sh'
  echo
  echo 'SYNOPSIS'
  echo "  $0 (--task <gradle task> <error message> | --command <shell command> ) [--continue] [--limit-to-path <file path>] [--check-lines-in <subfile path>]"
  echo
  echo DESCRIPTION
  echo '  Searches for a minimal set of files and/or lines required to reproduce a given build failure'
  echo
  echo OPTIONS
  echo
  echo '  --task <gradle task> <error message>`'
  echo '    Specifies that `./gradlew <gradle task>` must fail with error message <error message>'
  echo
  echo '  --command <shell command>'
  echo '    Specifies that <shell command> must succeed.'
  echo
  echo '  --continue'
  echo '    Attempts to pick up from a previous invocation of simplify-build-failure.sh'
  echo
  echo '  --limit-to-path <limitPath>'
  echo '    Will check only <limitPath> (plus subdirectories, if present) for possible simplications. This can make the simplification process faster if there are paths that you know are'
  echo '    uninteresting to you'
  echo
  echo '  --check-lines-in <subfile path>'
  echo '    Specifies that individual lines in files in <subfile path> will be considered for removal, too'
  exit 1
}

function notify() {
  echo simplify-build-failure.sh $1
  notify-send simplify-build-failure.sh $1
}

function failed() {
  notify failed
  exit 1
}

gradleTasks=""
errorMessage=""
gradleCommand=""
grepCommand=""
testCommand=""
resume=false
subfilePath=""
limitToPath=""

export ALLOW_MISSING_PROJECTS=true # so that if we delete entire projects then the AndroidX build doesn't think we made a spelling mistake

while [ "$1" != "" ]; do
  arg="$1"
  shift
  if [ "$arg" == "--continue" ]; then
    resume=true
    continue
  fi
  if [ "$arg" == "--task" ]; then
    gradleTasks="$1"
    shift
    errorMessage="$1"
    shift
    if [ "$gradleTasks" == "" ]; then
      usage
    fi

    if [ "$errorMessage" == "" ]; then
      usage
    fi

    gradleCommand="OUT_DIR=out ./gradlew $gradleTasks > log 2>&1"
    grepCommand="grep \"$errorMessage\" log"
    # Sleep in case Gradle fails very quickly
    # We don't want to run too many Gradle commands in a row or else the daemons might get confused
    testCommand="$gradleCommand; sleep 2; $grepCommand"
    continue
  fi
  if [ "$arg" == "--command" ]; then
    testCommand="$1"
    shift
    gradleCommand=""
    grepCommand=""
    if [ "$testCommand" == "" ]; then
      usage
    fi
    if echo "$testCommand" | grep -v OUT_DIR 2>/dev/null; then
      echo "Error: must set OUT_DIR in the test command to prevent concurrent Gradle executions from interfering with each other"
      exit 1
    fi
    continue
  fi
  if [ "$arg" == "--check-lines-in" ]; then
    subfilePath="$1"
    shift
    continue
  fi
  if [ "$arg" == "--limit-to-path" ]; then
    limitToPath="$1"
    shift
    continue
  fi
  echo "Unrecognized argument '$arg'"
  usage
done

if [ "$testCommand" == "" ]; then
  usage
fi

cd "$(dirname $0)"
scriptPath="$(pwd)"
cd ../..
supportRoot="$(pwd)"
checkoutRoot="$(cd $supportRoot/../.. && pwd)"

tempDir="$checkoutRoot/simplify-tmp"
if [ "$resume" == "true" ]; then
  if [ -d "$tempDir" ]; then
    echo "Not deleting temp dir $tempDir"
  fi
else
  echo "Removing temp dir $tempDir"
  rm "$tempDir" -rf
fi
referencePassingDir="$tempDir/base"
referenceFailingDir="$tempDir/failing"

rm "$referencePassingDir" -rf
if [ "$limitToPath" != "" ]; then
  mkdir -p "$(dirname $referencePassingDir)"
  cp -r "$supportRoot" "$referencePassingDir"
  rm "$referencePassingDir/$limitToPath" -rf
else
  mkdir -p "$referencePassingDir"
fi

if [ "$subfilePath" != "" ]; then
  if [ ! -e "$subfilePath" ]; then
    echo "$subfilePath" does not exist
    exit 1
  fi
fi

filtererStep1Work="$tempDir"
filtererStep1Output="$filtererStep1Work/bestResults"
fewestFilesOutputPath="$tempDir/fewestFiles"
if echo "$resume" | grep "true" >/dev/null && stat "$fewestFilesOutputPath" >/dev/null 2>/dev/null; then
  echo "Skipping first execution of diff-filterer, $fewestFilesOutputPath already exists"
else
  if [ "$resume" == "true" ]; then
    if stat "$filtererStep1Output" >/dev/null 2>/dev/null; then
      echo "Reusing $filtererStep1Output to resume first execution of diff-filterer"
      # Copy the previous results to resume from
      rm "$referenceFailingDir" -rf
      cp -rT "$filtererStep1Output" "$referenceFailingDir"
    else
      echo "Cannot resume previous execution; neither $fewestFilesOutputPath nor $filtererStep1Output exists"
      exit 1
    fi
  else
    # make a backup of the code so that the user can still make modifications to the source tree without interfering with diff-filterer.py
    rm "$referenceFailingDir" -rf
    cp -rT . "$referenceFailingDir"
    # remove some unhelpful settings
    sed -i 's/.*Werror.*//' "$referenceFailingDir/buildSrc/build.gradle"
  fi
  echo Running diff-filterer.py once to identify the minimal set of files needed to reproduce the error
  if ./development/file-utils/diff-filterer.py --assume-no-side-effects --work-path $filtererStep1Work --num-jobs 4 "$referenceFailingDir" "$referencePassingDir" "$testCommand"; then
    echo diff-filterer completed successfully
  else
    failed
  fi
fi


if [ "$subfilePath" == "" ]; then
  echo Splitting files into individual lines was not enabled. Done. See results at $filtererStep1Work/bestResults
else
  if [ "$subfilePath" == "." ]; then
    subfilePath=""
  fi
  if echo "$resume" | grep true >/dev/null && stat $fewestFilesOutputPath >/dev/null 2>/dev/null; then
    echo "Skipping recopying $filtererStep1Output to $fewestFilesOutputPath"
  else
    echo Copying minimal set of files into $fewestFilesOutputPath
    rm -rf "$fewestFilesOutputPath"
    cp -rT "$filtererStep1Output" "$fewestFilesOutputPath"
  fi

  echo Creating working directory for identifying individually smallest files
  noFunctionBodies_Passing="$tempDir/noFunctionBodies_Passing"
  noFunctionBodies_goal="$tempDir/noFunctionBodies_goal"
  noFunctionBodies_work="work"
  noFunctionBodies_sandbox="$noFunctionBodies_work/$subfilePath"
  noFunctionBodies_output="$tempDir/noFunctionBodies_output"

  # set up command for running diff-filterer against diffs within files
  filtererOptions="--num-jobs 4"
  if echo $subfilePath | grep -v buildSrc >/dev/null 2>/dev/null; then
    # If we're not making changes in buildSrc, then we want to keep the gradle caches around for more speed
    # If we are making changes in buildSrc, then Gradle doesn't necessarily do up-to-date checks correctly, and we want to clear the caches between builds
    filtererOptions="$filtererOptions --assume-no-side-effects"
  else
    if [ "$grepCommand" != "" ]; then
      filtererOptions="$filtererOptions --assume-no-side-effects"
      # If we're making changes in buildSrc, then we want to make sure that a clean build passes because Gradle doesn't always do up-to-date checks correctly when we're making strange changes in buildSrc
      # However, the build runs much more quickly when incremental than when clean
      # So, we first run an incremental build and then if it passes we run a clean build
      testCommand="$gradleCommand; $grepCommand && rm log out -rf && $gradleCommand --no-daemon; $grepCommand"
    fi
  fi

  if echo "$resume" | grep true >/dev/null && stat "$noFunctionBodies_output" >/dev/null 2>/dev/null; then
    echo "Skipping asking diff-filterer to remove function bodies because $noFunctionBodies_output already exists"
  else
    echo Splitting files into smaller pieces
    rm -rf "$noFunctionBodies_Passing" "$noFunctionBodies_goal"
    mkdir -p "$noFunctionBodies_Passing" "$noFunctionBodies_goal"
    cd "$noFunctionBodies_Passing"
    cp -rT "$fewestFilesOutputPath" "$noFunctionBodies_work"
    cp -rT "$noFunctionBodies_Passing" "$noFunctionBodies_goal"

    splitsPath="${subfilePath}.split"
    "${scriptPath}/impl/split.sh" --consolidate-leaves "$noFunctionBodies_sandbox" "$splitsPath"
    rm "$noFunctionBodies_sandbox" -rf

    echo Removing deepest lines
    cd "$noFunctionBodies_goal"
    "${scriptPath}/impl/split.sh" --remove-leaves "$noFunctionBodies_sandbox" "$splitsPath"
    rm "$noFunctionBodies_sandbox" -rf

    # TODO: maybe we should make diff-filterer.py directly support checking individual line differences within files rather than first running split.sh and asking diff-filterer.py to run join.sh
    # It would be harder to implement in diff-filterer.py though because diff-filterer.py would also need to support comparing against nonempty files too
    echo Running diff-filterer.py again to identify which function bodies can be removed
    if "$supportRoot/development/file-utils/diff-filterer.py" --assume-input-states-are-correct $filtererOptions --work-path "$(cd $supportRoot/../.. && pwd)" "$noFunctionBodies_Passing" "$noFunctionBodies_goal" "${scriptPath}/impl/join.sh ${splitsPath} ${noFunctionBodies_sandbox} && cd ${noFunctionBodies_work} && $testCommand"; then
      echo diff-filterer completed successfully
    else
      failed
    fi

    echo Re-joining the files
    rm -rf "${noFunctionBodies_output}"
    cp -rT "$(cd $supportRoot/../../bestResults && pwd)" "${noFunctionBodies_output}"
    cd "${noFunctionBodies_output}"
    "${scriptPath}/impl/join.sh" "${splitsPath}" "${noFunctionBodies_sandbox}"
  fi

  # prepare for another invocation of diff-filterer, to remove other code that is now unused
  smallestFilesInput="$tempDir/smallestFilesInput"
  smallestFilesGoal="$tempDir/smallestFilesGoal"
  smallestFilesWork="work"
  smallestFilesSandbox="$smallestFilesWork/$subfilePath"

  rm -rf "$smallestFilesInput" "$smallestFilesGoal"
  mkdir -p "$smallestFilesInput"
  cp -rT "${noFunctionBodies_output}" "$smallestFilesInput"

  echo Splitting files into individual lines
  cd "$smallestFilesInput"
  splitsPath="${subfilePath}.split"
  "${scriptPath}/impl/split.sh" "$smallestFilesSandbox" "$splitsPath"
  rm "$smallestFilesSandbox" -rf

  # Make a dir holding the destination file state
  if [ "$limitToPath" != "" ]; then
    # The user said they were only interested in trying to delete files under a certain path
    # So, our target state is the original state minus that path (and its descendants)
    mkdir -p "$smallestFilesGoal"
    cp -rT "$smallestFilesInput/$smallestFilesWork" "$smallestFilesGoal/$smallestFilesWork"
    cd "$smallestFilesGoal/$smallestFilesWork"
    rm "$limitToPath" -rf
    cd -
  else
    # The user didn't request to limit the search to a specific path, so we try to delete as many
    # files as possible
    mkdir -p "$smallestFilesGoal"
  fi

  echo Running diff-filterer.py again to identify the minimal set of lines needed to reproduce the error
  if "$supportRoot/development/file-utils/diff-filterer.py" $filtererOptions --work-path "$(cd $supportRoot/../.. && pwd)" "$smallestFilesInput" "$smallestFilesGoal" "${scriptPath}/impl/join.sh ${splitsPath} ${smallestFilesSandbox} && cd ${smallestFilesWork} && $testCommand"; then
    echo diff-filterer completed successfully
  else
    failed
  fi

  echo Re-joining the files
  smallestFilesOutput="$tempDir/smallestFilesOutput"
  rm -rf "$smallestFilesOutput"
  cp -rT "$(cd $supportRoot/../../bestResults && pwd)" "${smallestFilesOutput}"
  cd "${smallestFilesOutput}"
  "${scriptPath}/impl/join.sh" "${splitsPath}" "${smallestFilesSandbox}"

  echo "Done. See simplest discovered reproduction test case at ${smallestFilesOutput}"
fi
notify succeeded
