#!/bin/bash
set -e

# This is a helper script to be called by androidx.sh
# This script asks parse_profile_html.py to parse the appropriate report html files

cd "$(dirname $0)"

if [ "$DIST_DIR" == "" ]; then
  DIST_DIR=../../../../out/dist
fi

METRICS_DIR="$DIST_DIR/librarymetrics/build"

./parse_profile_html.py --input-profile "$(ls ../../../../out/androidx/build/reports/profile/*.html | sort | tail -n 2 | head -n 1)" --output-summary $METRICS_DIR/build_androidx.json
./parse_profile_html.py --input-profile "$(ls ../../../../out/ui/ui/build/reports/profile/*.html | sort | tail -n 2 | head -n 1)" --output-summary $METRICS_DIR/build_ui.json
