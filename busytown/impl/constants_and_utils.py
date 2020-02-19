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
 #
import os

# constants for parsing arguments
SKIPPED_ARG_SUFFIXES = ["python", ".py", "build"]
HELP_SYNTAX_LIST = ["help", "?", "man", "-h"]

DO_ANDROIDX, DO_COMPOSE, DO_BOTH, DO_DRY_RUN = "doAndroidx", "doCompose", "doBoth", "doDryRun"
BUILD_COMMANDS = [DO_ANDROIDX, DO_COMPOSE, DO_BOTH]
DEFAULT = "DEFAULT"

SOURCE_JARS, EXECUTION_DATA, BUILD_INFO, LIBRARY_METRICS = "mergeSourceJars", "mergeExecutionData", "mergeBuildInfo", "mergeLibraryMetrics"
MERGE_COMMANDS = [SOURCE_JARS, EXECUTION_DATA, BUILD_INFO, LIBRARY_METRICS]

# utility function to allow running these scripts from any directory with paths relative to androidx-master-dev/
def move_to_base_dir(print_pwd=True):
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    os.chdir("../../../../")
    if print_pwd: print("Currently in", os.getcwd())

move_to_base_dir(print_pwd=False)

# constants for the static parts of the build commands setting environment variables
OUT_DIR_ARG = "OUT_DIR=out"
ANDROID_HOME_ARG = "ANDROID_HOME=" + os.path.realpath("prebuilts/fullsdk-linux")
ui = "/ui"
PROJECT_DIR_ARG = "-p frameworks/support"

GRADLEW = "frameworks/support/gradlew"
GRADLEW_COMPOSE = "frameworks/support/ui/gradlew"

# a few more utility functions
def remove_suffix(string, suffix):
    if string.endswith(suffix): return string[:-len(suffix)]
    else: return string

def run_command(commandText, dry_run=False):
    if not dry_run:
        print('Running "' + commandText + '"')
        result = os.system(commandText)
        assert(os.WEXITSTATUS(result)) == 0 # stop if the command failed
    else:
        print('Not running "' + commandText + '"')
