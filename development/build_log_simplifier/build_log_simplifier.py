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
import argparse, collections, pathlib, os, re, sys

dir_of_this_script = str(pathlib.Path(__file__).parent.absolute())

parser = argparse.ArgumentParser(
    description="""USAGE:
    Simplifies a build.log from hundreds of megabytes to <100 lines. Prints output to terminal.
    Pass this script a filepath to parse. You should be able to type "python3 build_log_simplifier.py"
    And then drag-and-drop a log file onto the terminal window to get its path.

    Sample usage: python3 development/build_log_simplifier.py Users/owengray/Desktop/build.log
    """)
parser.add_argument("--validate", action="store_true", help="Validate that no unrecognized messages exist in the given log")
parser.add_argument("log_path", help="Filepath of log to process", nargs=1)

def select_failing_task_output(lines):
    tasks_of_interest = []
    # first, find tasks of interest
    for line in lines:
        if line.startswith("Execution failed for task"):
            tasks_of_interest.append(line.split("task '")[1][:-3])


    print("Detected these failing tasks: " + str(tasks_of_interest))

    # next, save all excerpts between start(interesting task) and end(interesting task)
    current_interesting_tasks = []
    retained_lines = []
    for line in lines:
        if line.startswith("Task ") and line.split(" ")[1] in tasks_of_interest:
            if line.split(" ")[-1].strip() == "Starting":
                current_interesting_tasks.append(line.split(" ")[1])
            elif line.split(" ")[-1].strip() == "Finished":
                current_interesting_tasks.remove(line.split(" ")[1])
                retained_lines.append(line)
        if current_interesting_tasks: retained_lines.append(line)
    if retained_lines:
        return retained_lines
    # if no output was created by any failing tasks, then maybe there could be useful output from
    # somewhere else
    return lines

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
                result.append("\tat java.base...")
            prev_line_is_boring = True
        else:
            result.append(line)
            prev_line_is_boring = False
    return result

def remove_known_uninteresting_lines(lines):
  skipLines = {
      "A fine-grained performance profile is available: use the --scan option.",
      "* Get more help at https://help.gradle.org",
      "Use '--warning-mode all' to show the individual deprecation warnings.",
      "See https://docs.gradle.org/6.5/userguide/command_line_interface.html#sec:command_line_warnings",

      "Note: Some input files use or override a deprecated API.",
      "Note: Recompile with -Xlint:deprecation for details.",
      "Note: Some input files use unchecked or unsafe operations.",
      "Note: Recompile with -Xlint:unchecked for details.",

      "w: ATTENTION!",
      "This build uses unsafe internal compiler arguments:",
      "-XXLanguage:-NewInference",
      "-XXLanguage:+InlineClasses",
      "This mode is not recommended for production use,",
      "as no stability/compatibility guarantees are given on",
      "compiler or generated code. Use it at your own risk!"
  }
  skipPrefixes = [
      "See the profiling report at:",

      "Deprecated Gradle features were used in this build"
  ]
  result = []
  for line in lines:
      stripped = line.strip()
      if stripped in skipLines:
          continue
      include = True
      for prefix in skipPrefixes:
          if stripped.startswith(prefix):
              include = False
              break
      if include:
          result.append(line)
  return result

def get_suppressions_path():
    return os.path.join(dir_of_this_script, "build_log_simplifier/messages.ignore")

# returns a list of regular expressions for lines of output that we want to ignore
def load_suppressions():
    suppressions_path = get_suppressions_path()
    infile = open(suppressions_path)
    lines = infile.readlines()
    infile.close()
    lines = [line.replace("\n", "") for line in lines]
    regexes = []
    for line in lines:
        line = line.strip()
        if line.startswith("#") or line == "":
            # skip comments
            continue
        regexes.append((line, re.compile(line)))
    return regexes

def remove_configured_uninteresting_lines(lines, validate_no_duplicates):
    suppressions = load_suppressions()
    result = []
    for line in lines:
        stripped = line.strip()
        matching_suppressions = []
        for suppression_text, suppression_regex in suppressions:
            if suppression_regex.fullmatch(stripped):
                matching_suppressions.append(suppression_text)
        if validate_no_duplicates and len(matching_suppressions) > 1:
           print("")
           print("build_log_simplifier.py: Invalid configuration: multiple message suppressions match the same message. Are some suppressions too broad?")
           print("")
           print("Line: '" + stripped + "'")
           print("")
           print(str(len(matching_suppressions)) + " Matching suppressions:")
           for suppression_text in matching_suppressions:
               print("'" + suppression_text + "'")
           exit(1)
        if len(matching_suppressions) < 1:
            result.append(line)
    return result

