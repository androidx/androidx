#!/bin/bash
set -e

cd "$(dirname $0)"

./build.sh --no-daemon "buildOnServer" -PverifyUpToDate
python3 ./merge_outputs.py DIST_DIR="$(cd $DIST_DIR && pwd)" "mergeBuildInfo"
