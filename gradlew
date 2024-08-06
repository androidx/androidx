#!/usr/bin/env bash
set -o pipefail
set -e

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# --------- androidx specific code needed for build server. ------------------

SCRIPT_PATH="$(cd $(dirname $0) && pwd -P)"
if [ -n "$OUT_DIR" ] ; then
    mkdir -p "$OUT_DIR"
    OUT_DIR="$(cd $OUT_DIR && pwd -P)"
    export GRADLE_USER_HOME="$OUT_DIR/.gradle"
    export TMPDIR="$OUT_DIR/tmp"
else
    CHECKOUT_ROOT="$(cd $SCRIPT_PATH/../.. && pwd -P)"
    export OUT_DIR="$CHECKOUT_ROOT/out"
    export GRADLE_USER_HOME=~/.gradle
fi

ORG_GRADLE_JVMARGS="$(cd $SCRIPT_PATH && grep org.gradle.jvmargs gradle.properties | sed 's/^/-D/')"
if [ -n "$DIST_DIR" ]; then
    mkdir -p "$DIST_DIR"
    DIST_DIR="$(cd $DIST_DIR && pwd -P)"

    # tell Gradle where to put a heap dump on failure
    ORG_GRADLE_JVMARGS="$(echo $ORG_GRADLE_JVMARGS | sed "s|$| -XX:HeapDumpPath=$DIST_DIR|")"

    # We don't set a default DIST_DIR in an else clause here because Studio doesn't use gradlew
    # and doesn't set DIST_DIR and we want gradlew and Studio to match
fi

# Loading the AIDL lexer requires disabling Lint's bytecode verification
export ANDROID_LINT_SKIP_BYTECODE_VERIFIER=true

# unset ANDROID_BUILD_TOP so that Lint doesn't think we're building the platform itself
unset ANDROID_BUILD_TOP
# ----------------------------------------------------------------------------

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn ( ) {
    echo "$*"
}

die ( ) {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
esac
platform_suffix="x86"
case "$(arch)" in
  arm64* )
    platform_suffix="arm64"
esac
# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# --------- androidx specific code needed for lint and java. ------------------

# Pick the correct fullsdk for this OS.
if [ $darwin == "true" ]; then
    plat="darwin"
else
    plat="linux"
fi

# Tests for lint checks default to using sdk defined by this variable. This removes a lot of
# setup from each lint module.
export ANDROID_HOME="$APP_HOME/../../prebuilts/fullsdk-$plat"
# override JAVA_HOME, because CI machines have it and it points to very old JDK
export ANDROIDX_JDK17="$APP_HOME/../../prebuilts/jdk/jdk17/$plat-$platform_suffix"
export ANDROIDX_JDK21="$APP_HOME/../../prebuilts/jdk/jdk21/$plat-$platform_suffix"
export JAVA_HOME=$ANDROIDX_JDK21
export STUDIO_GRADLE_JDK=$JAVA_HOME

# Warn developers if they try to build top level project without the full checkout
[ ! -d "$JAVA_HOME" ] && echo "Failed to find: $JAVA_HOME

Typically, this means either:
1. You are using the standalone AndroidX checkout, e.g. GitHub, which only supports
   building a subset of projects. See CONTRIBUTING.md for details.
2. You are using the repo checkout, but the last repo sync failed. Use repo status
   to check for projects which are partially-synced, e.g. showing ***NO BRANCH***." && exit -1

# ----------------------------------------------------------------------------

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --unix "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 1 -mindepth 1 -type d 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^($ROOTDIRS))"
    # Add a user-defined pattern to the cygpath arguments
    if [ "$GRADLE_CYGPATTERN" != "" ] ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"|egrep -c "$OURCYGPATTERN" -`
        CHECK2=`echo "$arg"|egrep -c "^-"`                                 ### Determine if an option

        if [ $CHECK -ne 0 ] && [ $CHECK2 -eq 0 ] ; then                    ### Added a condition
            eval `echo args$i`=`cygpath --path --ignore --mixed "$arg"`
        else
            eval `echo args$i`="\"$arg\""
        fi
        i=$((i+1))
    done
    case $i in
        (0) set -- ;;
        (1) set -- "$args0" ;;
        (2) set -- "$args0" "$args1" ;;
        (3) set -- "$args0" "$args1" "$args2" ;;
        (4) set -- "$args0" "$args1" "$args2" "$args3" ;;
        (5) set -- "$args0" "$args1" "$args2" "$args3" "$args4" ;;
        (6) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" ;;
        (7) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" ;;
        (8) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" ;;
        (9) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" "$args8" ;;
    esac
