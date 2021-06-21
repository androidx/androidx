#!/usr/bin/env python

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

"""Script that will remind developers to run updateApi."""

import argparse
import os.path
import sys



WARNING_COLOR = '\033[33m'
END_COLOR = '\033[0m'

WARNING_NO_API_FILES = """
{}**********************************************************************
You changed library classes, but you have no current.txt changes.
Did you forget to run ./gradlew updateApi?
**********************************************************************{}
""".format(WARNING_COLOR, END_COLOR)

WARNING_OLD_API_FILES = """
{}**********************************************************************
Your current.txt is older than your current changes in library classes.
Did you forget to re-run ./gradlew updateApi?
**********************************************************************{}
""".format(WARNING_COLOR, END_COLOR)


def main(args=None):
  if not ('ENABLE_UPDATEAPI_WARNING' in os.environ):
    sys.exit(0)

  parser = argparse.ArgumentParser()
  parser.add_argument('--file', '-f', nargs='*')
  parser.set_defaults(format=False)
  args = parser.parse_args()
  api_files = [f for f in args.file
               if f.endswith('.txt') and '/api/' in f]
  source_files = [f for f in args.file
               if (not "buildSrc/" in f and
                  "/src/main/" in f or
                  "/src/commonMain/" in f or
                  "/src/androidMain/" in f)]
  if len(source_files) == 0:
    sys.exit(0)

  if len(api_files) == 0:
    print(WARNING_NO_API_FILES)
    sys.exit(77) # 77 is a warning code in repohooks

  last_source_timestamp = max([os.path.getmtime(f) for f in source_files])
  last_api_timestamp = max([os.path.getmtime(f) for f in api_files])

  if last_source_timestamp > last_api_timestamp:
    print(WARNING_OLD_API_FILES)
    sys.exit(77) # 77 is a warning code in repohooks
  sys.exit(0)

if __name__ == '__main__':
  main()
