#!/bin/bash
# helper script to unzip KMP's host libraries into the given folder, if necessary (linux).
set -e

# find script
SCRIPT_DIR="$(cd $(dirname $0) && pwd)"
SYSROOT_DIR=$1
mkdir -p $SYSROOT_DIR
# TODO Can we find a more reliable way to get the kotlin native version?
KOTLIN_NATIVE_VERSION=$(grep "kotlin =" gradle/libs.versions.toml| cut -d "=" -f2|tr -d '"'|cut -d " " -f2)
# extract it into the out dir
$SCRIPT_DIR/../../../../prebuilts/androidx/konan/copy-linux-sysroot.sh \
    --kotlin-version $KOTLIN_NATIVE_VERSION \
    --target-dir $SYSROOT_DIR
