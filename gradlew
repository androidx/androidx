#!/usr/bin/env bash
set -o pipefail
set -e

##############################################################################
##
##  AndroidX Gradle start up script
##
##  Sets up the AndroidX build environment, handles AndroidX-only arguments
##  such as --ci and --strict, and then runs gradle/wrapper/gradlew.
##
##  Gradle generates gradle/wrapper/gradlew and development/update_gradle.sh
##  overwrites it on every Gradle update, so put AndroidX changes here.
##
##############################################################################

SCRIPT_PATH="$(cd $(dirname $0) && pwd -P)"

# Establish build environment variables: OUT_DIR and the directories
# derived from it, prebuilts JDK and SDK, and local.properties.
. "$SCRIPT_PATH/development/build-environment/build-environment.sh"

ORG_GRADLE_JVMARGS="$(cd $SCRIPT_PATH && grep org.gradle.jvmargs gradle.properties | sed 's/^/-D/')"
if [ -n "$DIST_DIR" ]; then
    mkdir -p "$DIST_DIR"
    DIST_DIR="$(cd $DIST_DIR && pwd -P)"

    # tell Gradle where to put a heap dump on failure
    ORG_GRADLE_JVMARGS="$(echo $ORG_GRADLE_JVMARGS | sed "s|$| -XX:HeapDumpPath=$DIST_DIR|")"

    # We don't set a default DIST_DIR in an else clause here because Studio doesn't use gradlew
    # and doesn't set DIST_DIR and we want gradlew and Studio to match
fi

# Some build machines (b/576281048) can only reach the internet over IPv6, but Java tries IPv4
# first, so connecting to the remote build cache times out. These machines may still have an IPv4
# default route that goes nowhere, so test whether IPv4 actually connects instead of checking for
# a route. Prefer IPv6 only if IPv4 fails and IPv6 works: Java won't fall back to IPv4 if IPv6
# fails, which would break machines with broken IPv6.
# The setting only works on the java command line, and Gradle doesn't put -D values from
# org.gradle.jvmargs there, so pass it via a file instead. The Kotlin daemon needs it too.
# TODO(fsladkey): remove once gcp-gradle-build-cache falls back between address families.
preferIpv6=false
if [[ "$(uname)" == Darwin* ]]; then
    probeHost=storage.googleapis.com
    if ! /usr/bin/nc -4 -z -G 2 "$probeHost" 443 >/dev/null 2>&1 &&
            /usr/bin/nc -6 -z -G 2 "$probeHost" 443 >/dev/null 2>&1; then
        preferIpv6=true
    fi
fi
if [ "$preferIpv6" = "true" ]; then
    ORG_GRADLE_JVMARGS="$(echo $ORG_GRADLE_JVMARGS | sed "s|$| @$SCRIPT_PATH/development/build-environment/prefer-ipv6.jvmargs|; s|-Dkotlin.daemon.jvm.options=|&-Djava.net.preferIPv6Addresses=true,|")"
fi

#TODO: Remove HOME_SYSTEM_PROPERTY_ARGUMENT if https://github.com/gradle/gradle/issues/11433 gets fixed
HOME_SYSTEM_PROPERTY_ARGUMENT=""
if [ "$GRADLE_USER_HOME" != "" ]; then
    HOME_SYSTEM_PROPERTY_ARGUMENT="-Duser.home=$GRADLE_USER_HOME"
fi

if [[ " ${@} " =~ " --clean " ]]; then
  cleanCaches=true
else
  cleanCaches=false
fi

if [[ " ${@} " =~ " --no-ci " ]]; then
  disableCi=true
else
  disableCi=false
fi

