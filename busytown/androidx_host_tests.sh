#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh test jacocoTestReport zipEcFiles \
    -Pandroidx.ignoreTestFailures \
    -Pandroidx.displayTestOutput=false \
    -Pandroidx.validateNoUnrecognizedMessages "$@"

echo "Completing $0 at $(date)"
