#!/bin/bash
set -e

scriptName="$(basename $0)"

function usage() {
  echo "NAME"
  echo "  diagnose-build-failure.sh"
  echo
  echo "SYNOPSIS"
  echo "  ./development/diagnose-build-failure/diagnose-build-failure.sh [--message <message>] [--timeout <seconds> ] '<tasks>'"
  echo
  echo "DESCRIPTION"
  echo "  Attempts to identify why "'`'"./gradlew <tasks>"'`'" fails"
  echo
  echo "OPTIONS"
  echo "--message <message>"
  echo "  Replaces the requirement for "'`'"./gradlew <tasks>"'`'" to fail with the requirement that it produces the given message"
  echo
  echo "SAMPLE USAGE"
  echo "  $0 assembleRelease # or any other arguments you would normally give to ./gradlew"
  echo
  echo "OUTPUT"
  echo "  diagnose-build-failure will conclude one of the following:"
  echo
  echo "  A) Some state saved in memory by the Gradle daemon is triggering an error"
  echo "  B) Your source files have been changed"
  echo "     To (slowly) generate a simpler reproduction case, you can run simplify-build-failure.sh"
  echo "  C) Some file in the out/ dir is triggering an error"
  echo "     If this happens, $scriptName will identify which file(s) specifically"
  echo "  D) The build is nondeterministic and/or affected by timestamps"
  echo "  E) The build via gradlew actually passes"
  exit 1
}

expectedMessage=""
timeoutSeconds=""
grepOptions=""
while true; do
  if [ "$#" -lt 1 ]; then
    usage
  fi
  arg="$1"
  shift
  if [ "$arg" == "--message" ]; then
    expectedMessage="$1"
    shift
    continue
  fi
  if [ "$arg" == "--timeout" ]; then
    timeoutSeconds="$1"
    shift
    continue
  fi

  gradleArgs="$arg"
  break
done
if [ "$gradleArgs" == "" ]; then
  usage
fi
if [ "$timeoutSeconds" == "" ]; then
  timeoutArg=""
else
  timeoutArg="--timeout $timeoutSeconds"
fi
# split Gradle arguments into options and tasks
gradleOptions=""
gradleTasks=""
for arg in $gradleArgs; do
  if [[ "$arg" == "-*" ]]; then
    gradleOptions="$gradleOptions $arg"
  else
    gradleTasks="$gradleTasks $arg"
  fi
done

if [ "$#" -gt 0 ]; then
  echo "Unrecognized argument: $1" >&2
  exit 1
fi

workingDir="$(pwd)"
if [ ! -e "$workingDir/gradlew" ]; then
  echo "Error; ./gradlew does not exist. Must cd to a dir containing a ./gradlew first" >&2
  # so that this script knows which gradlew to use (in frameworks/support or frameworks/support/ui)
  exit 1
fi

# resolve some paths
scriptPath="$(cd $(dirname $0) && pwd)"
vgrep="$scriptPath/impl/vgrep.sh"
supportRoot="$(cd $scriptPath/../.. && pwd)"
checkoutRoot="$(cd $supportRoot/../.. && pwd)"
tempDir="$checkoutRoot/diagnose-build-failure/"
if [ "$OUT_DIR" != "" ]; then
  mkdir -p "$OUT_DIR"
  OUT_DIR="$(cd $OUT_DIR && pwd)"
  EFFECTIVE_OUT_DIR="$OUT_DIR"
else
  EFFECTIVE_OUT_DIR="$checkoutRoot/out"
fi
if [ "$DIST_DIR" != "" ]; then
  mkdir -p "$DIST_DIR"
  DIST_DIR="$(cd $DIST_DIR && pwd)"
  EFFECTIVE_DIST_DIR=$DIST_DIR
else
  # If $DIST_DIR was unset, we leave it unset just in case setting it could affect the build
  # However, we still need to keep track of where the files are going to go, so
  # we set EFFECTIVE_DIST_DIR
  EFFECTIVE_DIST_DIR="$EFFECTIVE_OUT_DIR/dist"
fi
COLOR_WHITE="\e[97m"
COLOR_GREEN="\e[32m"

function checkStatusRepo() {
  repo status >&2
}

function checkStatusGit() {
  git status >&2
  git log -1 >&2
}

function checkStatus() {
  cd "$checkoutRoot"
  if [ "-e" .repo ]; then
    checkStatusRepo
  else
    checkStatusGit
  fi
}

# echos a shell command for running the build in the current directory
function getBuildCommand() {
  if [ "$expectedMessage" == "" ]; then
    testCommand="$* 2>&1"
  else
    testCommand="$* >log 2>&1; $vgrep '$expectedMessage' log $grepOptions"
  fi
  echo "$testCommand"
}

