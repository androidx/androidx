#!/bin/bash
#
# Copyright 2025 The Android Open Source Project
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

set -e

# Given a file containing a date as text, echos which week number it is
# Examples: input "2024-01-01" should give output "0", input "2024-01-07" should give output "1", input "2024-01-14" should give output "2"
function getWeekNumber() {
  text="$1"
  dayOfYearWithPrecedingZeros="$(date --date="$text" +"%j")"
  dayOfYear="$(echo $dayOfYearWithPrecedingZeros | sed 's/^0*//')"
  if [ "$dayOfYear" == "" ]; then
    # There is an error that we will catch later
    echo
  else
    echo "$(($dayOfYear / 7))"
  fi
}

function deleteOldOutDir() {
  echo "DELETING OLD $OUT_DIR"
  # file telling when the out dir was created
  createdAtFile=$OUT_DIR/created_at.txt
  # file telling when the out dir was last updated
  updatedAtFile=$OUT_DIR/updated_at.txt
  now="$(date)"

  # if this directory was created a long time ago, delete it
  if [ -e "$createdAtFile" ]; then
    createdAt="$(cat "$createdAtFile")"
    # out dir knows when it was created
    createdWeekNumber="$(getWeekNumber "$createdAt" || true)"
    if [ "$createdWeekNumber" == "" ]; then
      echo "Failed to parse $createdAtFile with text $createdAt" >&2
      rm -f "$createdAtFile"
      exit 1
    fi
    updatedWeekNumber="$(getWeekNumber "$now")"

    if [ "$createdWeekNumber" != "$updatedWeekNumber" ]; then
      echo "Deleting $OUT_DIR because it was created at $createdAt week $createdWeekNumber whereas now is $now week $updatedWeekNumber"
      rm -rf "$OUT_DIR"
    fi
  fi
  mkdir -p "$OUT_DIR"

  # record that this directory was updated
  echo "$now" > "$updatedAtFile"

  # if we haven't recorded when this directory was created, do that too
  if [ ! -e "$createdAtFile" ]; then
    cp "$updatedAtFile" "$createdAtFile"
  fi
}
