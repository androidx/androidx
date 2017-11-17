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
#  - The test result should be easily seen. (Can we report the results to the Sponge?)
#  - Run specific combination of the test (e.g. Only want to test ToT-ToT)
#  - Run specific test class / method by using argument.
#  - Support simultaneous multiple device connection

# Usage './runtest.sh'

CLIENT_MODULE_NAME_BASE="support-media-compat-test-client"
SERVICE_MODULE_NAME_BASE="support-media-compat-test-service"
CLIENT_VERSION=""
SERVICE_VERSION=""

function runTest() {
  echo "Running test: Client-$CLIENT_VERSION / Service-$SERVICE_VERSION"

  local CLIENT_MODULE_NAME="$CLIENT_MODULE_NAME_BASE$([ "$CLIENT_VERSION" = "tot" ] || echo "-previous")"
  local SERVICE_MODULE_NAME="$SERVICE_MODULE_NAME_BASE$([ "$SERVICE_VERSION" = "tot" ] || echo "-previous")"

  # Build test apks
  ./gradlew $CLIENT_MODULE_NAME:assembleDebugAndroidTest || (echo "Build failed. Aborting."; return 1)
  ./gradlew $SERVICE_MODULE_NAME:assembleDebugAndroidTest || (echo "Build failed. Aborting."; return 1)

  # Install the apks
  adb install -r -d "../../out/dist/$CLIENT_MODULE_NAME.apk" || (echo "Apk installation failed. Aborting."; return 1)
  adb install -r -d "../../out/dist/$SERVICE_MODULE_NAME.apk" || (echo "Apk installation failed. Aborting."; return 1)

  # Run the tests
  echo ">>>>>>>>>>>>>>>>>>>>>>>> Test Started: Client-$CLIENT_VERSION & Service-$SERVICE_VERSION <<<<<<<<<<<<<<<<<<<<<<<<"
  adb shell am instrument -w -r -e package android.support.mediacompat.client -e debug false -e client_version $CLIENT_VERSION \
     -e service_version $SERVICE_VERSION android.support.mediacompat.client.test/android.support.test.runner.AndroidJUnitRunner
  adb shell am instrument -w -r -e package android.support.mediacompat.service -e debug false -e client_version $CLIENT_VERSION \
     -e service_version $SERVICE_VERSION android.support.mediacompat.service.test/android.support.test.runner.AndroidJUnitRunner
  echo ">>>>>>>>>>>>>>>>>>>>>>>> Test Ended: Client-$CLIENT_VERSION & Service-$SERVICE_VERSION <<<<<<<<<<<<<<<<<<<<<<<<<<"
}


OLD_PWD=$(pwd)
if [[ $OLD_PWD != *"frameworks/support"* ]]; then
  echo "Current working directory is" $OLD_PWD.
  echo "Please re-run this script in any folder under frameworks/support."
  exit 1;
else
  # Change working directory to frameworks/support
  cd "$(echo $OLD_PWD | awk -F'frameworks/support' '{print $1}')"/frameworks/support
fi

echo "Choose the support library versions of the test you want to run:"
echo "    1. Client-ToT             / Service-ToT"
echo "    2. Client-ToT             / Service-Latest release"
echo "    3. Client-Latest release  / Service-ToT"
echo "    4. Run all of the above"
printf "Pick one of them: "

read ANSWER
case $ANSWER in
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
