#!/bin/bash
set -e

# This script verifies the set of translatable string.xml fields against
# those listed in the localization exports file.

cd "$(dirname "$0")/../.."

exports='../../vendor/google/tools/localization/exports'
if [ ! -e "$exports" ]; then
  echo 'Missing localization exports file, skipping verification...'
  exit 0
fi

tempdir=$(mktemp -d "${OUT_DIR:-/tmp/}$(basename "$0").XXXXXXXXXXXX")
trap 'rm -rf "$tempdir"' EXIT

# Find string.xml files that need translation
expect="$tempdir/expect.txt"
find . \
    \( \
      -iname '*sample*' \
      -o -iname '*donottranslate*' \
      -o -iname '*debug*' \
      -o -iname '*test*' \
    \) \
    -prune -o \
    -path '*/res/values/*strings.xml' \
    -print \
  | sed -n 's/.\///p' \
  | sort \
  > "$expect"

# Scrape string.xml files for platform branch
actual="$tempdir/actual.txt"
grep 'androidx-platform-dev' "$exports" \
  | grep -Eo '[^ ]+/strings\.xml' \
  | sort \
  > "$actual"

# Compare and report
diff=$(diff "${expect}" "${actual}" | { grep '<' || true; })
if [ -n "$diff" ]; then
  echo "Missing files in $exports:" &> 2
  diff "$expect" "$actual" | grep strings.xml | sed -n 's/< //p' &> 2
  echo &> 2
  echo 'See go/androidx/playbook#translations for more information' &> 2
  exit 1
fi
