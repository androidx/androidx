#!/usr/bin/env bash

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# --------- androidx specific code needed for build server. ------------------

SCRIPT_PATH="$(cd $(dirname $0) && pwd)"
if [ -n "$OUT_DIR" ] ; then
    mkdir -p "$OUT_DIR"
    OUT_DIR="$(cd $OUT_DIR && pwd)"
    export GRADLE_USER_HOME="$OUT_DIR/.gradle"
    export TMPDIR=$OUT_DIR
else
    CHECKOUT_ROOT="$(cd $SCRIPT_PATH/../.. && pwd)"
    export OUT_DIR="$CHECKOUT_ROOT/out"
fi

XMX_ARG="$(cd $SCRIPT_PATH && grep org.gradle.jvmargs gradle.properties | sed 's/^/-D/')"
if [ -n "$DIST_DIR" ]; then
    mkdir -p "$DIST_DIR"
    DIST_DIR="$(cd $DIST_DIR && pwd)"
    export LINT_PRINT_STACKTRACE=true

    #Set the initial heap size to match the max heap size,
    #by replacing a string like "-Xmx1g" with one like "-Xms1g -Xmx1g"
    XMX_ARG="$(echo $XMX_ARG | sed 's/-Xmx\([^ ]*\)/-Xms\1 -Xmx\1/')"

    # We don't set a default DIST_DIR in an else clause here because Studio doesn't use gradlew
    # and doesn't set DIST_DIR and we want gradlew and Studio to match
fi

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

function tryToDiagnosePossibleDaemonFailure() {
  # copy daemon logs
  if [ -n "$GRADLE_USER_HOME" ]; then
    if [ -n "$DIST_DIR" ]; then
      cp -r "$GRADLE_USER_HOME/daemon" "$DIST_DIR/gradle-daemon"
      cp ./hs_err* $DIST_DIR/ 2>/dev/null || true
    fi
  fi
}

function runGradle() {
  if "$JAVACMD" "${JVM_OPTS[@]}" $TMPDIR_ARG -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain $HOME_SYSTEM_PROPERTY_ARGUMENT $TMPDIR_ARG "$XMX_ARG" "$@"; then
    return 0
  else
    tryToDiagnosePossibleDaemonFailure
    # Print AndroidX-specific help message if build fails
    # Have to do this build-failure detection in gradlew rather than in build.gradle
    # so that this message still prints even if buildSrc itself fails
    echo
    echo See also development/diagnose-build-failure for help with build failures in this project.
    exit 1
  fi
}

runGradle "$@"
# Check whether we were given the "-PverifyUpToDate" argument
if [[ " ${@} " =~ " -PverifyUpToDate " ]]; then
  # Re-run Gradle, and find all tasks that are unexpectly out of date
  runGradle "$@" -PdisallowExecution --continue --info
fi
