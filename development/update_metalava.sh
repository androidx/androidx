#! /bin/bash

set -x

METALAVA_BUILD_ID="$1"
SCRIPT_PATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

if [ -z "$METALAVA_BUILD_ID" ]; then
    echo "You must specify a build id for Metalava: ./updateMetalava.sh <build_id>"
    exit 1
fi

python "$SCRIPT_PATH"/import_maven_artifacts.py -n com.android:metalava:1.3.0 -mb "$METALAVA_BUILD_ID"
sed -i "s/\(androidx\.playground\.metalavaBuildId=\)[0-9]*/\1$METALAVA_BUILD_ID/g" "$SCRIPT_PATH"/../playground-common/playground.properties

