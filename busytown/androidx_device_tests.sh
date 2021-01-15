#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh zipTestConfigsWithApks -Pandroidx.validateNoUnrecognizedMessages \
    -Pandroidx.enableAffectedModuleDetection \
    --offline "$@"

echo "Completing $0 at $(date)"
