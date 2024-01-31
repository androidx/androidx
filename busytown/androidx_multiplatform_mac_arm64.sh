#!/bin/bash
set -e

export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp

SCRIPT_DIR="$(cd $(dirname $0) && pwd)"
$SCRIPT_DIR/androidx_multiplatform_mac.sh -Pandroidx.lowMemory
