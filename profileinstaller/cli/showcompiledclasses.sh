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
  echo "Usage: ./showcompiledclasses.sh -p packagename [-c]"
  echo "      -p is the packagename to check"
  echo "      -c just display the counts"
  echo ""
  echo "This script greps the odex file for a package and prints the classes have been\
        compiled by art."
  exit 1
}

COUNT=0
while getopts cp: flag; do
  case $flag in
    p)
      PACKAGE=$OPTARG
      ;;
    c)
      COUNT=1
      ;;
    *)
      usage
      ;;
  esac
done

if [ -z "${PACKAGE}" ]; then
  usage
fi

OAT=$(adb shell dumpsys package dexopt | grep -A 1 $PACKAGE | grep status | cut -d":" -f2 |\
      cut -d "[" -f1 | cut -d " " -f 2)
RESULTS="$(adb shell oatdump --oat-file="${OAT}" |\
               grep -E "(OatClassSomeCompiled|OatClassAllCompiled)")"
if [[ $COUNT -eq 0 ]]; then
  echo "${RESULTS}"
else
  echo -n "OatClassAllCompiled: "
  echo "${RESULTS}" | grep "OatClassAllCompiled" -c
  echo -n "OatClassSomeCompiled: "
  echo "${RESULTS}" | grep "OatClassSomeCompiled" -c
fi