#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

impl/build.sh --no-daemon assembleDebug assembleAndroidTest -PuseMaxDepVersions --offline "$@"

echo "Completing $0 at $(date)"
