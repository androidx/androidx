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

import io
import unittest
from unittest import mock

import validate


class ValidateTests(unittest.TestCase):

  @mock.patch('sys.argv', ['validate.py', '--commit', 'Valid commit.'])
  def test_valid_commit(self):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 0)

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit', 'Outside of relnote CaseCase valid.']
  )
  def test_valid_casecase_in_backticks(self):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 0)

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit', 'Relnote: This has an InvalidCase word.']
  )
  @mock.patch('sys.stdout', new_callable=io.StringIO)
  def test_invalid_casecase(self, mock_stdout):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 1)
    self.assertIn(
        'Error: The following words use CamelCase in the `Relnote:` '
        'tag in the commit message, but are not surrounded by '
        'backticks (`):\nInvalidCase',
        mock_stdout.getvalue()
    )

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit', 'Relnote: InvalidCase AnotherInvalidCase']
  )
  @mock.patch('sys.stdout', new_callable=io.StringIO)
  def test_multiple_invalid_casecase(self, mock_stdout):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 1)
    output = mock_stdout.getvalue()
    self.assertIn(
        'Error: The following words use CamelCase in the `Relnote:` '
        'tag in the commit message, but are not surrounded by '
        'backticks (`):',
        output
    )
    self.assertIn('InvalidCase', output)
    self.assertIn('AnotherInvalidCase', output)

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit', 'Relnote: Check `ValidC` fail InvalidC.']
  )
  @mock.patch('sys.stdout', new_callable=io.StringIO)
  def test_mixed_casecase(self, mock_stdout):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 1)
    output = mock_stdout.getvalue()
    self.assertIn('InvalidC', output)
    self.assertNotIn('ValidC', output)

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit',
       'Relnote: """Long relnote\nsplit multiple lines CamelCase entries"""']
  )
  @mock.patch('sys.stdout', new_callable=io.StringIO)
  def test_multiline_relnote_with_quotes(self, mock_stdout):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 1)
    self.assertIn(
        'Error: The following words use CamelCase in the `Relnote:` '
        'tag in the commit message, but are not surrounded by '
        'backticks (`):\nCamelCase',
        mock_stdout.getvalue()
    )

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit',
       'Relnote: """Long valid relnote\nmultiple lines `CamelCase` entries"""']
  )
  def test_multiline_valid_relnote_with_quotes(self):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 0)

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit',
       'Relnote: "Long relnote\nmultiple lines CamelCase entries"']
  )
  @mock.patch('sys.stdout', new_callable=io.StringIO)
  def test_multiline_relnote_with_double_quotes_fails(self, mock_stdout):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 1)
    self.assertIn('Error: Multi-line release notes must be surrounded by triple quotes (""").', mock_stdout.getvalue())

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit',
       "Relnote: 'Long relnote\nmultiple lines CamelCase entries'"]
  )
  @mock.patch('sys.stdout', new_callable=io.StringIO)
  def test_multiline_relnote_with_single_quotes_fails(self, mock_stdout):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 1)
    self.assertIn('Error: Multi-line release notes must be surrounded by triple quotes (""").', mock_stdout.getvalue())

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit',
       'Relnote: "Valid long relnote\nmultiple lines `CamelCase` entries"']
  )
  @mock.patch('sys.stdout', new_callable=io.StringIO)
  def test_multiline_relnote_with_double_quotes_and_backticks_fails(self, mock_stdout):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 1)
    self.assertIn('Error: Multi-line release notes must be surrounded by triple quotes (""").', mock_stdout.getvalue())

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit',
       'This is a message\n\nRelnote: This is a very long\nrelnote message here\nBug: 123\nTest: Things']
  )
  @mock.patch('sys.stdout', new_callable=io.StringIO)
  def test_multiline_relnote_no_quotes(self, mock_stdout):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 1)
    self.assertIn('Error: Multi-line release notes must be surrounded by triple quotes (""").', mock_stdout.getvalue())

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit',
       'This is a message\n\nRelnote: """This is a very long\nrelnote message here"""\nBug: 123\nTest: Things']
  )
  def test_multiline_relnote_triple_quotes(self):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 0)

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit',
       'This is a message\n\nRelnote: This is a single line relnote message here\nBug: 123\nTest: Things']
  )
  def test_single_line_relnote_no_quotes(self):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 0)

  @mock.patch(
      'sys.argv',
      ['validate.py', '--commit',
       'This is a message\n\nRelnote: """This is a very long\nrelnote message here"""\nRelnote: Bad long\nrelnote\nBug: 123\nTest: Things']
  )
  @mock.patch('sys.stdout', new_callable=io.StringIO)
  def test_multiple_relnotes_one_bad(self, mock_stdout):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 1)
    self.assertIn('Error: Multi-line release notes must be surrounded by triple quotes (""").', mock_stdout.getvalue())

  @mock.patch('sys.argv', ['validate.py'])
  @mock.patch('sys.stderr', new_callable=io.StringIO)
  def test_no_commit_arg(self, mock_stderr):
    with self.assertRaises(SystemExit) as cm:
      validate.main()
    self.assertEqual(cm.exception.code, 2)
    self.assertIn(
        'the following arguments are required: --commit',
        mock_stderr.getvalue()
    )


if __name__ == '__main__':
  unittest.main()
