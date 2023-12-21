#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh test zipOwnersFiles createModuleInfo \
    -Pandroidx.ignoreTestFailures \
    -Pandroidx.displayTestOutput=false \
    "$@"

echo "Completing $0 at $(date)"
