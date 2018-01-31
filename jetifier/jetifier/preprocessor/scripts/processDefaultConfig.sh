#!/bin/sh

# Copyright (C) 2017 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

# WHAT IT DOES
# Grabs all the support libraries and runs them through a preprocessor
# using the default Jetifier config to generate the final mappings.

ROOT_DIR=$(dirname $(readlink -f $0))
OUT_DIR="$ROOT_DIR/out"
TEMP_LOG="$OUT_DIR/tempLog"

JETIFIER_DIR="$ROOT_DIR/../.."
BUILD_DIR="$ROOT_DIR/../../../../../../out/host/gradle/frameworks/support/jetifier"
DEFAULT_CONFIG="$JETIFIER_DIR/core/src/main/resources/default.config"
GENERATED_CONFIG="$JETIFIER_DIR/core/src/main/resources/default.generated.config"
PREPROCESSOR_DISTRO_PATH="$BUILD_DIR/preprocessor/build/distributions/preprocessor-1.0.zip"
PREPROCESSOR_BIN_PATH="$OUT_DIR/preprocessor-1.0/bin/preprocessor"
SUPPORT_LIBS_DOWNLOADED="$OUT_DIR/supportLibs"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

function exitAndFail() {
	cat $TEMP_LOG
	echo -e "${RED}FAILED${NC}"
	exit 1
}

function printSectionStart() {
	echo ""
	echo "======================================================"
	echo "$1"
	echo "======================================================"
}

function printSuccess() {
	echo -e "${GREEN}SUCCESS${NC}"
}

function buildProjectUsingGradle() {
	cd $1
	sh gradlew clean build $2 > $TEMP_LOG --stacktrace || exitAndFail 2>&1
}


rm -r $OUT_DIR
mkdir $OUT_DIR
echo "OUT dir is at '$OUT_DIR'"

printSectionStart "Downloading all affected support libraries"
wget -nd -i $ROOT_DIR/repo-links -P $SUPPORT_LIBS_DOWNLOADED

printSectionStart "Preparing Jetifier"
buildProjectUsingGradle $JETIFIER_DIR
echo "[OK] Clean build done"

unzip $PREPROCESSOR_DISTRO_PATH -d $OUT_DIR > /dev/null
echo "[OK] Copied & unziped jetifier preprocessor"

printSectionStart "Preprocessing mappings on support libraries"
sh $PREPROCESSOR_BIN_PATH -i "$SUPPORT_LIBS_DOWNLOADED" -o "$GENERATED_CONFIG" -c "$DEFAULT_CONFIG" -l verbose || exitAndFail
echo "[OK] Done, config generated into $GENERATED_CONFIG"

printSuccess
