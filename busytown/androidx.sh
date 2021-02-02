#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

# Run Gradle
impl/build.sh listTaskOutputs "$@"
impl/build.sh allProperties -Pandroidx.validateNoUnrecognizedMessages=false "$@" >/dev/null
subsets="MAIN COMPOSE FLAN MEDIA"
for subset in $subsets; do
  ANDROIDX_PROJECTS=$subset impl/build.sh tasks -Pandroidx.validateNoUnrecognizedMessages=false >/dev/null
done
impl/build.sh buildOnServer checkExternalLicenses \
    --profile "$@"

# Parse performance profile reports (generated with the --profile option above) and re-export the metrics in an easily machine-readable format for tracking
impl/parse_profile_htmls.sh

echo "Completing $0 at $(date)"
