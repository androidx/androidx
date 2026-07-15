#!/bin/bash
set -e

DOKKA_VERSION="$1"
DACKKA_CHECKOUT_PATH="$2"

if [[ $# -ne 2 ]] ; then
    echo "Usage ./development/update_dokka.sh <dokka_version> <path_to_dackka_checkout>"
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

# Location of dackka prebuilts relative to the checkout root
DACKKA_PREBUILTS_PATH="$DACKKA_CHECKOUT_PATH/prebuilts/dokka-devsite-plugin"

./development/importMaven/importMaven.sh "$ARTIFACTS_TO_DOWNLOAD" --allow-jetbrains-dev --override-prebuilts-path $DACKKA_PREBUILTS_PATH
