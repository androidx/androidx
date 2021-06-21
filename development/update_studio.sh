#!/bin/bash
# Get versions
AGP_VERSION=${1:-7.1.0-alpha01}
STUDIO_VERSION_STRING=${2:-"Android Studio Bumblebee (2021.1.1) Canary 1"}
STUDIO_IFRAME_LINK=`curl "https://developer.android.com/studio/archive.html" | grep iframe | sed "s/.*src=\"\([a-zA-Z0-9\/\._]*\)\".*/https:\/\/android-dot-devsite-v2-prod.appspot.com\1/g"`
STUDIO_LINK=`curl -s $STUDIO_IFRAME_LINK | grep -C30 "$STUDIO_VERSION_STRING" | grep Linux | tail -n 1 | sed 's/.*a href="\(.*\).*"/\1/g'`
STUDIO_VERSION=`echo $STUDIO_LINK | sed "s/.*ide-zips\/\(.*\)\/android-studio-.*/\1/g"`

# Update AGP
./development/importMaven/import_maven_artifacts.py -n com.android.tools.build:gradle:$AGP_VERSION
./development/importMaven/import_maven_artifacts.py -n androidx.databinding:viewbinding:$AGP_VERSION
AAPT2_VERSIONS=`curl "https://dl.google.com/dl/android/maven2/com/android/tools/build/group-index.xml" | grep aapt2-proto | sed 's/.*versions="\(.*\)"\/>/\1/g'`
AAPT2_VERSION=`echo $AAPT2_VERSIONS | sed "s/.*\($AGP_VERSION-[0-9]*\).*/\1/g"`
./development/importMaven/import_maven_artifacts.py -n com.android.tools.build:aapt2:$AAPT2_VERSION:linux
./development/importMaven/import_maven_artifacts.py -n com.android.tools.build:aapt2:$AAPT2_VERSION:osx
./development/importMaven/import_maven_artifacts.py -n com.android.tools.build:aapt2:$AAPT2_VERSION
LINT_VERSIONS=`curl "https://dl.google.com/dl/android/maven2/com/android/tools/lint/group-index.xml" | grep lint | sed 's/.*versions="\(.*\)"\/>/\1/g'`
LINT_MINOR_VERSION=`echo $AGP_VERSION | sed 's/[0-9]\+\.\(.*\)/\1/g'`
LINT_VERSION=`echo $LINT_VERSIONS | sed "s/.*[,| ]\([0-9]\+\.$LINT_MINOR_VERSION\).*/\1/g"`
./development/importMaven/import_maven_artifacts.py -n com.android.tools.lint:lint:$LINT_VERSION
./development/importMaven/import_maven_artifacts.py -n com.android.tools.lint:lint-tests:$LINT_VERSION
./development/importMaven/import_maven_artifacts.py -n com.android.tools.lint:lint-gradle:$LINT_VERSION

# Update studio_versions.properties
sed -i "s/androidGradlePlugin = .*/androidGradlePlugin = $AGP_VERSION/g" gradle/libs.versions.toml
sed -i "s/androidLint = .*/androidLint = $LINT_VERSION/g" gradle/libs.versions.toml
sed -i "s/androidStudio = .*/androidStudio = $STUDIO_VERSION/g" gradle/libs.versions.toml
