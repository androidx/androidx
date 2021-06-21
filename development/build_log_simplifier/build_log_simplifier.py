#!/usr/bin/python3
#
# Copyright (C) 2016 The Android Open Source Project
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
import argparse, collections, os, re, sys

dir_of_this_script = os.path.dirname(os.path.realpath(__file__))

parser = argparse.ArgumentParser(
    description="""USAGE:
    Simplifies a build.log from hundreds of megabytes to <100 lines. Prints output to terminal.
    Pass this script a filepath to parse. You should be able to type "python3 build_log_simplifier.py"
    And then drag-and-drop a log file onto the terminal window to get its path.

    Sample usage: python3 development/build_log_simplifier.py Users/owengray/Desktop/build.log
    """)
parser.add_argument("--validate", action="store_true", help="Validate that no unrecognized messages exist in the given log")
parser.add_argument("--update", action="store_true", help="Update our list of recognized messages to include all messages from the given log")
parser.add_argument("--gc", action="store_true", help="When generating a new exemptions file, exclude any exemptions that were not found in the given log. Only relevant with --update or --validate")
parser.add_argument("log_path", help="Filepath of log(s) to process", nargs="+")

# a regexes_matcher can quickly identify which of a set of regexes matches a given text
class regexes_matcher(object):
    def __init__(self, regexes):
        self.regex_texts = regexes
        self.children = None
        self.matcher = None

    # returns a list of regexes that match the given text
    def get_matching_regexes(self, text, expect_match=True):
        if expect_match and len(self.regex_texts) > 1:
            # If we already expect our matcher to match, we can directly jump to asking our children
            return self.query_children_for_matching_regexes(text)
        # It takes more time to match lots of regexes than to match one composite regex
        # So, we try to match one composite regex first
        if self.matches(text):
            if len(self.regex_texts) > 1:
                # At least one child regex matches, so we have to determine which ones
                return self.query_children_for_matching_regexes(text)
            else:
                return self.regex_texts
        # Our composite regex yielded no matches
        return []

    # queries our children for regexes that match <text>
    def query_children_for_matching_regexes(self, text):
        # Create children if they don't yet exist
        self.ensure_split()
        # query children and join their results
        results = []
        for child in self.children:
            results += child.get_matching_regexes(text, False)
        return results

    # Returns the index of the first regex matching this string, or None of not found
    def index_first_matching_regex(self, text):
        if len(self.regex_texts) <= 1:
            if len(self.regex_texts) == 0:
                return None
            if self.matches(text):
                return 0
            return None
        if not self.matches(text):
            return None
        self.ensure_split()
        count = 0
        for child in self.children:
            child_index = child.index_first_matching_regex(text)
            if child_index is not None:
                return count + child_index
            count += len(child.regex_texts)
        return None

    # Create children if they don't yet exist
    def ensure_split(self):
        if self.children is None:
            # It takes more time to compile a longer regex, but it also takes more time to
            # test lots of small regexes.
            # In practice, this number of children seems to result in fast execution
            num_children = min(len(self.regex_texts), 32)
            child_start = 0
            self.children = []
            for i in range(num_children):
                child_end = int(len(self.regex_texts) * (i + 1) / num_children)
                self.children.append(regexes_matcher(self.regex_texts[child_start:child_end]))
                child_start = child_end


    def matches(self, text):
        if self.matcher is None:
            full_regex_text = "(?:" + ")|(?:".join(self.regex_texts) + ")"
            self.matcher = re.compile(full_regex_text)
        return self.matcher.fullmatch(text)


def print_failing_task_names(lines):
    tasks_of_interest = []
    # first, find tasks of interest
    for line in lines:
        if line.startswith("Execution failed for task"):
            tasks_of_interest.append(line.split("task '")[1][:-3])

    print("Detected these failing tasks: " + str(tasks_of_interest))

