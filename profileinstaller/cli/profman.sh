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
  echo "Usage: ./profman.sh -p packagename [-c] [-r]"
  echo "      -p is the packagename to check"
  echo "      -c display the cur profile"
  echo "      -r display the ref profile"
  echo ""
  echo "Dump the profile in cur or ref (or both)"
  exit 1
}

CUR=0
REF=0
while getopts crp: flag; do
  case $flag in
    p)
      PACKAGE=$OPTARG
      ;;
    c)
      CUR=1
      ;;
    r)
      REF=1
      ;;
    *)
      usage
      ;;
  esac
done

if [ -z "${PACKAGE}" ]; then
  usage
fi

if [[ $CUR -gt 0 ]]; then
  adb shell profman --dump-only --profile-file="/data/misc/profiles/ref/${PACKAGE}/primary.prof"
fi

if [[ $REF -gt 0 ]]; then
  adb shell profman --dump-only --profile-file="/data/misc/profiles/cur/0/${PACKAGE}/primary.prof"
fi