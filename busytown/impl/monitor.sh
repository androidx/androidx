#!/bin/bash
set -e

function usage() {
  echo "Usage: $0 <timeout seconds> <command to run on timeout>"
  echo "Waits for up to <timeout seconds> for the parent process to exit"
  echo "If the parent process hasn't exited in that time, runs the given command"
  exit 1
}

waitSeconds="$1"
shift
runOnTimeout="$*"
count=0
if [ "$waitSeconds" == "" ]; then
  usage
fi
if [ "$runOnTimeout" == "" ]; then
  usage
fi
while true; do
  if ! ps -f "$PPID" >/dev/null 2>/dev/null; then
    # parent process is done, so we don't need to monitor anymore
    exit 0
  fi
  sleep 1
  count="$((count + 1))"
  if [ "$count" -gt "$waitSeconds" ]; then
    separator="#########################################################################################################################"
    echo "$separator" >&2
    echo "Parent process $PPID running longer than the expected $waitSeconds seconds. monitor.sh now running $runOnTimeout" >&2
    echo "$separator" >&2
    bash -c "$runOnTimeout"
    exit 1
  fi
done
