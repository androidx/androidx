#!/bin/bash
set -e

function runGradle() {
  kmpArgs="-Pandroidx.compose.multiplatformEnabled=true -Pandroidx.enabled.kmp.target.platforms=+native"
  echo running ./gradlew $kmpArgs "$@"
  if ./gradlew $kmpArgs "$@"; then
    echo succeeded: ./gradlew $kmpArgs "$@"
  else
    echo failed: ./gradlew $kmpArgs "$@"
    return 1
  fi
}

# This script regenerates signature-related information (dependency-verification-metadata and keyring)
function regenerateVerificationMetadata() {
  echo "regenerating verification metadata and keyring"
  # regenerate metadata
  # Need to run a clean build, https://github.com/gradle/gradle/issues/19228
  runGradle --stacktrace --write-verification-metadata pgp,sha256 --export-keys --dry-run --clean -Pandroidx.update.signatures=true bOS :docs-kmp:zipCombinedKmpDocs

  # update verification metadata file
  # also remove 'version=' lines, https://github.com/gradle/gradle/issues/20192
  cat gradle/verification-metadata.dryrun.xml | sed 's/ \(trusted-key.*\)version="[^"]*"/\1/' > gradle/verification-metadata.xml

  # rename keyring
  mv gradle/verification-keyring-dryrun.keys gradle/verification-keyring.keys

  # remove temporary files
  rm -f gradle/verification-keyring-dryrun.gpg
  rm -f gradle/verification-metadata.dryrun.xml
}
regenerateVerificationMetadata

echo
echo 'Done. Please check that these changes look correct (`git diff`)'
