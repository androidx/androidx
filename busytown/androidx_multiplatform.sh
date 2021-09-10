#!/bin/bash
set -e

cd "$(dirname $0)"

# Disabled due to b/185938795
./androidx.sh -Pandroidx.compose.multiplatformEnabled=true "$@"
