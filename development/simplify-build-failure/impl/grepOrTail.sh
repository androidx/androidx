#!/bin/bash

# usage: grepOrTail.sh <query> <filepath>
# This script searches for the given query inside the given file
# If the query is not found, then this script displays the bottom of the file and then fails

query="$1"
filepath="$2"

if grep -C 10 "$query" "$filepath"; then
  exit 0
fi

tail -n 40 "$filepath"
exit 1
