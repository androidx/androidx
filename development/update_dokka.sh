#!/bin/bash
set -e

DOKKA_VERSION="$1"

if [[ $# -eq 0 ]] ; then
    echo "Usage ./development/update_dokka.sh <dokka_version>"
    exit 1
fi

# Download maven artifacts
ARTIFACTS_TO_DOWNLOAD="org.jetbrains.dokka:analysis-kotlin-api:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:analysis-kotlin-descriptors:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:analysis-kotlin-symbols:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:analysis-markdown:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:dokka-base:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:dokka-base-test-utils:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:dokka-cli:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:dokka-core:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:dokka-test-api:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:dokka-gradle-plugin:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:android-documentation-plugin:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:all-modules-page-plugin:$DOKKA_VERSION,"
ARTIFACTS_TO_DOWNLOAD+="org.jetbrains.dokka:templating-plugin:$DOKKA_VERSION"

./development/importMaven/importMaven.sh "$ARTIFACTS_TO_DOWNLOAD" --allow-jetbrains-dev
