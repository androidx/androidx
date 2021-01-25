#!/bin/bash
# This script runs kotlin test app compilation with ksp and kapt repeatedly to measure time spent for each of them.
# Each build is a clean build for the test project
declare -A totals
declare -A taskTotals

function log {
    echo $1
}

# parses the given profile file, extracts task durations that we are interested in and adds them to the global tracking
# usage: parseTimes profileFileURI prefix
function parseTimes {
    filePath=$1
    prefix=$2
    # get the times
    result=`curl -s $filePath|grep :$prefix -A1`
    total=0
    taskName="ERROR-$prefix"
    while read -r line
        do
        if [[ "$line" == *"numeric"* ]]; then
            taskTime=`echo $line|awk -F'[>s<]' '{print $5*1000}'`
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
    type=$1
    useKsp=-1
    if [ "$type" = "ksp" ]; then
        echo "will use ksp"
        useKsp=1
    elif [ "$type" = "kapt" ]; then
        echo "will use kapt"
        useKsp=0
    else
        echo "bad arg '$type'"
        exit
    fi
    cmd="./gradlew --profile room:integration-tests:room-testapp-kotlin:clean room:integration-tests:room-testapp-kotlin:assembleAndroidTest -PuseKsp=$useKsp"
    log "Executing $cmd"
    profileFile=`$cmd|grep -v "androidx-plugin"|awk '/profiling report at:/ {print $6}'`
    log "result: $profileFile"
    parseTimes $profileFile $type
}

# Runs the compilation with kapt and ksp for the given number of times
# usage: runTest 3
function runTest {
    limit=$1
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

runTest 10
printData totals
printData taskTotals

