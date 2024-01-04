#!/bin/bash
set -e

KOTLIN_VERSION="$1"

# Download maven artifacts
ARTIFACTS_TO_DOWNLOAD="org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION,"
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

./development/importMaven/importMaven.sh --allow-jetbrains-dev "$ARTIFACTS_TO_DOWNLOAD"

# Import konan binaries
./development/importMaven/importMaven.sh import-konan-binaries --konan-compiler-version "$KOTLIN_VERSION"
