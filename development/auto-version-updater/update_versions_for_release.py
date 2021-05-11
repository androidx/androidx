#!/usr/bin/python3
#
# Copyright (C) 2020 The Android Open Source Project
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
import sys
import os
import argparse
from datetime import date
import subprocess
from shutil import rmtree
from shutil import copyfile
from distutils.dir_util import copy_tree
from distutils.dir_util import DistutilsFileError

# Import the JetpadClient from the parent directory
sys.path.append("..")
from JetpadClient import *

# cd into directory of script
os.chdir(os.path.dirname(os.path.abspath(__file__)))

FRAMEWORKS_SUPPORT_FP = os.path.abspath(os.path.join(os.getcwd(), '..', '..'))
LIBRARY_VERSIONS_REL = './buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt'
LIBRARY_VERSIONS_FP = os.path.join(FRAMEWORKS_SUPPORT_FP, LIBRARY_VERSIONS_REL)

# Set up input arguments
parser = argparse.ArgumentParser(
    description=("""Updates androidx library versions for a given release date.
        This script takes in a the release date as millisecond since the epoch,
        which is the unique id for the release in Jetpad.  It queries the
        Jetpad db, then creates an output json file with the release information.
        Finally, updates LibraryVersions.kt and runs updateApi."""))
parser.add_argument(
    'date',
    help='Milliseconds since epoch')
parser.add_argument(
    '--no-commit', action="store_true",
    help='If specified, this script will not commit the changes')

