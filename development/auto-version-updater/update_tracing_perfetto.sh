#!/usr/bin/env bash

#
# Usage:
# ./update_perfetto.sh ANDROIDX_CHECKOUT CURRENT_VERSION NEW_VERSION
#
# Example:
# ./update_perfetto.sh /Volumes/android/androidx-main/frameworks/support 1.0.0-alpha04 1.0.0-alpha05
#

set -euo pipefail

ANDROIDX_CHECKOUT="$(cd "$1"; pwd -P .)" # gets absolute path of root dir
CURRENT_VERSION="$2"
NEW_VERSION="$3"

OUT_DIR="$ANDROIDX_CHECKOUT/../../out"
OUT_DIR_PROJECT_ZIPS="$OUT_DIR/dist/per-project-zips"

PREBUILTS="$ANDROIDX_CHECKOUT/../../prebuilts"
PREBUILTS_TRACING_PERFETTO_BINARY="$PREBUILTS/androidx/internal/androidx/tracing/tracing-perfetto-binary"

function echo_colour() {
    ORANGE='\033[0;33m'
    NO_COLOUR='\033[0m'
    echo -e "${ORANGE}${*}${NO_COLOUR}"
}

function do_update() {
    # update version in code
    cd "$ANDROIDX_CHECKOUT"
    echo_colour "Updating version in code..."
    sed -r -i"" "s/tracingPerfettoVersion = \"$CURRENT_VERSION\"/tracingPerfettoVersion = \"$NEW_VERSION\"/" \
      benchmark/benchmark-macro/src/androidTest/java/androidx/benchmark/macro/perfetto/PerfettoSdkHandshakeTest.kt
    sed -r -i"" "s/TRACING_PERFETTO = \"$CURRENT_VERSION\"/TRACING_PERFETTO = \"$NEW_VERSION\"/" \
      libraryversions.toml
    sed -r -i"" "s/#define VERSION \"$CURRENT_VERSION\"/#define VERSION \"$NEW_VERSION\"/" \
      tracing/tracing-perfetto-binary/src/main/cpp/tracing_perfetto.cc
    sed -r -i"" "s/const val libraryVersion = \"$CURRENT_VERSION\"/const val libraryVersion = \"$NEW_VERSION\"/" \
      tracing/tracing-perfetto/src/androidTest/java/androidx/tracing/perfetto/jni/test/PerfettoNativeTest.kt
    sed -r -i"" "s/const val version = \"$CURRENT_VERSION\"/const val version = \"$NEW_VERSION\"/" \
      tracing/tracing-perfetto/src/main/java/androidx/tracing/perfetto/jni/PerfettoNative.kt
    echo_colour "Updating version in code... ✓"

    # build new binaries
    echo_colour "Building new binaries..."
    ./gradlew :tracing:tracing-perfetto-binary:createProjectZip -DTRACING_PERFETTO_REUSE_PREBUILTS_AAR=false
    echo_colour "Building new binaries... ✓"

    # copy binaries to prebuilts
    echo_colour "Copying files to prebuilts..."
    project_zip=$(find "$OUT_DIR_PROJECT_ZIPS" -type f -maxdepth 1 -name "*tracing*perfetto*binary*$NEW_VERSION*.zip")
    dst_dir="$PREBUILTS_TRACING_PERFETTO_BINARY/$NEW_VERSION"
    if [ -a "$dst_dir" ]; then rm -rf "$dst_dir"; fi
    mkdir "$dst_dir"
    unzip -xjqq "$project_zip" "**/$NEW_VERSION/**" -d "$dst_dir"
    echo_colour "Copying files to prebuilts... ✓"

    # update SHA
    echo_colour "Updating binary checksums..."
    for arch in armeabi-v7a arm64-v8a x86 x86_64; do
        cd "$dst_dir"
        checksum=$(unzip -cxqq "*tracing*binary*$NEW_VERSION*.aar" "**/$arch/libtracing_perfetto.so" | shasum -a256 | awk '{print $1}')
        cd "$ANDROIDX_CHECKOUT"
        sed -r -i"" "s/\"$arch\" to \"[a-z0-9]{64}\"/\"$arch\" to \"$checksum\"/" \
          tracing/tracing-perfetto/src/main/java/androidx/tracing/perfetto/jni/PerfettoNative.kt
    done
    echo_colour "Updating binary checksums... ✓"

    # all done
    echo_colour "UPDATE SUCCESSFUL"
}

do_update
