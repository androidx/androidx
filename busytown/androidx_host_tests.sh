#!/bin/bash
set -e

cd $(dirname $0)

impl/build.sh --no-daemon test jacocoTestReport zipEcFiles --info --offline -Pandroidx.enableAffectedModuleDetection

#python3 ./merge_outputs.py mergeExecutionData DIST_DIR="$DIST_DIR"
