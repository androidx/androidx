#!/bin/bash
set -e

cd "$(dirname $0)"

./build.sh --no-daemon test --info -PuseMaxDepVersions --offline -Pandroidx.enableAffectedModuleDetection
