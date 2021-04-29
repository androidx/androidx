#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

# Run Gradle
EXIT_VALUE=0
if impl/build.sh buildOnServer checkExternalLicenses listTaskOutputs validateAllProperties \
    --profile "$@"; then
  EXIT_VALUE=0
else
  EXIT_VALUE=1
fi

# Parse performance profile reports (generated with the --profile option above) and re-export the metrics in an easily machine-readable format for tracking
impl/parse_profile_htmls.sh

echo "Completing $0 at $(date) with exit value $EXIT_VALUE"

exit "$EXIT_VALUE"
