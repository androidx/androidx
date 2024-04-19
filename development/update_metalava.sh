#! /bin/bash

set -x

METALAVA_VERSION="$1"
METALAVA_BUILD_ID="$2"
SCRIPT_PATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

if [ -z "$METALAVA_BUILD_ID" ]; then
    echo "You must specify a build id for Metalava: ./updateMetalava.sh <metalava_version> <build_id>"
    exit 1
fi

"$SCRIPT_PATH"/importMaven/importMaven.sh com.android.tools.metalava:metalava:"$METALAVA_VERSION" --metalava-build-id "$METALAVA_BUILD_ID" --redownload
sed -i "s/\(androidx\.playground\.metalavaBuildId=\)[0-9]*/\1$METALAVA_BUILD_ID/g" "$SCRIPT_PATH"/../playground-common/playground.properties
sed -i "s/^\\(metalava\\s*=.*version\\s*=\\s*\"\\)[^\"]*/\\1$METALAVA_VERSION/" "$SCRIPT_PATH/../gradle/libs.versions.toml"

