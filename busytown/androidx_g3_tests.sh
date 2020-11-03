#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh --no-daemon createArchive \
    -Pandroidx.enableAffectedModuleDetection

echo "Completing $0 at $(date)"
