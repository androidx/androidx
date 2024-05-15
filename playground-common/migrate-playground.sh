#!/bin/bash
set -x
set -e
function relativize() {
    python3 -c "import os.path; print(os.path.relpath('$1', '$2'))"
}

PLAYGROUND_PROJECTS=( $(find . -not -path "playground" -name "settings.gradle" -exec grep -l "selectProjectsFromAndroidX" {} \+) )

TARGET_PG_ROOT="./playground-projects"

function deleteOldPlaygroundFiles {
    OLD_PG=$(relativize $1)
    rm -f "$OLD_PG/gradle"
    rm -f "$OLD_PG/gradlew"
    rm -f "$OLD_PG/gradlew.bat"
    rm -f "$OLD_PG/gradle.properties"
    rm -f "$OLD_PG/settings.gradle"
    rm -rf "$OLD_PG/buildSrc"
    rm -rf "$OLD_PG/.idea"
}

function createNewPlaygroundIn {
    SETTINGS_FILE=$(relativize $1)
    NEW_PG=$(relativize $2)
    echo "create PG from $SETTINGS_FILE into $NEW_PG"
    mkdir -p $NEW_PG
    $(cp $SETTINGS_FILE $NEW_PG/.)
    ls $NEW_PG
    SETUP_PG_REL_PATH=$(realpath playground-common/setup-playground.sh)
    echo "gonna execute cd $NEW_PG && $SETUP_PG_REL_PATH)"
    (cd $NEW_PG; $SETUP_PG_REL_PATH)
    REL_PLUGIN_PATH=$(relativize "playground-common/configure-plugin-management.gradle" "$NEW_PG")
    REL_ROOT_PATH=$(relativize "." "$NEW_PG")
    NEW_SETTINGS_FILE=$(relativize "$NEW_PG/settings.gradle")

    echo "will replace pg path to $REL_PLUGIN_PATH"
    sed -i '' -E "s#\".*configure-plugin-management.gradle\"#\"$REL_PLUGIN_PATH\"#g" $NEW_SETTINGS_FILE
    echo "will replace setupPlayground calls"
    sed -i '' -E "s#setupPlayground\(\".*\"\)#setupPlayground\(\"$REL_ROOT_PATH\"\)#g" $NEW_SETTINGS_FILE
}

function migrateOldPlayground {
    OLD_PG=$1
    NEW_PG="$TARGET_PG_ROOT/$OLD_PG-playground"
    createNewPlaygroundIn "$OLD_PG/settings.gradle" $NEW_PG
    deleteOldPlaygroundFiles $OLD_PG
}

for OLD_PLAYGROUND_PROJECT in "${PLAYGROUND_PROJECTS[@]}"
do
    PG_PATH=$(dirname $OLD_PLAYGROUND_PROJECT)
    migrateOldPlayground $PG_PATH
done
