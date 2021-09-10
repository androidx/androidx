#!/bin/bash
# Copyright 2017 The Android Open Source Project
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
# limitations under the License.

# A script that runs media2 tests between different versions.

# Usage './runtest.sh <version_combination_number> [option]'

CLIENT_MODULE_NAME_BASE="media2:media2-session:version-compat-tests:client"
SERVICE_MODULE_NAME_BASE="media2:media2-session:version-compat-tests:service"
CLIENT_VERSION=""
SERVICE_VERSION=""
CLIENT_TEST_TARGET=""
SERVICE_TEST_TARGET=""
VERSION_COMBINATION=""
ERROR_CODE=0

function printRunTestUsage() {
  echo "Usage: ./runtest.sh <version_combination_number> [option]"
  echo ""
  echo "Version combination number:"
  echo "    1. Client-ToT             / Service-ToT"
  echo "    2. Client-ToT             / Service-Latest release"
  echo "    3. Client-Latest release  / Service-ToT"
  echo "    4. Run all of the above"
  echo ""
  echo "Option:"
  echo "    -t <class/method>: Only run the specific test class/method."
}

function runTest() {
  echo ">>>>>>>>>>>>>>>>>>>>>>>> Test Started: Client-$CLIENT_VERSION & Service-$SERVICE_VERSION <<<<<<<<<<<<<<<<<<<<<<<<"

  local CUSTOM_OPTIONS="-Pandroid.testInstrumentationRunnerArguments.client_version=$CLIENT_VERSION"
  CUSTOM_OPTIONS="$CUSTOM_OPTIONS -Pandroid.testInstrumentationRunnerArguments.service_version=$SERVICE_VERSION"

  local CLIENT_MODULE_NAME="$CLIENT_MODULE_NAME_BASE$([ "$CLIENT_VERSION" = "tot" ] || echo "-previous")"
  local SERVICE_MODULE_NAME="$SERVICE_MODULE_NAME_BASE$([ "$SERVICE_VERSION" = "tot" ] || echo "-previous")"

  local TEST_DEVICES="$ANDROID_SERIAL"
  # TODO(b/156594425): Remove following check when previous module depends on lowering minSdk to 16
  if [ "$CLIENT_VERSION" = "previous" ] || [ "$SERVICE_VERSION" = "previous" ]; then
    local DEVICES="${ANDROID_SERIAL/,/ }"
    if [[ -z "${DEVICES}" ]]; then
      for DEVICE in $($ADB devices | tail -n +2 | awk '{print $1}'); do
        DEVICES="$DEVICES $DEVICE"
      done
    fi

    TEST_DEVICES=""
    for DEVICE in $DEVICES; do
      if [[ -z "$DEVICE" ]]; then
        continue
      fi
      # Do not use $($ADB shell getprop ro.build.version.sdk) directly.
      # It ends with '\r' on the SDK 16, and cause error in arithmetic comparison.
      DEVICE_SDK_VERSION=$($ADB -s $DEVICE shell getprop ro.build.version.sdk | sed 's/[^0-9]//')
      if ! [[ "$DEVICE_SDK_VERSION" -ge "19" ]]; then
        echo "Skipping test on $DEVICE. Only ToT-ToT is supported on the older device (SDK<19)"
      else
        TEST_DEVICES="$TEST_DEVICES$DEVICE,"
      fi
    done
    if [[ -z "$TEST_DEVICES" ]]; then
      echo "No eligible device for test"
      exit 1
    fi
  fi

  if [[ -n "${TEST_DEVICES}" ]]; then
    TEST_DEVICES="${TEST_DEVICES%,}"
    echo "Running on ${TEST_DEVICES}"
  fi

  echo "Building modules"
  ./gradlew $CLIENT_MODULE_NAME:assembleAndroidTest || { echo "Client build failed. Aborting."; exit 1; }
  ./gradlew $SERVICE_MODULE_NAME:assembleAndroidTest || { echo "Service build failed. Aborting."; exit 1; }

  if [[ -z "$SERVICE_TEST_TARGET" ]]; then
    echo "Running client tests"
    ANDROID_SERIAL=$TEST_DEVICES ./gradlew $SERVICE_MODULE_NAME:installDebugAndroidTest || { echo "Service install failed. Aborting."; exit 1; }
    ANDROID_SERIAL=$TEST_DEVICES ./gradlew $CLIENT_MODULE_NAME:connectedAndroidTest $CLIENT_TEST_TARGET $CUSTOM_OPTIONS || ERROR_CODE=1
  fi

  if [[ -z "$CLIENT_TEST_TARGET" ]]; then
    echo "Running service tests"
    ANDROID_SERIAL=$TEST_DEVICES ./gradlew $CLIENT_MODULE_NAME:installDebugAndroidTest || { echo "Client install failed. Aborting."; exit 1; }
    ANDROID_SERIAL=$TEST_DEVICES ./gradlew $SERVICE_MODULE_NAME:connectedAndroidTest $SERVICE_TEST_TARGET $CUSTOM_OPTIONS || ERROR_CODE=1
  fi

  echo ">>>>>>>>>>>>>>>>>>>>>>>> Test Ended: Client-$CLIENT_VERSION & Service-$SERVICE_VERSION <<<<<<<<<<<<<<<<<<<<<<<<<<"
}


