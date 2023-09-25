#!/bin/bash
set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

# Temporarily disable running checkApi while metalava breaking change is landed
# $SCRIPT_PATH/impl/build-metalava-and-androidx.sh \
#  listTaskOutputs \
#  checkApi
