#!/usr/bin/env bash

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

usage() {
  echo "Usage: ./lsprofile.sh -p packagename"
  echo "      -p is the packagename to check"
  echo ""
  echo "This script displays the sizes of ref and cur profiles."
  exit 1
}

while getopts p: flag; do
  case $flag in
    p)
      PACKAGE=$OPTARG
      ;;
    *)
      usage
      ;;
  esac
done

if [ -z "${PACKAGE}" ]; then
  usage
fi

run() {
  echo -n "cur: "
  adb shell ls -la "/data/misc/profiles/cur/0/${PACKAGE}/primary.prof" | cut -f5 -d" "
  echo -n "ref: "
  adb shell ls -la "/data/misc/profiles/ref/${PACKAGE}/primary.prof" | cut -f5 -d" "
}

run