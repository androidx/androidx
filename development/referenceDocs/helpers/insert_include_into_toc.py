#
# Copyright 2022 The Android Open Source Project
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

import os
import sys

def should_insert_before(line, lib_name):
    prefix = "- title: \"androidx."
    return line.startswith(prefix) and line[len(prefix):] > lib_name

def exec(path):
    lib_name = path.split("/")[-2]
    toc_file = "reference/androidx/_toc.yaml"
    with open(toc_file, "r") as file:
        lines = file.readlines()
    idx = next(
        (idx for idx, line in enumerate(lines) if should_insert_before(line, lib_name)),
        len(lines)
    )
    lines.insert(idx, f"- include: /{path}\n")
    with open(toc_file, "w") as file:
        file.write("".join(lines))

exec(sys.argv[1])