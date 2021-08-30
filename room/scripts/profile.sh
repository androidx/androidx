#! /bin/bash
set -eu
# Script to run compilation profile and build a flame graph for it.

function usage {
    echo "usage: ./profile.sh <gradle tasks>
    ./profile.sh --name my_run \
        :room:integration-tests:room-testapp-kotlin:kspWithKspDebugAndroidTestKotlin \
        --filter *K2JVMCompiler*


    Arguments:
    -h: print usage
    --name <report name>: Name of the report (used in output file if output file is not set)
    --filter <stacktrace include filter>: Regex to filter stacktraces. e.g. *room*
    --outputFile <file path>: Path to the output file
    --preset [kapt|ksp]: Predefined tasks to run ksp/kapt profile on the KotlinTestApp

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
        --preset)
            if [ "$2" = "kapt" ]; then
                GRADLE_ARGS=":room:integration-tests:room-testapp-kotlin:kaptGenerateStubsWithKaptDebugAndroidTestKotlin \
                    :room:integration-tests:room-testapp-kotlin:kaptWithKaptDebugAndroidTestKotlin"
                ADDITIONAL_AGENT_PARAMS="$ADDITIONAL_AGENT_PARAMS,include=*AbstractKapt3Extension*"
            elif [ "$2" = "ksp" ]; then
                GRADLE_ARGS=":room:integration-tests:room-testapp-kotlin:kspWithKspDebugAndroidTestKotlin"
                ADDITIONAL_AGENT_PARAMS="$ADDITIONAL_AGENT_PARAMS,include=*AbstractKotlinSymbolProcessingExtension*"
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
    $GRADLEW --no-daemon \
        --init-script $SCRIPT_DIR/rerun-requested-task-init-script.gradle \
        -p $PROJECT_DIR $GRADLE_ARGS \
        -Dkotlin.compiler.execution.strategy="in-process"  \
        -Dorg.gradle.jvmargs="-agentpath:$AGENT_PATH=start,event=cpu,$AGENT_PARAMS,interval=500000" #sample every .5 ms
}

profile $GRADLE_ARGS, $REPORT_NAME

function openFileInChrome {
    for i in {1..5}; do
        # wait until file is written
        sleep 2
        if test -f "$1"; then
            google-chrome $1
            return 0
        fi
    done
    echo "Couldn't find the output file. $OUTPUT_FILE"
}

# open it in chrome once done
openFileInChrome $OUTPUT_FILE