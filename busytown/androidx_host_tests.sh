#!/bin/bash
set -e

cd "$(dirname $0)"

impl/build.sh --no-daemon test jacocoTestReport zipEcFiles --offline \
    -Pandroidx.enableAffectedModuleDetection \
    -Pandroidx.ignoreTestFailures \
    -Pandroidx.allWarningsAsErrors "$@"

# TODO: un-comment this when AMD is fixed (b/147824472)
#python3 ./merge_outputs.py mergeExecutionData