function setGradleAndKotlinDaemonMemoryLimits() {
  local is_running_in_ci="$1"
  OS_NAME=$(uname -s)
  TOTAL_SYSTEM_MEM_GB=0

  if [[ "${OS_NAME}" == "Linux" ]]; then
    # Get total memory in KB on the machine
    TOTAL_MEM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
    TOTAL_SYSTEM_MEM_GB=$(( TOTAL_MEM_KB / 1024 / 1024 ))
  elif [[ "${OS_NAME}" == "Darwin" ]]; then
    # Get total memory in bytes on macOS using sysctl, then convert to GB
    TOTAL_MEM_BYTES=$(sysctl -n hw.memsize)
    TOTAL_SYSTEM_MEM_GB=$(( TOTAL_MEM_BYTES / 1024 / 1024 / 1024 ))
  fi

  if (( TOTAL_SYSTEM_MEM_GB > 0 )); then
    # calculate 40% of system memory
    CALCULATED_MEM_GB=$(( TOTAL_SYSTEM_MEM_GB * 40 / 100 ))

    # clamp between min 8 GB and max 38 GB
    if (( CALCULATED_MEM_GB < 8 )); then
      GRADLE_MEM_GB=8
    elif (( CALCULATED_MEM_GB > 38 )); then
      GRADLE_MEM_GB=38
    else
      GRADLE_MEM_GB=${CALCULATED_MEM_GB}
    fi

    KOTLIN_MEM_GB=$(( GRADLE_MEM_GB / 2 ))

    if (( KOTLIN_MEM_GB > 16 )); then
      KOTLIN_MEM_GB=16
    fi

    GRADLE_MEMORY_OVERRIDE="${GRADLE_MEM_GB}g"
    KOTLIN_MEMORY_OVERRIDE="${KOTLIN_MEM_GB}g"

    # If we are running in CI, we want to additionally set
    if [[ "$is_running_in_ci" = "true" ]]; then
      # Set the initial heap size to match the max heap size,
      # by replacing a string like "-Xmx1g" with one like "-Xms1g -Xmx1g"
      # First sed command replaces Gradle daemon -Xmx and second replaces Kotlin compiler daemon -Xmx
      ORG_GRADLE_JVMARGS="$(echo "${ORG_GRADLE_JVMARGS}" | sed "s/-Xmx\([^ ]*\)/-Xms${GRADLE_MEMORY_OVERRIDE} -Xmx${GRADLE_MEMORY_OVERRIDE}/" | sed "s/,-Xmx\([^, ]*\)/,-Xms${KOTLIN_MEMORY_OVERRIDE},-Xmx${KOTLIN_MEMORY_OVERRIDE}/")"
    else
      # If we are not running in CI, we want to only set the max heap size for gradle and kotlin daemons.
      ORG_GRADLE_JVMARGS="$(echo "${ORG_GRADLE_JVMARGS}" | sed "s/-Xmx\([^ ]*\)/-Xmx${GRADLE_MEMORY_OVERRIDE}/" | sed "s/,-Xmx\([^, ]*\)/,-Xmx${KOTLIN_MEMORY_OVERRIDE}/")"
    fi
  fi
}

if [[ " ${@} " =~ " --ci " ]]; then
  setGradleAndKotlinDaemonMemoryLimits "true"
else
  setGradleAndKotlinDaemonMemoryLimits "false"
fi

# Expand some arguments
for compact in "--ci" "--strict" "--clean" "--no-ci"; do
  expanded=""
  if [ "$compact" == "--ci" ]; then
    if [ "$disableCi" == "false" ]; then
      expanded="--strict\
       --stacktrace\
       -Pandroidx.summarizeStderr\
       -Pandroidx.enableAffectedModuleDetection\
       -Pandroidx.printTimestamps\
       -Pandroid.experimental.forceDeterministicApk=true\
       --no-watch-fs"
    fi
  fi
  if [ "$compact" == "--strict" ]; then
    if [[ " ${@} " =~ " -Pandroidx.useMaxDepVersions " ]]; then
      expanded="-Pandroidx.validateNoUnrecognizedMessages"
    else
      expanded="-Pandroidx.validateNoUnrecognizedMessages\
       -Pandroidx.verifyUpToDate"
    fi
    if [ "$USE_ANDROIDX_REMOTE_BUILD_CACHE" == "" -o "$USE_ANDROIDX_REMOTE_BUILD_CACHE" == "false" ]; then
      expanded="$expanded --offline"
    fi
  fi
  # if compact is something else then we parsed the argument above but
  # still have to remove it (expanded == "") to avoid confusing Gradle

  # check whether this particular compat argument was passed (and therefore needs expansion)
  if [[ " ${@} " =~ " $compact " ]]; then
    # Expand an individual argument
    # Start by making a copy of our list of arguments and iterating through the copy
    for arg in "$@"; do
      # Remove this argument from our list of arguments.
      # By the time we've completed this loop, we will have removed the original copy of
      # each argument, and potentially re-added a new copy or an expansion of each.
      shift
      # Determine whether to expand this argument
      if [ "$arg" == "$compact" ]; then
        # Add the expansion to our arguments
        set -- "$@" $expanded
        if [ "$expanded" != "" ]; then
          echo "gradlew expanded '$compact' into '$expanded'"
          echo
        fi
        # We avoid re-adding this argument itself back into the list for two reasons:
        # 1. This argument might not be directly understood by Gradle
        # 2. We want to enforce that all behaviors enabled by this flag can be toggled independently,
        # so we don't want it to be easy to inadvertently check for the presence of this flag
        # specifically
      else
        # Add this argument back into our arguments
        set -- "$@" "$arg"
      fi
    done
  fi
