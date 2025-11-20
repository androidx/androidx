#!/bin/bash
set -e

SCRIPT_DIR="$(cd $(dirname $0) && pwd)"

PREBUILTS_DIR="$SCRIPT_DIR/../../../../prebuilts"

# Determine OS and architecture
OS=$(uname -s)
ARCH=$(uname -m)

# Select the appropriate JDK path
case "$OS:$ARCH" in
  Linux:x86_64)
    JDK_PATH="$PREBUILTS_DIR/jdk/jdk21/linux-x86/bin"
    ;;
  Darwin:x86_64)
    JDK_PATH="$PREBUILTS_DIR/jdk/jdk21/darwin-x86/bin"
    ;;
  Darwin:arm64)
    JDK_PATH="$PREBUILTS_DIR/jdk/jdk21/darwin-arm64/bin"
    ;;
  *)
    echo "Unsupported OS/architecture: $OS:$ARCH" >&2
    exit 1
    ;;
esac

# Use the correct jps and jstack
jps="$JDK_PATH/jps"
jstack="$JDK_PATH/jstack"

javaProcessesOutput="$($jps -lmv | grep -v jps)"
echo
echo "Outputting java stack information to stdout (see build.log)" >&2
echo "Java processes: $javaProcessesOutput"
echo
javaPids="$(echo "$javaProcessesOutput" | sed 's/ .*//g')"
echo "Getting stack for processes: $javaPids"
echo
for pid in $javaPids; do
  echo "$jstack $pid"
  "$jstack" "$pid" || true
  echo
done
echo "Done getting stack for processes: $javaPids"
