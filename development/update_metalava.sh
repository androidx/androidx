#! /bin/bash

set -x

METALAVA_VERSION="$1"
METALAVA_BUILD_ID="$2"
SCRIPT_PATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

if [ -z "$METALAVA_VERSION" ]; then
    echo "You must specify a build id for Metalava: ./updateMetalava.sh <metalava_version> <build_id>"
    exit 1
fi

python "$SCRIPT_PATH"/importMaven/import_maven_artifacts.py -n com.android.tools.metalava:metalava:"$METALAVA_VERSION" -mb "$METALAVA_BUILD_ID"
sed -i "s/\(androidx\.playground\.metalavaBuildId=\)[0-9]*/\1$METALAVA_BUILD_ID/g" "$SCRIPT_PATH"/../playground-common/playground.properties

