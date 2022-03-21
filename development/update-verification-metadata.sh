#!/bin/bash
set -e

# This script regenerates signature-related information (dependency-verification-metadata and keyring)
function regenerateTrustedKeys() {
  echo "regenerating list of trusted keys"
  # regenerate metadata
  # Need to run a clean build, https://github.com/gradle/gradle/issues/19228
  ./gradlew --write-verification-metadata pgp,sha256 --dry-run --clean bOS
  # extract and keep only the <trusted-keys> section
  WORK_DIR=gradle/update-keys-temp
  rm -rf "$WORK_DIR"
  mkdir -p "$WORK_DIR"

  # extract the middle of the new file, https://github.com/gradle/gradle/issues/18569
  grep -B 10000 "<trusted-keys>" gradle/verification-metadata.dryrun.xml > "$WORK_DIR/new.head"
  grep -A 10000 "</trusted-keys>" gradle/verification-metadata.dryrun.xml > "$WORK_DIR/new.tail"
  numTopLines="$(cat "$WORK_DIR/new.head" | wc -l)"
  numTopLinesPlus1="$(($numTopLines + 1))"
  numBottomLines="$(cat "$WORK_DIR/new.tail" | wc -l)"
  numLines="$(cat gradle/verification-metadata.dryrun.xml | wc -l)"
  numMiddleLines="$(($numLines - $numTopLines - $numBottomLines))"
  # also remove 'version=' lines, https://github.com/gradle/gradle/issues/20192
  cat gradle/verification-metadata.dryrun.xml | tail -n "+$numTopLinesPlus1" | head -n "$numMiddleLines" | sed 's/ version="[^"]*"//' > "$WORK_DIR/new.middle"

  # extract the top and bottom of the old file
  grep -B 10000 "<trusted-keys>" gradle/verification-metadata.xml > "$WORK_DIR/old.head"
  grep -A 10000 "</trusted-keys>" gradle/verification-metadata.xml > "$WORK_DIR/old.tail"

  # update file
  cat "$WORK_DIR/old.head" "$WORK_DIR/new.middle" "$WORK_DIR/old.tail" > gradle/verification-metadata.xml

  # remove temporary files
  rm -rf "$WORK_DIR"
  rm -rf gradle/verification-metadata.dryrun.xml
}
regenerateTrustedKeys

# updates the keyring, including sorting entries and removing duplicates
function regenerateKeyring() {
  # a separate step from regenerating the verification metadata, https://github.com/gradle/gradle/issues/20138
  echo "regenerating keyring"
  ./gradlew --write-verification-metadata sha256 --export-keys --dry-run bOS

  echo "sorting keyring and removing duplicates"
  # sort and unique the keyring
  # https://github.com/gradle/gradle/issues/20140
  # `sed 's/$/NEWLINE/g'` adds the word NEWLINE at the end of each line
  # `tr -d '\n'` deletes the actual newlines
  # `sed` again adds a newline at the end of each key, so each key is one line
  # `sort` orders the keys deterministically
  # `uniq` removes identical keys
  # `sed 's/NEWLINE/\n/g'` puts the newlines back
  cat gradle/verification-keyring-dryrun.keys \
    | sed 's/$/NEWLINE/g' \
    | tr -d '\n' \
    | sed 's/\(-----END PGP PUBLIC KEY BLOCK-----\)/\1\n/g' \
    | grep "END PGP PUBLIC KEY BLOCK" \
    | sort \
    | uniq \
    | sed 's/NEWLINE/\n/g' \
    > gradle/verification-keyring.keys

  # remove unused files
  rm -f gradle/verification-keyring-dryrun.gpg
  rm -f gradle/verification-keyring-dryrun.keys
  rm -f gradle/verification-metadata.dryrun.xml
}
regenerateKeyring

echo
echo "Done. Please check that these changes look correct ('git diff')"
