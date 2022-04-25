#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

EXIT_VALUE=0

# Validate translation exports, if present
if ! impl/check_translations.sh; then
  EXIT_VALUE=1
else
  # Run Gradle
  if ! impl/build.sh buildOnServer checkExternalLicenses listTaskOutputs validateProperties \
      -Pandroidx.enableComposeCompilerMetrics=true \
      -Pandroidx.enableComposeCompilerReports=true \
      --profile "$@"; then
    EXIT_VALUE=1
  fi

  # Parse performance profile reports (generated with the --profile option above) and re-export
  # the metrics in an easily machine-readable format for tracking
  impl/parse_profile_htmls.sh
fi

echo "Completing $0 at $(date) with exit value $EXIT_VALUE"

exit "$EXIT_VALUE"
