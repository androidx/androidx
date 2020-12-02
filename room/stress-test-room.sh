#!/bin/bash

# This script runs a bunch of room related compilations repeatedly
# to avoid a native library loading bug in xerial
# https://github.com/xerial/sqlite-jdbc/issues/97

set -e

cd "$(dirname $0)/.."

if [ ! -f gradlew ]; then
    echo "./gradlew does not exist! Make sure I live under frameworks/support/room"
    exit 1
fi

NOW=`date +%F-%H-%M-%S`
SUCCESS_MSG="BUILD SUCCESSFUL"
OUTPUT_PREFIX="o_"
ERROR_PREFIX="e_"
OUTPUT_FOLDER="room_stress_test_output_${NOW}"
REPEAT=50
rm -rf $OUTPUT_FOLDER
mkdir $OUTPUT_FOLDER

function printResult {
   success=`grep "${SUCCESS_MSG}" $1 | wc -l`
   if [ $success -eq 1 ]
   then
       echo "SUCCESS"
   else
       echo "FAIL"
   fi
}

echo "output folder:${OUTPUT_FOLDER}. Will run ${REPEAT} times"

for (( i=0; i<$REPEAT; i++ ))
do
   echo "START RUN $i"
   ./gradlew --no-build-cache --stacktrace  \
         :room:integration-tests:room-testapp-noappcompat:asAnTest \
         :room:integration-tests:room-testapp-autovalue:asAnTest \
         :room:integration-tests:room-testapp:asAnTest \
         :room:integration-tests:room-testapp-kotlin:asAnTest \
         > "${OUTPUT_FOLDER}/${OUTPUT_PREFIX}${i}" 2>"${OUTPUT_FOLDER}/${ERROR_PREFIX}${i}" || true
   echo "END RUN $i"
   printResult "${OUTPUT_FOLDER}/${OUTPUT_PREFIX}${i}"
done

totalSuccess=`grep "${SUCCESS_MSG}" ${OUTPUT_FOLDER}/* |wc -l`
echo "${totalSuccess} over ${REPEAT} succeeded"