def shorten_uninteresting_stack_frames(lines):
    result = []
    prev_line_is_boring = False
    for line in lines:
        if line.startswith("\tat org.gradle"):
            if not prev_line_is_boring:
                result.append("\tat org.gradle...\n")
            prev_line_is_boring = True
        elif line.startswith("\tat java.base"):
            if not prev_line_is_boring:
                result.append("\tat java.base...\n")
            prev_line_is_boring = True
        else:
            result.append(line)
            prev_line_is_boring = False
    return result

# Returns the path of the config file holding exemptions for deterministic/consistent output.
# These exemptions can be garbage collected via the `--gc` argument
def get_deterministic_exemptions_path():
    return os.path.join(dir_of_this_script, "messages.ignore")

# Returns the path of the config file holding exemptions for nondetermistic/flaky output.
# These exemptions will not be garbage collected via the `--gc` argument
def get_flake_exemptions_path():
    return os.path.join(dir_of_this_script, "message-flakes.ignore")

# Returns a regexes_matcher that matches what is described by our config file
# Ignores comments and ordering in our config file
def build_exemptions_matcher(config_lines):
    config_lines = [line.replace("\n", "") for line in config_lines]
    regexes = []
    for line in config_lines:
        line = line.strip()
        if line.startswith("#") or line == "":
            # skip comments
            continue
        regexes.append(line)
        if remove_control_characters(line) != line:
            raise Exception("Unexpected control characters found in configuration line:\n\n " +
                "'" + line + "'\n\n. This line is unexpected to match anything. Is this a copying mistake?")

    return regexes_matcher(sorted(regexes))

# Returns a regexes_matcher that matches the content of our config file
# Can match comments
# Respects ordering in the config
# This is used for editing the config file itself
def build_exemptions_code_matcher(config_lines):
    config_lines = [line.strip() for line in config_lines]
    regexes = []
    for line in config_lines:
        line = line.strip()
        if line == "":
            continue
        regexes.append(line)
    return regexes_matcher(regexes)

def remove_by_regexes(lines, config_lines, validate_no_duplicates):
    fast_matcher = build_exemptions_matcher(config_lines)
    result = []
    for line in lines:
        stripped = line.strip()
        matching_exemptions = fast_matcher.get_matching_regexes(stripped, expect_match=True)
        if validate_no_duplicates and len(matching_exemptions) > 1:
           print("")
           print("build_log_simplifier.py: Invalid configuration: multiple message exemptions match the same message. Are some exemptions too broad?")
           print("")
           print("Line: '" + stripped + "'")
           print("")
           print(str(len(matching_exemptions)) + " Matching exemptions:")
           for exemption_text in matching_exemptions:
               print("'" + exemption_text + "'")
           exit(1)
        if len(matching_exemptions) < 1:
            result.append(line)
    return result

def collapse_consecutive_blank_lines(lines):
    result = []
    prev_blank = True
    for line in lines:
        if line.strip() == "":
            if not prev_blank:
                result.append(line)
            prev_blank = True
        else:
            result.append(line)
            prev_blank = False
    return result

def extract_task_name(line):
    prefix = "> Task "
    if line.startswith(prefix):
        return line[len(prefix):].strip()
    return None

def is_task_line(line):
    return extract_task_name(line) is not None

def extract_task_names(lines):
    names = []
    for line in lines:
        name = extract_task_name(line)
        if name is not None and name not in names:
            names.append(name)
    return names

# If a task has no output (or only blank output), this function removes the task (and its output)
# For example, turns this:
#  > Task :a
#  > Task :b
#  some message
#
# into this:
#
#  > Task :b
#  some message
def collapse_tasks_having_no_output(lines):
    result = []
    # When we see a task name, we might not emit it if it doesn't have any output
    # This variable is that pending task name, or none if we have no pending task
    pending_task = None
    pending_blanks = []
    for line in lines:
        is_section = is_task_line(line) or line.startswith("> Configure project ") or line.startswith("FAILURE: Build failed with an exception.")
        if is_section:
            pending_task = line
            pending_blanks = []
        elif line.strip() == "":
            # If we have a pending task and we found a blank line, then hold the blank line,
            # and only output it if we later find some nonempty output
            if pending_task is not None:
                pending_blanks.append(line)
            else:
                result.append(line)
        else:
            # We found some nonempty output, now we emit any pending task names
            if pending_task is not None:
                result.append(pending_task)
                result += pending_blanks
                pending_task = None
                pending_blanks = []
            result.append(line)
    return result

