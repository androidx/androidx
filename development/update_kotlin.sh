#!/bin/bash
set -e

KOTLIN_VERSION="$1"
KSP_VERSION="$2"

if [[ $# -eq 0 ]] ; then
    echo "Usage ./development/update_kotlin.sh <kotlin_version> [<ksp_version>]"
    exit 1
fi

# Download maven artifacts
ARTIFACTS_TO_DOWNLOAD="org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD="org.jetbrains.kotlin:kotlin-build-tools-impl:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-test:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-test-junit:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-test-annotations-common:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-test-common:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-dom-api-compat:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-stdlib-common:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-stdlib-jdk8:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-klib-commonizer-embeddable:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-compiler:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-compiler-embeddable:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-parcelize-runtime:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-annotation-processing-gradle:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-parcelize-compiler:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-bom:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-metadata-jvm:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:swift-export-embeddable:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:abi-tools:$KOTLIN_VERSION,"

ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-native-prebuilt:$KOTLIN_VERSION:linux-x86_64@tar.gz,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-native-prebuilt:$KOTLIN_VERSION:macos-x86_64@tar.gz,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-native-prebuilt:$KOTLIN_VERSION:macos-aarch64@tar.gz,"

if [ "$KSP_VERSION" ]; then
    ARTIFACTS_TO_DOWNLOAD+="com.google.devtools.ksp:symbol-processing:$KSP_VERSION,"
    ARTIFACTS_TO_DOWNLOAD+="com.google.devtools.ksp:symbol-processing-api:$KSP_VERSION,"
    ARTIFACTS_TO_DOWNLOAD+="com.google.devtools.ksp:symbol-processing-cmdline:$KSP_VERSION,"
    ARTIFACTS_TO_DOWNLOAD+="com.google.devtools.ksp:symbol-processing-gradle-plugin:$KSP_VERSION,"
    ARTIFACTS_TO_DOWNLOAD+="com.google.devtools.ksp:symbol-processing-aa-embeddable:$KSP_VERSION,"
fi

./development/importMaven/importMaven.sh "$ARTIFACTS_TO_DOWNLOAD" --allow-jetbrains-dev

# symlink native compiler prebuilt archives from prebuilts/androidx/external to prebuilts/androidx/konan
# to make KonanPrebuiltsSetup.kt work.
rm -fr "../../prebuilts/androidx/konan/nativeCompilerPrebuilts/releases"

REAL_NATIVE_PREBUILT_DIR="../../../../../external/org/jetbrains/kotlin/kotlin-native-prebuilt/$KOTLIN_VERSION/"

LINUX_DIR="../../prebuilts/androidx/konan/nativeCompilerPrebuilts/releases/$KOTLIN_VERSION/linux-x86_64"
mkdir -p "$LINUX_DIR"
ln -s -f "$REAL_NATIVE_PREBUILT_DIR/kotlin-native-prebuilt-$KOTLIN_VERSION-linux-x86_64.tar.gz" \
    "$LINUX_DIR/kotlin-native-prebuilt-linux-x86_64-$KOTLIN_VERSION.tar.gz"
ln -s -f "$REAL_NATIVE_PREBUILT_DIR/kotlin-native-prebuilt-$KOTLIN_VERSION-linux-x86_64.tar.gz.asc" \
    "$LINUX_DIR/kotlin-native-prebuilt-linux-x86_64-$KOTLIN_VERSION.tar.gz.asc"

MAC_ARM_DIR="../../prebuilts/androidx/konan/nativeCompilerPrebuilts/releases/$KOTLIN_VERSION/macos-aarch64"
mkdir -p "$MAC_ARM_DIR"
ln -s -f "$REAL_NATIVE_PREBUILT_DIR/kotlin-native-prebuilt-$KOTLIN_VERSION-macos-aarch64.tar.gz" \
    "$MAC_ARM_DIR/kotlin-native-prebuilt-macos-aarch64-$KOTLIN_VERSION.tar.gz"
ln -s -f "$REAL_NATIVE_PREBUILT_DIR/kotlin-native-prebuilt-$KOTLIN_VERSION-macos-aarch64.tar.gz.asc" \
    "$MAC_ARM_DIR/kotlin-native-prebuilt-macos-aarch64-$KOTLIN_VERSION.tar.gz.asc"

MAC_X86_DIR="../../prebuilts/androidx/konan/nativeCompilerPrebuilts/releases/$KOTLIN_VERSION/macos-x86_64"
mkdir -p "$MAC_X86_DIR"
ln -s -f "$REAL_NATIVE_PREBUILT_DIR/kotlin-native-prebuilt-$KOTLIN_VERSION-macos-x86_64.tar.gz" \
    "$MAC_X86_DIR/kotlin-native-prebuilt-macos-x86_64-$KOTLIN_VERSION.tar.gz"
ln -s -f "$REAL_NATIVE_PREBUILT_DIR/kotlin-native-prebuilt-$KOTLIN_VERSION-macos-x86_64.tar.gz.asc" \
    "$MAC_X86_DIR/kotlin-native-prebuilt-macos-x86_64-$KOTLIN_VERSION.tar.gz.asc"
