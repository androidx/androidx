#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh buildTestApks \
    -Pandroidx.enableAffectedModuleDetection \
    -Pandroidx.dependentProjects \
    -Pandroidx.validateNoUnrecognizedMessages \
    --offline "$@"

echo "Completing $0 at $(date)"
