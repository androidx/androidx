#!/usr/bin/env python3

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

ANDROIDX_BUILD_ID_HELP = '''
  The build id of https://ci.android.com/builds/branches/aosp-androidx-main/grid?
  to use for fetching androidx prebuilts.
'''

METALAVA_BUILD_ID_HELP = '''
  The build id of https://ci.android.com/builds/branches/aosp-metalava-master/grid?
  to use for metalava prebuilt fetching.
'''

ALLOW_JETBRAINS_DEV_HELP = '''
  Whether or not to allow artifacts to be fetched from Jetbrains' dev repository
  E.g. https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev
'''

FETCH_KMP_ARTIFACTS_HELP = '''
  If set, we'll fetch all KMP artifacts as well
  E.g. passing -n "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1" will fetch native targets
  of coroutines-core as well.
'''

if sys.version_info[0] < 3: raise Exception("Python 2 is not supported by this script. If your system python calls python 2 after python 2 end-of-life on Jan 1 2020, you should probably change it.")

def main():
    """Parses the command line arguments, and executes the gradle script
    which downloads the maven artifacts.
    """
    os.chdir(os.path.dirname(sys.argv[0]))
    parser = argparse.ArgumentParser(
        description='Helps download maven artifacts to prebuilts.')
    parser.add_argument('-n', '--name', help=NAME_HELP,
                        required=True, dest='name')
    parser.add_argument('-ab', '--androidx-build-id', help=ANDROIDX_BUILD_ID_HELP,
                        required=False, dest='androidx_build_id')
    parser.add_argument('-mb', '--metalava-build-id', help=METALAVA_BUILD_ID_HELP,
                        required=False, dest='metalava_build_id')
    parser.add_argument('-ajd', '--allow-jetbrains-dev', help=ALLOW_JETBRAINS_DEV_HELP,
                        required=False, action='store_true')
    parser.add_argument('-kmp', '--fetch-kmp-artifacts', help=FETCH_KMP_ARTIFACTS_HELP,
                        required=False, action='store_true')
    parse_result = parser.parse_args()
    artifact_name = parse_result.name

    command = 'importMaven.sh %s' % (artifact_name)
    # AndroidX Build Id
    androidx_build_id = parse_result.androidx_build_id
    if (androidx_build_id):
      command = command + ' --androidx-build-id %s' % (androidx_build_id)
    # Metalava Build Id
    metalava_build_id = parse_result.metalava_build_id
    if (metalava_build_id):
      command = command + ' --metalava-build-id %s' % (metalava_build_id)
    if (parse_result.allow_jetbrains_dev):
      command = command + ' --allow-jetbrains-dev'

    my_directory = os.path.dirname(sys.argv[0])
    sys.exit("""
        This script is deprecated and will be removed. Please execute:
        %s/%s
        See %s/README.md for more details.
    """ % (my_directory, command, my_directory))

if __name__ == '__main__':
    main()
