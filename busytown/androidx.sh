#!/bin/bash
set -e

cd "$(dirname $0)"

# Run Gradle
impl/build.sh --no-daemon listTaskOutputs "$@"
impl/build.sh --no-daemon buildOnServer checkExternalLicenses \
    -PverifyUpToDate \
    -Pandroidx.coverageEnabled=true \
    -Pandroidx.allWarningsAsErrors --profile "$@"

# Merge some output files
python3 impl/merge_outputs.py "mergeBuildInfo" "mergeLibraryMetrics" "mergeSourceJars"

# Parse performance profile reports (generated with the --profile option above) and re-export the metrics in an easily machine-readable format for tracking
impl/parse_profile_htmls.sh
