#!/bin/bash

## This script merges a commit with discarding all the changes in it.
## It is useful when you don't want to merge some changes, but want to change a base commit, solving all future conflicts.

set -e

if [ -z "$1" ]; then
echo "Specify the commit. For example: ./mergeEmpty.sh origin/jb-main"
exit 1
fi

COMMIT=$1

ROOT_DIR="$(dirname "$0")/.."

(
    cd $ROOT_DIR; 
    NEW_COMMIT=$(git commit-tree -p HEAD -p $COMMIT -m"Merge empty $COMMIT" $(git write-tree));
    git reset --hard $NEW_COMMIT;
)
