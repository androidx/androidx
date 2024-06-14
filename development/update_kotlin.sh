#!/bin/bash
set -e

KOTLIN_VERSION="$1"

ALLOW_JETBRAINS_DEV=""
for arg in "$@"
do
    if [ "$arg" == "--allow-jetbrains-dev" ]; then
      ALLOW_JETBRAINS_DEV="--allow-jetbrains-dev"
    fi
done

# Download maven artifacts
ARTIFACTS_TO_DOWNLOAD="org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD="org.jetbrains.kotlin:kotlin-build-tools-impl:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-test:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-test-junit:$KOTLIN_VERSION,"
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

ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-native-prebuilt:$KOTLIN_VERSION:linux-x86_64@tar.gz,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-native-prebuilt:$KOTLIN_VERSION:macos-x86_64@tar.gz,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-native-prebuilt:$KOTLIN_VERSION:macos-aarch64@tar.gz,"

./development/importMaven/importMaven.sh "$ALLOW_JETBRAINS_DEV" "$ARTIFACTS_TO_DOWNLOAD"

 ./development/importMaven/importMaven.sh import-konan-binaries --konan-compiler-version "$KOTLIN_VERSION"

