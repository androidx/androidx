#!/bin/bash
set -e

# This is a helper script to be called by androidx.sh
# This script locates, parses, and merges build profiling information from various report files

cd "$(dirname $0)"

if [ "$OUT_DIR" == "" ]; then
  OUT_DIR=../../../../out
fi
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$OUT_DIR/dist"
fi

METRICS_DIR="$DIST_DIR/librarymetrics/build"
INTERMEDIATES_DIR=$OUT_DIR

# Find the metrics files that exist
METRICS_FILES="$(echo $OUT_DIR/androidx/*/build/build-metrics.json | grep -v '*' || true)"

# Look for a profile file and attempt to make a metrics json out of it
PROFILE_FILES="$OUT_DIR/androidx/build/reports/profile/*.html"
if ls $PROFILE_FILES >/dev/null 2>&1 ; then
  # parse the profile file and generate a .json file summarizing it
  PROFILE_JSON=$INTERMEDIATES_DIR/build_androidx.json
  ./parse_profile_html.py --input-profile "$(ls $PROFILE_FILES | sort | tail -n 2 | head -n 1)" --output-summary $PROFILE_JSON
  METRICS_FILES="$METRICS_FILES $PROFILE_JSON"
fi

if [ "$METRICS_FILES" != "" ]; then
  # merge all profiles
  mkdir -p "$METRICS_DIR"
  # concatenate files, and replace "}{" with ", ", ignoring whitespace
  cat $METRICS_FILES | sed 's/ *} *{ */, /g' > $METRICS_DIR/build_androidx.json
  # remove metrics files so that next time if Gradle skips emitting them then we don't get old results
  rm -f $METRICS_FILES
fi
