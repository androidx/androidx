#!/bin/bash
set -e
echo "This is a test of building on a mac"
echo "expect: OS = Darwin"
echo "actual: OS = $(uname)"

cd "$(dirname $0)"

# Build is disabled until we get capacity.

# export ANDROIDX_PROJECTS=KMP
# # disable cache, for some reason, not getting credentials on this machine
# export USE_ANDROIDX_REMOTE_BUILD_CACHE=false
# impl/build.sh buildOnServer --no-configuration-cache