#!/bin/bash
set -e

# This is a helper script to be called by androidx.sh
# This script asks parse_profile_html.py to parse the appropriate report html files

cd "$(dirname $0)"

if [ "$DIST_DIR" == "" ]; then
  DIST_DIR=../../../../out/dist
fi

METRICS_DIR="$DIST_DIR/librarymetrics/build"

# If a profile file exists, parse it. If not, do nothing
PROFILE_FILES="../../../../out/androidx/build/reports/profile/*.html"
if ls $PROFILE_FILES >/dev/null 2>&1 ; then
  ./parse_profile_html.py --input-profile "$(ls $PROFILE_FILES | sort | tail -n 2 | head -n 1)" --output-summary $METRICS_DIR/build_androidx.json
fi
