#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh test linuxX64Test \
    -Pandroidx.ignoreTestFailures \
    -Pandroidx.displayTestOutput=false \
    -Pandroidx.enabled.kmp.target.platforms=+native \
    "$@"

echo "Completing $0 at $(date)"
