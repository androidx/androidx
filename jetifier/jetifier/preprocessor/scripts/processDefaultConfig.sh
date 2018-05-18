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

CHECKOUT_DIR="$ROOT_DIR/../../../../../.."
JETIFIER_DIR="$ROOT_DIR/../.."
BUILD_DIR="$ROOT_DIR/../../../../../../out/host/gradle/frameworks/support"
DEFAULT_CONFIG="$JETIFIER_DIR/core/src/main/resources/default.config"
GENERATED_CONFIG="$JETIFIER_DIR/core/src/main/resources/default.generated.config"
PREPROCESSOR_DISTRO_PATH="$BUILD_DIR/jetifier-preprocessor/build/distributions/jetifier-preprocessor.zip"
PREPROCESSOR_BIN_PATH="$OUT_DIR/jetifier-preprocessor/bin/jetifier-preprocessor"
SUPPORT_LIBS_BUILD_NUMBER="4631572"
APP_TOOLKIT_BUILD_NUMBER="4669041"
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
	"$FETCH_ARTIFACT" --bid "$SUPPORT_LIBS_BUILD_NUMBER" --target support_library "top-of-tree-m2repository-$SUPPORT_LIBS_BUILD_NUMBER.zip" "$SUPPORT_LIBS_DOWNLOADED/support-lib.zip"
	"$FETCH_ARTIFACT" --bid "$APP_TOOLKIT_BUILD_NUMBER" --target support_library_app_toolkit "top-of-tree-m2repository-$APP_TOOLKIT_BUILD_NUMBER.zip" "$SUPPORT_LIBS_DOWNLOADED/arch.zip"
	cd -


	unzip -oj "$SUPPORT_LIBS_DOWNLOADED/support-lib.zip" -d "$SUPPORT_LIBS_UNPACKED"
	unzip -oj "$SUPPORT_LIBS_DOWNLOADED/arch.zip" -d "$SUPPORT_LIBS_UNPACKED"
	#find "$CHECKOUT_DIR/prebuilts/maven_repo/android/com/android/support/" -type f -name "*design-*28.0.0*.aar" -exec cp '{}' -t "$SUPPORT_LIBS_UNPACKED" \;
	cp "$CHECKOUT_DIR/prebuilts/maven_repo/android/com/android/support/multidex/1.0.3/multidex-1.0.3.aar" "$SUPPORT_LIBS_UNPACKED/multidex.aar"
	cp "$CHECKOUT_DIR/prebuilts/maven_repo/android/com/android/support/multidex-instrumentation/1.0.3/multidex-instrumentation-1.0.3.aar" "$SUPPORT_LIBS_UNPACKED/multidex-instrumentation.aar"
	find "$SUPPORT_LIBS_UNPACKED" -type f -name "jetifier*" -exec rm -f {} \;
}

DATA_BINDING_VERSION=`curl https://dl.google.com/dl/android/maven2/com/android/databinding/baseLibrary/maven-metadata.xml|xmllint --format -|grep latest|awk '{split($NAME,a,"[><]"); print a[3]}'`
function pullDataBinding() {
	NAME=$1
	TYPE=$2
	curl "https://dl.google.com/dl/android/maven2/com/android/databinding/$NAME/$DATA_BINDING_VERSION/$NAME-$DATA_BINDING_VERSION.$TYPE" -o "$SUPPORT_LIBS_UNPACKED/databinding-$NAME.$TYPE"
}

function pullConstraint() {
	NAME=$1
	TYPE=$2
	curl "https://dl.google.com/dl/android/maven2/com/android/support/constraint/$NAME/1.1.0/$NAME-1.1.0.$TYPE" -o "$SUPPORT_LIBS_UNPACKED/$NAME.$TYPE"
}

function pullTest() {
	NAME=$1
	curl "https://dl.google.com/dl/android/maven2/com/android/support/test/$NAME/1.0.2/$NAME-1.0.2.aar" -o "$SUPPORT_LIBS_UNPACKED/$NAME.aar"
}
# Unfortunately this doesn't make a coffee using a lever machine. It only downloads espresso artifacts.
function pullEspresso() {
	NAME=$1
	curl "https://dl.google.com/dl/android/maven2/com/android/support/test/espresso/$NAME/3.0.2/$NAME-3.0.2.aar" -o "$SUPPORT_LIBS_UNPACKED/$NAME.aar"
}
function pullEspressoIdling() {
	NAME=$1
	curl "https://dl.google.com/dl/android/maven2/com/android/support/test/espresso/idling/$NAME/3.0.2/$NAME-3.0.2.aar" -o "$SUPPORT_LIBS_UNPACKED/$NAME.aar"
}

getPreRenamedSupportLib
pullDataBinding "baseLibrary" "jar"
pullDataBinding "adapters" "aar"
pullDataBinding "library" "aar"
pullConstraint "constraint-layout" "aar"
pullConstraint "constraint-layout-solver" "jar"

pullTest "monitor"
pullTest "rules"
pullTest "runner"
# FYI: We skip orchestrator since it is apk
pullEspresso "espresso-accessibility"
pullEspresso "espresso-contrib"
pullEspresso "espresso-core"
pullEspresso "espresso-idling-resource"
pullEspresso "espresso-intents"
pullEspresso "espresso-remote"
pullEspresso "espresso-web"
pullEspressoIdling "idling-concurrent"
pullEspressoIdling "idling-net"
curl "https://dl.google.com/dl/android/maven2/com/android/support/test/janktesthelper/janktesthelper-v23/1.0.1/janktesthelper-v23-1.0.1.aar" -o "$SUPPORT_LIBS_UNPACKED/janktesthelper-v23.aar"
curl "https://dl.google.com/dl/android/maven2/com/android/support/test/uiautomator/uiautomator-v18/2.1.3/uiautomator-v18-2.1.3.aar" -o "$SUPPORT_LIBS_UNPACKED/uiautomator-v18.aar"
# FYI: We skip test-services since it is apk

# exposed-instrumentation-api-publish skipped as it is deprecated
# testing-support-lib skipped as it is deprecated

printSectionStart "Preparing Jetifier"
buildProjectUsingGradle $JETIFIER_DIR/../..
echo "[OK] Clean build done"

unzip $PREPROCESSOR_DISTRO_PATH -d $OUT_DIR > /dev/null
echo "[OK] Copied & unziped jetifier preprocessor"

printSectionStart "Preprocessing mappings on support libraries"
sh $PREPROCESSOR_BIN_PATH -i "$SUPPORT_LIBS_UNPACKED" -o "$GENERATED_CONFIG" -c "$DEFAULT_CONFIG" -l verbose || die
echo "[OK] Done, config generated into $GENERATED_CONFIG"

printSuccess
