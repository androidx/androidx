#
# Copyright 2021 The Android Open Source Project
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
#

# CHANGEME:
DEBUG=false

SCRIPT=`realpath $0`
SCRIPT_DIR=`dirname $SCRIPT`
SUPPORT_DIR=$SCRIPT_DIR/../../../../../
TMP_DIR=`mktemp -d`

pushd $SUPPORT_DIR

echo "===START=== Rebuilding apk..."
ANDROIDX_PROJECTS=COMPOSE ./gradlew \
  :profileinstaller:profileinstaller:integration-tests:testapp:clean
if [ $DEBUG = true ]; then
  ANDROIDX_PROJECTS=COMPOSE ./gradlew \
    :profileinstaller:profileinstaller:integration-tests:testapp:assembleDebug
else
  ANDROIDX_PROJECTS=COMPOSE ./gradlew \
    :profileinstaller:profileinstaller:integration-tests:testapp:assembleRelease
fi
echo "===/DONE=== Rebuilding apk..."

echo "===START=== Uninstalling..."
adb uninstall androidx.profileinstaller.integration.testapp
echo "===/DONE=== Uninstalling"

echo "===START=== Repackaging apk..."
if [ $DEBUG = true ]; then
  $SCRIPT_DIR/repackage.py --out $TMP_DIR/out.apk --debug true
else
  $SCRIPT_DIR/repackage.py --out $TMP_DIR/out.apk
fi
echo "===/DONE=== Repackaging apk..."

echo "===START=== Installing apk..."
adb install $TMP_DIR/out.apk > /dev/null
echo "===/DONE=== Installing apk..."

echo "===START=== Installing apk..."
adb shell am start -n androidx.profileinstaller.integration.testapp/.MainActivity
echo "===/DONE==="

echo "===START=== Waiting 10 seconds for profile..."
sleep 10
echo "===/DONE=== Waiting 10 seconds for profile..."

echo "===START=== Force stopping app"
adb shell am force-stop androidx.profileinstaller.integration.testapp
echo "===/DONE=== Force stopping app"

echo "===START=== Root + Remount"
adb root >/dev/null
adb remount 2>/dev/null
echo "===/DONE=== Root + Remount"

echo "Profile found written to cur directory..."
CUR_SIZE=$(adb shell stat -c%s /data/misc/profiles/cur/0/androidx.profileinstaller.integration.testapp/primary.prof 2>/dev/null)
REF_SIZE=$(adb shell stat -c%s /data/misc/profiles/ref/androidx.profileinstaller.integration.testapp/primary.prof 2>/dev/null)
echo "Cur: $CUR_SIZE"
echo "Ref: $REF_SIZE"

echo "===START=== Compile speed-profile"
adb shell cmd package compile -m speed-profile -f androidx.profileinstaller.integration.testapp
echo "===/DONE=== Compile speed-profile"

CUR_SIZE=$(adb shell stat -c%s /data/misc/profiles/cur/0/androidx.profileinstaller.integration.testapp/primary.prof 2>/dev/null)
REF_SIZE=$(adb shell stat -c%s /data/misc/profiles/ref/androidx.profileinstaller.integration.testapp/primary.prof 2>/dev/null)
echo "Cur: $CUR_SIZE"
echo "Ref: $REF_SIZE"

APK_LOCATION=$(adb shell dumpsys package dexopt | grep "\[androidx\.profileinstaller\.integration\.testapp\]" -A1 | tail -n 1 | cut -d':' -f 2)
APK_DIR=$(dirname $APK_LOCATION)

adb shell ls -la $APK_DIR/oat/arm64/