# Echos a shell command for testing the state in the current directory
# Status can be inverted by the '--invert' flag
# The dir of the state being tested is $testDir
# The dir of the source code is $workingDir
function getTestStateCommand() {
  successStatus=0
  failureStatus=1
  if [[ "$1" == "--invert" ]]; then
    successStatus=1
    failureStatus=0
    shift
  fi

  setupCommand="testDir=\$(pwd)
$scriptPath/impl/restore-state.sh . $workingDir --move && cd $workingDir
"
  buildCommand="$*"
  cleanupCommand="$scriptPath/impl/backup-state.sh \$testDir $workingDir --move >/dev/null"

  fullFiltererCommand="$setupCommand
if $buildCommand >/dev/null 2>/dev/null; then
  $cleanupCommand
  exit $successStatus
else
  $cleanupCommand
  exit $failureStatus
fi"

  echo "$fullFiltererCommand"
}

function runBuild() {
  testCommand="$(getBuildCommand $*)"
  cd "$workingDir"
  echo Running $testCommand
  if bash -c "$testCommand"; then
    echo -e "$COLOR_WHITE"
    echo
    echo '`'$testCommand'`' succeeded
    return 0
  else
    echo -e "$COLOR_WHITE"
    echo
    echo '`'$testCommand'`' failed
    return 1
  fi
}

function backupState() {
  cd "$scriptPath"
  backupDir="$1"
  shift
  ./impl/backup-state.sh "$backupDir" "$workingDir" "$@"
}

function restoreState() {
  cd "$scriptPath"
  backupDir="$1"
  ./impl/restore-state.sh "$backupDir" "$workingDir"
}

function clearState() {
  restoreState /dev/null
}

echo >&2
echo "diagnose-build-failure making sure that we can reproduce the build failure" >&2
if runBuild ./gradlew -Pandroidx.summarizeStderr $gradleArgs; then
  echo >&2
  echo "This script failed to reproduce the build failure." >&2
  echo "If the build failure you were observing was in Android Studio, then:"
  echo '  Were you launching Android Studio by running `./studiow`?'
  echo "  Try asking a team member why Android Studio is failing but gradlew is succeeding"
  echo "If you previously observed a build failure, then this means one of:"
  echo "  The state of your build is different than when you started your previous build"
  echo "    You could ask a team member if they've seen this error."
  echo "  The build is nondeterministic"
  echo "    If this seems likely to you, then please open a bug."
  exit 1
else
  echo >&2
  echo "Reproduced build failure" >&2
fi

if [ "$expectedMessage" == "" ]; then
  summaryLog="$EFFECTIVE_DIST_DIR/logs/error_summary.log"
  echo
  echo "No failure message specified. Computing appropriate failure message from $summaryLog"
  echo
  longestLine="$(awk '{ if (length($0) > maxLength) {maxLength = length($0); longestLine = $0} } END { print longestLine }' $summaryLog)"
  echo "Longest line:"
  echo
  echo "$longestLine"
  echo
  grepOptions="-F" # interpret grep query as a fixed string, not a regex
  if grep $grepOptions "$longestLine" "$summaryLog" >/dev/null 2>/dev/null; then
    echo "We will use this as the message to test for"
    echo
    expectedMessage="$longestLine"
  else
    echo "The identified line could not be found in the summary log via grep. Is it possible that diagnose-build-failure did not correctly escape the message?"
    exit 1
  fi
fi

echo
echo "diagnose-build-failure stopping the Gradle Daemon and rebuilding" >&2
cd "$supportRoot"
./gradlew --stop || true
if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo >&2
  echo "The build passed when disabling the Gradle Daemon" >&2
  echo "This suggests that there is some state saved in the Gradle Daemon that is causing a failure." >&2
  echo "Unfortunately, this script does not know how to diagnose this further." >&2
  echo "You could ask a team member if they've seen this error." >&2
  exit 1
else
  echo >&2
  echo "The build failed even with the Gradle Daemon disabled." >&2
  echo "This may mean that there is state stored in a file somewhere, triggering the build to fail." >&2
  echo "We will investigate the possibility of saved state next." >&2
  echo >&2
  # We're going to immediately overwrite the user's current state,
  # so we can simply move the current state into $tempDir/prev rather than copying it
  backupState "$tempDir/prev" --move
fi

echo >&2
echo "Checking whether a clean build passes" >&2
clearState
backupState "$tempDir/empty"
successState="$tempDir/empty"
if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo >&2
  echo "The clean build passed, so we can now investigate what cached state is triggering this build to fail." >&2
  backupState "$tempDir/clean"
else
  echo >&2
  echo "The clean build also reproduced the issue." >&2
  echo "This may mean that everyone is observing this issue" >&2
  echo "This may mean that something about this checkout is different from others'" >&2
  echo "You may be interested in running development/simplify-build-failure/simplify-build-failure.sh to identify the minimal set of source files required to reproduce this error" >&2
  echo "Checking the status of the checkout:" >&2
  checkStatus
  exit 1
fi

