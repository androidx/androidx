#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh test linuxX64Test -Pandroidx.useMaxDepVersions \
    -Pandroidx.displayTestOutput=false \
    -Pandroidx.enabled.kmp.target.platforms=+native \
    -Pandroidx.ignoreTestFailures "$@"

echo "Completing $0 at $(date)"
