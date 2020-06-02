#!/bin/bash
set -e

cd "$(dirname $0)"

impl/build.sh --no-daemon test -PuseMaxDepVersions --offline \
    -Pandroidx.enableAffectedModuleDetection \
    -Pandroidx.coverageEnabled=true \
    -Pandroidx.ignoreTestFailures "$@"
