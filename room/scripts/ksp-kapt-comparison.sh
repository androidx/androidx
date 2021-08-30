#!/bin/bash
# This script runs kotlin test app compilation with ksp and kapt repeatedly to measure time spent for each of them.
# Each build is executed once first (to cache all other tasks), and then N times for just ksp/kapt tasks.
set -e
declare -A totals
declare -A taskTotals

function log {
    echo $1
}
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIR="$SCRIPT_DIR/.."
# move to the project directory
cd $PROJECT_DIR;

KSP_TASK=":room:integration-tests:room-testapp-kotlin:kspWithKspDebugAndroidTestKotlin"
KAPT_TASK=":room:integration-tests:room-testapp-kotlin:kaptGenerateStubsWithKaptDebugAndroidTestKotlin \
    :room:integration-tests:room-testapp-kotlin:kaptWithKaptDebugAndroidTestKotlin"
# parses the given profile file, extracts task durations that we are interested in and adds them to the global tracking
# usage: parseTimes profileFileURI prefix
function parseTimes {
    local filePath=$1
    local prefix=$2
    # get the times
    local result=`curl -s $filePath|grep :$prefix -A1`
    local total=0
    local taskName="ERROR-$prefix"
    while read -r line
        do
        if [[ "$line" == *"numeric"* ]]; then
            local taskTime=`echo $line|awk -F'[>s<]' '{print $5*1000}'`
            total=$(($total + $taskTime))
            taskTotals[$taskName]=$((taskTotals[$taskName] + $taskTime))
        elif [[ "$line" == *":"* ]]; then
            taskName=`echo $line|awk -F'[><]' '{print $3}'|awk -F'[:]' '{print $NF}'`
        fi
    done <<< $result
    echo "total time spent for in this run  $prefix: $total"
    totals[$prefix]=$((totals[$prefix] + $total))
}

# Runs the kotlin integration test app with either ksp or kapt then records the duration of tasks.
# usage: runBuild ksp / runBuild kapt
function runBuild {
    local type=$1
    local task=""
    if [ "$type" = "ksp" ]; then
        task=$KSP_TASK
    elif [ "$type" = "kapt" ]; then
        task=$KAPT_TASK
    else
        echo "bad arg '$type'"
        exit 1
    fi
    local cmd="./gradlew --no-daemon --init-script \
        $SCRIPT_DIR/rerun-requested-task-init-script.gradle \
        --profile $task"
    log "Executing $cmd"
    local profileFile=`$cmd|grep -v "buildSrc"|awk '/profiling report at:/ {print $6}'`
    log "result: $profileFile"
    parseTimes $profileFile $type
}

# Runs the compilation with kapt and ksp for the given number of times
# usage: runTest 3
function runTest {
    local limit=$1
    for (( c=1; c<=$limit; c++ ))
    do
        echo "run #$c of $limit"
        runBuild "ksp"
        runBuild "kapt"
    done
}

function printData {
    local -n data=$1
    echo "$1:"
    for i in "${!data[@]}"
    do
        echo "$i : ${data[$i]} ms"
    done
}

# build once so all other tasks are cached
./gradlew $KSP_TASK $KAPT_TASK

runTest 10
printData totals
printData taskTotals
