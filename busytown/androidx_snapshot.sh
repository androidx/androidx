#!/bin/bash
set -e

cd $(dirname $0)

SNAPSHOT=true ./build.sh --no-daemon createArchive --offline
