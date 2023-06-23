#!/bin/bash
set -e

echo "Starting $0 at $(date)"

cd "$(dirname $0)"

export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp

impl/build.sh zipTestConfigsWithApks zipOwnersFiles createModuleInfo "$@"

echo "Completing $0 at $(date)"
