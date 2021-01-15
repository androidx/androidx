#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

SNAPSHOT=true impl/build.sh \
    createArchive \
    -Pandroidx.enableAffectedModuleDetection=false \
    -Pandroidx.validateNoUnrecognizedMessages \
    --offline "$@"

echo "Completing $0 at $(date)"
