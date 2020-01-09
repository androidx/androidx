#!/bin/bash
set -e

SCRIPT_DIR="$(cd $(dirname $0) && pwd)"
# TODO(b/141549086): move this mkdir logic and DIST_DIR logic into doAllTheBuild.py once this script (androidx.sh) is under presubmit testing
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$SCRIPT_DIR/../../../out/dist"
fi
mkdir -p "$DIST_DIR"

python3 "$SCRIPT_DIR/build.py" "doBoth" DIST_DIR="$(cd $DIST_DIR && pwd)" --no-daemon "buildOnServer" -PverifyUpToDate
python3 "$SCRIPT_DIR/merge_outputs.py" DIST_DIR="$(cd $DIST_DIR && pwd)" "mergeBuildInfo"
