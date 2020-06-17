#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh --no-daemon buildTestApks \
    -Pandroidx.enableAffectedModuleDetection \
    -Pandroidx.dependentProjects \
    -Pandroidx.coverageEnabled=true \
    -Pandroidx.allWarningsAsErrors --offline "$@"

echo "Completing $0 at $(date)"
