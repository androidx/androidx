#!/bin/bash
set -e
SCRIPT_PATH="$(cd $(dirname $0) && pwd)"

# Use this flag to temporarily disable `checkApi`
# while landing Metalava w/ breaking API changes
METALAVA_INTEGRATION_ENFORCED=false

# The default targets to build if no arguments
# are provided on the command line.
DEFAULT_TARGETS=" \
  listTaskOutputs \
  checkApi \
  "

if $METALAVA_INTEGRATION_ENFORCED
then
  # If no arguments are provided on the command line
  # then use the defaults otherwise pass the command
  # line arguments through.
  $SCRIPT_PATH/impl/build-metalava-and-androidx.sh \
    ${1:-$DEFAULT_TARGETS} \
    "${@:2}"
fi
