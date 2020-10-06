#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh --no-daemon test jacocoTestReport zipEcFiles --offline \
    -Pandroidx.enableAffectedModuleDetection \
    -Pandroidx.ignoreTestFailures \
    -Pandroidx.displayTestOutput=false \
    -Pandroidx.coverageEnabled=true \
    -Pandroidx.validateNoUnrecognizedMessages \
    -Pandroidx.allWarningsAsErrors "$@"

python3 impl/merge_outputs.py mergeExecutionData

echo "Completing $0 at $(date)"