def collapse_consecutive_blank_lines(lines):
    result = []
    prev_blank = False
    for line in lines:
        if line.strip() == "":
            if not prev_blank:
                result.append(line)
            prev_blank = True
        else:
            result.append(line)
            prev_blank = False
    return result

# If multiple tasks have no output, this function removes all but the first and last
# For example, turns this:
#  > Task :a
#  > Task :b
#  > Task :c
#  > Task :d
# into this:
#  > Task :a
#  > Task ...
#  > Task :d
def collapse_tasks_having_no_output(lines):
    result = []
    pending_tasks = []
    for line in lines:
        is_task = line.startswith("> Task ")
        if is_task:
            pending_tasks.append(line)
        elif line.strip() == "":
            # If only blank lines occur between tasks, skip those blank lines
            if len(pending_tasks) > 0:
              pending_tasks.append(line)
            else:
              result.append(line)
        else:
            if len(pending_tasks) > 0:
                result.append(pending_tasks[0])
                if len(pending_tasks) > 2:
                    result.append("> Task ...\n")
                if len(pending_tasks) > 1:
                    result.append(pending_tasks[-1])
                pending_tasks = []
            result.append(line)
    return result

# Normalizes some filepaths to more easily simplify/skip some messages
def normalize_paths(lines):
    # get path of this script
    dir_of_this_script = pathlib.Path(__file__).parent.absolute()
    repository_root = os.path.dirname(dir_of_this_script)
    checkout_root = os.path.abspath(os.path.join(repository_root, "../.."))

    # get OUT_DIR and DIST_DIR
    out_dir = None
    dist_dir = None
    out_marker = "OUT_DIR="
    dist_marker = "DIST_DIR="
    for line in lines:
        if out_marker in line:
            out_dir = line.split(out_marker)[1].strip()
            continue
        if dist_marker in line:
            dist_dir = line.split(dist_marker)[1].strip()
            continue
        if out_dir is not None and dist_dir is not None:
            break

    # Remove any mentions of these paths, and replace them with consistent values
    remove_paths = collections.OrderedDict()
    if dist_dir is not None:
       remove_paths[dist_dir] = "$DIST_DIR"
    if out_dir is not None:
       remove_paths[out_dir] = "$OUT_DIR"
    remove_paths[repository_root] = "$SUPPORT"
    remove_paths[checkout_root] = "$CHECKOUT"
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

def generate_suggested_suppressions(messages, dest_path):
    # load existing suppressions
    infile = open(get_suppressions_path())
    suppression_lines = infile.readlines()
    infile.close()

    # generate new suggestions
    for line in messages:
        stripped = line.strip()
        if stripped.startswith("> Task"):
            # Don't need suppressions for task names; we automatically suppress them if we're suppressing their content
            continue
        # escape message
        escaped = re.escape(stripped)
        escaped = escaped.replace("\ ", " ") # spaces don't need to be escaped
        escaped = escaped + "\n"
        escaped = generalize_hashes(escaped)
        escaped = generalize_numbers(escaped)
        if not escaped in suppression_lines:
            suppression_lines.append(escaped)

    # write
    dest_file = open(dest_path, 'w')
    dest_file.write("".join(suppression_lines))
    dest_file.close()

def main():
    arguments = parser.parse_args()

    # read file
    log_path = arguments.log_path[0]
    infile = open(log_path)
    lines = infile.readlines()
    infile.close()
    # remove lines we're not interested in
    if not arguments.validate:
        lines = select_failing_task_output(lines)
    lines = normalize_paths(lines)
    lines = shorten_uninteresting_stack_frames(lines)
    lines = remove_known_uninteresting_lines(lines)
    lines = remove_configured_uninteresting_lines(lines, arguments.validate)
    lines = collapse_consecutive_blank_lines(lines)
    lines = collapse_tasks_having_no_output(lines)

    # process results
    if arguments.validate:
        if len(lines) != 0:
            print("")
            print("build_log_simplifier.py: Error: Found new messages!")
            print("")
            print("".join(lines))
            print("Error: build_log_simplifier.py found " + str(len(lines)) + " new messages found in " + log_path + ".")
            new_suppressions_path = log_path + ".ignore"
            generate_suggested_suppressions(lines, new_suppressions_path)
            print("")
            print("Please fix or suppress these new messages in the tool that generates them.")
            print("If you cannot, then you can exempt them by doing:")
            print("")
            print("  1. cp " + new_suppressions_path + " " + get_suppressions_path())
            print("  2. modify the new lines to be appropriately generalized")
            print("")
            print("Note that if you exempt these messages by updating the exemption file, it will only take affect for CI builds and not for Android Studio.")
            print("Additionally, adding more exemptions to this exemption file runs more slowly than fixing or suppressing the message where it is generated.")
            exit(1)
    else:
        print("".join(lines))

if __name__ == "__main__":
    main()
