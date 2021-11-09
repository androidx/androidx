#!/bin/bash
set -e

query="$1"
filepath="$2"
grepOptions="$3"
if [ "$query" == "" ]; then
  echo "Usage: vgrep.sh <query> <log>"
  echo "Uses grep to search for <query> in <log>, and inverts the return code"
  echo "Additionally, if the query is not found, displays the bottom of the log file"
  exit 2
fi

if grep $grepOptions "$1" "$filepath"; then
  exit 1
fi
tail -n 40 "$filepath"
exit 0
