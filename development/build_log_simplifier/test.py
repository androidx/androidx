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

from build_log_simplifier import normalize_paths
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

def test_normalize_paths():
    print("test_normalize_paths")
    lines = [
      "CHECKOUT=/usr/home/me/workspace",
      "/usr/home/me/workspace/external/protobuf/somefile.h: somewarning",
      "Building CXX object protobuf-target/CMakeFiles/libprotobuf.dir/usr/home/me/workspace/external/somefile.cc"
    ]
    expected_normalized = [
      "CHECKOUT=$CHECKOUT",
      "$CHECKOUT/external/protobuf/somefile.h: somewarning",
      "Building CXX object protobuf-target/CMakeFiles/libprotobuf.dir$CHECKOUT/external/somefile.cc"
    ]
    actual = normalize_paths(lines)
    if expected_normalized != actual:
        fail("test_normalize_paths returned incorrect response.\n" +
            "Input: " + str(lines) + "\n" + 
            "Output: " + str(actual) + "\n" +
            "Expected output: " + str(expected_normalized)
        )

def test_regexes_matcher_index_first_matching_regex():
    print("test_regexes_matcher_index_first_matching_regex")
    regexes = ["first", "double", "single", "double"]
    matcher = regexes_matcher(regexes)
    assert(matcher.index_first_matching_regex("first") == 0)
    assert(matcher.index_first_matching_regex("double") == 1)
    assert(matcher.index_first_matching_regex("single") == 2)
    assert(matcher.index_first_matching_regex("absent") is None)

def validate_suggested_exemptions(lines, config, expected_config):
    suggested_config = generate_suggested_exemptions(lines, config)
    if suggested_config != expected_config:
        fail("generate_suggested_exemptions incorrect response.\n" +
             "Lines: " + str(lines) + ",\n" +
             "config: " + str(config) + ",\n" +
             "expected suggestion: " + str(expected_config) + ",\n"
             "actual suggestion  : " + str(suggested_config))


def test_generate_suggested_exemptions():
    print("test_generate_suggested_exemptions")
    lines = [
        "> Task :one",
        "task one message one",
        "task one message two",
        "> Task :two",
        "task two message one",
        "duplicate line",
        "> Task :three",
        "task three message one",
        "duplicate line"
    ]

    expect_config = [
        "# > Task :one",
        "task one message one",
        "task one message two",
        "# > Task :two",
        "task two message one",
        "duplicate line",
        "# > Task :three",
        "task three message one"
    ]

    # generate config starting with nothing
    validate_suggested_exemptions(lines, [], expect_config)

    # remove one line from config, regenerate config, line should return
    config2 = expect_config[:1] + expect_config[2:]
    validate_suggested_exemptions(lines, config2, expect_config)

    # if there is an existing config with the tasks in the other order, the tasks should stay in that order
    # and the new line should be inserted after the previous matching line
    config3 = [
        "# > Task :two",
        "task two message one",
	"duplicate line",
        "# > Task :one",
        "task one message two",
        "# > Task :three",
        "task three message one"
    ]
    expect_config3 = [
        "# > Task :two",
        "task two message one",
	"duplicate line",
        "# > Task :one",
        "task one message one",
        "task one message two",
        "# > Task :three",
        "task three message one"
    ]
    validate_suggested_exemptions(lines, config3, expect_config3)

def test_collapse_tasks_having_no_output():
    print("test_collapse_tasks_having_no_output")
    lines = [
        "> Task :no-output1",
        "> Task :some-output1",
        "output1",
        "> Task :empty-output",
        "",
        "> Task :blanks-around-output",
        "",
        "output inside blanks",
        "",
        "> Task :no-output2"
        "> Task :no-output3"
    ]
    expected = [
        "> Task :some-output1",
        "output1",
	"> Task :blanks-around-output",
        "",
        "output inside blanks",
        ""
    ]
    actual = collapse_tasks_having_no_output(lines)
    if (actual != expected):
        fail("collapse_tasks_having_no_output gave incorrect error. Expected: " + str(expected) + ", actual = " + str(actual))

def main():
    test_normalize_paths()
    test_regexes_matcher()

if __name__ == "__main__":
    main()
