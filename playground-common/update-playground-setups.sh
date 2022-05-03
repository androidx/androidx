#!/bin/bash
# Helper script to update all playground setups when we make changes
# on how playground works (in other words, when we update setup-playground.sh).
function absPath {
    python3 -c "import os.path; print(os.path.abspath('$1'))"
}

WORKING_DIR=$(pwd)
PLAYGROUND_REL_PATH=$(dirname $0)
SUPPORT_ROOT_ABS_PATH=$(absPath "$PLAYGROUND_REL_PATH/..")
PLAYGROUND_ABS_PATH=$(absPath $PLAYGROUND_REL_PATH)

# re-runs the playground setup script on the given folder
function setupPlayground {
    echo "setting up playground in $1"
    cd $1
    $PLAYGROUND_ABS_PATH/setup-playground.sh
    echo "finished setting up playground in $1"
}

# find all playground settings files
PLAYGROUND_SETTINGS_FILES=$(egrep -lir --include=settings.gradle "setupPlayground" $SUPPORT_ROOT_ABS_PATH)
for SETTINGS_FILE in $PLAYGROUND_SETTINGS_FILES
do
    PROJECT_DIR=$(absPath $(dirname $SETTINGS_FILE))
    setupPlayground $PROJECT_DIR
done
cd $WORKING_DIR