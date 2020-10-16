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

# cd into directory of script
os.chdir(os.path.dirname(os.path.abspath(__file__)))

FRAMEWORKS_SUPPORT_FP = os.path.abspath(os.path.join(os.getcwd(), '..', '..'))
SAMPLE_OWNERS_FP = os.path.abspath(os.path.join(os.getcwd(), 'groupId', 'OWNERS'))
SAMPLE_SRC_FP = os.path.abspath(os.path.join(os.getcwd(), 'groupId', 'artifactId'))
SETTINGS_GRADLE_FP = os.path.abspath(os.path.join(os.getcwd(), '..', '..', "settings.gradle"))
LIBRARY_VERSIONS_REL = './buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt'
LIBRARY_VERSIONS_FP = os.path.join(FRAMEWORKS_SUPPORT_FP, LIBRARY_VERSIONS_REL)
LIBRARY_GROUPS_REL = './buildSrc/src/main/kotlin/androidx/build/LibraryGroups.kt'
LIBRARY_GROUPS_FP = os.path.join(FRAMEWORKS_SUPPORT_FP, LIBRARY_GROUPS_REL)

# Set up input arguments
parser = argparse.ArgumentParser(
    description=("""Genereates new project in androidx."""))
parser.add_argument(
    'group_id',
    help='group_id for the new library')
parser.add_argument(
    'artifact_id',
    help='artifact_id for the new library')

