
#
# Copyright 2026 The Android Open Source Project
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

OS=$(uname -a | awk '{print $1}')
if [ "$OS" != "Linux" ]; then
  echo "This script must be run on cloudtop"
fi

# Instructions for how to update the build id can be found here: go/androidx/playbook#update-aswb
NEW_ASWB_BUILD_ID=${1:-"16488178"}

function sedInPlace() {
  TEMP_FILE=".sedOutput.tmp"
  sed "$1" $2 > $TEMP_FILE
  cat $TEMP_FILE > $2
  rm $TEMP_FILE
}

echo "Updating ASwB"
CURRENT_ASWB_BUILD_ID=`sed -n "s/^aswbBuildId = \"\([^\"]*\)\"/\1/p" gradle/libs.versions.toml`

function checkGcloudPermissions() {
  local bucket="gs://androidx-aswb-binaries"
  local test_file=".upload_test_$(date +%s)"

  # 1. Check if the user is authenticated and the token is valid
  if ! gcloud auth print-access-token > /dev/null 2>&1; then
    echo "ERROR: Your gcloud credentials are missing or expired."
    echo "Please run: gcloud auth login"
    return 1
  fi

  # 2. Check upload permissions by transferring a zero-byte temporary file
  touch "$test_file"
  if gcloud storage cp "$test_file" "$bucket/$test_file" > /dev/null 2>&1; then
    # Cleanup remote and local artifacts
    gcloud storage rm "$bucket/$test_file" > /dev/null 2>&1
    rm -f "$test_file"
    echo "GCS Upload permissions verified for $bucket"
    return 0
  else
    rm -f "$test_file"
    echo "ERROR: You do not have permission to upload to $bucket."
    echo "Please ensure your active account has the 'Storage Object Creator' role on this bucket."
    return 1
  fi
}

if [ "$CURRENT_ASWB_BUILD_ID" == "$NEW_ASWB_BUILD_ID" ]; then
  echo "No change in ASwB build id - update not occurring"
else
  echo "Updating ASwB build id from '$CURRENT_ASWB_BUILD_ID' to '$NEW_ASWB_BUILD_ID'"

  # if the user doesn't have permission to upload to GCS, we don't want to proceed with downloading
  # artifacts.
  checkGcloudPermissions || exit 1

  sedInPlace "s/aswbBuildId = \".*/aswbBuildId = \"$NEW_ASWB_BUILD_ID\"/g" gradle/libs.versions.toml

  # make a temp directory
  mkdir tmp-aswb-install && cd tmp-aswb-install

  ASWB_FULL_VERSION_LINUX=$(rapture showpkg :android-studio-with-blaze-canary | grep "Version:" | grep "$NEW_ASWB_BUILD_ID" | awk '{print $2}' | head -n 1)
  ASWB_FULL_VERSION_MAC=$(rapture -u=corp-mule showpkg :android-studio-with-blaze-canary | grep "Version:" | grep "$NEW_ASWB_BUILD_ID" | awk '{print $2}' | head -n 1)

  # download the package from rapture
  rapture download :android-studio-with-blaze-canary=$ASWB_FULL_VERSION_LINUX
  rapture -u=corp-mule download :android-studio-with-blaze-canary=${ASWB_FULL_VERSION_MAC}

  # rename. This is for use by the ASWB task
  mv *.deb android-studio-with-blaze-canary-$NEW_ASWB_BUILD_ID.deb
  mv *.burrito android-studio-with-blaze-canary-$NEW_ASWB_BUILD_ID.burrito

  # upload to gcs
  gcloud storage cp android-studio-with-blaze-canary-$NEW_ASWB_BUILD_ID.deb "gs://androidx-aswb-binaries/glinux"
  gcloud storage cp android-studio-with-blaze-canary-$NEW_ASWB_BUILD_ID.burrito "gs://androidx-aswb-binaries/mac"

  cd .. && rm -rf tmp-aswb-install
fi

echo "Update completed, please raise a CL with the change in libs.versions.toml"
