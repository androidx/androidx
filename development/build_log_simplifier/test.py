#!/usr/bin/python3
#
# Copyright (C) 2020 The Android Open Source Project
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

from build_log_simplifier import regexes_matcher
import re

def fail(message):
    print(message)
    exit(1)

def test_regexes_matcher():
    # For each of the given queries, we ask a regexes_matcher to identify which regexes
    # match them, and compare to the right answer
    queries = ["", "a", "aa", "aaa", "b", "bb", "ab", "c", "a*"]
    regexes = ["a", "a*", "aa*", "b", "b*", "a*b"]
    matcher = regexes_matcher(regexes)
    for query in queries:
        simple_matches = [regex for regex in regexes if re.compile(regex).fullmatch(query)]
        fast_matches = matcher.get_matching_regexes(query)
        if simple_matches != fast_matches:
            fail("regexes_matcher returned incorrect results for '" + query + "'. Expected = " + str(simple_matches) + ", actual = " + str(fast_matches))
        print("Query = '" + query + "', matching regexes = " + str(simple_matches))

def main():
    test_regexes_matcher()

if __name__ == "__main__":
    main()
