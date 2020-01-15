#!/bin/bash
set -e

cd $(dirname $0)

./build.sh --no-daemon test jacocoTestReport zipEcFiles --info --offline -Pandroidx.enableAffectedModuleDetection
