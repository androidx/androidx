#!/usr/bin/env python3

# Future improvements:
# - link bugs and code references
# - handle multiple bug references per line (currently only fetches one)
# - fetch more than the first ~900 entries from Buganizer
# - check if CLI tool is available + authenticated

import pathlib, re, subprocess

outfile = "closed_bugs.csv"

print("Searching code for bug references")
androidx_root = pathlib.Path(__file__).parent.parent.resolve()
grep_cmd = subprocess.run(
    ["egrep",

     # -I excludes binary files
     # -i case insensitive search
     # -n include line numbers
     # -r recursive
     "-Iinr",

     # regex for buganizer format ("b/123456789")
     "b\/[[:digit:]]{8,9}",

     # Files and directories to include and exclude
     "--exclude-dir=.idea",
     "--include=*.gradle",
     "--include=*.java",
     "--include=*.kt",
     "--include=*.xml",

     # Search all of the AndroidX repo checkout
     f"{androidx_root}"
     ],
    capture_output=True,
    text=True
)
raw_output_lines = grep_cmd.stdout.split("\n")

print("Cleaning up search results")
bug_dict = {} # mapping of bug id to list of filename + line number
for line in raw_output_lines:
    regex_result = re.search('b\/[0-9]{8,9}', line)
    if regex_result is not None:
        bug_id = regex_result.group(0).removeprefix("b/")
        file = line.split(":")[0].removeprefix(str(androidx_root))
        linenum = line.split(":")[1]

        if bug_id in bug_dict:
            matching_files = bug_dict[bug_id]
        else:
            matching_files = set()
        matching_files.add(f"{file}:{linenum}")
        bug_dict[bug_id] = matching_files
print(f"Found {len(bug_dict)} bugs")

# Create bug id query string.
# The CLI tool fails if there are too many bugs (>900?); only use the first 900.
bug_ids = list(bug_dict.keys())
bug_ids.sort()
joined_ids = "|".join(bug_ids[0:899])

# Query buganizer to determine which of the given bugs are closed.
# Store the issue, reporter, and assignee of the matching [closed] bugs.
print("Querying Buganizer to find how many of these bugs are resolved")
bugged_cmd = subprocess.run(
    ["bugged", "search", f"id:({joined_ids})", "status:closed", "--columns=issue,reporter,assignee"],
    capture_output=True,
    text=True  # capture output as String instead of byte sequence
)
closed_bug_list = bugged_cmd.stdout.split("\n")

# Remove header and trailing rows of Buganizer query result
closed_bug_list.pop(0)
closed_bug_list.pop()
print(f"{len(closed_bug_list)} have been resolved")

# Combine buganizer results with file search results and write to CSV
csv_str = "bug_id,reporter,assignee,files\n"
for line in closed_bug_list:
    elements = re.split(" +", line)
    bug_id = elements[0]
    reporter = elements[1]
    assignee = elements[2]
    matching_files = bug_dict[bug_id]
    line_str = f"b/{bug_id},{reporter},{assignee},"

    # The list of matching file(s) are enclosed in double quotes to preserve \n in the csv
    line_str += ("\"" + "\n".join(matching_files) + "\"")

    csv_str += line_str + "\n"

print(csv_str, file=open(outfile, 'w'))
print(f"Wrote results to {outfile}")