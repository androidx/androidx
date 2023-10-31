#!/bin/bash
set -e

daemonProcessesOutput="$(ps -ef | grep GradleDaemon | grep -v grep)"
echo "Gradle daemon processes: $daemonProcessesOutput"
daemonPids="$(echo $daemonProcessesOutput | sed 's/  */ /g' | cut -d ' ' -f 2)"
echo "Getting stack for processes: $daemonPids"
echo "$daemonPids" | xargs -n 1 jstack
echo "Done getting stack for processes: $daemonPids"
