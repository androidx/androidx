#!/bin/bash
# Helper script to kick-off a playground project setup.
# This is intended to be used when we create a new Playground project or update existing ones
# if we do structural changes in Playground's setup.

function relativize() {
    python -c "import os.path; print(os.path.relpath('$1', '$2'))"
}

PLAYGROUND_REL_PATH=$(dirname $0)
WORKING_DIR=$(pwd)

# create gradle symlinks
rm -rf gradle
ln -s "${PLAYGROUND_REL_PATH}/gradle" gradle
rm -rf gradlew
ln -s "${PLAYGROUND_REL_PATH}/gradlew" gradlew
rm -rf gradlew.bat
ln -s "${PLAYGROUND_REL_PATH}/gradlew.bat" gradlew.bat
rm -rf gradle.properties
ln -s "${PLAYGROUND_REL_PATH}/androidx-shared.properties" gradle.properties

ANDROIDX_IDEA_DIR="${PLAYGROUND_REL_PATH}/../.idea"

# cleanup .idea, we'll re-create it
rm -rf .idea
mkdir .idea

# create idea directories first .idea config directories that are tracked in git
git ls-tree -d -r HEAD --name-only --full-name $ANDROIDX_IDEA_DIR|xargs mkdir -p

# get a list of all .idea files that are in git tree
# we excluse vcs as it is used for multiple repo setup which we don't need in playground
TRACKED_IDEA_FILES=( $(git ls-tree -r HEAD --name-only --full-name $ANDROIDX_IDEA_DIR| grep -v vcs| grep -v Compose) )

# create a symlink for each one of them
for IDEA_CONFIG_FILE in "${TRACKED_IDEA_FILES[@]}"
do
    # path to the actual .idea config file
    ORIGINAL_FILE="$PLAYGROUND_REL_PATH/../$IDEA_CONFIG_FILE"
    TARGET_DIR=$(dirname $IDEA_CONFIG_FILE)
    # relativize it wrt to the file we'll create
    REL_PATH=$(relativize $ORIGINAL_FILE $TARGET_DIR )
    # symlink to the original idea file
    ln -s $REL_PATH $IDEA_CONFIG_FILE
    # forse add the file to git
    git add -f $IDEA_CONFIG_FILE
done