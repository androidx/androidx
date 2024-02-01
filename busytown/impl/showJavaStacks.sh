#!/bin/bash
set -e

javaProcessesOutput="$(jps -lmv | grep -v jps)"
echo
echo "Outputting java stack information to stdout (see build.log)" >&2
echo "Java processes: $javaProcessesOutput"
echo
javaPids="$(echo "$javaProcessesOutput" | sed 's/ .*//g')"
echo "Getting stack for processes: $javaPids"
echo
for pid in $javaPids; do
  echo jstack "$pid"
  jstack "$pid" || true
  echo
done
echo "Done getting stack for processes: $javaPids"

