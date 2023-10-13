#!/bin/bash
set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

# Use this flag to temporarily disable `checkApi`
# while landing Metalava w/ breaking API changes
METALAVA_INTEGRATION_ENFORCED=true

if $METALAVA_INTEGRATION_ENFORCED
then
    $SCRIPT_PATH/impl/build-metalava-and-androidx.sh \
        listTaskOutputs \
        checkApi
fi
