#!/bin/bash
set -e

SCRIPT_DIR="$(cd $(dirname $0) && pwd)"
# TODO(b/141549086): move this mkdir logic and DIST_DIR logic into doAllTheBuild.py once this script (androidx.sh) is under presubmit testing
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$SCRIPT_DIR/../../../out/dist"
fi
mkdir -p "$DIST_DIR"

python "$SCRIPT_DIR/doAllTheBuild.py" DIST_DIR="$(cd $DIST_DIR && pwd)" --no-daemon
