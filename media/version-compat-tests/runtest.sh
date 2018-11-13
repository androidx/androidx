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
MEDIA_COMPAT_CLIENT_TEST_JAVA_PACKAGE="android.support.mediacompat.client"
MEDIA_COMPAT_SERVICE_TEST_JAVA_PACKAGE="android.support.mediacompat.service"
TEST_VERSIONS=("MaxDepVersions" "MinDepVersions")
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
  for version in "${TEST_VERSIONS[@]}"; do
    local TEST_TASK_NAME="assemble${version}DebugAndroidTest"

    # Build test apks
    ./gradlew $CLIENT_MODULE_NAME:$TEST_TASK_NAME || { echo "Build failed. Aborting."; exit 1; }
    ./gradlew $SERVICE_MODULE_NAME:$TEST_TASK_NAME || { echo "Build failed. Aborting."; exit 1; }

    # Search for the apks.
    # Need to search under out/host instead of out/dist, because MaxDepVersions doesn't drop apks there.
    local CLIENT_APK=$(find ../../out/host -iname "$CLIENT_MODULE_NAME-$version-debug-androidTest.apk")
    local SERVICE_APK=$(find ../../out/host -iname "$SERVICE_MODULE_NAME-$version-debug-androidTest.apk")

    # Install the apks
    adb install -r $CLIENT_APK || { echo "Failed to install $CLIENT_APK. Aborting."; exit 1; }
    adb install -r $SERVICE_APK || { echo "Failed to install $SERVICE_APK. Aborting."; exit 1; }

    # Run the tests
    local test_command="adb shell am instrument -w -e debug false -e client_version $CLIENT_VERSION -e service_version $SERVICE_VERSION"
    local client_test_runner="android.support.mediacompat.client.test/androidx.test.runner.AndroidJUnitRunner"
    local service_test_runner="android.support.mediacompat.service.test/androidx.test.runner.AndroidJUnitRunner"

    echo ">>>>>>>>>>>>>>>>>>>>>>>> Test Started: Client-$CLIENT_VERSION & Service-$SERVICE_VERSION (Version=$version) <<<<<<<<<<<<<<<<<<<<<<<<"

    if [[ $OPTION_TEST_TARGET == *"client"* ]]; then
      ${test_command} $OPTION_TEST_TARGET ${client_test_runner}
    elif [[ $OPTION_TEST_TARGET == *"service"* ]]; then
      ${test_command} $OPTION_TEST_TARGET ${service_test_runner}
    else
      # Since there is no MediaSession APIs in previous support library, don't run the test.
      # Instead, only run mediacompat tests.
      if [[ $CLIENT_VERSION != "tot" || $SERVICE_VERSION != "tot" ]]; then
        ${test_command} "-e package $MEDIA_COMPAT_CLIENT_TEST_JAVA_PACKAGE" ${client_test_runner}
        ${test_command} "-e package $MEDIA_COMPAT_SERVICE_TEST_JAVA_PACKAGE" ${service_test_runner}
      else
        ${test_command} ${client_test_runner}
        ${test_command} ${service_test_runner}
      fi
    fi

    echo ">>>>>>>>>>>>>>>>>>>>>>>> Test Ended: Client-$CLIENT_VERSION & Service-$SERVICE_VERSION (Version=$version) <<<<<<<<<<<<<<<<<<<<<<<<<<"
  done
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