fi

# Split up the JVM_OPTS And GRADLE_OPTS values into an array, following the shell quoting and substitution rules
function splitJvmOpts() {
    JVM_OPTS=("$@")
}
eval splitJvmOpts $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS
JVM_OPTS[${#JVM_OPTS[*]}]="-Dorg.gradle.appname=$APP_BASE_NAME"

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
       --no-watch-fs\
       -Pandroidx.highMemory\
       --profile"
    fi
  fi
  if [ "$compact" == "--strict" ]; then
    expanded="-Pandroidx.validateNoUnrecognizedMessages\
     -Pandroidx.verifyUpToDate"
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

# workaround for https://github.com/gradle/gradle/issues/18386
if [[ " ${@} " =~ " --profile " ]]; then
  mkdir -p reports
fi

raiseMemory=false
if [[ " ${@} " =~ " -Pandroidx.highMemory " ]]; then
    raiseMemory=true
fi
if [[ " ${@} " =~ " -Pandroidx.lowMemory " ]]; then
  if [ "$raiseMemory" == "true" ]; then
    echo "androidx.lowMemory overriding androidx.highMemory"
    echo
  fi
  raiseMemory=false
fi

if [ "$raiseMemory" == "true" ]; then
  # Set the initial heap size to match the max heap size,
  # by replacing a string like "-Xmx1g" with one like "-Xms1g -Xmx1g"
  MAX_MEM=32g
  ORG_GRADLE_JVMARGS="$(echo $ORG_GRADLE_JVMARGS | sed "s/-Xmx\([^ ]*\)/-Xms$MAX_MEM -Xmx$MAX_MEM/")"

  # Increase the compiler cache size: b/260643754 . Remove when updating to JDK 20 ( https://bugs.openjdk.org/browse/JDK-8295724 )
  ORG_GRADLE_JVMARGS="$(echo $ORG_GRADLE_JVMARGS | sed "s|$| -XX:ReservedCodeCacheSize=576M|")"
fi

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

function removeCaches() {
  rm -rf $SCRIPT_PATH/.gradle
  rm -rf $SCRIPT_PATH/buildSrc/.gradle
  rm -f  $SCRIPT_PATH/local.properties
  if [ "$GRADLE_USER_HOME" != "" ]; then
    rm -rf "$GRADLE_USER_HOME"
  else
    rm -rf ~/.gradle
  fi
  # https://github.com/gradle/gradle/issues/18386
  rm -rf $SCRIPT_PATH/reports
  rm -rf $SCRIPT_PATH/build
  rm -rf $OUT_DIR
}

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
  if [ "$processOutput" == "true" ]; then
    wrapper="$SCRIPT_PATH/development/build_log_processor.sh"
  else
    wrapper=""
  fi

  RETURN_VALUE=0
  set -- "$@" -Dorg.gradle.projectcachedir="$OUT_DIR/gradle-project-cache"
  # Disabled in Studio until these errors become shown (b/268380971) or computed more quickly (https://github.com/gradle/gradle/issues/23272)
  if [[ " ${@} " =~ " --dependency-verification=" ]]; then
    VERIFICATION_ARGUMENT="" # already specified by caller
  else
    VERIFICATION_ARGUMENT=--dependency-verification=strict
  fi
  if $wrapper "$JAVACMD" "${JVM_OPTS[@]}" $TMPDIR_ARG -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain $HOME_SYSTEM_PROPERTY_ARGUMENT $TMPDIR_ARG $VERIFICATION_ARGUMENT "$ORG_GRADLE_JVMARGS" "$@"; then
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