# Removes color characters and other ANSI control characters from this input
control_character_regex = re.compile(r"""
        \x1B  # Escape
        (?:   # 7-bit C1 Fe (except CSI)
            [@-Z\\-_]
        |     # or [ for CSI, followed by a control sequence
            \[
            [0-?]*  # Parameters
            [ -/]*  # Intermediate bytes
            [@-~]   # End
        )
        """, re.VERBOSE)

def remove_control_characters(line):
    return control_character_regex.sub("", line)

# Normalizes some filepaths to more easily simplify/skip some messages
def normalize_paths(lines):
    # get OUT_DIR, DIST_DIR, and the path of the root of the checkout
    out_dir = None
    dist_dir = None
    checkout_dir = None
    gradle_user_home = None
    # we read checkout_root from the log file in case this build was run in a location,
    # such as on a build server
    out_marker = "OUT_DIR="
    dist_marker = "DIST_DIR="
    checkout_marker = "CHECKOUT="
    gradle_user_home_marker="GRADLE_USER_HOME="
    for line in lines:
        if line.startswith(out_marker):
            out_dir = line.split(out_marker)[1].strip()
            continue
        if line.startswith(dist_marker):
            dist_dir = line.split(dist_marker)[1].strip()
            continue
        if line.startswith(checkout_marker):
            checkout_dir = line.split(checkout_marker)[1].strip()
            continue
        if line.startswith(gradle_user_home_marker):
            gradle_user_home = line.split(gradle_user_home_marker)[1].strip()
            continue
        if out_dir is not None and dist_dir is not None and checkout_dir is not None and gradle_user_home is not None:
            break

    # Remove any mentions of these paths, and replace them with consistent values
    # Make sure to put these paths in the correct order so that more-specific paths will
    # be matched first
    remove_paths = collections.OrderedDict()
    if gradle_user_home is not None:
        remove_paths[gradle_user_home] = "$GRADLE_USER_HOME"
    if dist_dir is not None:
        remove_paths[dist_dir] = "$DIST_DIR"
    if out_dir is not None:
        remove_paths[out_dir] = "$OUT_DIR"
    if checkout_dir is not None:
        remove_paths[checkout_dir + "/frameworks/support"] = "$SUPPORT"
        remove_paths[checkout_dir] = "$CHECKOUT"
    result = []
    for line in lines:
        for path in remove_paths:
            if path in line:
                replacement = remove_paths[path]
                line = line.replace(path + "/", replacement + "/")
                line = line.replace(path, replacement)
        result.append(line)
    return result

# Given a regex with hashes in it like ".gradle/caches/transforms-2/files-2.1/73f631f487bd87cfd8cb2aabafbac6a8",
# tries to return a more generalized regex like ".gradle/caches/transforms-2/files-2.1/[0-9a-f]{32}"
def generalize_hashes(message):
    hash_matcher = "[0-9a-f]{32}"
    return re.sub(hash_matcher, hash_matcher, message)

# Given a regex with numbers in it like ".gradle/caches/transforms-2/files-2.1/73f631f487bd87cfd8cb2aabafbac6a8"
# tries to return a more generalized regex like ".gradle/caches/transforms-[0-9]*/files-[0-9]*.[0-9]*/73f631f487bd87cfd8cb2aabafbac6a8"
def generalize_numbers(message):
    matcher = "[0-9]+"
    generalized = re.sub(matcher, matcher, message)
    # the above replacement corrupts strings of the form "[0-9a-f]{32}", so we fix them before returning
    return generalized.replace("[[0-9]+-[0-9]+a-f]{[0-9]+}", "[0-9a-f]{32}")

