#!/bin/bash
set -e
cd "$(dirname "$0")"

export ANDROIDX_PROJECTS=MAC

export USE_ANDROIDX_REMOTE_BUILD_CACHE=gcp

sharedArgs="-Pandroidx.lowMemory $*"

# Temporary network tests to debug intermittent cache connectivity failures
set -x
# 1. Check DNS resolution time
time nslookup storage.googleapis.com

# 2. Test raw TCP connectivity to standard HTTPS ports
nc -zv storage.googleapis.com 443

# 3. Test latency and transport via curl
curl -w "Time to connect: %{time_connect}\nTime to total: %{time_total}\n" -o /dev/null -s https://storage.googleapis.com

# 4. Check available file descriptors and ephemeral ports
ulimit -n
netstat -an | grep ESTABLISHED | wc -l
set +x

impl/build.sh buildOnServer createAllArchives listTaskOutputs checkExternalLicenses "$sharedArgs"