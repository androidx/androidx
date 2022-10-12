#!/bin/bash
set -e
cd "$(dirname $0)"

export ANDROIDX_PROJECTS=KMP

# disable GCP cache, these machines don't have credentials.
export USE_ANDROIDX_REMOTE_BUILD_CACHE=false

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh allTests \
    --no-configuration-cache \
    -Pandroidx.ignoreTestFailures \
    -Pandroidx.displayTestOutput=false \
    "$@"

echo "Completing $0 at $(date)"