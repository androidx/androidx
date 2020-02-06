#!/usr/bin/python

#
# Copyright 2019, The Android Open Source Project
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

"""Script used to find IDEA warning suppression that doesn't work for command line builds."""

import os
import sys

MAIN_DIRECTORY = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))

# first level line filter - we ignore lines that don't contain the following for speed
ROOT_MATCH = "noinspection"

# Map of invalid pattern to correct replacement
# These could be regex for fancy whitespace matching, but don't seem to need in practice
MATCHERS = {
    '//noinspection deprecation' : '@SuppressWarnings("deprecation")',
    '//noinspection unchecked' : '@SuppressWarnings("unchecked")',
}

## Check a line for any invalid suppressions, and return it if found
def getReportForLine(filename, i, line, lines):
  for bad, good in MATCHERS.iteritems():
    if bad in line:
      context = "".join(lines[i:i+3])
      return "\n{}:{}:\nError: unsupported comment suppression\n{}Instead, use: {}\n".format(
          filename, i, context, good)
  return ""

def main():
  report = ""

  for filename in sys.argv[1:]:
    # suppress comments ignored in kotlin, but may as well block there too
    if not filename.endswith(".java") and not filename.endswith(".kt"):
      continue

    filename = os.path.join(MAIN_DIRECTORY, filename)

    # check file exists
    if not os.path.isfile(filename):
      continue

    with open(filename, "r") as f:
      lines = f.readlines()

      linetuples = map(lambda i: [i, lines[i]], range(len(lines)))
      for i, line in linetuples:
        if ROOT_MATCH in line:
          report += getReportForLine(filename, i, line, lines)

  if not report:
    sys.exit(0)

  print "Invalid, IDEA-specific warning suppression found. These cause warnings during compilation."
  print report
  sys.exit(1)

if __name__ == '__main__':
  main()
