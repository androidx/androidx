#!/usr/bin/python

"""
Copyright 2018 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import argparse, os, subprocess, sys

NAME_HELP = '''
  The name of the artifact you want to add to the prebuilts folder.
  E.g. android.arch.work:work-runtime-ktx:1.0.0-alpha07
'''


def main():
    """Parses the command line arguments, and executes the gradle script
    which downloads the maven artifacts.
    """
    os.chdir(os.path.dirname(sys.argv[0]))
    parser = argparse.ArgumentParser(
        description='Helps download maven artifacts to prebuilts.')
    parser.add_argument('-n', '--name', help=NAME_HELP,
                        required=True, dest='name')
    parse_result = parser.parse_args()
    artifact_name = parse_result.name
    command = './gradlew --build-file build.gradle.kts -PartifactName=%s' % (
        artifact_name)
    process = subprocess.Popen(command,
                               shell=True,
                               stdin=subprocess.PIPE)
    process.communicate()


if __name__ == '__main__':
    main()
