#!/bin/bash
# Helper script to kick-off a playground project setup.
# This is intended to be used when we create a new Playground project or update existing ones
# if we do structural changes in Playground's setup.

function relativize() {
    python3 -c "import os.path; print(os.path.relpath('$1', '$2'))"
}

PLAYGROUND_REL_PATH=$(dirname $0)
WORKING_DIR=$(pwd)

function symlink() {
    SRC=$1
    TARGET=$2
    echo "symlinking $SRC to $TARGET / dirname: $(dirname $TARGET)"
    TARGET_PARENT_DIR=$(dirname $TARGET)
    REL_PATH_TO_TARGET_PARENT=$(relativize $SRC $WORKING_DIR/$TARGET_PARENT_DIR)
    rm -rf $TARGET
    ln -s $REL_PATH_TO_TARGET_PARENT $TARGET
    # echo "symlink $REL_PATH_TO_TARGET_PARENT to $TARGET"
    
}

symlink "${PLAYGROUND_REL_PATH}/gradle" gradle
symlink "${PLAYGROUND_REL_PATH}/../buildSrc" buildSrc
# create gradle symlinks
symlink "${PLAYGROUND_REL_PATH}/gradle" gradle
symlink "${PLAYGROUND_REL_PATH}/gradlew" gradlew
symlink "${PLAYGROUND_REL_PATH}/gradlew.bat" gradlew.bat
symlink "${PLAYGROUND_REL_PATH}/androidx-shared.properties" gradle.properties
symlink "${PLAYGROUND_REL_PATH}/../buildSrc" buildSrc

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
    # TARGET_DIR=$(dirname $IDEA_CONFIG_FILE)
    # # relativize it wrt to the file we'll create
    # REL_PATH=$(relativize $ORIGINAL_FILE $TARGET_DIR )
    # symlink to the original idea file
    # ln -s $REL_PATH $IDEA_CONFIG_FILE
    # # forse add the file to git
    # git add -f $IDEA_CONFIG_FILE
    symlink $ORIGINAL_FILE $IDEA_CONFIG_FILE
done
