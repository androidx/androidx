#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

CHECKOUT_DIR="$(cd ../../.. && pwd)"
OUT_DIR="$CHECKOUT_DIR/out"
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$OUT_DIR/dist"
fi

function hashOutDir() {
  hashFile=out.hashes
  echo "hashing out dir and saving into $DIST_DIR/$hashFile"
  # We hash files in parallel for more performance (-P <number>)
  # We limit the number of files hashed by any one process (-n <number>) to lower the risk of one
  # process having to do much more work than the others.
  # We do allow each process to hash multiple files (also -n <number>) to avoid spawning too many processes
  # It would be nice to copy all files, but that takes a while
  time (cd $OUT_DIR && find -type f | grep -v "$hashFile" | xargs --no-run-if-empty -P 32 -n 64 sha1sum > $DIST_DIR/$hashFile)
  echo "done hashing out dir"
}
hashOutDir

# Run Gradle
impl/build.sh buildOnServer checkExternalLicenses listTaskOutputs validateAllProperties \
    --profile "$@"

# Parse performance profile reports (generated with the --profile option above) and re-export the metrics in an easily machine-readable format for tracking
impl/parse_profile_htmls.sh

echo "Completing $0 at $(date)"
