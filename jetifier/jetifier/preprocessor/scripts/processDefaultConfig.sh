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

ROOT_DIR="$(cd $(dirname $0) && pwd)"
OUT_DIR="$ROOT_DIR/out"
TEMP_LOG="$OUT_DIR/tempLog"

CHECKOUT_DIR="$ROOT_DIR/../../../../../.."
JETIFIER_DIR="$ROOT_DIR/../.."
BUILD_DIR="$ROOT_DIR/../../../../../../out/support"
DEFAULT_CONFIG="$JETIFIER_DIR/core/src/main/resources/default.config"
GENERATED_CONFIG="$JETIFIER_DIR/core/src/main/resources/default.generated.config"
PREPROCESSOR_DISTRO_PATH="$BUILD_DIR/jetifier-preprocessor/build/distributions/jetifier-preprocessor.zip"
PREPROCESSOR_BIN_PATH="$OUT_DIR/jetifier-preprocessor/bin/jetifier-preprocessor"
SUPPORT_LIBS_BUILD_NUMBER="4631572"
APP_TOOLKIT_BUILD_NUMBER="4669041"
SUPPORT_LIBS_UNPACKED="$OUT_DIR/supportLibs/unpacked"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

function die() {
	echo -e "${RED}ERROR!${NC} $@"
	exit 1
}