def print_e(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


def ask_yes_or_no(question):
    """Primpts a yes or no question to the user.

    Args:
        question: the question to asked.

    Returns:
        boolean representing yes or no.
    """
    while(True):
        reply = str(input(question+' (y/n): ')).lower().strip()
        if reply:
            if reply[0] == 'y': return True
            if reply[0] == 'n': return False
        print("Please respond with y/n")


def run_update_api():
    """Runs updateApi from the frameworks/support root.
    """
    gradle_cmd = "cd " + FRAMEWORKS_SUPPORT_FP + " && ./gradlew updateApi"
    try:
        subprocess.check_output(gradle_cmd, stderr=subprocess.STDOUT, shell=True)
    except subprocess.CalledProcessError:
        print_e('FAIL: Unable run updateApi with command: %s' % gradle_cmd)
        return None
    return True


def convert_prerelease_type_to_num(prerelease_type):
    """" Convert a prerelease suffix type to its numeric equivalent.

    Args:
        prerelease_type: the androidx SemVer version prerelease suffix.

    Returns:
        An int representing that suffix.
    """
    if prerelease_type == 'alpha':
        return 0
    if prerelease_type == 'beta':
        return 1
    if prerelease_type == 'rc':
        return 2
    # Stable defaults to 9
    return 9


def parse_version(version):
    """Converts a SemVer androidx version string into a list of ints.

    Accepts a SemVer androidx version string, such as "1.2.0-alpha02" and
    returns a list of integers representing the version in the following format:
    [<major>,<minor>,<bugfix>,<prerelease-suffix>,<prerelease-suffix-revision>]
    For example 1.2.0-alpha02" returns [1,2,0,0,2]

    Args:
        version: the androidx version string.

    Returns:
        a list of integers representing the version.
    """
    version_elements = version.split('-')[0].split('.')
    version_list = []
    for element in version_elements:
        version_list.append(int(element))
    # Check if version contains prerelease suffix
    version_prerelease_suffix = version.split('-')[-1]
    # Account for suffixes with only 1 suffix number, i.e. "1.1.0-alphaX"
    version_prerelease_suffix_rev = version_prerelease_suffix[-2:]
    version_prerelease_suffix_type = version_prerelease_suffix[:-2]
    if not version_prerelease_suffix_rev.isnumeric():
        version_prerelease_suffix_rev = version_prerelease_suffix[-1:]
        version_prerelease_suffix_type = version_prerelease_suffix[:-1]
    version_list.append(convert_prerelease_type_to_num(version_prerelease_suffix_type))
    if version.find("-") == -1:
        # Version contains no prerelease suffix
        version_list.append(99)
    else:
        version_list.append(int(version_prerelease_suffix_rev))
    return version_list


def get_higher_version(version_a, version_b):
    """Given two androidx SemVer versions, returns the greater one.

    Args:
        version_a: first version to be compared.
        version_b: second version to be compared.

    Returns:
        The greater of version_a and version_b.
    """
    version_a_list = parse_version(version_a)
    version_b_list = parse_version(version_b)
    for i in range(len(version_a_list)):
        if version_a_list[i] > version_b_list[i]:
            return version_a
        if version_a_list[i] < version_b_list[i]:
            return version_b
    return version_a


def should_update_version_in_library_versions_kt(line, new_version):
    """Returns true if the new_version is greater than the version in line.

    Args:
        line: a line in LibraryVersions.kt file.
        new_version: the version to check again.

    Returns:
        True if should update version, false otherwise.
    """
    if 'Version(' not in line:
        return False
    # Find the first piece with a numeric first character.
    split_current_line = line.split('"')
    i = 1
    while (not split_current_line[i][0].isnumeric() and
           i < len(split_current_line)):
        i += 1
    if i == len(split_current_line):
        return False
    version = split_current_line[i]
    return new_version == get_higher_version(version, new_version)


def increment_version(version):
    """Increments an androidx SemVer version.

    If the version is alpha or beta, the suffix is simply incremented.
    Otherwise, it chooses the next minor version.

    Args:
        version: the version to be incremented.

    Returns:
        The incremented version.
    """
    if "alpha" in version or "beta" in version:
        version_prerelease_suffix = version[-2:]
        new_version_prerelease_suffix = int(version_prerelease_suffix) + 1
        new_version = version[:-2] + "%02d" % (new_version_prerelease_suffix,)
    else:
        version_minor = version.split(".")[1]
        new_version_minor = str(int(version_minor) + 1)
        new_version = version.split(".")[0] + "." + new_version_minor + ".0-alpha01"
    return new_version


def update_versions_in_library_versions_kt(group_id, artifact_id, old_version):
    """Updates the versions in the LibrarVersions.kt file.

    This will take the old_version and increment it to find the appropriate
    new version.

    Args:
        group_id: group_id of the existing library
        artifact_id: artifact_id of the existing library
        old_version: old version of the existing library

    Returns:
        True if the version was updated, false otherwise.
    """
    group_id_variable_name = group_id.replace("androidx.","").replace(".","_").upper()
    artifact_id_variable_name = artifact_id.replace("androidx.","").replace("-","_").upper()
    new_version = increment_version(old_version)
    # Special case Compose because it uses the same version variable.
    if group_id_variable_name.startswith("COMPOSE"):
        group_id_variable_name = "COMPOSE"

    # Open file for reading and get all lines
    with open(LIBRARY_VERSIONS_FP, 'r') as f:
        library_versions_lines = f.readlines()
    num_lines = len(library_versions_lines)
    updated_version = False

    # First check any artifact ids with unique versions.
    for i in range(num_lines):
        cur_line = library_versions_lines[i]
        # Skip any line that doesn't declare a version
        if 'Version(' not in cur_line: continue
        version_variable_name = cur_line.split('val ')[1].split(' =')[0]
        if artifact_id_variable_name == version_variable_name:
            if not should_update_version_in_library_versions_kt(cur_line, new_version):
                break
            # Found the correct variable to modify
            if version_variable_name == "COMPOSE":
                new_version_line = ("    val COMPOSE = Version("
                                    "System.getenv(\"COMPOSE_CUSTOM_VERSION\") "
                                    "?: \"" + new_version + "\")\n")
            else:
                new_version_line = "    val " + version_variable_name + \
                                   " = Version(\"" + new_version + "\")\n"
            library_versions_lines[i] = new_version_line
            updated_version = True
            break

    if not updated_version:
        # Then check any group ids.
        for i in range(num_lines):
            cur_line = library_versions_lines[i]
            # Skip any line that doesn't declare a version
            if 'Version(' not in cur_line: continue
            version_variable_name = cur_line.split('val ')[1].split(' =')[0]
            if group_id_variable_name == version_variable_name:
                if not should_update_version_in_library_versions_kt(cur_line, new_version):
                    break
                # Found the correct variable to modify
                if version_variable_name == "COMPOSE":
                    new_version_line = ("    val COMPOSE = Version("
                                        "System.getenv(\"COMPOSE_CUSTOM_VERSION\") "
                                        "?: \"" + new_version + "\")\n")
                else:
                    new_version_line = "    val " + version_variable_name + \
                                       " = Version(\"" + new_version + "\")\n"
                library_versions_lines[i] = new_version_line
                updated_version = True
                break

    # Open file for writing and update all lines
    with open(LIBRARY_VERSIONS_FP, 'w') as f:
        f.writelines(library_versions_lines)
    return updated_version


def commit_updates(release_date):
    subprocess.check_call(['git', 'add', FRAMEWORKS_SUPPORT_FP])
    # ensure that we've actually made a change:
    staged_changes = subprocess.check_output('git diff --cached', stderr=subprocess.STDOUT, shell=True)
    if not staged_changes:
        return
    msg = "'Update versions for release id %s\n\nThis commit was generated from the command:\n%s\n\n%s'" % (release_date, " ".join(sys.argv), "Test: ./gradlew checkApi")
    subprocess.check_call(['git', 'commit', '-m', msg])
    subprocess.check_output('yes | repo upload . --current-branch --no-verify --label Presubmit-Ready+1', stderr=subprocess.STDOUT, shell=True)

def main(args):
    # Parse arguments and check for existence of build ID or file
    args = parser.parse_args()
    if not args.date:
        parser.error("You must specify a release date in Milliseconds since epoch")
        sys.exit(1)
    release_json_object = getJetpadRelease(args.date, False)
    non_updated_libraries = []
    for group_id in release_json_object["modules"]:
        for artifact in release_json_object["modules"][group_id]:
            updated = False
            if artifact["branch"].startswith("aosp-androidx-"):
                # Only update versions for artifacts released from the AOSP
                # androidx-main branch or from androidx release branches, but
                # not from any other development branch.
                updated = update_versions_in_library_versions_kt(group_id,
                    artifact["artifactId"], artifact["version"])
            if not updated:
                non_updated_libraries.append("%s:%s:%s" % (group_id,
                                             artifact["artifactId"],
                                             artifact["version"]))
    if non_updated_libraries:
        print("The following libraries were not updated:")
        for library in non_updated_libraries:
            print("\t", library)
    print("Updated library versions. \nRunning updateApi for the new "
          "versions, this may take a minute...", end='')
    if run_update_api():
        print("done.")
    else:
        print_e("failed.  Please investigate manually.")
    if not args.no_commit:
        commit_updates(args.date)


if __name__ == '__main__':
    main(sys.argv)
