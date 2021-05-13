#!/bin/bash
set -e

scriptName="$(basename $0)"

function usage() {
  echo "NAME"
  echo "  diagnose-build-failure.sh"
  echo
  echo "SYNOPSIS"
  echo "  ./development/diagnose-build-failure/diagnose-build-failure.sh [--message <message>] '<tasks>'"
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
  gradleArgs="$arg"
  break
done
if [ "$gradleArgs" == "" ]; then
  usage
fi

if [ "$#" -gt 0 ]; then
  echo "Unrecognized argument: $1"
  exit 1
fi

workingDir="$(pwd)"
if [ ! -e "$workingDir/gradlew" ]; then
  echo "Error; ./gradlew does not exist. Must cd to a dir containing a ./gradlew first"
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
fi
if [ "$DIST_DIR" != "" ]; then
  mkdir -p "$DIST_DIR"
  DIST_DIR="$(cd $DIST_DIR && pwd)"
fi
COLOR_WHITE="\e[97m"
COLOR_GREEN="\e[32m"

function checkStatusRepo() {
  repo status
}

function checkStatusGit() {
  git status
  git log -1
}

function checkStatus() {
  cd "$checkoutRoot"
  if [ "-e" .repo ]; then
    checkStatusRepo
  else
    checkStatusGit
  fi
}

function getBuildCommand() {
  if [ "$expectedMessage" == "" ]; then
    testCommand="$*"
  else
    testCommand="$* 2>&1 | $vgrep '$expectedMessage'"
  fi
  echo "$testCommand"
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

echo
echo "Making sure that we can reproduce the build failure"
if runBuild ./gradlew $gradleArgs; then
  echo
  echo "This script failed to reproduce the build failure."
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
  echo
  echo "Reproduced build failure"
fi

echo
echo "Stopping the Gradle Daemon and rebuilding"
cd "$supportRoot"
./gradlew --stop || true
if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo
  echo "The build passed when disabling the Gradle Daemon"
  echo "This suggests that there is some state saved in the Gradle Daemon that is causing a failure."
  echo "Unfortunately, this script does not know how to diagnose this further."
  echo "You could ask a team member if they've seen this error."
  exit 1
else
  echo
  echo "The build failed even with the Gradle Daemon disabled."
  echo "This may mean that there is state stored in a file somewhere, triggering the build to fail."
  echo "We will investigate the possibility of saved state next."
  echo
  # We're going to immediately overwrite the user's current state,
  # so we can simply move the current state into $tempDir/prev rather than copying it
  backupState "$tempDir/prev" --move
fi

echo
echo "Checking whether a clean build passes"
clearState
backupState "$tempDir/empty"
successState="$tempDir/empty"
if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo
  echo "The clean build passed, so we can now investigate what cached state is triggering this build to fail."
  backupState "$tempDir/clean"
else
  echo
  echo "The clean build also reproduced the issue."
  echo "This may mean that everyone is observing this issue"
  echo "This may mean that something about your checkout is different from others'"
  echo "You may be interested in running development/simplify-build-failure/simplify-build-failure.sh to identify the minimal set of source files required to reproduce this error"
  echo "Checking the status of your checkout:"
  checkStatus
  exit 1
fi

echo
echo "Checking whether a second build passes when starting from the output of the first clean build"
if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo
  echo "The next build after the clean build passed, so we can use the output of the first clean build as the successful state to compare against"
  successState="$tempDir/clean"
else
  echo
  echo "The next build after the clean build failed."
  echo "Although this is unexpected, we should still be able to diagnose it."
  echo "This might be slower than normal, though, because it may require us to rebuild more things more often"
fi

echo
echo "Next we'll double-check that after restoring the failing state, the build fails"
restoreState "$tempDir/prev"
if runBuild ./gradlew --no-daemon $gradleArgs; then
  echo
  echo "After restoring the saved state, the build passed."
  echo "This might mean that there is additional state being saved somewhere else that this script does not know about"
  echo "This might mean that the success or failure status of the build is dependent on timestamps."
  echo "This might mean that the build is nondeterministic."
  echo "Unfortunately, this script does not know how to diagnose this further."
  echo "You could:"
  echo "  Ask a team member if they know where the state may be stored"
  echo "  Ask a team member if they recognize the build error"
  exit 1
else
  echo
  echo "After restoring the saved state, the build failed. This confirms that this script is successfully saving and restoring the relevant state"
fi

# Now ask diff-filterer.py to run a binary search to determine what the relevant differences are between "$tempDir/prev" and "$tempDir/clean"
echo
echo "Binary-searching the contents of the two output directories until the relevant differences are identified."
echo "This may take a while."
echo
setupCommand="work=\$(pwd)
$scriptPath/impl/restore-state.sh . $workingDir --move && cd $workingDir
"
buildCommand="$(getBuildCommand "./gradlew --no-daemon $gradleArgs")"
cleanupCommand="$scriptPath/impl/backup-state.sh \$work $workingDir --move >/dev/null"
fullFiltererCommand="$setupCommand
if $buildCommand; then
  $cleanupCommand
  exit 0
else
  $cleanupCommand
  exit 1
fi"
if $supportRoot/development/file-utils/diff-filterer.py --assume-input-states-are-correct --work-path $tempDir $successState $tempDir/prev "$fullFiltererCommand"; then
  echo
  echo "There should be something wrong with the above file state"
  echo "Hopefully the output from diff-filterer.py above is enough information for you to figure out what is wrong"
  echo "If not, you could ask a team member about your original error message and see if they have any ideas"
else
  echo
  echo "Something went wrong running diff-filterer.py"
  echo "Maybe that means the build is nondeterministic"
  echo "Maybe that means that there's something wrong with this script ($0)"
fi
