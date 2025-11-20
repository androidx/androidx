#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)/.."

# This target is for testing that clean builds work correctly
# We disable the remote cache for this target unless it was already enabled
if [ "$USE_ANDROIDX_REMOTE_BUILD_CACHE" == "" ]; then
  export USE_ANDROIDX_REMOTE_BUILD_CACHE=false
fi

EXIT_VALUE=0

# Validate translation exports, if present
if ! busytown/impl/check_translations.sh; then
  EXIT_VALUE=1
else
  # Run Gradle
  # If/when we enable desktop, enable VerifyDependencyVersionsTask.kt/shouldVerifyConfiguration
  if ! busytown/impl/build.sh buildOnServer createAllArchives checkExternalLicenses listTaskOutputs exportSboms generateJavaKzip generateKotlinKzip \
      --no-daemon "$@"; then
    EXIT_VALUE=1
  else
    # Run merge-kzips only if Gradle succeeds. Script merges kzips outputted by bOS task
    busytown/impl/merge-kzips.sh || EXIT_VALUE=1
  fi

  # Parse performance profile reports (generated with the --profile option) and re-export
  # the metrics in an easily machine-readable format for tracking
  busytown/impl/parse_profile_data.sh
fi

echo "Completing $0 at $(date) with exit value $EXIT_VALUE"

exit "$EXIT_VALUE"
