#!/bin/bash
set -e

# This script pulls the given ksp version and deletes the old one.
# It is not a solid script so use with care. It does not check what your
# current local status is and instead just creates a new branch and
# fetches prebuilts while also updating the version in androidX.

# move to support dir
cd "$(dirname $0)/../.."

function usage {
    echo "error: $1"
    echo "usage: ./update_ksp.sh <version> <local branch name>"
    echo "e.g. ./update_ksp.sh 1.4.32-1.0.0-alpha07 ksp-alpha07"
    exit 1
}

REPO=$(which repo)
if [ -z "$REPO" ]; then
    usage "cannot find repo"
fi

KSP_VERSION=$1
if [ -z "$KSP_VERSION" ]; then
    usage "must pass ksp version"
fi
if [ -z "$2" ]; then
    echo "a"
    BRANCH_NAME=$KSP_VERSION
else
    BRANCH_NAME=$2
fi

echo "$KSP_VERSION / $BRANCH_NAME"

# create branch
repo abandon $BRANCH_NAME . platform/prebuilts/androidx/external 2> /dev/null || true
repo start $BRANCH_NAME . platform/prebuilts/androidx/external

# other projects depend on ksp prebuilts so we don't delete them anymore.

# download
development/importMaven/importMaven.sh com.google.devtools.ksp:symbol-processing-gradle-plugin:$KSP_VERSION
development/importMaven/importMaven.sh com.google.devtools.ksp:symbol-processing:$KSP_VERSION

# update build version
sed -i '' "s/ksp = \".*\"/ksp = \"$KSP_VERSION\"/" gradle/libs.versions.toml

echo "done"