done

# check whether the user has requested profiling via yourkit
yourkitArgPrefix="androidx.profile.yourkitAgentPath"
yourkitAgentPath=""
if [[ " ${@}" =~ " -P$yourkitArgPrefix" ]]; then
  for arg in "$@"; do
    if echo "$arg" | grep "${yourkitArgPrefix}=" >/dev/null; then
      yourkitAgentPath="$(echo "$arg" | sed "s/-P${yourkitArgPrefix}=//")"
    fi
  done
  if [ "$yourkitAgentPath" == "" ]; then
    echo "Error: $yourkitArgPrefix must be set to the path of the YourKit Java agent" >&2
    exit 1
  fi
  if [ ! -e "$yourkitAgentPath" ]; then
    echo "Error: $yourkitAgentPath does not exist" >&2
    exit 1
  fi
  # add the agent to the path
  export _JAVA_OPTIONS="$_JAVA_OPTIONS -agentpath:$yourkitAgentPath"
  # add arguments
  set -- "$@" --no-daemon --rerun-tasks

  # lots of blank lines because these messages are important
  echo
  echo
  echo
  echo
  echo
  # suggest --clean
  if [ "$cleanCaches" == "false" ]; then
    echo "When setting $yourkitArgPrefix you may also want to pass --clean"
  fi
  COLOR_YELLOW="\u001B[33m"
  COLOR_CLEAR="\u001B[0m"

  echo -e "${COLOR_YELLOW}Also be sure to start the YourKit user interface and connect to the appropriate Java process (probably the Gradle Daemon)${COLOR_CLEAR}"
  echo
  echo
  echo
  echo
  echo
fi

if [[ " ${@} " =~ " --scan " ]]; then
  if [[ " ${@} " =~ " --offline " ]]; then
    echo "--scan incompatible with --offline"
    echo "you could try --no-ci"
    exit 1
  fi
fi

# Move any preexisting build scan to make room for a new one
# After moving a build scan several times it eventually gets deleted
function rotateBuildScans() {
  filePrefix="$1"
  iPlus1="10"
  for i in $(seq 9 -1 1); do
    mv "${filePrefix}.${i}.zip" "${filePrefix}.${iPlus1}.zip" 2>/dev/null || true
    iPlus1=$i
  done
  mv ${filePrefix}.zip "${filePrefix}.1.zip" 2>/dev/null || true
}

