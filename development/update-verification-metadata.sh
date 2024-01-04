#!/bin/bash
set -e

# This script updates trust entries in gradle/verification-metadata.xml

# Usage: $0 [--no-dry-run] [<task>]

# --no-dry-run
#   Don't pass --dry-run to Gradle, so Gradle executes the corresponding tasks.
#   This is not normally necessary but in some cases can be a useful workaround.
#   When https://github.com/gradle/gradle/issues/26289 is resolved, we should reevaluate this behavior
#
# <task>
#   The task to ask Gradle to run. By default this is 'bOS'
#   When --no-dry-run is removed, we should reevaluate this behavior

dryrun=true
task="bOS"

while [ "$1" != "" ]; do
  arg="$1"
  shift
  if [ "$arg" == "--no-dry-run" ]; then
    dryrun=false
    continue
  fi
  task="$arg"
done

function runGradle() {
  echo running ./gradlew "$@"
  if ./gradlew "$@"; then
    echo succeeded: ./gradlew "$@"
  else
    echo failed: ./gradlew "$@"
    return 1
  fi
}

# This script regenerates signature-related information (dependency-verification-metadata and keyring)
function regenerateVerificationMetadata() {
  echo "regenerating verification metadata and keyring"
  # regenerate metadata
  # Need to run a clean build, https://github.com/gradle/gradle/issues/19228
  # Resolving Configurations before task execution is expected. b/297394547
  dryrunArg=""
  if [ "$dryrun" == "true" ]; then
    dryrunArg="--dry-run"
  fi
  runGradle --stacktrace --write-verification-metadata pgp,sha256 --export-keys $dryrunArg --clean -Pandroidx.update.signatures=true -Pandroid.dependencyResolutionAtConfigurationTime.disallow=false -Pandroidx.enabled.kmp.target.platforms=+native $task

  # update verification metadata file

  # first, make sure the resulting file is named "verification-metadata.xml"
  if [ "$dryrun" == "true" ]; then
    mv gradle/verification-metadata.dryrun.xml gradle/verification-metadata.xml
  fi

  # next, remove 'version=' lines https://github.com/gradle/gradle/issues/20192
  sed -i 's/\(trusted-key.*\)version="[^"]*"/\1/' gradle/verification-metadata.xml

  # rename keyring
  mv gradle/verification-keyring-dryrun.keys gradle/verification-keyring.keys 2>/dev/null || true

  # remove temporary files
  rm -f gradle/verification-keyring-dryrun.gpg
  rm -f gradle/verification-keyring.gpg
}
regenerateVerificationMetadata

echo
echo 'Done. Please check that these changes look correct (`git diff`)'
echo "If Gradle did not make all expected updates to verification-metadata.xml, you can try '--no-dry-run'. This is slow so you may also want to specify a task. Example: $0 --dry-run exportSboms"