function printOk() {
	echo -e "${GREEN}[OK]${NC} $@"
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

function downloadPackage() {
	PACKAGE=$1
	ARTIFACTS=("${!2}")

	for IN in "${ARTIFACTS[@]}"
	do
	   :
		SPLIT=(${IN//:/ })
		ARTIFACT=${SPLIT[0]}
		VERSION=${SPLIT[1]}
		TYPE=${SPLIT[2]}
		FILE_NAME=`echo "$PACKAGE-$ARTIFACT-$VERSION.$TYPE" | sed -e 's,/,-,g'`
		url="https://dl.google.com/dl/android/maven2/$PACKAGE/$ARTIFACT/$VERSION/$ARTIFACT-$VERSION.$TYPE"
		wget $url -O "$SUPPORT_LIBS_UNPACKED/$FILE_NAME" > /dev/null 2>&1 || die "FAILED to download $url!"
		printOk "Downloaded '$PACKAGE:$ARTIFACT:$VERSION@$TYPE'"
	done
}


rm -rf $OUT_DIR
mkdir $OUT_DIR
mkdir -p $SUPPORT_LIBS_UNPACKED
echo "OUT dir is at '$OUT_DIR'"


DATA_BINDING_VERSION=`curl https://dl.google.com/dl/android/maven2/com/android/databinding/baseLibrary/maven-metadata.xml|xmllint --format -|grep latest|awk '{split($NAME,a,"[><]"); print a[3]}'`
DATABINDING=(
	"baseLibrary:$DATA_BINDING_VERSION:jar"
	"adapters:$DATA_BINDING_VERSION:aar"
	"library:$DATA_BINDING_VERSION:aar"
)
downloadPackage "com/android/databinding" DATABINDING[@]


CONSTRAINT=(
	"constraint-layout:1.1.3:aar"
	"constraint-layout-solver:1.1.3:jar"
)
downloadPackage "com/android/support/constraint" CONSTRAINT[@]


# Support artifacts
SUPPORT=(
	"animated-vector-drawable:28.0.0:aar"
	"appcompat-v7:28.0.0:aar"
	"asynclayoutinflater:28.0.0:aar"
	"car:28.0.0-alpha5:aar"
	"cardview-v7:28.0.0:aar"
	"collections:28.0.0:jar"
	"coordinatorlayout:28.0.0:aar"
	"cursoradapter:28.0.0:aar"
	"customtabs:28.0.0:aar"
	"customview:28.0.0:aar"
	"design:28.0.0:aar"
#	"design-ANY" - obsolete (moved to design)
	"documentfile:28.0.0:aar"
	"drawerlayout:28.0.0:aar"
	"exifinterface:28.0.0:aar"
	"gridlayout-v7:28.0.0:aar"
	"heifwriter:28.0.0:aar"
# 	instantvideo - obsolete
	"interpolator:28.0.0:aar"
	"leanback-v17:28.0.0:aar"
	"loader:28.0.0:aar"
	"localbroadcastmanager:28.0.0:aar"
# 	"media2:28.0.0-alpha03:aar"
	"mediarouter-v7:28.0.0-alpha5:aar"
	"multidex:1.0.3:aar"
	"multidex-instrumentation:1.0.3:aar"
	"palette-v7:28.0.0:aar"
	"percent:28.0.0:aar"
	"preference-leanback-v17:28.0.0:aar"
# 	"preference v14" - empty (merged to v7)
	"preference-v7:28.0.0:aar"
	"print:28.0.0:aar"
	"recommendation:28.0.0:aar"
	"recyclerview-selection:28.0.0:aar"
	"recyclerview-v7:28.0.0:aar"
	"slices-builders:28.0.0:aar"
	"slices-core:28.0.0:aar"
	"slices-view:28.0.0:aar"
	"slidingpanelayout:28.0.0:aar"
	"support-annotations:28.0.0:jar"
	"support-compat:28.0.0:aar"
	"support-content:28.0.0-alpha1:aar"
	"support-core-ui:28.0.0:aar"
	"support-core-utils:28.0.0:aar"
	"support-dynamic-animation:28.0.0:aar"
	"support-emoji:28.0.0:aar"
	"support-emoji-appcompat:28.0.0:aar"
	"support-emoji-bundled:28.0.0:aar"
	"support-fragment:28.0.0:aar"
	"support-media-compat:28.0.0:aar"
	"support-tv-provider:28.0.0:aar"
	"support-v13:28.0.0:aar"
	"support-v4:28.0.0:aar"
	"support-vector-drawable:28.0.0:aar"
	"swiperefreshlayout:28.0.0:aar"
	"transition:28.0.0:aar"
	"versionedparcelable:28.0.0:aar"
	"viewpager:28.0.0:aar"
	"wear:28.0.0:aar"
# 	"wearable" - obsolete
	"webkit:28.0.0:aar"
#	"textclassifier:28.0.0:aar" - not released yet
#	"activity:28.0.0:aar" - not released yet
#	"biometric:28.0.0-alpha03:aar" - not released yet
)
downloadPackage "com/android/support" SUPPORT[@]

ARCH_CORE=(
	"common:1.1.1:jar"
	"core:1.0.0-alpha3:aar"
	"core-testing:1.1.1:aar"
	"runtime:1.1.1:aar"
)
downloadPackage "android/arch/core" ARCH_CORE[@]


ARCH_LIFECYCLE=(
	"common:1.1.1:jar"
	"common-java8:1.1.1:jar"
	"compiler:1.1.1:jar"
	"extensions:1.1.1:aar"
	"livedata:1.1.1:aar"
	"livedata-core:1.1.1:aar"
	"reactivestreams:1.1.1:aar"
	"runtime:1.1.1:aar"
	"viewmodel:1.1.1:aar"
)
downloadPackage "android/arch/lifecycle" ARCH_LIFECYCLE[@]

#TODO: add android.arch.navigation once it gets migrated


ARCH_PAGING=(
	"common:1.0.1:jar"
	"runtime:1.0.1:aar"
	"rxjava2:1.0.1:aar"
)
downloadPackage "android/arch/paging" ARCH_PAGING[@]


ARCH_PERSISTANCE=(
	"db:1.1.1:aar"
	"db-framework:1.1.1:aar"
)
downloadPackage "android/arch/persistence" ARCH_PERSISTANCE[@]


ARCH_ROOM=(
	"common:1.1.1:jar"
	"compiler:1.1.1:jar"
	"guava:1.1.1:aar"
	"migration:1.1.1:jar"
	"runtime:1.1.1:aar"
	"rxjava2:1.1.1:aar"
	"testing:1.1.1:aar"
)
downloadPackage "android/arch/persistence/room" ARCH_ROOM[@]

#TODO: add androidx.arch.work once it gets migrated


TEST=(
	"monitor:1.0.2:aar"
	"rules:1.0.2:aar"
	"runner:1.0.2:aar"
)
downloadPackage "com/android/support/test" TEST[@]


ESPRESSO=(
	# FYI: We skip orchestrator since it is apk
	"espresso-accessibility:3.0.2:aar"
	"espresso-contrib:3.0.2:aar"
	"espresso-core:3.0.2:aar"
	"espresso-idling-resource:3.0.2:aar"
	"espresso-intents:3.0.2:aar"
	"espresso-remote:3.0.2:aar"
	"espresso-web:3.0.2:aar"
)
downloadPackage "com/android/support/test/espresso" ESPRESSO[@]


ESPRESSO_IDLING=(
	"idling-concurrent:3.0.2:aar"
	"idling-net:3.0.2:aar"
)
downloadPackage "com/android/support/test/espresso/idling" ESPRESSO_IDLING[@]

TEST_JANKTESTHELPER=("janktesthelper-v23:1.0.1:aar")
downloadPackage "com/android/support/test/janktesthelper" TEST_JANKTESTHELPER[@]

TEST_UIAUTOMATOR=("uiautomator-v18:2.1.3:aar")
downloadPackage "com/android/support/test/uiautomator" TEST_UIAUTOMATOR[@]

# FYI:
# test-services is skipped skipped as it is an apk
# exposed-instrumentation-api-publish skipped as it is deprecated
# testing-support-lib skipped as it is deprecated

printOk "All artifacts downloaded"

printSectionStart "Preparing Jetifier"
buildProjectUsingGradle $JETIFIER_DIR/../..
printOk "Clean build done"

unzip $PREPROCESSOR_DISTRO_PATH -d $OUT_DIR > /dev/null
printOk "Copied & unziped jetifier preprocessor"

printSectionStart "Preprocessing mappings on support libraries"
sh $PREPROCESSOR_BIN_PATH -i "$SUPPORT_LIBS_UNPACKED" -o "$GENERATED_CONFIG" -c "$DEFAULT_CONFIG" -l verbose || die
printOk "Done, config generated into $GENERATED_CONFIG"

printSuccess
