#!/bin/bash
set -e

# Compute absolute filepath for DIST_DIR
DIST_DIR="$(mkdir -p $DIST_DIR && cd $DIST_DIR && pwd)"
cd "$(dirname $0)"

# Run Gradle
DIST_DIR=$DIST_DIR impl/build.sh --no-daemon "buildOnServer" -PverifyUpToDate --profile "$@"

# Merge some output files
python3 impl/merge_outputs.py DIST_DIR=$DIST_DIR "mergeBuildInfo"

# Parse performance profile reports (generated with the --profile option above) and re-export the metrics in an easily machine-readable format for tracking
DIST_DIR=$DIST_DIR impl/parse_profile_htmls.sh
