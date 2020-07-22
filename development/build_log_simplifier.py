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
import sys

def usage():
    print("""USAGE:
    Simplifies a build.log from hundreds of megabytes to <100 lines. Prints output to terminal.
    Pass this script a filepath to parse. You should be able to type "python3 build_log_simplifier.py"
    And then drag-and-drop a log file onto the terminal window to get its path.

    Sample usage: python3 development/build_log_simplifier.py Users/owengray/Desktop/build.log
    """)
    exit(1)

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
      "-XXLanguage:+NonParenthesizedAnnotationsOnFunctionalTypes",
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
                result += pending_tasks[0]
                if len(pending_tasks) > 2:
                    result += "> Task ...\n"
                if len(pending_tasks) > 1:
                    result += pending_tasks[-1]
                pending_tasks = []
            result.append(line)
    return result

try:
    build_log_loc = sys.argv[1]

    infile = open(build_log_loc)
    lines = infile.readlines()
    infile.close()

    lines = select_failing_task_output(lines)
    lines = shorten_uninteresting_stack_frames(lines)
    lines = remove_known_uninteresting_lines(lines)
    lines = collapse_consecutive_blank_lines(lines)
    lines = collapse_tasks_having_no_output(lines)

    print(len(lines))
    print(''.join(lines))
except Exception as e:
    print("An error occurred! "+str(e))
    usage()
