#!/bin/bash

function usage() {
  echo "usage: $0 <gradle_arguments>"
  echo
  echo "Runs the ktlint Gradle task with the provided gradle_arguments, if any of the files to check with .kt(s). Specify files to check using --file=<path> arguments."
  exit 1
}

if [ "$1" == "" ]; then
  usage
fi

PROJECT_ROOT=$(dirname "$0")/..

if echo "$@" | tr ' ' '\n' | grep -q "^--file=.\+\.kts\?$"; then
  exec "$PROJECT_ROOT"/gradlew -q -p "$PROJECT_ROOT" --continue :ktlintCheckFile --configuration-cache "$@"
fi
