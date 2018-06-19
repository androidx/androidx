#!/bin/bash
#
# Imports core library sources for an ExoPlayer release from GitHub, repackages
# and hides them for use as part of the implementation of MediaPlayer2 for pre-P
# Android builds.

TAG=r2.8.1
FROM_PACKAGE=com.google.android.exoplayer2
TO_PACKAGE=androidx.media.exoplayer.external

REPOSITORY=https://github.com/google/ExoPlayer.git
SOURCE_DIRECTORY=library/core/src/main/java/${FROM_PACKAGE//./\/}
SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DESTINATION_DIRECTORY="${SCRIPT_DIRECTORY}/../../../../external/exoplayer-repackaged/src/main/java/${TO_PACKAGE//./\/}"
GOOGLE_JAVA_FORMAT="${SCRIPT_DIRECTORY}/../../../../prebuilts/tools/common/google-java-format/google-java-format"

# Check out the required version of ExoPlayer from GitHub.
TEMPORARY_DIRECTORY=$(mktemp -d)
git clone --branch "${TAG}" "${REPOSITORY}" "${TEMPORARY_DIRECTORY}"
pushd "${TEMPORARY_DIRECTORY}"
cd "${SOURCE_DIRECTORY}"

# Switch usages of the internal package to the external one.
find . -name "*.java" -type f -print0 | xargs -0 \
    perl -p -i -e \
    "s/${FROM_PACKAGE//./\.}/${TO_PACKAGE}/g"
# Hide top-level interfaces/classes without javadoc.
find . -name "*.java" -type f -print0 | xargs -0 \
    perl -p -i -e \
    'BEGIN {undef $/;} s/\n\n((?:\@.*\n)*[^\s][^\/\*]* )(interface|class)/\n\n\/**\n * \@hide\n *\/\n\@RestrictTo(LIBRARY_GROUP)\n\1\2/'
# Use androidx.annotation instead of Android support annotations.
find . -name "*.java" -type f -print0 | xargs -0 \
    perl -p -i -e \
    's/android\.support\.annotation/androidx.annotation/g'
# Hide top-level interfaces/classes with multiline javadoc.
find . -name "*.java" -type f -print0 | xargs -0 \
    perl -p -i -e \
    'BEGIN {undef $/;} s/^ \*\/\n((?:\@.*\n)*.*)(interface|class)/ *\n * \@hide\n *\/\n\@RestrictTo(LIBRARY_GROUP)\n\1\2/m'
# Hide top-level interfaces/classes with one line javadoc.
find . -name "*.java" -type f -print0 | xargs -0 \
    perl -p -i -e \
    'BEGIN {undef $/;} s/^\/\*\*(.*) \*\/\n((?:\@.*\n)*.*)(interface|class)/\/**\n *\1\n * \n * \@hide\n *\/\n\@RestrictTo(LIBRARY_GROUP)\n\2\3/m'
# Import the the required symbols.
find . -name "*.java" -type f -print0 | xargs -0 \
    perl -p -i -e \
    's/^(package .*)/\1\n\nimport static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;\nimport androidx.annotation.RestrictTo;/'
# Run google-java-format to use AOSP style and organize imports.
find . -name "*.java" -type f -print0 | xargs -0 \
    "${GOOGLE_JAVA_FORMAT}" --aosp -i

mkdir -p "${DESTINATION_DIRECTORY}"
cp -R . "${DESTINATION_DIRECTORY}"
popd

