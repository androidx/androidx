#!/bin/bash
set -e
cd "$(dirname $0)"

# build just KMP projects. This will also enable native targets.
export ANDROIDX_PROJECTS=KMP

# disable cache, NS does not allow it yet: b/235227707
export USE_ANDROIDX_REMOTE_BUILD_CACHE=false

# run build in a sandbox
../development/sandbox/run-without-network.sh impl/build.sh buildOnServer --no-configuration-cache --no-daemon
