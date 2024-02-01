#!/bin/bash
set -e
cd "$(dirname $0)"

# Builds all projects that support KMP except for Compose-specific projects which are already
# covered by androidx_compose_multiplatform.sh.

# Must be run on Linux

# build just INFRAROGUE projects. This will also enable native targets.
export ANDROIDX_PROJECTS=INFRAROGUE   # TODO: Switch from `INFRAROGUE` to `KMP`

# disable cache, NS does not allow it yet: b/235227707
export USE_ANDROIDX_REMOTE_BUILD_CACHE=false

# run build in a sandbox
../development/sandbox/run-without-network.sh impl/build.sh \
    buildOnServer \
    listTaskOutputs \
    allHostTests \
    -Pandroidx.ignoreTestFailures \
    -Pandroidx.displayTestOutput=false
