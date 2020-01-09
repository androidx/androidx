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
import os,sys,json
from constants_and_utils import *

def usage():
    print("""
    Runs gradle build(s). You may want to use the target-specific shell scripts rather than call this directly
    This script recognizes and consumes the following arguments in any order (pasing all others through to gradle):
        doBoth           (equivalent to "-doAndroidX -doCompose". Runs both gradle builds, e.g. if you want to merge outputs)
        doCompose        (do the compose build)
        doAndroidX       (do the androidx build) (default if no '-do*' argument passed, due to being most common use case)
        doDryRun         (prevents commands form being run, but commands are still printed: dry-run the commands)

        DIST_DIR=<a path to a directory>    if this is not passed, the default value is out/dist.
                                            Should be absolute or relative to e.g. androidx-master-dev/

    In addition, OUT_DIR, ANDROID_HOME, and -p project directory are hardcoded, and will be overwritten if set

    Sample usage: time busytown/build.py DIST_DIR=out/dist assembleDebug test doBoth doDryRun -PuseMaxDepVersions --stacktrace --rerun-tasks > buildLog.log
    Other example usages appear in the shell scripts for each target (e.g. androidx.sh)
    """)
    exit(1)

def main():
    move_to_base_dir()
    which_builds, dry_run, dist_dir, gradle_args = parse_args()
    androidx_base_command, compose_base_command = compute_gradle_commands(dist_dir)
    if DO_ANDROIDX in which_builds: run_command(" ".join(androidx_base_command + gradle_args), dry_run=dry_run)
    if DO_COMPOSE in which_builds: run_command(" ".join(compose_base_command + gradle_args), dry_run=dry_run)

def parse_args():
    which_builds = [DEFAULT]
    dry_run = False
    dist_dir = "out/dist"
    gradle_args = []
    for arg in sys.argv:
        if any(arg.endswith(SKIPPED_SUFFIX) for SKIPPED_SUFFIX in SKIPPED_ARG_SUFFIXES): continue
        elif arg in BUILD_COMMANDS:
            which_builds.append(arg)
            which_builds.remove(DEFAULT)
        elif arg == DO_DRY_RUN: dry_run = True # if we are mocking, we print but do not run commands
        elif "dist_dir" in arg.lower():
            dist_dir = arg.split("=")[1]
            dist_dir = remove_suffix(dist_dir, '/')
            dist_dir = remove_suffix(dist_dir, '/ui')
    # --task keyword is part of gradle's help syntax, so if that's present they're not asking for help with this script
        elif any(help_keyword in arg for help_keyword in HELP_SYNTAX_LIST) and not "--task" in sys.argv: usage()
        elif arg.startswith("do"): print("ERROR:", arg, "is an invalid keyword"); usage()
        else: gradle_args.append(arg)
    if which_builds == [DEFAULT]: which_builds = [DO_ANDROIDX] # default is androidx
    if DO_BOTH in which_builds: which_builds = [DO_ANDROIDX, DO_COMPOSE]
    return which_builds, dry_run, dist_dir, gradle_args

def compute_gradle_commands(dist_dir):
    androidx_base_command = ["DIST_DIR="+dist_dir, OUT_DIR_ARG, ANDROID_HOME_ARG, GRADLEW, PROJECT_DIR_ARG]
    compose_base_command = ["DIST_DIR="+dist_dir+ui, OUT_DIR_ARG+ui, ANDROID_HOME_ARG, GRADLEW_COMPOSE, PROJECT_DIR_ARG+ui]
    return androidx_base_command, compose_base_command

if __name__ == "__main__": main()
