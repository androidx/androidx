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

# this function is used in update_aswb.sh and update_studio.sh for updating the aswb version
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
