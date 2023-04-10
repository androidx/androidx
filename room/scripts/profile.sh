#! /bin/bash
set -eu
# Script to run compilation profile and build a flame graph for it.

function usage {
    echo "usage: ./profile.sh <gradle tasks>
    ./profile.sh --name my_run \
        --target gradle \
        :room:integration-tests:room-testapp-kotlin:kspWithKspGenJavaDebugAndroidTestKotlin \
        --filter *K2JVMCompiler*
    ./profile.sh --name my_run \
        --target test \
        :room:room-compiler-processing:test \
        --tests RawTypeCreationScenarioTest


    Arguments:
    -h: print usage
    --name <report name>: Name of the report (used in output file if output file is not set)
    --filter <stacktrace include filter>: Regex to filter stacktraces. e.g. *room*
    --outputFile <file path>: Path to the output file
    --preset [kapt|ksp]: Predefined tasks to run ksp/kapt profile on the KotlinTestApp
    --target [gradle|test]: The target process type to profile. Gradle for compilation,
                            test for profile test tasks. Defaults to gradle

    It will run the task once without profiling, stop gradle daemon and re-run it again while also
    disabling up-to-date checks for the given tasks.

    You also need to have ASYNC_PROFILER_PATH environment variable set to an installation of
    Async Profiler (https://github.com/jvm-profiling-tools/async-profiler)
    "
    exit 1
}

WORKING_DIR=`pwd`
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
OUTPUT_DIR="$SCRIPT_DIR/outputs"
PROJECT_DIR="$SCRIPT_DIR/.."

GRADLEW="$SCRIPT_DIR/../gradlew" # using playground file

if  [ -z ${ASYNC_PROFILER_PATH+x} ]; then # +x here to workaround unbound variable check
    echo "invalid agent path, make sure you have ASYNC_PROFILER_PATH environment variable set to AsyncProfiler installation root"
    usage
fi

AGENT_PATH="$ASYNC_PROFILER_PATH/build/libasyncProfiler.so"
if test ! -f $AGENT_PATH; then
    echo "invalid agent path, make sure you have ASYNC_PROFILER_PATH environment variable set to AsyncProfiler installation root"
    usage
fi

REPORT_NAME="profile"
GRADLE_ARGS=""
ADDITIONAL_AGENT_PARAMS=""
AGENT_TARGET="gradle"

while [ $# -gt 0 ]; do
    case $1 in
        -h|"-?")
            usage
            ;;
        --name)
            REPORT_NAME=$2
            shift
            ;;
        --filter)
            ADDITIONAL_AGENT_PARAMS="$ADDITIONAL_AGENT_PARAMS,include=$2"
            shift
            ;;
        --outputFile)
            OUTPUT_FILE=$2
            shift
            ;;
        --target)
            if [ "$2" = "gradle" ]; then
                AGENT_TARGET="gadle"
            elif [ "$2" = "test" ]; then
                AGENT_TARGET="test"
                # add RealProfileScope to tracing automatically. Otherwise, it will be dominated
                # by compilation.
                ADDITIONAL_AGENT_PARAMS="$ADDITIONAL_AGENT_PARAMS,include=*RealProfileScope*"
            else
                echo "invalid target: $2"
                exit
            fi
            shift
            ;;
        --preset)
            if [ "$2" = "kapt" ]; then
                GRADLE_ARGS=":room:integration-tests:room-testapp-kotlin:kaptGenerateStubsWithKaptDebugAndroidTestKotlin \
                    :room:integration-tests:room-testapp-kotlin:kaptWithKaptDebugAndroidTestKotlin"
                ADDITIONAL_AGENT_PARAMS="$ADDITIONAL_AGENT_PARAMS,include=*AbstractKapt3Extension*"
                AGENT_TARGET="gradle"
            elif [ "$2" = "ksp" ]; then
                GRADLE_ARGS=":room:integration-tests:room-testapp-kotlin:kspWithKspGenJavaDebugAndroidTestKotlin"
                ADDITIONAL_AGENT_PARAMS="$ADDITIONAL_AGENT_PARAMS,include=*AbstractKotlinSymbolProcessingExtension*"
                AGENT_TARGET="gradle"
            else
                echo "invalid preset: $2"
                exit
            fi
            shift
            ;;
        *)
            GRADLE_ARGS="${GRADLE_ARGS} $1"
    esac
    shift
done

#sets an ouytput file if none defined
function setOutputFile {
    local number=0
    OUTPUT_FILE="${OUTPUT_DIR}/$1_$number.html"
    while [ -e "$OUTPUT_FILE" ]; do
        OUTPUT_FILE="${OUTPUT_DIR}/${1}_$((++number)).html"
    done
}

function createParentDirectoryForOutput {
    PARENT_DIR="$(dirname "$OUTPUT_FILE")"
    $(mkdir -p $PARENT_DIR)
}

if  [ -z ${OUTPUT_FILE+x} ]; then # +x here to workaround unbound variable check
    setOutputFile $REPORT_NAME
fi
createParentDirectoryForOutput
echo "gradle args: $GRADLE_ARGS"
echo "agent params: $ADDITIONAL_AGENT_PARAMS"
echo "output file: $OUTPUT_FILE"


set -x
function profile {
    local TASK=$1

    local AGENT_PARAMS="file=${OUTPUT_FILE}${ADDITIONAL_AGENT_PARAMS}"
    $GRADLEW -p $PROJECT_DIR $GRADLE_ARGS
    local AGENT_PARAMETER_NAME="-Dorg.gradle.jvmargs"
    if [ $AGENT_TARGET = "test" ]; then
        AGENT_PARAMETER_NAME="-Dandroidx.room.testJvmArgs"
    fi
    $GRADLEW --no-daemon \
        --init-script $SCRIPT_DIR/rerun-requested-task-init-script.gradle \
        --init-script $SCRIPT_DIR/attach-async-profiler-to-tests-init-script.gradle \
        -p $PROJECT_DIR \
        --no-configuration-cache \
        $GRADLE_ARGS \
        -Dkotlin.compiler.execution.strategy="in-process"  \
        $AGENT_PARAMETER_NAME="-agentpath:$AGENT_PATH=start,event=cpu,$AGENT_PARAMS,interval=500000" #sample every .5 ms
}

profile $GRADLE_ARGS, $REPORT_NAME

function openFileInChrome {
    for i in {1..5}; do
        # wait until file is written
        sleep 2
        if test -f "$1"; then
            case "`uname`" in
                Darwin* )
                    open $1
                    ;;
                Linux* )
                    google-chrome $1
                    ;;
            esac
            return 0
        fi
    done
    echo "Couldn't find the output file. $OUTPUT_FILE"
}

# open it in chrome once done
openFileInChrome $OUTPUT_FILE
