#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

# Run Gradle
time impl/build.sh --no-daemon listTaskOutputs "$@"
time impl/build.sh --no-daemon buildOnServer checkExternalLicenses \
    allProperties \
    -PverifyUpToDate \
    -Pandroidx.coverageEnabled=true \
    -Pandroidx.allWarningsAsErrors --profile "$@"

# Merge some output files
time python3 impl/merge_outputs.py "mergeBuildInfo" "mergeLibraryMetrics" "mergeSourceJars"

# Parse performance profile reports (generated with the --profile option above) and re-export the metrics in an easily machine-readable format for tracking
time impl/parse_profile_htmls.sh

echo "Completing $0 at $(date)"
