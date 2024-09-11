#!/usr/bin/env python3

# This script helps to rename and copy the golden images required to run the screenshot tests to the golden directory.
# To generate new golden images for a test, run the test on the emulator with the required device type, and download the screenshots from the device using adb
# The screenshots directory contains test.textproto files describing the screenshots and the folder they should be placed in to run the tests successfully
#
# Example of a textproto file:
# androidx.test.screenshot.proto.ScreenshotResultProto$ScreenshotResult@37d7ba5f
# current_screenshot_file_name: "androidx.emoji2.emojipicker.EmojiViewTest_testClear_sdk_g64_arm64_actual.png"
# location_of_golden_in_repo: "emoji2/emoji2-emojipicker/draw_and_clear_sdk_g64_arm64.png"
# repo_root_path: "platform/frameworks/support-golden"
# result: MISSING_GOLDEN
# result_value: 3

import argparse
import os
import shutil

def main():
  parser = argparse.ArgumentParser()
  parser.add_argument("--screenshots-dir-path", required=True, dest="input_dir")
  args = parser.parse_args()
  rename_and_copy_files_to_new_location(args.input_dir)

def rename_and_copy_files_to_new_location(input_path):
  #Relative path to golden directory
  GOLDENDIR_REL_PATH = '../../../golden/'
  SCRIPT_PATH = os.path.realpath(os.path.dirname(__file__))
  GOLDENDIR_FULL_PATH = os.path.join(SCRIPT_PATH, GOLDENDIR_REL_PATH)
  output_path = GOLDENDIR_FULL_PATH

  # Fetching all screenshots
  files = os.listdir(input_path)
  copied_files = 0
  for file in files:
    if not file.endswith('.textproto'):
       continue
    current_file_path = os.path.join(input_path, file)
    with open(current_file_path) as current_file:
        input = ''
        output = ''
        for line in current_file.readlines():
            if line.startswith('current_screenshot_file_name'):
                input = input_path+line.split(':')[1].strip().replace('"', '')
            elif line.startswith('location_of_golden_in_repo'):
                output = output_path+line.split(':')[1].strip().replace('"', '')
        if input == '' or output == '':
           print('There was an error processing file -', file)
        else:
          shutil.copy(input, output)
          copied_files += 1

  print(copied_files, 'golden images copied successfully')

if __name__ == "__main__":
  main()