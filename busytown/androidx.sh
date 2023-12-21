#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

# This target is for testing that clean builds work correctly
# We disable the remote cache for this target unless it was already enabled
if [ "$USE_ANDROIDX_REMOTE_BUILD_CACHE" == "" ]; then
  export USE_ANDROIDX_REMOTE_BUILD_CACHE=false
fi

EXIT_VALUE=0

# Validate translation exports, if present
if ! impl/check_translations.sh; then
  EXIT_VALUE=1
else
  # Run Gradle
  if ! impl/build.sh buildOnServer createAllArchives checkExternalLicenses listTaskOutputs \
      -Pandroidx.enableComposeCompilerMetrics=true \
      -Pandroidx.enableComposeCompilerReports=true \
      -Pandroidx.constraints=true \
      # If/when we enable desktop, enable VerifyDependencyVersionsTask.kt/shouldVerifyConfiguration
      -Pandroidx.enabled.kmp.target.platforms=-desktop \
      --no-daemon \
      --profile "$@"; then
    EXIT_VALUE=1
  fi

  # Parse performance profile reports (generated with the --profile option above) and re-export
  # the metrics in an easily machine-readable format for tracking
  impl/parse_profile_data.sh
fi

echo "Completing $0 at $(date) with exit value $EXIT_VALUE"

exit "$EXIT_VALUE"