function runGradle() {
  if [ "$TMPDIR" != "" ]; then
    mkdir -p "$TMPDIR"
    TMPDIR_ARG="-Djava.io.tmpdir=$TMPDIR"
  fi

  processOutput=false
  if [[ " ${@} " =~ " -Pandroidx.validateNoUnrecognizedMessages " ]]; then
    processOutput=true
  fi
  if [[ " ${@} " =~ " -Pandroidx.summarizeStderr " ]]; then
    processOutput=true
  fi
  if [[ "${@} " =~ " -Pandroidx.printTimestamps " ]]; then
    processOutput=true
  fi
  # Skip build_log_processor as we are re-running an already successful build but with --dry-run so we can cache it
  if [[ "${@} " =~ " --dry-run" && $ENABLE_PRESUBMIT_COMPATIBLE_CC_STORE == "true" ]]; then
    processOutput=false
  fi

  if [ "$processOutput" == "true" ]; then
    wrapper="$SCRIPT_PATH/development/build_log_processor.sh"
  else
    wrapper=""
  fi

  RETURN_VALUE=0
  set -- "$@" -Dorg.gradle.projectcachedir="$ANDROIDX_PROJECT_CACHE_DIR"
  # Disabled in Studio until these errors become shown (b/268380971) or computed more quickly (https://github.com/gradle/gradle/issues/23272)
  if [[ " ${@} " =~ " --dependency-verification=" ]]; then
    VERIFICATION_ARGUMENT="" # already specified by caller
  else
    VERIFICATION_ARGUMENT=--dependency-verification=strict
  fi
  # The daemon gets its temp dir from $TMPDIR_ARG. Don't pass it in GRADLE_OPTS too: GRADLE_OPTS is
  # exported to the daemon and every process it starts, including IDEs launched by studiow.
  if $wrapper "$SCRIPT_PATH/gradle/wrapper/gradlew" $HOME_SYSTEM_PROPERTY_ARGUMENT $TMPDIR_ARG $VERIFICATION_ARGUMENT "$ORG_GRADLE_JVMARGS" "$@"; then
    RETURN_VALUE=0
  else
    # Print AndroidX-specific help message if build fails
    # Have to do this build-failure detection in gradlew rather than in build.gradle
    # so that this message still prints even if buildSrc itself fails
    echo
    echo For help with unexpected failures, see development/diagnose-build-failure/README.md
    echo
    RETURN_VALUE=1
  fi

  # If the caller specified where to save data, then also save the build scan data
  if [ "$DIST_DIR" != "" ]; then
    if [ "$GRADLE_USER_HOME" != "" ]; then
      scanDir="$GRADLE_USER_HOME/build-scan-data"
      if [ -e "$scanDir" ]; then
        if [[ "$DISALLOW_TASK_EXECUTION" != "" ]]; then
          zipPrefix="$DIST_DIR/scan-up-to-date"
        else
          zipPrefix="$DIST_DIR/scan"
        fi
        rotateBuildScans "$zipPrefix"
        zipPath="${zipPrefix}.zip"
        cd "$GRADLE_USER_HOME/build-scan-data"
        zip -q -r "$zipPath" .
        cd -
      fi
    fi
  fi
  return $RETURN_VALUE
}

if [ "$cleanCaches" == true ]; then
  echo "IF ./gradlew --clean FIXES YOUR BUILD; OPEN A BUG."
  echo "In nearly all cases, it should not be necessary to run a clean build."
  echo
  # one case where it is convenient to have a clean build is for double-checking that a build failure isn't due to an incremental build failure
  # another case where it is convenient to have a clean build is for performance testing
  # another case where it is convenient to have a clean build is when you're modifying the build and may have introduced some errors but haven't shared your changes yet (at which point you should have fixed the errors)

  echo "Stopping Gradle daemons"
  runGradle --stop || true
  echo

  backupDir=~/androidx-build-state-backup
  ./development/diagnose-build-failure/impl/backup-state.sh "$backupDir" --move # prints that it is saving state into this dir"

  echo "To restore this state later, run:"
  echo
  echo "  ./development/diagnose-build-failure/impl/restore-state.sh $backupDir"
  echo
  echo "Running Gradle"
  echo
fi

if [[ "$DISALLOW_TASK_EXECUTION" != "" ]]; then
  echo "Setting 'DISALLOW_TASK_EXECUTION' directly is forbidden. Did you mean -Pandroidx.verifyUpToDate ?"
  echo "See TaskUpToDateValidator.java for more information"
  exit 1
fi

# Unless ALLOW_PUBLIC_REPOS is set, the Gradle daemon gets the agent that stops it from looking up
# host names that aren't on its allowlist, and AndroidXImplPlugin passes it on to host tests.
if [ -z "${ALLOW_PUBLIC_REPOS+set}" ]; then
  ORG_GRADLE_JVMARGS="$ORG_GRADLE_JVMARGS $("$SCRIPT_PATH/development/host-allowlist/javaagent-arg.sh")"
fi

runGradle "$@"
# Check whether we were given the "-Pandroidx.verifyUpToDate" argument
if [[ " ${@} " =~ " -Pandroidx.verifyUpToDate " ]]; then
  # Re-run Gradle, and find all tasks that are unexpectly out of date
  if ! DISALLOW_TASK_EXECUTION=true runGradle "$@" --continue; then
    echo >&2
    echo "TaskUpToDateValidator's second build failed. To reproduce, try running './gradlew -Pandroidx.verifyUpToDate <failing tasks>'" >&2
    exit 1
  fi
fi
