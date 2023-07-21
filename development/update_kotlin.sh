#!/bin/bash
set -e

export KOTLIN_VERSION="1.9.0-dev-6188"

# Download and place konan
export KONAN_DIR=../../prebuilts/androidx/konan/nativeCompilerPrebuilts/dev/$KOTLIN_VERSION/linux-x86_64
mkdir -p $KONAN_DIR
curl -o $KONAN_DIR/kotlin-native-prebuilt-linux-x86_64-$KOTLIN_VERSION.tar.gz https://download-cf.jetbrains.com/kotlin/native/builds/dev/$KOTLIN_VERSION/linux-x86_64/kotlin-native-prebuilt-linux-x86_64-$KOTLIN_VERSION.tar.gz

# Download maven artifacts
ARTIFACTS_TO_DOWNLOAD="org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlinx-serialization-compiler-plugin-embeddable:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-test:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-test-junit:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-stdlib-jdk8:$KOTLIN_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.kotlin:kotlin-klib-commonizer-embeddable:$KOTLIN_VERSION"

./development/importMaven/importMaven.sh --allow-jetbrains-dev "$ARTIFACTS_TO_DOWNLOAD"
