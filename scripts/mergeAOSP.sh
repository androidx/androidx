#!/bin/bash

## This script simplifies merge of AOSP branches to the JetBrains fork.
## The JetBrains fork contains multiple files/folders that are fully developed independently of AOSP,
## and should not be updated by the upstream version

set -e

if [ -z "$1" ]; then
echo "Specify the commit. For example: ./mergeAOSP.sh androidx/compose-ui/1.6.0-alpha08"
exit 1
fi

COMMIT=$1

ROOT_DIR="$(dirname "$0")/.."

(cd $ROOT_DIR; git merge $COMMIT --no-commit --no-ff || true)

# reset subfolder to the HEAD state
resetSubFolder() {
    (
        cd $ROOT_DIR;
        git reset -- "$1" >/dev/null || true;                    # resets MERGE state
        git checkout --no-overlay -- "$1" >/dev/null || true;    # sets state to the HEAD
        git clean -fdx -- "$1" >/dev/null || true;               # removes new files
    )
}

resetSubFolder "./buildSrc"
resetSubFolder "./compose/**/build.gradle"	
resetSubFolder "./lifecycle/**/build.gradle"
resetSubFolder "./navigation/**/build.gradle"
resetSubFolder "./.github"	
resetSubFolder "./.idea"	
resetSubFolder "./.run"	
resetSubFolder "CONTRIBUTING.md"	
resetSubFolder "README.md"	
resetSubFolder "gradlew"	
resetSubFolder "gradlew.bat"	
resetSubFolder "gradle.properties"	
resetSubFolder "./gradle/**"	
resetSubFolder "build.gradle"	
resetSubFolder "settings.gradle"	
resetSubFolder "./collection/**/jvmMain/**"	
resetSubFolder "./collection/**/jvmTest/**"	
resetSubFolder "./compose/**/nativeMain/**"	
resetSubFolder "./compose/**/skikoMain/**"	
resetSubFolder "./compose/**/desktopMain/**"	
resetSubFolder "./compose/**/skikoTest/**"	
resetSubFolder "./compose/**/desktopTest/**"	
resetSubFolder "./compose/desktop/**"	