echo >&2
echo "Checking whether a second build passes when starting from the output of the first clean build" >&2
if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo >&2
  echo "The next build after the clean build passed, so we can use the output of the first clean build as the successful state to compare against" >&2
  successState="$tempDir/clean"
else
  echo >&2
  echo "The next build after the clean build failed." >&2
  echo "Although this is unexpected, we should still be able to diagnose it." >&2
  echo "This might be slower than normal, though, because it may require us to rebuild more things more often" >&2
fi

echo >&2
echo "Next we'll double-check that after restoring the failing state, the build fails" >&2
restoreState "$tempDir/prev"
if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo >&2
  echo "After restoring the saved state, the build passed." >&2
  echo "This might mean that there is additional state being saved somewhere else that this script does not know about" >&2
  echo "This might mean that the success or failure status of the build is dependent on timestamps." >&2
  echo "This might mean that the build is nondeterministic." >&2
  echo "Unfortunately, this script does not know how to diagnose this further." >&2
  echo "You could:" >&2
  echo "  Ask a team member if they know where the state may be stored" >&2
  echo "  Ask a team member if they recognize the build error" >&2
  exit 1
else
  echo >&2
  echo "After restoring the saved state, the build failed. This confirms that this script is successfully saving and restoring the relevant state" >&2
fi

# Ask diff-filterer.py to run a binary search to determine the minimum set of tasks that must be passed to reproduce this error
# (it's possible that the caller passed more tasks than needed, particularly if the caller is a script)
requiredTasksDir="$tempDir/requiredTasks"
function determineMinimalSetOfRequiredTasks() {
  echo Calculating the list of tasks to run
  allTasksLog="$tempDir/tasks.log"
  restoreState "$successState"
  rm -f "$allTasksLog"
  bash -c "cd $workingDir && ./gradlew --no-daemon --dry-run $gradleArgs > $allTasksLog 2>&1" || true

  # process output and split into files
  taskListFile="$tempDir/tasks.list"
  cat "$allTasksLog" | grep '^:' | sed 's/ .*//' > "$taskListFile"
  requiredTasksWork="$tempDir/requiredTasksWork"
  rm -rf "$requiredTasksWork"
  cp -r "$tempDir/prev" "$requiredTasksWork"
  mkdir -p "$requiredTasksWork/tasks"
  bash -c "cd $requiredTasksWork/tasks && split -l 1 '$taskListFile'"

  rm -rf "$requiredTasksDir"
  # Build the command for passing to diff-filterer.
  # We call xargs because the full set of tasks might be too long for the shell, and xargs will
  # split into multiple gradlew invocations if needed.
  # We also cd into the tasks/ dir before calling 'cat' to avoid reaching its argument length limit.
  # note that the variable "$testDir" gets set by $getTestStateCommand
  buildCommand="$(getBuildCommand "rm -f log && (cd \$testDir/tasks && cat *) | xargs --no-run-if-empty ./gradlew $gradleOptions")"

  # command for moving state, running build, and moving state back
  fullFiltererCommand="$(getTestStateCommand --invert $buildCommand)"

  if $supportRoot/development/file-utils/diff-filterer.py $timeoutArg --work-path "$tempDir" "$requiredTasksWork" "$tempDir/prev"  "$fullFiltererCommand"; then
    echo diff-filterer successfully identified a minimal set of required tasks. Saving into $requiredTasksDir >&2
    cp -r "$tempDir/bestResults/tasks" "$requiredTasksDir"
  else
    echo diff-filterer was unable to identify a minimal set of tasks required to reproduce the error >&2
    exit 1
  fi
}
determineMinimalSetOfRequiredTasks
# update variables
gradleTasks="$(cat $requiredTasksDir/*)"
gradleArgs="$gradleOptions $gradleTasks"

# Now ask diff-filterer.py to run a binary search to determine what the relevant differences are between "$tempDir/prev" and "$tempDir/clean"
echo >&2
echo "Binary-searching the contents of the two output directories until the relevant differences are identified." >&2
echo "This may take a while."
echo >&2

# command for running a build
buildCommand="$(getBuildCommand "./gradlew --no-daemon $gradleArgs")"
# command for moving state, running build, and moving state back
fullFiltererCommand="$(getTestStateCommand $buildCommand)"

if $supportRoot/development/file-utils/diff-filterer.py $timeoutArg --assume-input-states-are-correct --work-path $tempDir $successState $tempDir/prev "$fullFiltererCommand"; then
  echo >&2
  echo "There should be something wrong with the above file state" >&2
  echo "Hopefully the output from diff-filterer.py above is enough information for you to figure out what is wrong" >&2
  echo "If not, you could ask a team member about your original error message and see if they have any ideas" >&2
else
  echo >&2
  echo "Something went wrong running diff-filterer.py" >&2
  echo "Maybe that means the build is nondeterministic" >&2
  echo "Maybe that means that there's something wrong with this script ($0)" >&2
fi
