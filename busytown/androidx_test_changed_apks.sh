#!/bin/bash
set -e

cd "$(dirname $0)"

./build.sh --no-daemon buildTestApks -Pandroidx.enableAffectedModuleDetection -Pandroidx.changedProjects --offline