OLD_PWD=$(pwd)

if ! cd "$(echo $OLD_PWD | awk -F'frameworks/support' '{print $1}')"/frameworks/support &> /dev/null
then
  echo "Current working directory is $OLD_PWD"
  echo "Please re-run this script in any folder under frameworks/support."
  exit 1;
fi

if [ "`uname`" == "Darwin" ]; then
  PLATFORM="darwin"
else
  PLATFORM="linux"
fi
ADB="../../prebuilts/fullsdk-$PLATFORM/platform-tools/adb"
if [ ! -f "$ADB" ]; then
  echo "adb not found at $ADB, finding adb in \$PATH..." 1>&2
  command -v adb > /dev/null 2>&1 || { echo "adb not found in \$PATH" 1>&2; exit 1; }
  ADB="adb"
fi

case ${1} in
  1|2|3|4)
    VERSION_COMBINATION=${1}
    shift
    ;;
  *)
    printRunTestUsage
    exit 1;
esac

while (( "$#" )); do
  case ${1} in
    -t)
      if [[ ${2} == *"client"* ]]; then
        CLIENT_TEST_TARGET="-Pandroid.testInstrumentationRunnerArguments.class=${2}"
      elif [[ ${2} == *"service"* ]]; then
        SERVICE_TEST_TARGET="-Pandroid.testInstrumentationRunnerArguments.class=${2}"
      else
        echo "Wrong test class/method name. Aborting."
        echo "It should be in the form of \"<FULL_CLASS_NAME>[#METHOD_NAME]\"."
        exit 1;
      fi
      shift 2
      ;;
    *)
      printRunTestUsage
      exit 1;
  esac
done

case ${VERSION_COMBINATION} in
  1)
     CLIENT_VERSION="tot"
     SERVICE_VERSION="tot"
     runTest
     ;;
  2)
     CLIENT_VERSION="tot"
     SERVICE_VERSION="previous"
     runTest
     ;;
  3)
     CLIENT_VERSION="previous"
     SERVICE_VERSION="tot"
     runTest
     ;;
  4)
     CLIENT_VERSION="tot"
     SERVICE_VERSION="tot"
     runTest

     CLIENT_VERSION="tot"
     SERVICE_VERSION="previous"
     runTest

     CLIENT_VERSION="previous"
     SERVICE_VERSION="tot"
     runTest
     ;;
esac

echo
case ${ERROR_CODE} in
  0)
    echo -e "\033[1;32mTEST SUCCESSFUL\033[0m: All of tests passed."
    ;;
  1)
    echo -e "\033[1;31mTEST FAILED\033[0m: Some of tests failed."
    ;;
esac
exit $ERROR_CODE
