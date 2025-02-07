#!/usr/bin/env python3

#
# Copyright 2025, The Android Open Source Project
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

"""Script that enforces Relnote: any file path in the commit contains module substring"""

import argparse
import os.path
import re
import sys

ERROR_RELNOTE_REQUIRED = """
RELNOTE: is required for commits that contain changes in {}

Please add a RELNOTE to the commit or RELNOTE: N/A if a release note is not applicable to the
commit.

A RELNOTE is required for all commits that changes the release artifacts.

A RELNOTE can be N/A for commit messages that only effects tooling, documentation, directory
structure, etc., but not the release artifacts.
"""

def main(args=None):
    parser = argparse.ArgumentParser(
        prog="requirerelnote",
        description="Check if RELNOTE is required")
    parser.add_argument('--file', nargs='+')
    parser.add_argument('--module')
    parser.add_argument('--commit')

    args = parser.parse_args()

    source_files = [f for f in args.file
               if (not "buildSrc/" in f and
                  "/src/main/" in f or
                  "/src/commonMain/" in f or
                  "/src/androidMain/" in f)]
    module_files = [f for f in source_files
                if (args.module in f)]

    if not module_files:
        sys.exit(0)

    """Following copied (with minor edits) from hooks.py:check_commit_msg_relnote_for_current_txt"""
    """Check if the commit contain the 'Relnote:' stanza."""
    field = 'Relnote'
    regex = fr'^{field}: .+$'
    check_re = re.compile(regex, re.IGNORECASE)

    found = []
    for line in args.commit.splitlines():
        if check_re.match(line):
            found.append(line)

    if not found:
        print(ERROR_RELNOTE_REQUIRED.format(args.module))
        sys.exit(1)

    sys.exit(0)

if __name__ == '__main__':
  main()