#!/bin/bash
set -e

function usage() {
  echo "Confirms that no unexpected, generated files exist in the source repository"
  echo
  echo "Usage: $0 <timestamp file>"
  echo
  echo "<timestamp file>: any file newer than this one will be considered an error unless it is already exempted"
  return 1
}

# parse arguments
# a file whose timestamp is the oldest acceptable timestamp for source files
COMPARE_TO_FILE="$1"
if [ "$COMPARE_TO_FILE" == "" ]; then
  usage
fi

# get script path
SCRIPT_DIR="$(cd $(dirname $0) && pwd)"
SOURCE_DIR="$(cd $SCRIPT_DIR/../.. && pwd)"

# confirm that no files in the source repo were unexpectedly created (other than known exemptions)
function checkForGeneratedFilesInSourceRepo() {

  # Paths that are still expected to be generated and that we have to allow
  # If you need add or remove an exemption here, update cleanBuild.sh too
  EXEMPT_PATHS=".gradle buildSrc/.gradle local.properties reports build"
  # put "./" in front of each path to match the output from 'find'
  EXEMPT_PATHS="$(echo " $EXEMPT_PATHS" | sed 's| | ./|g')"
  # build a `find` argument for skipping descending into the exempt paths
  EXEMPTIONS_ARGUMENT="$(echo $EXEMPT_PATHS | sed 's/ /\n/g' | sed 's|\(.*\)|-path \1 -prune -o|g' | xargs echo)"

  # Search for files that were created or updated more recently than the build start.
  # Unfortunately we can't also include directories because the `test` task seems to update
  # the modification time in several projects
  GENERATED_FILES="$(cd $SOURCE_DIR && find . $EXEMPTIONS_ARGUMENT -newer $COMPARE_TO_FILE -type f)"
  UNEXPECTED_GENERATED_FILES=""
  for f in $GENERATED_FILES; do
    exempt=false
    for exemption in $EXEMPT_PATHS; do
      if [ "$f" == "$exemption" ]; then
        exempt=true
        break
      fi
      if [ "$f" == "$(dirname $exemption)" ]; then
        # When the exempt directory gets created, its parent dir will be modified
        # So, we ignore changes to the parent dir too (but not necessarily changes in sibling dirs)
        exempt=true
        break
      fi
    done
    if [ "$exempt" == "false" ]; then
      UNEXPECTED_GENERATED_FILES="$UNEXPECTED_GENERATED_FILES $f"
    fi
  done
  if [ "$UNEXPECTED_GENERATED_FILES" != "" ]; then
    echo >&2
    echo "Unexpectedly found these files generated or modified by the build:

${UNEXPECTED_GENERATED_FILES}

Generated files should go in OUT_DIR instead because that is where developers expect to find them
(to make it easier to diagnose build problems: inspect or delete these files)" >&2

    # copy these new files into DIST_DIR in case anyone wants to inspect them
    COPY_TO=$DIST_DIR/new_files
    for f in $UNEXPECTED_GENERATED_FILES; do
      dest="$COPY_TO/$f"
      mkdir -p "$(dirname $dest)"
      cp "$SOURCE_DIR/$f" "$dest"
    done
    echo >&2
    echo Copied these generated files into $COPY_TO >&2
    exit 1
  fi
}
echo checking compared to $COMPARE_TO_FILE
checkForGeneratedFilesInSourceRepo
