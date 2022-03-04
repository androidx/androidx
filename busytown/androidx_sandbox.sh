#!/bin/bash
set -e

cd "$(dirname $0)"

cd ..

# --no-configuration-cache because of b/221086214

development/sandbox/run-without-network.sh \
    ./gradlew \
    --no-configuration-cache \
    --offline \
    --stacktrace \
    collection2:collection2:tasks
