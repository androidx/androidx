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
    export TMPDIR=$OUT_DIR
else
    CHECKOUT_ROOT="$(cd $SCRIPT_PATH/../.. && pwd -P)"
    export OUT_DIR="$CHECKOUT_ROOT/out"
fi

ORG_GRADLE_JVMARGS="$(cd $SCRIPT_PATH && grep org.gradle.jvmargs gradle.properties | sed 's/^/-D/')"
if [ -n "$DIST_DIR" ]; then
    mkdir -p "$DIST_DIR"
    DIST_DIR="$(cd $DIST_DIR && pwd -P)"
    export LINT_PRINT_STACKTRACE=true

    #Set the initial heap size to match the max heap size,
    #by replacing a string like "-Xmx1g" with one like "-Xms1g -Xmx1g"
    MAX_MEM=24g
    ORG_GRADLE_JVMARGS="$(echo $ORG_GRADLE_JVMARGS | sed "s/-Xmx\([^ ]*\)/-Xms$MAX_MEM -Xmx$MAX_MEM/")"

    # tell Gradle where to put a heap dump on failure
    ORG_GRADLE_JVMARGS="$(echo $ORG_GRADLE_JVMARGS | sed "s|$| -XX:HeapDumpPath=$DIST_DIR|")"

    # We don't set a default DIST_DIR in an else clause here because Studio doesn't use gradlew
    # and doesn't set DIST_DIR and we want gradlew and Studio to match
fi

# unset ANDROID_BUILD_TOP so that Lint doesn't think we're building the platform itself
unset ANDROID_BUILD_TOP
# ----------------------------------------------------------------------------

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.

JAVA_OPTS="$JAVA_OPTS -Dkotlin.incremental.compilation=true" # b/188565660

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
DEFAULT_JVM_OPTS="-DLINT_API_DATABASE=$APP_HOME/../../prebuilts/fullsdk-$plat/platform-tools/api/api-versions.xml"

# Tests for lint checks default to using sdk defined by this variable. This removes a lot of
# setup from each lint module.
export ANDROID_HOME="$APP_HOME/../../prebuilts/fullsdk-$plat"
# override JAVA_HOME, because CI machines have it and it points to very old JDK
export JAVA_HOME="$APP_HOME/../../prebuilts/jdk/jdk11/$plat-x86"
export JAVA_TOOLS_JAR="$APP_HOME/../../prebuilts/jdk/jdk8/$plat-x86/lib/tools.jar"
export STUDIO_GRADLE_JDK=$JAVA_HOME

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
if [ "$TMPDIR" != "" ]; then
  TMPDIR_ARG="-Djava.io.tmpdir=$TMPDIR"
fi

if [[ " ${@} " =~ " --clean " ]]; then
  cleanCaches=true
else
  cleanCaches=false
fi

# workaround for https://github.com/gradle/gradle/issues/18386
if [[ " ${@} " =~ " --profile " ]]; then
  mkdir -p reports
fi

# Expand some arguments
for compact in "--ci" "--strict" "--clean"; do
  if [ "$compact" == "--ci" ]; then
    expanded="--strict\
     --stacktrace\
     -Pandroidx.summarizeStderr\
     -Pandroidx.enableAffectedModuleDetection\
     --no-watch-fs"
  fi
  if [ "$compact" == "--strict" ]; then
    expanded="-Pandroidx.allWarningsAsErrors\
     -Pandroidx.validateNoUnrecognizedMessages\
     -Pandroidx.verifyUpToDate\
     --no-watch-fs\
     --no-daemon\
     --offline"
  fi
  if [ "$compact" == "--clean" ]; then
    expanded="" # we parsed the argument above but we still have to remove it to avoid confusing Gradle
  fi

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

if [ "$cleanCaches" == true ]; then
  echo "IF ./gradlew --clean FIXES YOUR BUILD; OPEN A BUG."
  echo "In nearly all cases, it should not be necessary to run a clean build."
  echo
  echo "You may be more interested in running:"
  echo
  echo "  ./development/diagnose-build-failure/diagnose-build-failure.sh $*"
  echo
  echo "which attempts to diagnose more details about build failures."
  echo
  echo "Removing caches"
  # one case where it is convenient to have a clean build is for double-checking that a build failure isn't due to an incremental build failure
  # another case where it is convenient to have a clean build is for performance testing
  # another case where it is convenient to have a clean build is when you're modifying the build and may have introduced some errors but haven't shared your changes yet (at which point you should have fixed the errors)
  echo

  removeCaches
fi

function runGradle() {
  processOutput=false
  if [[ " ${@} " =~ " -Pandroidx.validateNoUnrecognizedMessages " ]]; then
    processOutput=true
  fi
  if [[ " ${@} " =~ " -Pandroidx.summarizeStderr " ]]; then
    processOutput=true
  fi
  if [ "$processOutput" == "true" ]; then
    wrapper="$SCRIPT_PATH/development/build_log_processor.sh"
  else
    wrapper=""
  fi

  PROJECT_CACHE_DIR_ARGUMENT="--project-cache-dir $OUT_DIR/gradle-project-cache"
  if $wrapper "$JAVACMD" "${JVM_OPTS[@]}" $TMPDIR_ARG -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain $HOME_SYSTEM_PROPERTY_ARGUMENT $TMPDIR_ARG $PROJECT_CACHE_DIR_ARGUMENT "$ORG_GRADLE_JVMARGS" "$@"; then
    return 0
  else
    # Print AndroidX-specific help message if build fails
    # Have to do this build-failure detection in gradlew rather than in build.gradle
    # so that this message still prints even if buildSrc itself fails
    echo
    echo For help with unexpected failures, see development/diagnose-build-failure/README.md
    echo
    return 1
  fi
}

if [[ " ${@} " =~ " -PdisallowExecution " ]]; then
  echo "Passing '-PdisallowExecution' directly is forbidden. Did you mean -Pandroidx.verifyUpToDate ?"
  echo "See TaskUpToDateValidator.java for more information"
  exit 1
fi

if [[ " ${@} " =~ " -PverifyUpToDate " ]]; then
  echo "-PverifyUpToDate has been renamed to -Pandroidx.verifyUpToDate"
  exit 1
fi

runGradle "$@"
# Check whether we were given the "-Pandroidx.verifyUpToDate" argument
if [[ " ${@} " =~ " -Pandroidx.verifyUpToDate " ]]; then
  # Re-run Gradle, and find all tasks that are unexpectly out of date
  if ! runGradle "$@" -PdisallowExecution --continue; then
    echo >&2
    echo "TaskUpToDateValidator's second build failed, -PdisallowExecution specified" >&2
    exit 1
  fi
fi
