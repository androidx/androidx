#!/bin/bash
set -e

query="$1"
if [ "$query" == "" ]; then
  echo "Usage: vgrep.sh <query>"
  echo "Runs grep and inverts the return code"
  exit 2
fi

if grep "$1"; then
  exit 1
fi
exit 0
