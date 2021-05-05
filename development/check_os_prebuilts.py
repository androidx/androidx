#!/usr/bin/env python3

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

import sys

#### ####
# This preupload script ensures that when an operating system specific (linux /
# osx) prebuilt is added to prebuilts/ the same prebuilt is available on both platforms.
#### ####

def main():
  # Dict of file names without their platform name mapped to the full name
  # E.g foo/bar-linux.jar would be split into foo/bar-.jar : foo/bar-linux.jar
  os_specific_files = {}

  for filename in sys.argv[1:]:
    if "osx" in filename or "linux" in filename:
      stripped_filename = filename.replace("osx", "").replace("linux", "")
      if stripped_filename in os_specific_files.keys():
        # Corresponding linux/osx pair, so no need to track
        os_specific_files.pop(stripped_filename)
      else:
        os_specific_files[stripped_filename] = filename

  # No matching files
  if not os_specific_files:
    sys.exit(0)

  print("The following operating system specific files were found in your commit:\n\033[91m")
  for filename in os_specific_files.values():
    print(filename)
  print ("""\033[0m\nPlease make sure to import the corresponding prebuilts for missing platforms.
If you imported a prebuilt similar to foo:bar:linux, try foo:bar:osx and vice versa.
If there is no corresponding prebuilt, or only adding a prebuilt for one platform is intended, run:
\033[92mrepo upload --no-verify\033[0m
to skip this warning.""")
  sys.exit(1)


if __name__ == "__main__":
  main()
