#!/bin/bash

function usage() {
  echo "usage: $0 [--skip-if-empty] <gradle_arguments>"
  echo
  echo "Runs the ktfmt Gradle task with the provided gradle_arguments, if any of the files to check with .kt(s). Specify files to check using --file=<path> arguments."
  echo
  echo "--skip-if-empty: don't output a usage error if no arguments are provided"
  exit 1
}

if [ "$1" == "" ]; then
  usage
fi

if [ "$1" == "--skip-if-empty" ]; then
  shift
fi

PROJECT_ROOT=$(dirname "$0")/..


if echo "$@" | tr ' ' '\n' | grep -q "^--file=.\+\.kts\?$"; then
  exec "$PROJECT_ROOT"/gradlew -q -p "$PROJECT_ROOT" --continue :ktCheckFile  --configuration-cache "$@"
fi
