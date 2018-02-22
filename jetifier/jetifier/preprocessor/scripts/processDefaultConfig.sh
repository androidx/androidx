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

set -e

ROOT_DIR=$(dirname $(readlink -f $0))
OUT_DIR="$ROOT_DIR/out"
TEMP_LOG="$OUT_DIR/tempLog"

JETIFIER_DIR="$ROOT_DIR/../.."
BUILD_DIR="$ROOT_DIR/../../../../../../out/host/gradle/frameworks/support"
DEFAULT_CONFIG="$JETIFIER_DIR/core/src/main/resources/default.config"
GENERATED_CONFIG="$JETIFIER_DIR/core/src/main/resources/default.generated.config"
PREPROCESSOR_DISTRO_PATH="$BUILD_DIR/jetifier-preprocessor/build/distributions/jetifier-preprocessor.zip"
PREPROCESSOR_BIN_PATH="$OUT_DIR/jetifier-preprocessor/bin/jetifier-preprocessor"
SUPPORT_LIBS_BUILD_NUMBER="4560478"
SUPPORT_LIBS_DOWNLOADED="$OUT_DIR/supportLibs/downloaded"
SUPPORT_LIBS_UNPACKED="$OUT_DIR/supportLibs/unpacked"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

function die() {
	echo "$@"
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
	sh gradlew :jetifier-preprocessor:clean :jetifier-preprocessor:uploadArchives $2 > $TEMP_LOG --stacktrace
}


rm -rf $OUT_DIR
mkdir $OUT_DIR
echo "OUT dir is at '$OUT_DIR'"

function getPreRenamedSupportLib() {
	INPUT_FILENAME="top-of-tree-m2repository-$SUPPORT_LIBS_BUILD_NUMBER.zip"
	printSectionStart "Downloading all affected support libraries"
	mkdir -p "$SUPPORT_LIBS_DOWNLOADED"

	if [ "$FETCH_ARTIFACT" == "" ]; then
		if which fetch_artifact; then
			FETCH_ARTIFACT="$(which fetch_artifact)"
		fi
	fi
	if [ ! -f "$FETCH_ARTIFACT" ]; then
		die "fetch_artifact not found. Please set the environment variable FETCH_ARTIFACT equal to the path of fetch_artifact and try again"
	fi

	cd "$SUPPORT_LIBS_DOWNLOADED"
	"$FETCH_ARTIFACT" --bid "$SUPPORT_LIBS_BUILD_NUMBER" --target support_library "$INPUT_FILENAME" "$SUPPORT_LIBS_DOWNLOADED/support-lib-${SUPPORT_LIBS_BUILD_NUMBER}.zip"
	"$FETCH_ARTIFACT" --bid "$SUPPORT_LIBS_BUILD_NUMBER" --target support_library_app_toolkit "$INPUT_FILENAME" "$SUPPORT_LIBS_DOWNLOADED/arch-${SUPPORT_LIBS_BUILD_NUMBER}.zip"
	cd -


	unzip -oj "$SUPPORT_LIBS_DOWNLOADED/support-lib-${SUPPORT_LIBS_BUILD_NUMBER}.zip" -d "$SUPPORT_LIBS_UNPACKED"
	unzip -oj "$SUPPORT_LIBS_DOWNLOADED/arch-${SUPPORT_LIBS_BUILD_NUMBER}.zip" -d "$SUPPORT_LIBS_UNPACKED"
}
getPreRenamedSupportLib

printSectionStart "Preparing Jetifier"
buildProjectUsingGradle $JETIFIER_DIR/../..
echo "[OK] Clean build done"

unzip $PREPROCESSOR_DISTRO_PATH -d $OUT_DIR > /dev/null
echo "[OK] Copied & unziped jetifier preprocessor"

printSectionStart "Preprocessing mappings on support libraries"
sh $PREPROCESSOR_BIN_PATH -i "$SUPPORT_LIBS_UNPACKED" -o "$GENERATED_CONFIG" -c "$DEFAULT_CONFIG" -l verbose || die
echo "[OK] Done, config generated into $GENERATED_CONFIG"

printSuccess
