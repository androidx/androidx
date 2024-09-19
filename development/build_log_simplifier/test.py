#!/usr/bin/env python3
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

from build_log_simplifier import collapse_consecutive_blank_lines
from build_log_simplifier import collapse_tasks_having_no_output
from build_log_simplifier import extract_task_names
from build_log_simplifier import remove_unmatched_exemptions
from build_log_simplifier import suggest_missing_exemptions
from build_log_simplifier import normalize_paths
from build_log_simplifier import regexes_matcher
from build_log_simplifier import remove_control_characters
import re

def fail(message):
    print(message)
    exit(1)

def test_regexes_matcher_get_matching_regexes():
    print("test_regexes_matcher_get_matching_regexes")
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

def test_detect_task_names():
    print("test_detect_task_names")
    lines = [
        "> Task :one\n",
        "some output\n",
        "> Task :two\n",
        "more output\n"
    ]
    task_names = [":one", ":two"]
    detected_names = extract_task_names(lines)
    if detected_names != task_names:
        fail("extract_task_names returned incorrect response\n" +
            "Input   : " + str(lines) + "\n" +
            "Output  : " + str(detected_names) + "\n" +
            "Expected: " + str(task_names)
        )

def test_remove_unmatched_exemptions():
    print("test_remove_unmatched_exemptions")
    lines = [
        "task two message one",
        "task four message one",
    ]

    current_config = [
        "# > Task :one",
        "task one message one",
        "# TODO(bug): remove this",
        "# > Task :two",
        "task two message one",
        "# TODO(bug): remove this too",
        "# > Task :three",
        "task three message one",
        "# > Task :four",
        "task four message one",
        "# TODO: maybe remove this too?",
        "# > Task :five",
        "task five message one"
    ]

    expected_config = [
        "# TODO(bug): remove this",
        "# > Task :two",
        "task two message one",
        "# > Task :four",
        "task four message one",
    ]

    actual_updated_config = remove_unmatched_exemptions(lines, current_config)
    if actual_updated_config != expected_config:
        fail("test_remove_unmatched_exemptions gave incorrect response.\n\n" +
            "Input log             : " + str(lines) + "\n\n" +
            "Input config          : " + str(current_config) + "\n\n" +
            "Expected output config: " + str(expected_config) + "\n\n" +
            "Actual output config  : " + str(actual_updated_config))

def test_suggest_missing_exemptions():
    print("test_suggest_missing_exemptions")
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

    # also validate that "> Configure project" gets ignored too
    config4 = [
        "# > Configure project a",
        "some warning"
    ]
    lines4 = [
        "> Configure project b",
        "some warning"
    ]
    expect_config4 = config4
    validate_suggested_exemptions(lines4, config4, expect_config4)

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
        "> Task :no-output2",
        "> Task :no-output3",
        "FAILURE: Build failed with an exception.\n"
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
        fail("collapse_tasks_having_no_output gave incorrect error.\n" +
            "Expected: " + str(expected) + "\n" +
            "Actual = " + str(actual))

def test_collapse_consecutive_blank_lines():
    print("test_collapse_consecutive_blank_lines")
    lines = [
        "",
        "> Task :a",
        "",
        "   ",
        "\n\n",
        "> Task :b",
        " ",
        ""
    ]
    expected_collapsed = [
        "> Task :a",
        "",
        "> Task :b",
        " "
    ]
    actual_collapsed = collapse_consecutive_blank_lines(lines)
    if actual_collapsed != expected_collapsed:
        fail("collapse_consecutive_blank_lines returned incorrect response.\n"
            "Input: " + lines + "\n" +
            "Output: " + actual_collapsed + "\n" +
            "Expected output: " + expected_collapsed
        )

def validate_suggested_exemptions(lines, config, expected_config):
    suggested_config = suggest_missing_exemptions(lines, config)
    if suggested_config != expected_config:
        fail("suggest_missing_exemptions incorrect response.\n" +
             "Lines: " + str(lines) + ",\n" +
             "config: " + str(config) + ",\n" +
             "expected suggestion: " + str(expected_config) + ",\n"
             "actual suggestion  : " + str(suggested_config))

def test_remove_control_characters():
    print("test_remove_control_characters")
    given = [
        # a line starting with several color codes in it
        "[1msrc/main/java/androidx/arch/core/internal/FastSafeIterableMap.java:39: [33mwarning: [0mMethod androidx.arch.core.internal.FastSafeIterableMap.get(K) references hidden type androidx.arch.core.internal.SafeIterableMap.Entry<K,V>. [HiddenTypeParameter]",
        # a line with a variety of characters, none of which are color codes
        "space tab\tCAPITAL underscore_ slash/ colon: number 1 newline\n",
    ]
    expected = [
        "src/main/java/androidx/arch/core/internal/FastSafeIterableMap.java:39: warning: Method androidx.arch.core.internal.FastSafeIterableMap.get(K) references hidden type androidx.arch.core.internal.SafeIterableMap.Entry<K,V>. [HiddenTypeParameter]",
        "space tab\tCAPITAL underscore_ slash/ colon: number 1 newline\n",
    ]
    actual = [remove_control_characters(line) for line in given]
    if actual != expected:
        fail("remove_control_charactres gave incorrect response.\n\n" +
            "Input          : " + str(given) + ".\n\n" +
            "Expected output: " + str(expected) + ".\n\n" +
            "Actual output  : " + str(actual) + ".")


def main():
    test_collapse_consecutive_blank_lines()
    test_collapse_tasks_having_no_output()
    test_detect_task_names()
    test_suggest_missing_exemptions()
    test_normalize_paths()
    test_regexes_matcher_get_matching_regexes()
    test_regexes_matcher_index_first_matching_regex()
    test_remove_control_characters()
    test_remove_unmatched_exemptions()

if __name__ == "__main__":
    main()
