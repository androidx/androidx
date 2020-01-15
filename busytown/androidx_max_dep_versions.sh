#!/bin/bash
set -e

cd $(dirname $0)

./build.sh --no-daemon assembleDebug assembleAndroidTest -PuseMaxDepVersions --offline
