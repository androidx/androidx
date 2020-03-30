#!/bin/bash
set -e

cd "$(dirname $0)"

impl/build.sh --no-daemon test jacocoTestReport zipEcFiles --offline \
    -Pandroidx.enableAffectedModuleDetection \
    -Pandroidx.ignoreTestFailures \
    -Pandroidx.coverageEnabled=true \
    -Pandroidx.allWarningsAsErrors "$@"

python3 impl/merge_outputs.py mergeExecutionData
