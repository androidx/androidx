#!/bin/bash
# Get versions
AGP_VERSION=${1:-4.1.0-beta01}
STUDIO_VERSION_STRING=${2:-Android Studio 4.1 Beta 1}
STUDIO_IFRAME_LINK=`curl "https://developer.android.com/studio/archive.html" | grep iframe | sed "s/.*src=\"\([a-zA-Z0-9\/\._]*\)\".*/https:\/\/android-dot-devsite-v2-prod.appspot.com\1/g"`
STUDIO_LINK=`curl -s $STUDIO_IFRAME_LINK | grep -C30 "$STUDIO_VERSION_STRING" | grep Linux | tail -n 1 | sed 's/.*a href="\(.*\).*"/\1/g'`
STUDIO_VERSION=`echo $STUDIO_LINK | sed "s/.*ide-zips\/\(.*\)\/android-studio-ide-\([0-9]\+\)\.\([0-9]\+\).*/\1/g"`
IDEA_MAJOR_VERSION=`echo $STUDIO_LINK | sed "s/.*ide-zips\/\(.*\)\/android-studio-ide-\([0-9]\+\)\.\([0-9]\+\).*/\2/g"`
STUDIO_BUILD_NUMBER=`echo $STUDIO_LINK | sed "s/.*ide-zips\/\(.*\)\/android-studio-ide-\([0-9]\+\)\.\([0-9]\+\).*/\3/g"`

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
sed -i "s/agp=.*/agp=$AGP_VERSION/g" buildSrc/studio_versions.properties
sed -i "s/lint=.*/lint=$LINT_VERSION/g" buildSrc/studio_versions.properties
sed -i "s/studio_version=.*/studio_version=$STUDIO_VERSION/g" buildSrc/studio_versions.properties
sed -i "s/idea_major_version=.*/idea_major_version=$IDEA_MAJOR_VERSION/g" buildSrc/studio_versions.properties
sed -i "s/studio_build_number=.*/studio_build_number=$STUDIO_BUILD_NUMBER/g" buildSrc/studio_versions.properties
