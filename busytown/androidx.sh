#!/bin/bash
set -e

cd "$(dirname $0)"

impl/build.sh --no-daemon "buildOnServer" -PverifyUpToDate
python3 impl/merge_outputs.py DIST_DIR="$(cd $DIST_DIR && pwd)" "mergeBuildInfo"
