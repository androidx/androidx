#!/usr/bin/python

#
# Copyright 2017, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

""" Run this script when you need to update test-data/expected."""
import sys

if len(sys.argv) != 2:
    print("You need to specify the only one param: file with test failures")
    sys.exit()

with open(sys.argv[1]) as f:
    content = f.readlines()

with open("src/tests/test-data/expected/license.txt") as license:
    licenseLines = license.readlines()


def writeToFile(fileName, lines):
    file = open("src/tests/test-data/expected/" + fileName, "w")
    for line in lines:
        file.write(line)

    file.close()


state = 0
filename = ""
expected = "Expected file:"
fileLines = []
for line in content:
    if (state == 0 and line.startswith(expected)):
        state = 1
        filename  = line[line.rfind("/") + 1 : len(line) - 2]
        print(filename)

    if state == 1 and line.startswith("Actual Source:"):
        state = 2
        continue

    if state == 2:
        fileLines.append(line)

    if state == 2 and line.rstrip() == "}":
        writeToFile(filename, licenseLines + fileLines[1:])
        state = 0
        fileLines = []

