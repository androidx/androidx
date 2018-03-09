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

# A script that runs media-compat-test between different versions.
#
# Preconditions:
#  - Exactly one test device should be connected.
#
# TODO:
#  - Support simultaneous multiple device connection

# Usage './runtest.sh <version_combination_number> [option]'

CLIENT_MODULE_NAME_BASE="support-media-compat-test-client"
SERVICE_MODULE_NAME_BASE="support-media-compat-test-service"
CLIENT_VERSION=""
SERVICE_VERSION=""
OPTION_TEST_TARGET=""

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
  echo "Running test: Client-$CLIENT_VERSION / Service-$SERVICE_VERSION"

  local CLIENT_MODULE_NAME="$CLIENT_MODULE_NAME_BASE$([ "$CLIENT_VERSION" = "tot" ] || echo "-previous")"
  local SERVICE_MODULE_NAME="$SERVICE_MODULE_NAME_BASE$([ "$SERVICE_VERSION" = "tot" ] || echo "-previous")"

  # Build test apks
  ./gradlew $CLIENT_MODULE_NAME:assembleDebugAndroidTest || { echo "Build failed. Aborting."; exit 1; }
  ./gradlew $SERVICE_MODULE_NAME:assembleDebugAndroidTest || { echo "Build failed. Aborting."; exit 1; }

  # Install the apks
  adb install -r -d "../../out/dist/$CLIENT_MODULE_NAME.apk" || { echo "Apk installation failed. Aborting."; exit 1; }
  adb install -r -d "../../out/dist/$SERVICE_MODULE_NAME.apk" || { echo "Apk installation failed. Aborting."; exit 1; }

  # Run the tests
  local test_command="adb shell am instrument -w -e debug false -e client_version $CLIENT_VERSION -e service_version $SERVICE_VERSION"
  local client_test_runner="android.support.mediacompat.client.test/android.support.test.runner.AndroidJUnitRunner"
  local service_test_runner="android.support.mediacompat.service.test/android.support.test.runner.AndroidJUnitRunner"

  echo ">>>>>>>>>>>>>>>>>>>>>>>> Test Started: Client-$CLIENT_VERSION & Service-$SERVICE_VERSION <<<<<<<<<<<<<<<<<<<<<<<<"

  if [[ $OPTION_TEST_TARGET == *"client"* ]]; then
    ${test_command} $OPTION_TEST_TARGET ${client_test_runner}
  elif [[ $OPTION_TEST_TARGET == *"service"* ]]; then
    ${test_command} $OPTION_TEST_TARGET ${service_test_runner}
  else
    ${test_command} ${client_test_runner}
    ${test_command} ${service_test_runner}
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

if [[ $# -eq 0 || $1 -le 0 || $1 -gt 4 ]]
then
  printRunTestUsage
  exit 1;
fi

if [[ ${2} == "-t" ]]; then
  if [[ ${3} == *"client"* || ${3} == *"service"* ]]; then
    OPTION_TEST_TARGET="-e class ${3}"
  else
    echo "Wrong test class/method name. Aborting."
    echo "It should be in the form of \"<FULL_CLASS_NAME>[#METHOD_NAME]\"."
    exit 1;
  fi
fi

case ${1} in
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
