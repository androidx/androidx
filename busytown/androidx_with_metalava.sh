#!/bin/bash
set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

$SCRIPT_PATH/impl/build-metalava-and-androidx.sh \
  listTaskOutputs \
  checkApi
