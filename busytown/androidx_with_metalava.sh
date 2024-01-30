#!/bin/bash
set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

# Use this flag to temporarily disable `checkApi`
# while landing Metalava w/ breaking API changes
DURING_METALAVA_UPDATE=false

if [ ! $DURING_METALAVA_UPDATE ]
then
$SCRIPT_PATH/impl/build-metalava-and-androidx.sh \
  listTaskOutputs \
  checkApi
fi
