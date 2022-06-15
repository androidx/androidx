#!/bin/bash
set -e
cd "$(dirname $0)"

export ANDROIDX_PROJECTS=KMP

# disable GCP cache, these machines don't have credentials.
export USE_ANDROIDX_REMOTE_BUILD_CACHE=false

impl/build.sh buildOnServer --no-configuration-cache
