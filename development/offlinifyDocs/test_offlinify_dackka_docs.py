#!/usr/bin/env python3

from filecmp import cmp, dircmp
from offlinify_dackka_docs import check_library, process_input, STYLE_FILENAME
import os
from pathlib import Path
from shutil import rmtree
import unittest

TEST_PATH = os.path.join(Path(__file__).parent.absolute(), 'testData')
OUTPUT_PATH = os.path.join(TEST_PATH, 'temp')

class TestOfflinifyDackkaDocs(unittest.TestCase):
  def setUp(self):
    os.mkdir(OUTPUT_PATH)

  def tearDown(self):
    rmtree(OUTPUT_PATH)

  """
  Verify that the directories have the same subdirectories and files.
  """
  def check_dirs(self, actual, expected):
    css_path = os.path.join(actual, STYLE_FILENAME)
    self.assertTrue(os.path.exists(css_path))

    dcmp = dircmp(actual, expected, hide=[STYLE_FILENAME, '.DS_Store'])
    self.check_dircmp(dcmp)

  """
  Recursively process the dircmp result to test the directories and files are equal.
  """
  def check_dircmp(self, dcmp):
    # Make sure there's nothing in one dir and not the other
    self.assertEqual(dcmp.left_only, [])
    self.assertEqual(dcmp.right_only, [])

    # dircmp checks that the same files exist, not that there contents are equal--verify that here
    for file in dcmp.common_files:
      left_path = os.path.join(dcmp.left, file)
      right_path = os.path.join(dcmp.right, file)
      self.assertTrue(cmp(left_path, right_path, shallow=False))

    for sub, sub_dcmp in dcmp.subdirs.items():
      self.check_dircmp(sub_dcmp)

  def test_all_libraries(self):
    input_path = os.path.join(TEST_PATH, 'input')
    process_input(input_path, OUTPUT_PATH, None)

    expected_output_path = os.path.join(TEST_PATH, 'outputAllLibs')
    self.check_dirs(OUTPUT_PATH, expected_output_path)

  def test_one_library(self):
    input_path = os.path.join(TEST_PATH, 'input')
    library = check_library('library', input_path, OUTPUT_PATH)
    process_input(input_path, OUTPUT_PATH, library)

    expected_output_path = os.path.join(TEST_PATH, 'outputOneLib')
    self.check_dirs(OUTPUT_PATH, expected_output_path)

if __name__ == '__main__':
  unittest.main()
