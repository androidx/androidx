#!/usr/bin/env python3

#
# Copyright 2026, The Android Open Source Project
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

"""Script that enforces Relnote: in the commit message has no issues."""

import argparse
import re
import sys


def main(args=None):
  parser = argparse.ArgumentParser(prog='validateCommitMessage')
  parser.add_argument('--commit', required=True)

  args = parser.parse_args(args)

  if args.commit:
    relnote_pattern = re.compile(
        r'^Relnote:\s*(?:"""(.*?)"""|"(.*?)"|'
        r'((?:(?!^\s*$|^[a-zA-Z0-9-]+:).|\n)*))',
        re.IGNORECASE | re.MULTILINE | re.DOTALL
    )

    all_case_case_words = []
    suggested_replacements = []
    for m in relnote_pattern.finditer(args.commit):
      full_match = m.group(0)
      match = next(g for g in m.groups() if g is not None)

      if m.groups()[0] is None and m.groups()[1] is None and '\n' in match.strip():
        print(
            'Error: Multi-line release notes must be surrounded by '
            'quotes (") or triple quotes (""").'
        )
        sys.exit(1)
      # Remove all words surrounded by backticks
      text_without_backticks = re.sub(r'`[^`]*`', '', match)
      # Find CaseCase words: words containing at least one lowercase letter
      # followed by an uppercase letter
      pattern = r'\b[a-zA-Z0-9_]*[a-z][A-Z][a-zA-Z0-9_]*\b'
      case_case_words = re.findall(pattern, text_without_backticks)

      if case_case_words:
        all_case_case_words.extend(case_case_words)
        parts = full_match.split('`')
        for i in range(0, len(parts), 2):
          for word in set(case_case_words):
            parts[i] = re.sub(r'\b' + re.escape(word) + r'\b', f'`{word}`', parts[i])
        suggested_replacements.append('`'.join(parts).strip())
      else:
        suggested_replacements.append(full_match.strip())

    if all_case_case_words:
      words = ', '.join(sorted(set(all_case_case_words)))
      replacements = '\n\n'.join(suggested_replacements)
      print(
          'Error: The following words use CamelCase in the `Relnote:` '
          'tag in the commit message, but are not surrounded by '
          f'backticks (`):\n{words}\n\n'
          f'Suggested replacement:\n{replacements}'
      )
      sys.exit(1)

  sys.exit(0)


if __name__ == '__main__':
  main()
