#!/bin/bash
set -e

SCRIPT_DIR="$(cd $(dirname $0) && pwd)"
$SCRIPT_DIR/androidx_multiplatform_mac.sh -Pandroidx.lowMemory