def print_e(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

def cp(src_path_dir, dst_path_dir):
    """Copies all files in the src_path_dir into the dst_path_dir

    Args:
        src_path_dir: the source directory, which must exist
        dst_path_dir: the distination directory
    """
    if not os.path.exists(dst_path_dir):
        os.makedirs(dst_path_dir)
    if not os.path.exists(src_path_dir):
        print_e('cp error: Source path %s does not exist.' % src_path_dir)
        return None
    try:
        copy_tree(src_path_dir, dst_path_dir)
    except DistutilsFileError as err:
        print_e('FAIL: Unable to copy %s to destination %s' % (src_path_dir, dst_path_dir))
        return None
    return dst_path_dir

def rm(path):
    if os.path.isdir(path):
        rmtree(path)
    elif os.path.exists(path):
        os.remove(path)

def generate_package_name(group_id, artifact_id):
    final_group_id_word = group_id.split(".")[-1]
    artifact_id_suffix = artifact_id.replace(final_group_id_word, "")
    artifact_id_suffix = artifact_id_suffix.replace("-", ".")
    return group_id + artifact_id_suffix

def validate_name(group_id, artifact_id):
    if not group_id.startswith("androidx."):
        print_e("Group ID must start with androidx.")
        return False
    final_group_id_word = group_id.split(".")[-1]
    if not artifact_id.startswith(final_group_id_word):
        print_e("Artifact ID must use the final word in the group Id " + \
                "as the prefix.  For example, `androidx.foo.bar:bar-qux`" + \
                "or `androidx.foo:foo-bar` are valid names.")
        return False
    return True

def get_year():
    return str(date.today().year)

def get_group_id_version_macro(group_id):
    group_id_version_macro = group_id.replace("androidx.", "").replace(".", "-").upper()
    if group_id.startswith("androidx.compose"):
        group_id_version_macro = "COMPOSE"
    return group_id_version_macro

def sed(before, after, file):
    with open(file) as f:
       file_contents = f.read()
    new_file_contents = file_contents.replace(before, after)
    # write back the file
    with open(file,"w") as f:
        f.write(new_file_contents)

def get_gradle_project_coordinates(group_id, artifact_id):
    coordinates = group_id.replace("androidx", "").replace(".",":")
    coordinates += ":" + artifact_id
    return coordinates

def run_update_api(group_id, artifact_id):
    gradle_coordinates = get_gradle_project_coordinates(group_id, artifact_id)
    gradle_cmd = "cd " + FRAMEWORKS_SUPPORT_FP + " && ./gradlew " + gradle_coordinates + ":updateApi"
    try:
        subprocess.check_output(gradle_cmd, stderr=subprocess.STDOUT, shell=True)
    except subprocess.CalledProcessError:
        print_e('FAIL: Unable run updateApi with command: %s' % gradle_cmd)
        return None

def get_group_id_path(group_id):
    """Generates the group ID filepath

    Given androidx.foo.bar, the structure will be:
    frameworks/support/foo/bar

    Args:
        group_id: group_id of the new library
    """
    return FRAMEWORKS_SUPPORT_FP + "/" + group_id.replace("androidx.", "").replace(".", "/")

def get_full_artifact_path(group_id, artifact_id):
    """Generates the full artifact ID filepath

    Given androidx.foo.bar:bar-qux, the structure will be:
    frameworks/support/foo/bar/bar-qux

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
    """
    group_id_path = get_group_id_path(group_id)
    return group_id_path + "/" + artifact_id


def create_directories(group_id, artifact_id):
    """Creates the standard directories for the given group_id and artifact_id.

    Given androidx.foo.bar:bar-qux, the structure will be:
    frameworks/support/foo/bar/bar-qux/build.gradle
    frameworks/support/foo/bar/bar-qux/src/main/AndroidManifest.xml
    frameworks/support/foo/bar/bar-qux/src/androidTest/AndroidManifest.xml
    frameworks/support/foo/bar/bar-qux/api/current.txt

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
    """
    full_artifact_path = get_full_artifact_path(group_id, artifact_id)
    if not os.path.exists(full_artifact_path):
        os.makedirs(full_artifact_path)

    # Copy over the OWNERS file if it doesn't exit
    group_id_path = get_group_id_path(group_id)
    if not os.path.exists(group_id_path + "/OWNERS"):
        copyfile(SAMPLE_OWNERS_FP, group_id_path + "/OWNERS")

    # Copy the full src structure
    cp(SAMPLE_SRC_FP, full_artifact_path)

    # Populate the YEAR
    year = get_year()
    sed("<YEAR>", year, full_artifact_path + "/build.gradle")
    sed("<YEAR>", year, full_artifact_path + "/src/androidTest/AndroidManifest.xml")
    sed("<YEAR>", year, full_artifact_path + "/src/main/AndroidManifest.xml")
    # Populate the PACKAGE
    package = generate_package_name(group_id, artifact_id)
    sed("<PACKAGE>", package, full_artifact_path + "/src/androidTest/AndroidManifest.xml")
    sed("<PACKAGE>", package, full_artifact_path + "/src/main/AndroidManifest.xml")
    # Populate the VERSION macro
    group_id_version_macro = get_group_id_version_macro(group_id)
    sed("<GROUPID>", group_id_version_macro, full_artifact_path + "/build.gradle")
    # Update the name and description in the build.gradle
    sed("<NAME>", group_id + ":" + artifact_id, full_artifact_path + "/build.gradle")

def get_new_settings_gradle_line(group_id, artifact_id):
    """Generates the line needed for frameworks/support/settings.gradle.

    For a library androidx.foo.bar:bar-qux, the new gradle command will be
    the form:
    ./gradlew :foo:bar:bar-qux:<command>

    We special case on compose that we can properly populate the build type
    of either MAIN or COMPOSE.

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
    """

    build_type = "MAIN"
    if group_id.contains("compose") or group_id.contains("androidx.ui"):
        build_type = "COMPOSE"

    gradle_cmd = get_gradle_project_coordinates(group_id, artifact_id)
    sub_filepath = group_id.replace("androidx.", "").replace(".", "/") + \
                   "/" + artifact_id
    return "includeProject(\"" + gradle_cmd + \
           "\", \"" + sub_filepath + "\", [BuildType." + build_type + "])\n"

def update_settings_gradle(group_id, artifact_id):
    """Updates frameworks/support/settings.gradle with the new library.

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
    """
    # Open file for reading and get all lines
    with open(SETTINGS_GRADLE_FP, 'r') as f:
        settings_gradle_lines = f.readlines()
    num_lines = len(settings_gradle_lines)

    new_settings_gradle_line = get_new_settings_gradle_line(group_id, artifact_id)
    for i in range(num_lines):
        cur_line = settings_gradle_lines[i]
        if "includeProject" not in cur_line:
            continue
        # Iterate through until you found the alphabetical place to insert the new line
        if new_settings_gradle_line <= cur_line:
            insert_line = i
            break
        else:
            insert_line = i + 1
    settings_gradle_lines.insert(insert_line, new_settings_gradle_line)

    # Open file for writing and update all lines
    with open(SETTINGS_GRADLE_FP, 'w') as f:
        f.writelines(settings_gradle_lines)


def insert_new_group_id_into_library_versions_kt(group_id, artifact_id):
    """Inserts a group ID into the LibrarVersions.kt file.

    If one already exists, then this function just returns and reuses
    the existing one.

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
    """
    new_group_id_insert_line = 0
    new_group_id_variable_name = group_id.replace("androidx.","").replace(".","_").upper()

    # Open file for reading and get all lines
    with open(LIBRARY_VERSIONS_FP, 'r') as f:
        library_versions_lines = f.readlines()
    num_lines = len(library_versions_lines)

    for i in range(num_lines):
        cur_line = library_versions_lines[i]
        # Skip any line that doesn't declare a version
        if 'Version(' not in cur_line: continue
        group_id_variable_name = cur_line.split('val ')[1].split(' =')[0]
        # If the group ID already exists, we're done!
        if group_id_variable_name == new_group_id_variable_name:
            return
        # Iterate through until you found the alphabetical place to
        # insert the new groupId
        if new_group_id_variable_name <= group_id_variable_name:
            new_group_id_insert_line = i
            break
        else:
            new_group_id_insert_line = i + 1

    # Failed to find a spot for the new groupID, so append it to the end
    # of the LibraryGroup list
    library_versions_lines.insert(new_group_id_insert_line,
                                  "    val " + new_group_id_variable_name + \
                                  " = Version(\"1.0.0-alpha01\")\n")

    # Open file for writing and update all lines
    with open(LIBRARY_VERSIONS_FP, 'w') as f:
        f.writelines(library_versions_lines)
    insert_new_group_id_int_library_groups_kt(group_id, artifact_id)


def insert_new_group_id_int_library_groups_kt(group_id, artifact_id):
    """Inserts a group ID into the LibraryGroups.kt file.

    If one already exists, then this function just returns and resuses
    the existing one. Defaults to requires same version.

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
    """
    new_group_id_insert_line = 0
    new_group_id_variable_name = group_id.replace("androidx.","").replace(".","_").upper()

    # Open file for reading and get all lines
    with open(LIBRARY_GROUPS_FP, 'r') as f:
        library_groups_lines = f.readlines()
    num_lines = len(library_groups_lines)

    for i in range(num_lines):
        cur_line = library_groups_lines[i]
        # Skip any line that doesn't declare a version
        if 'LibraryGroup(' not in cur_line: continue
        group_id_variable_name = cur_line.split('val ')[1].split(' =')[0]
        # If the group ID already exists, we're done!
        if group_id_variable_name == new_group_id_variable_name:
            return
        # Iterate through until you found the alphabetical place to
        # insert the new groupId
        if new_group_id_variable_name <= group_id_variable_name:
            new_group_id_insert_line = i
            break
        else:
            new_group_id_insert_line = i + 1

    # Failed to find a spot for the new groupID, so append it to the end of
    # the LibraryGroup list
    library_groups_lines.insert(new_group_id_insert_line,
                                "    val " + new_group_id_variable_name + \
                                " = LibraryGroup(\"" + group_id + \
                                "\", LibraryVersions." + \
                                new_group_id_variable_name + ")\n")

    # Open file for writing and update all lines
    with open(LIBRARY_GROUPS_FP, 'w') as f:
        f.writelines(library_groups_lines)

def main(args):
    # Parse arguments and check for existence of build ID or file
    args = parser.parse_args()
    if not args.group_id or not args.artifact_id:
        parser.error("You must specify a group_id and an artifact_id")
        sys.exit(1)
    if not validate_name(args.group_id, args.artifact_id):
        sys.exit(1)

    create_directories(args.group_id, args.artifact_id)
    update_settings_gradle(args.group_id, args.artifact_id)
    insert_new_group_id_into_library_versions_kt(args.group_id,
                                                 args.artifact_id)
    print("Success. Running updateApi for the new library, " + \
          "this may take a minute.")
    run_update_api(args.group_id, args.artifact_id)

if __name__ == '__main__':
    main(sys.argv)