#!/bin/bash

## This script set the state of a subfolder to the state in some commit, creating a merge commit.
## Warning!!! Snapping subfolders breaks the base commit and future merges of the destination branch. To fix it, merge the destination branch back to the source branch, discarding all changes.

set -e

if [ -z "$1" ]; then
echo "Specify the snapping commit and the subfolder. For example: ./snapSubfolder.sh androidx/compose-material3/1.2.0-alpha02 compose/material3/material3"
exit 1
fi

if [ -z "$2" ]; then
echo "Specify the snapping commit and the subfolder. For example: ./snapSubfolder.sh androidx/compose-material3/1.2.0-alpha02 compose/material3/material3"
exit 1
fi

COMMIT=$1
FOLDER=$2

ROOT_DIR="$(dirname "$0")/.."

(
    cd $ROOT_DIR;
    git checkout --no-overlay $COMMIT -- $FOLDER;
    NEW_COMMIT=$(git commit-tree -p HEAD -p $COMMIT -m"Snap $COMMIT, subfolder $FOLDER" $(git write-tree));
    git reset --hard $NEW_COMMIT;
)
