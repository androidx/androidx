#! /bin/bash
# helper script to build importMaven and execute with the given arguments.
set -e
WORKING_DIR=`pwd`
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
# build importMaven
(cd $SCRIPT_DIR && ./gradlew installDist -q)
# execute the output binary
(SUPPORT_REPO=$SCRIPT_DIR/../.. $SCRIPT_DIR/build/install/importMaven/bin/importMaven $@)