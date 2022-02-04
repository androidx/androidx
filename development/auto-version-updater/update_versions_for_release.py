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
import toml

# Import the JetpadClient from the parent directory
sys.path.append("..")
from JetpadClient import *

# cd into directory of script
os.chdir(os.path.dirname(os.path.abspath(__file__)))

FRAMEWORKS_SUPPORT_FP = os.path.abspath(os.path.join(os.getcwd(), '..', '..'))
LIBRARY_VERSIONS_REL = './libraryversions.toml'
LIBRARY_VERSIONS_FP = os.path.join(FRAMEWORKS_SUPPORT_FP, LIBRARY_VERSIONS_REL)
COMPOSE_VERSION_REL = './compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/ComposeVersion.kt'
COMPOSE_VERSION_FP = os.path.join(FRAMEWORKS_SUPPORT_FP, COMPOSE_VERSION_REL)
VERSION_CHECKER_REL = './compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/VersionChecker.kt'
VERSION_CHECKER_FP = os.path.join(FRAMEWORKS_SUPPORT_FP, VERSION_CHECKER_REL)

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


def should_update_version_in_library_versions_toml(old_version, new_version):
    """Returns true if the new_version is greater than the version in line.

    Args:
        old_version: the old version from libraryversions.toml file.
        new_version: the version to check again.

    Returns:
        True if should update version, false otherwise.
    """
    return new_version == get_higher_version(old_version, new_version)


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


def increment_version_within_minor_version(version):
    """Increments an androidx SemVer version without bumping the minor version.

    Args:
        version: the version to be incremented.

    Returns:
        The incremented version.
    """
    if "alpha" in version or "beta" in version or "rc0" in version:
        version_prerelease_suffix = version[-2:]
        new_version_prerelease_suffix = int(version_prerelease_suffix) + 1
        new_version = version[:-2] + "%02d" % (new_version_prerelease_suffix,)
    else:
        bugfix_version = version.split(".")[2]
        new_bugfix_version = str(int(bugfix_version) + 1)
        new_version = ".".join(version.split(".")[0:2]) + "." + new_bugfix_version
    return new_version


