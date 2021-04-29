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

SCRIPT=`realpath $0`
SCRIPT_DIR=`dirname $SCRIPT`
SUPPORT_DIR=$SCRIPT_DIR/../../../../../
TMP_DIR=`mktemp -d`

if [ $# -eq 0 ]; then
  echo "Usage: build_bundle_launch.sh [outfile-for-installed-profile]"
  echo
  echo -e "\t e.g. build_bundle_launch.sh out.prof"
  exit 1
fi

echo -n "Rebuilding apk..."
$SUPPORT_DIR/gradlew \
  :profileinstaller:profileinstaller:integration-tests:testapp:clean \
  :profileinstaller:profileinstaller:integration-tests:testapp:assembleDebug \
  > /dev/null
echo "done"
adb uninstall androidx.profileinstaller.integration.testapp >/dev/null
$SCRIPT_DIR/repackage.py --out $TMP_DIR/out.apk
adb install $TMP_DIR/out.apk > /dev/null
adb shell am start -n androidx.profileinstaller.integration.testapp/.MainActivity > /dev/null

echo -en "Waiting 10 seconds for profile..."
sleep 20
echo "and copying"

adb shell am force-stop androidx.profileinstaller.integration.testapp

adb root >/dev/null
adb remount 2>/dev/null

# pull the transcoded file to check offline
pushd $TMP_DIR >/dev/null
adb pull /data/misc/profiles/cur/0/androidx.profileinstaller.integration.testapp/primary.prof \
    >/dev/null
src_file=`realpath primary.prof`
popd >/dev/null

if [ ! -f $src_file ]; then
  echo "🛑  profile failed to write, aborting 🪲"
  rm -rf $TMP_DIR
  exit 1
else
  cp $src_file $1 >/dev/null
  echo "Pulled: $1 (transcoded profile profileinstaller wrote)"
  rm -rf $TMP_DIR
fi;

echo -en "Running AOT..."
adb shell cmd package compile -m speed-profile -f androidx.profileinstaller.integration.testapp \
    >/dev/null
echo "done"

refout=$(adb shell ls /data/misc/profiles/ref/androidx.profileinstaller.integration.testapp/)
if [ -z $refout ]; then
  echo "🛑  After AOT ls of reference directory was EMPTY 🪲"
else
  echo $refout
fi;

