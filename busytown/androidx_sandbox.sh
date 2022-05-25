#!/bin/bash
set -e

cd "$(dirname $0)"

cd ..

# --no-configuration-cache because of b/221086214
offline_gradle() {
    development/sandbox/run-without-network.sh \
        ./gradlew \
        --no-configuration-cache \
        --offline \
        --stacktrace \
        "$*"
}

offline_gradle collection2:collection2:tasks
offline_gradle -Pandroidx.kmp.linux.enabled=true \
               collection2:collection2:linuxX64Test
