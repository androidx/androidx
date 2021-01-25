#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh --no-daemon zipTestConfigsWithApks -Pandroidx.validateNoUnrecognizedMessages \
    -PverifyUpToDate \
    -Pandroidx.coverageEnabled=true \
    -Pandroidx.enableAffectedModuleDetection \
    -Pandroidx.allWarningsAsErrors --offline "$@"

echo "Completing $0 at $(date)"