def update_versions_in_library_versions_toml(group_id, artifact_id, old_version):
    """Updates the versions in the libraryversions.toml file.

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
    if (group_id_variable_name.startswith("COMPOSE") and
        group_id_variable_name != "COMPOSE_MATERIAL3"):
            group_id_variable_name = "COMPOSE"

    # Open toml file
    library_versions = toml.load(LIBRARY_VERSIONS_FP)
    updated_version = False

    # First check any artifact ids with unique versions.
    if artifact_id_variable_name in library_versions["versions"]:
        old_version = library_versions["versions"][artifact_id_variable_name]
        if should_update_version_in_library_versions_toml(old_version, new_version):
            library_versions["versions"][artifact_id_variable_name] = new_version
            updated_version = True

    if not updated_version:
        # Then check any group ids.
        if group_id_variable_name in library_versions["versions"]:
            old_version = library_versions["versions"][group_id_variable_name]
            if should_update_version_in_library_versions_toml(old_version, new_version):
                library_versions["versions"][group_id_variable_name] = new_version
                updated_version = True

    # sort the entries
    library_versions["versions"] = dict(sorted(library_versions["versions"].items()))

    # Open file for writing and write toml back
    with open(LIBRARY_VERSIONS_FP, 'w') as f:
        toml.dump(library_versions, f, encoder=toml.TomlPreserveInlineDictEncoder())
    return updated_version



def parse_version_checker_line(line):
    runtime_version_str = line.split(' to ')[0].strip()
    if runtime_version_str.isnumeric():
        runtime_version = int(runtime_version_str)
    else:
        print_e("Could not parse Compose runtime version in %s. "
                "Skipping the update of the Compose runtime version."
                % VERSION_CHECKER_FP)
        return None, None
    compose_version = line.split(' to ')[1].split('"')[1].strip()
    return (runtime_version, compose_version)



def get_compose_to_runtime_version_map(compose_to_runtime_version_map):
    """Generates the compose to runtime version map from VersionChecker.kt

    Args:
        lines: lines from VERSION_CHECKER_FP (VersionChecker.kt)
        compose_to_runtime_version_map: map to populate
    Returns:
        highest_index: the highest line index reached in the file
    """
    # Highest line index in the map, used for new alpha/beta versions.
    highest_index = -1
    with open(VERSION_CHECKER_FP, 'r') as f:
        version_checker_lines = f.readlines()
    num_lines = len(version_checker_lines)
    for i in range(num_lines):
        cur_line = version_checker_lines[i]
        # Skip any line that doesn't declare a version map.
        if ' to "' not in cur_line: continue
        (runtime_version, compose_version) = parse_version_checker_line(cur_line)
        # If it returned none, we couldn't parse properly, so return.
        if not runtime_version: return
        compose_to_runtime_version_map[compose_version] = {
            "runtime_version": runtime_version,
            "file_index": i
        }
        if i > highest_index: highest_index = i
    return highest_index

def update_compose_runtime_version(group_id, artifact_id, old_version):
    """Updates the compose runtime version in ComposeVersion.kt / VersionChecker.kt

    This will take the old_version and increment it to find the appropriate
    new version.

    Internal version = current_version + 100 if alpha/beta + 1 if rc/stable

    Args:
        group_id: group_id of the existing library
        artifact_id: artifact_id of the existing library
        old_version: old version of the existing library

    Returns:
        Nothing
    """
    # New runtime version that will be used
    new_compose_runtime_version = 0
    # New compose version that will be used
    updated_compose_version = increment_version_within_minor_version(old_version)
    # Highest index in the file, used for new alpha/beta versions.
    highest_index = -1
    # Map of the compose version to it's runtime version.
    compose_to_runtime_version_map = {}

    highest_index = get_compose_to_runtime_version_map(compose_to_runtime_version_map)

    # If the value has already been added, we're done!  We can return!
    if updated_compose_version in compose_to_runtime_version_map.keys():
        return
    # If the old value isn't in the map, we can't be sure how to update,
    # so we skip.
    if old_version not in compose_to_runtime_version_map.keys():
        print_e("Could not parse Compose runtime version in %s. "
                "Skipping the update of the Compose runtime version."
                % VERSION_CHECKER_FP)
        return

    # Open file for reading and get all lines, so we can update the current compose version.
    with open(VERSION_CHECKER_FP, 'r') as f:
        version_checker_lines = f.readlines()
    num_lines = len(version_checker_lines)

    for i in range(num_lines):
        cur_line = version_checker_lines[i]
        # Skip any line that doesn't declare the compiler/compose version
        if 'const val compilerVersion: String = ' not in cur_line: continue
        current_version = cur_line.split('const val compilerVersion: String = ')[1].strip('"\n')
        # Only update if we have a higher version.
        version_to_keep = get_higher_version(current_version, updated_compose_version)
        new_version_line = '        const val compilerVersion: String = "%s"\n' % version_to_keep
        version_checker_lines[i] = new_version_line
        break

    old_runtime_version = compose_to_runtime_version_map[old_version]["runtime_version"]
    if "alpha" in updated_compose_version or "beta" in updated_compose_version:
        new_compose_runtime_version = old_runtime_version + 100
    else:
        new_compose_runtime_version = old_runtime_version + 1
    new_version_line = '            %d to "%s",\n' % (new_compose_runtime_version, updated_compose_version)
    insert_line = compose_to_runtime_version_map[old_version]["file_index"] + 1
    version_checker_lines.insert(insert_line, new_version_line)

    # Open file for reading and get all lines
    with open(COMPOSE_VERSION_FP, 'r') as f:
        compose_version_lines = f.readlines()
    num_lines = len(compose_version_lines)

    for i in range(num_lines):
        cur_line = compose_version_lines[i]
        # Skip any line that doesn't declare the version
        if 'const val version: Int = ' not in cur_line: continue
        version_str = cur_line.split('const val version: Int = ')[1].strip()

        if version_str.isnumeric():
            current_runtime_version = int(version_str)
        else:
            print_e("Could not parse Compose runtime version in %s."
                    "Skipping the update of the Compose runtime version."
                    % COMPOSE_VERSION_FP)
            return
        # Only update if we have a new higher version.
        if current_runtime_version < new_compose_runtime_version:
            new_version_line = '    const val version: Int = %d\n' % new_compose_runtime_version
            compose_version_lines[i] = new_version_line
        break


    # Open file for writing and update all lines
    with open(COMPOSE_VERSION_FP, 'w') as f:
        f.writelines(compose_version_lines)

    # Open file for writing and update all lines
    with open(VERSION_CHECKER_FP, 'w') as f:
        f.writelines(version_checker_lines)

    return


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
                updated = update_versions_in_library_versions_toml(group_id,
                                                                   artifact["artifactId"], artifact["version"])
            if (group_id == "androidx.compose.runtime" and
                artifact["artifactId"] == "runtime"):
                update_compose_runtime_version(group_id,
                                               artifact["artifactId"],
                                               artifact["version"])
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
