#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh --no-daemon test -PuseMaxDepVersions --offline \
    -Pandroidx.enableAffectedModuleDetection \
    -Pandroidx.coverageEnabled=true \
    -Pandroidx.displayTestOutput=false \
    -Pandroidx.validateNoUnrecognizedMessages \
    -Pandroidx.ignoreTestFailures "$@"

echo "Completing $0 at $(date)"
