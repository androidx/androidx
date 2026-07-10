#!/bin/bash
set -e

echo "Starting $0 at $(date)"
source "$(dirname "$0")/setup_build_env_vars.sh"
source "$(dirname "$0")/record_build_metrics.sh"
source "$(dirname "$0")/impl/delete_old_out.sh"

cd "$(dirname $0)"

setup_build_env_vars
start_time=$(initialize_start_time)

export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp
export ENABLE_PRESUBMIT_COMPATIBLE_CC_STORE=true

deleteOldOutDir

# This target runs in incremental mode, but we do not want to restore the APKs and configs from
# previous runs
# find script
SCRIPT_DIR="$(pwd)"
TEST_XML_CONFIGS="$SCRIPT_DIR/../../../out/test-xml-configs"
echo "Deleting $TEST_XML_CONFIGS"
rm -fr $TEST_XML_CONFIGS

impl/build.sh zipTestConfigsWithApks zipOwnersFiles createModuleInfo "$@"

record_build_metrics "$start_time"

echo "Completing $0 at $(date)"