# Given a list of output messages and a list of existing exemption lines,
# generates a new list of exemption lines
def generate_suggested_exemptions(messages, config_lines, remove_unmatched_lines):
    new_config = suggest_missing_exemptions(messages, config_lines)
    if remove_unmatched_lines:
        new_config = remove_unmatched_exemptions(messages, new_config)
    return new_config

# Given a list of output messages and a list of existing exemption lines,
# generates an augmented list of exemptions containing any necessary new exemptions
def suggest_missing_exemptions(messages, config_lines):
    # given a message, finds the index of the existing exemption for that message, if any
    existing_matcher = build_exemptions_code_matcher(config_lines)
    # the index of the previously matched exemption
    previous_found_index = -1
    # map from line index to list of lines to insert there
    insertions_by_position = collections.defaultdict(lambda: [])
    insertions_by_task_name = collections.OrderedDict()
    # current task generating any subsequent output
    pending_task_line = None
    # new, suggested exemptions
    new_suggestions = set()
    # generate new suggestions
    for line in messages:
        line = line.strip()
        if line == "":
            continue
        # save task name
        is_section = False
        if is_task_line(line) or line.startswith("> Configure project "):
            # If a task creates output, we record its name
            line = "# " + line
            pending_task_line = line
            is_section = True
        # determine where to put task name
        current_found_index = existing_matcher.index_first_matching_regex(line)
        if current_found_index is not None:
            # We already have a mention of this line
            # We don't need to exempt it again, but this informs where to insert our next exemption
            previous_found_index = current_found_index
            pending_task_line = None
            continue
        # skip outputting task names for tasks that don't output anything
        if is_section:
            continue

        # escape message
        escaped = re.escape(line)
        escaped = escaped.replace("\ ", " ") # spaces don't need to be escaped
        escaped = generalize_hashes(escaped)
        escaped = generalize_numbers(escaped)
        # confirm that we haven't already inserted this message
        if escaped in new_suggestions:
            continue
        # insert this regex into an appropriate position
        if pending_task_line is not None:
            # We know which task this line came from, and it's a task that didn't previously make output
            if pending_task_line not in insertions_by_task_name:
                insertions_by_task_name[pending_task_line] = []
            insertions_by_task_name[pending_task_line].append(escaped)
        else:
            # This line of output didn't come from a new task
            # So we append it after the previous line that we found
            insertions_by_position[previous_found_index].append(escaped)
        new_suggestions.add(escaped)

    # for each regex for which we chose a position in the file, insert it there
    exemption_lines = []
    for i in range(len(existing_matcher.regex_texts)):
        exemption_lines.append(existing_matcher.regex_texts[i])
        if i in insertions_by_position:
            exemption_lines += insertions_by_position[i]
    # for regexes that could not be assigned to a task, insert them next
    if -1 in insertions_by_position:
        exemption_lines += insertions_by_position[-1]
    # for regexes that were simply assigned to certain task names, insert the there, grouped by task
    for task_name in insertions_by_task_name:
        exemption_lines.append(task_name)
        exemption_lines += insertions_by_task_name[task_name]
    return exemption_lines

# Searches for config lines in <config_lines> that match no line in <messages>
# Create and returns a new list of config lines, which excludes unmatched lines and
# any corresponding comments
def remove_unmatched_exemptions(messages, config_lines):
    existing_matcher = build_exemptions_matcher(config_lines)
    matched_config_lines = set()
    # find all of the regexes that match at least one message
    for line in messages:
        line = line.strip()
        if line.startswith("#"):
            continue
        for regex in existing_matcher.get_matching_regexes(line):
            matched_config_lines.add(regex)
    # generate a new list of config lines
    # keep config lines that were matched in the list of messages
    # keep comments where there remains a matched config line before the next comment
    # skip comments that were previously followed by other config lines that were deleted
    result = []
    pending_comments = [] # comments that we haven't yet decided to keep or not
    found_unused_line_after_comment = False
    for line in config_lines:
        if line.startswith("#"):
            # We found a comment
            if found_unused_line_after_comment:
                # We found an unused config line more recently than the previous comment,
                # and now we've found a new comment.
                if len(pending_comments) > 0:
                    # We also haven't found any used config lines more recently than the previous comment
                    # Presumably these pending comments were intended to describe the lines that we're removing
                    # So, we skip emitting these pending comments too
                    pending_comments = []
            pending_comments.append(line)
            found_unused_line_after_comment = False
            continue
        matched = (line in matched_config_lines)
        if matched:
            # If this config line is being used, then we keep its comments too
            result += pending_comments
            pending_comments = []
            result.append(line)
        else:
            found_unused_line_after_comment = True
    # If there are any comments at the bottom of the file, then keep them too
    if not found_unused_line_after_comment:
        result += pending_comments
    return result

# opens a file and reads the lines in it
def readlines(path):
    infile = open(path)
    lines = infile.readlines()
    infile.close()
    return lines

def writelines(path, lines):
    destfile = open(path, 'w')
    destfile.write("\n".join(lines))
    destfile.close()

def main():
    arguments = parser.parse_args()

    # read each file
    log_paths = arguments.log_path
    all_lines = []
    for log_path in log_paths:
        lines = readlines(log_path)
        lines = [remove_control_characters(line) for line in lines]
        lines = normalize_paths(lines)
        all_lines += lines
    # load configuration
    flake_exemption_regexes = readlines(get_flake_exemptions_path())
    deterministic_exemption_regexes = readlines(get_deterministic_exemptions_path())
    exemption_regexes = flake_exemption_regexes + deterministic_exemption_regexes
    # load configuration
    # remove lines we're not interested in
    update = arguments.update or arguments.gc
    validate = update or arguments.validate
    interesting_lines = all_lines
    if not validate:
        print_failing_task_names(interesting_lines)
    interesting_lines = remove_by_regexes(interesting_lines, exemption_regexes, validate)
    interesting_lines = collapse_tasks_having_no_output(interesting_lines)
    interesting_lines = collapse_consecutive_blank_lines(interesting_lines)

    # process results
    if update:
        if arguments.gc or len(interesting_lines) != 0:
            update_path = get_deterministic_exemptions_path()
            # filter out any inconsistently observed messages so we don't try to exempt them twice
            all_lines = remove_by_regexes(all_lines, flake_exemption_regexes, validate)
            # update the deterministic exemptions file based on the result
            suggested = generate_suggested_exemptions(all_lines, deterministic_exemption_regexes, arguments.gc)
            writelines(update_path, suggested)
            print("build_log_simplifier.py updated exemptions " + update_path)
    elif validate:
        if len(interesting_lines) != 0:
            print("")
            print("=" * 80)
            print("build_log_simplifier.py: Error: Found " + str(len(interesting_lines)) + " new lines of warning output!")
            print("")
            print("The new output:")
            print("  " + "  ".join(interesting_lines))
            print("")
            print("To reproduce this failure:")
            print("  Try $ ./gradlew -Pandroidx.validateNoUnrecognizedMessages --rerun-tasks " + " ".join(extract_task_names(interesting_lines)))
            print("")
            print("Instructions:")
            print("  Fix these messages if you can.")
            print("  Otherwise, you may suppress them.")
            print("  See also https://android.googlesource.com/platform/frameworks/support/+/androidx-main/development/build_log_simplifier/VALIDATION_FAILURE.md")
            print("")
            new_exemptions_path = log_paths[0] + ".ignore"
            # filter out any inconsistently observed messages so we don't try to exempt them twice
            all_lines = remove_by_regexes(all_lines, flake_exemption_regexes, validate)
            # update deterministic exemptions file based on the result
            suggested = generate_suggested_exemptions(all_lines, deterministic_exemption_regexes, arguments.gc)
            writelines(new_exemptions_path, suggested)
            print("Files:")
            print("  Full Log                   : " + ",".join(log_paths))
            print("  Baseline                   : " + get_deterministic_exemptions_path())
            print("  Autogenerated new baseline : " + new_exemptions_path)
            exit(1)
    else:
        interesting_lines = shorten_uninteresting_stack_frames(interesting_lines)
        print("".join(interesting_lines))

if __name__ == "__main__":
    main()
