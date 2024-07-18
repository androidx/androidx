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
from enum import Enum
from textwrap import dedent
from shutil import rmtree
from shutil import copyfile
from distutils.dir_util import copy_tree
from distutils.dir_util import DistutilsFileError
import re

try:
    # non-default python3 module, be helpful if it is missing
    import toml
except ModuleNotFoundError as e:
    print(e)
    print("Consider running `pip install toml` to install this module")
    exit(-1)

# cd into directory of script
os.chdir(os.path.dirname(os.path.abspath(__file__)))

FRAMEWORKS_SUPPORT_FP = os.path.abspath(os.path.join(os.getcwd(), '..', '..'))
SAMPLE_OWNERS_FP = os.path.abspath(os.path.join(os.getcwd(), 'kotlin-template', 'groupId', 'OWNERS'))
SAMPLE_JAVA_SRC_FP = os.path.abspath(os.path.join(os.getcwd(), 'java-template', 'groupId', 'artifactId'))
SAMPLE_KOTLIN_SRC_FP = os.path.abspath(os.path.join(os.getcwd(), 'kotlin-template', 'groupId', 'artifactId'))
SAMPLE_COMPOSE_SRC_FP = os.path.abspath(os.path.join(os.getcwd(), 'compose-template', 'groupId', 'artifactId'))
NATIVE_SRC_FP = os.path.abspath(os.path.join(os.getcwd(), 'native-template', 'groupId', 'artifactId'))
SETTINGS_GRADLE_FP = os.path.abspath(os.path.join(os.getcwd(), '..', '..', "settings.gradle"))
LIBRARY_VERSIONS_REL = './libraryversions.toml'
LIBRARY_VERSIONS_FP = os.path.join(FRAMEWORKS_SUPPORT_FP, LIBRARY_VERSIONS_REL)
DOCS_TOT_BUILD_GRADLE_REL = './docs-tip-of-tree/build.gradle'
DOCS_TOT_BUILD_GRADLE_FP = os.path.join(FRAMEWORKS_SUPPORT_FP, DOCS_TOT_BUILD_GRADLE_REL)

# Set up input arguments
parser = argparse.ArgumentParser(
    description=("""Genereates new project in androidx."""))
parser.add_argument(
    'group_id',
    help='group_id for the new library')
parser.add_argument(
    'artifact_id',
    help='artifact_id for the new library')


class ProjectType(Enum):
    KOTLIN = 0
    JAVA = 1
    NATIVE = 2

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

def mv_dir(src_path_dir, dst_path_dir):
    """Moves a directory from src_path_dir to dst_path_dir.

    Args:
        src_path_dir: the source directory, which must exist
        dst_path_dir: the distination directory
    """
    if os.path.exists(dst_path_dir):
        print_e('rename error: Destination path %s already exists.' % dst_path_dir)
        return None
    # If moving to a new parent directory, create that directory
    parent_dst_path_dir = os.path.dirname(dst_path_dir)
    if not os.path.exists(parent_dst_path_dir):
        os.makedirs(parent_dst_path_dir)
    if not os.path.exists(src_path_dir):
        print_e('mv error: Source path %s does not exist.' % src_path_dir)
        return None
    try:
        os.rename(src_path_dir, dst_path_dir)
    except OSError as error:
        print_e('FAIL: Unable to copy %s to destination %s' % (src_path_dir, dst_path_dir))
        print_e(error)
        return None
    return dst_path_dir

def rename_file(src_file, new_file_name):
    """Renames a file from src_file to new_file_name, within the same directory.

    Args:
        src_file: the source file, which must exist
        new_file_name: the new file name
    """
    if not os.path.exists(src_file):
        print_e('mv file error: Source file %s does not exist.' % src_file)
        return None
    # Check that destination directory already exists
    parent_src_file_dir = os.path.dirname(src_file)
    new_file_path = os.path.join(parent_src_file_dir, new_file_name)
    if os.path.exists(new_file_path):
        print_e('mv file error: Source file %s already exists.' % new_file_path)
        return None
    try:
        os.rename(src_file, new_file_path)
    except OSError as error:
        print_e('FAIL: Unable to rename %s to destination %s' % (src_file, new_file_path))
        print_e(error)
        return None
    return new_file_path

def create_file(path):
    """
    Creates an empty file if it does not already exist.
    """
    open(path, "a").close()

def generate_package_name(group_id, artifact_id):
    final_group_id_word = group_id.split(".")[-1]
    artifact_id_suffix = re.sub(r"\b%s\b" % final_group_id_word, "", artifact_id)
    artifact_id_suffix = artifact_id_suffix.replace("-", ".")
    if (final_group_id_word == artifact_id):
      return group_id +  artifact_id_suffix
    elif (final_group_id_word != artifact_id):
      if ("." in artifact_id_suffix):
        return group_id +  artifact_id_suffix
      else:
        return group_id + "." + artifact_id_suffix

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
    group_id_version_macro = group_id.replace("androidx.", "").replace(".", "_").upper()
    if group_id == "androidx.compose":
        group_id_version_macro = "COMPOSE"
    elif group_id.startswith("androidx.compose"):
        group_id_version_macro = group_id.replace("androidx.compose.", "").replace(".",
                                                  "_").upper()
    return group_id_version_macro

def sed(before, after, file):
    with open(file) as f:
       file_contents = f.read()
    new_file_contents = file_contents.replace(before, after)
    # write back the file
    with open(file,"w") as f:
        f.write(new_file_contents)

def remove_line(line_to_remove, file):
    with open(file) as f:
       file_contents = f.readlines()
    new_file_contents = []
    for line in file_contents:
        if line_to_remove not in line:
            new_file_contents.append(line)
    # write back the file
    with open(file,"w") as f:
        f.write("".join(new_file_contents))

def ask_yes_or_no(question):
    while(True):
        reply = str(input(question+' (y/n): ')).lower().strip()
        if reply:
            if reply[0] == 'y': return True
            if reply[0] == 'n': return False
        print("Please respond with y/n")

def ask_project_type():
    """Asks the user which type of project they wish to create"""
    message = dedent("""
        Please choose the type of project you would like to create:
        1: Kotlin (AAR)
        2: Java (AAR / JAR)
        3: Native (AAR)
    """).strip()
    while(True):
        reply = str(input(message + "\n")).strip()
        if reply == "1": return ProjectType.KOTLIN
        if reply == "2":
            if confirm_java_project_type():
                return ProjectType.JAVA
        if reply == "3": return ProjectType.NATIVE
        print("Please respond with one of the presented options")

def confirm_java_project_type():
    return ask_yes_or_no("All new androidx projects are expected and encouraged "
    "to use Kotlin. Java projects should only be used if "
    "there is a business need to do so. "
    "Please ack to proceed:")

def ask_library_purpose():
    question = ("Project description (please complete the sentence): "
        "This library makes it easy for developers to... ")
    while(True):
        reply = str(input(question)).strip()
        if reply: return reply
        print("Please input a description!")

def ask_project_description():
    question = ("Please provide a project description: ")
    while(True):
        reply = str(input(question)).strip()
        if reply: return reply
        print("Please input a description!")

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
    return True

def get_library_type(artifact_id):
    """Returns the appropriate androidx.build.LibraryType for the project.
    """
    if "sample" in artifact_id:
        library_type = "SAMPLES"
    elif "compiler" in artifact_id:
        library_type = "ANNOTATION_PROCESSOR"
    elif "lint" in artifact_id:
        library_type = "LINT"
    elif "inspection" in artifact_id:
        library_type = "IDE_PLUGIN"
    else:
        library_type = "PUBLISHED_LIBRARY"
    return library_type

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

def get_package_documentation_file_dir(group_id, artifact_id):
    """Generates the full package documentation directory

    Given androidx.foo.bar:bar-qux, the structure will be:
    frameworks/support/foo/bar/bar-qux/src/main/java/androidx/foo/package-info.java

    For Kotlin:
    frameworks/support/foo/bar/bar-qux/src/main/java/androidx/foo/<group>-<artifact>-documentation.md

    For Compose:
    frameworks/support/foo/bar/bar-qux/src/commonMain/kotlin/androidx/foo/<group>-<artifact>-documentation.md

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
    """
    full_artifact_path = get_full_artifact_path(group_id, artifact_id)
    if "compose" in group_id:
        group_id_subpath = "/src/commonMain/kotlin/" + \
                        group_id.replace(".", "/")
    else:
        group_id_subpath = "/src/main/java/" + \
                        group_id.replace(".", "/")
    return full_artifact_path + group_id_subpath

def get_package_documentation_filename(group_id, artifact_id, project_type):
    """Generates the documentation filename

    Given androidx.foo.bar:bar-qux, the structure will be:
    package-info.java

    or for Kotlin:
    <group>-<artifact>-documentation.md

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
        is_kotlin_project: whether or not the library is a kotin project
    """
    if project_type == ProjectType.JAVA:
        return "package-info.java"
    else:
        formatted_group_id = group_id.replace(".", "-")
        return "%s-%s-documentation.md" % (formatted_group_id, artifact_id)

def is_compose_project(group_id, artifact_id):
    """Returns true if project can be inferred to be a compose / Kotlin project
    """
    return  "compose" in group_id or "compose" in artifact_id

def create_directories(group_id, artifact_id, project_type, is_compose_project):
    """Creates the standard directories for the given group_id and artifact_id.

    Given androidx.foo.bar:bar-qux, the structure will be:
    frameworks/support/foo/bar/bar-qux/build.gradle
    frameworks/support/foo/bar/bar-qux/src/main/java/androidx/foo/bar/package-info.java
    frameworks/support/foo/bar/bar-qux/src/main/java/androidx/foo/bar/artifact-documentation.md
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

    # Copy the full src structure, depending on the project source code
    if is_compose_project:
        print("Auto-detected Compose project.")
        cp(SAMPLE_COMPOSE_SRC_FP, full_artifact_path)
    elif project_type == ProjectType.NATIVE:
        cp(NATIVE_SRC_FP, full_artifact_path)
    elif project_type == ProjectType.KOTLIN:
        cp(SAMPLE_KOTLIN_SRC_FP, full_artifact_path)
    else:
        cp(SAMPLE_JAVA_SRC_FP, full_artifact_path)

    # Java only libraries have no dependency on android.
    # Java-only produces a jar, whereas an android library produces an aar.
    if (project_type == ProjectType.JAVA and
            (get_library_type(artifact_id) == "LINT" or
        ask_yes_or_no("Is this a java-only library? Java-only libraries produce"
                      " JARs, whereas Android libraries produce AARs."))):
        sed("com.android.library", "java-library",
            full_artifact_path + "/build.gradle")
        sed("org.jetbrains.kotlin.android", "kotlin",
            full_artifact_path + "/build.gradle")

    # Atomic group Ids have their version configured automatically,
    # so we can remove the version line from the build file.
    if is_group_id_atomic(group_id):
        remove_line("mavenVersion = LibraryVersions.",
                    full_artifact_path + "/build.gradle")

    # If the project is a library that produces a jar/aar that will go
    # on GMaven, ask for a special project description.
    if get_library_type(artifact_id) == "PUBLISHED_LIBRARY":
        project_description = ask_library_purpose()
    else:
        project_description = ask_project_description()

    # Set up the package documentation.
    full_package_docs_dir = get_package_documentation_file_dir(group_id, artifact_id)
    package_docs_filename = get_package_documentation_filename(group_id, artifact_id, project_type)
    full_package_docs_file = os.path.join(full_package_docs_dir, package_docs_filename)
    # Compose projects use multiple main directories, so we handle it separately
    if is_compose_project:
        # Kotlin projects use -documentation.md files, so we need to rename it appropriately.
        rename_file(full_artifact_path + "/src/commonMain/kotlin/groupId/artifactId-documentation.md",
                    package_docs_filename)
        mv_dir(full_artifact_path + "/src/commonMain/kotlin/groupId", full_package_docs_dir)
    else:
        if project_type != ProjectType.JAVA:
            # Kotlin projects use -documentation.md files, so we need to rename it appropriately.
            # We also rename this file for native projects in case they also have public Kotlin APIs
            rename_file(full_artifact_path + "/src/main/java/groupId/artifactId-documentation.md",
                        package_docs_filename)
        mv_dir(full_artifact_path + "/src/main/java/groupId", full_package_docs_dir)

    # Populate the library type
    library_type = get_library_type(artifact_id)
    if project_type == ProjectType.NATIVE and library_type == "PUBLISHED_LIBRARY":
        library_type = "PUBLISHED_NATIVE_LIBRARY"
    sed("<LIBRARY_TYPE>", library_type, full_artifact_path + "/build.gradle")

    # Populate the YEAR
    year = get_year()
    sed("<YEAR>", year, full_artifact_path + "/build.gradle")
    sed("<YEAR>", year, full_package_docs_file)

    # Populate the PACKAGE
    package = generate_package_name(group_id, artifact_id)
    sed("<PACKAGE>", package, full_package_docs_file)
    sed("<PACKAGE>", package, full_artifact_path + "/build.gradle")

    # Populate the VERSION macro
    group_id_version_macro = get_group_id_version_macro(group_id)
    sed("<GROUPID>", group_id_version_macro, full_artifact_path + "/build.gradle")
    # Update the name and description in the build.gradle
    sed("<NAME>", group_id + ":" + artifact_id, full_artifact_path + "/build.gradle")
    if project_type == ProjectType.NATIVE:
        sed("<NAME>", artifact_id, full_artifact_path + "/src/main/cpp/CMakeLists.txt")
        sed("<TARGET>", artifact_id, full_artifact_path + "/build.gradle")
        create_file(full_artifact_path + "/src/main/cpp/" + artifact_id + ".cpp")
    sed("<DESCRIPTION>", project_description, full_artifact_path + "/build.gradle")


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
    if is_compose_project(group_id, artifact_id):
        build_type = "COMPOSE"

    gradle_cmd = get_gradle_project_coordinates(group_id, artifact_id)
    return "includeProject(\"" + gradle_cmd + "\", [BuildType." + build_type + "])\n"

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

def get_new_docs_tip_of_tree_build_grade_line(group_id, artifact_id):
    """Generates the line needed for docs-tip-of-tree/build.gradle.

    For a library androidx.foo.bar:bar-qux, the new line will be of the form:
    docs(project(":foo:bar:bar-qux"))

    If it is a sample project, then the new line will be of the form:
    samples(project(":foo:bar:bar-qux-sample"))

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
    """

    gradle_cmd = get_gradle_project_coordinates(group_id, artifact_id)
    prefix = "docs"
    if "sample" in gradle_cmd:
        prefix = "samples"
    return "    %s(project(\"%s\"))\n" % (prefix, gradle_cmd)

def update_docs_tip_of_tree_build_grade(group_id, artifact_id):
    """Updates docs-tip-of-tree/build.gradle with the new library.

    We ask for confirmation if the library contains either "benchmark"
    or "test".

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
    """
    # Confirm with user that we want to generate docs for anything
    # that might be a test or a benchmark.
    if ("test" in group_id or "test" in artifact_id
        or "benchmark" in group_id or "benchmark" in artifact_id):
        if not ask_yes_or_no(("Should tip-of-tree documentation be generated "
                              "for project %s:%s?" % (group_id, artifact_id))):
            return

    # Open file for reading and get all lines
    with open(DOCS_TOT_BUILD_GRADLE_FP, 'r') as f:
        docs_tot_bg_lines = f.readlines()
    num_lines = len(docs_tot_bg_lines)

    new_docs_tot_bq_line = get_new_docs_tip_of_tree_build_grade_line(group_id, artifact_id)
    for i in range(num_lines):
        cur_line = docs_tot_bg_lines[i]
        if "project" not in cur_line:
            continue
        # Iterate through until you found the alphabetical place to insert the new line
        if new_docs_tot_bq_line.split("project")[1] <= cur_line.split("project")[1]:
            insert_line = i
            break
        else:
            insert_line = i + 1
    docs_tot_bg_lines.insert(insert_line, new_docs_tot_bq_line)

    # Open file for writing and update all lines
    with open(DOCS_TOT_BUILD_GRADLE_FP, 'w') as f:
        f.writelines(docs_tot_bg_lines)


def insert_new_group_id_into_library_versions_toml(group_id):
    """Inserts a group ID into the libraryversions.toml file.

    If one already exists, then this function just returns and reuses
    the existing one.

    Args:
        group_id: group_id of the new library
    """
    new_group_id_variable_name = group_id.replace("androidx.","").replace(".","_").upper()

    # Open toml file
    library_versions = toml.load(LIBRARY_VERSIONS_FP)
    if not new_group_id_variable_name in library_versions["versions"]:
        library_versions["versions"][new_group_id_variable_name] = "1.0.0-alpha01"
    if not new_group_id_variable_name in library_versions["groups"]:
        decoder = toml.decoder.TomlDecoder()
        group_entry = decoder.get_empty_inline_table()
        group_entry["group"] = group_id
        group_entry["atomicGroupVersion"] = "versions." + new_group_id_variable_name
        library_versions["groups"][new_group_id_variable_name] = group_entry

    # Sort the entries
    library_versions["versions"] = dict(sorted(library_versions["versions"].items()))
    library_versions["groups"] = dict(sorted(library_versions["groups"].items()))

    # Open file for writing and update toml
    with open(LIBRARY_VERSIONS_FP, 'w') as f:
        versions_toml_file_string = toml.dumps(library_versions, encoder=toml.TomlPreserveInlineDictEncoder())
        versions_toml_file_string_new = re.sub(",]", " ]", versions_toml_file_string)
        f.write(versions_toml_file_string_new)


def is_group_id_atomic(group_id):
    """Checks if a group ID is atomic using the libraryversions.toml file.

    If one already exists, then this function evaluates the group id
    and returns the appropriate atomicity.  Otherwise, it returns
    False.

    Example of an atomic library group:
        ACTIVITY = { group = "androidx.work", atomicGroupVersion = "WORK" }
    Example of a non-atomic library group:
        WEAR = { group = "androidx.wear" }

    Args:
        group_id: group_id of the library we're checking.
    """
    library_versions = toml.load(LIBRARY_VERSIONS_FP)
    for library_group in library_versions["groups"]:
      if group_id == library_versions["groups"][library_group]["group"]:
          return "atomicGroupVersion" in library_versions["groups"][library_group]

    return False


def print_todo_list(group_id, artifact_id, project_type):
    """Prints to the todo list once the script has finished.

    There are some pieces that can not be automated or require human eyes.
    List out the appropriate todos so that the users knows what needs
    to be done prior to uploading.

    Args:
        group_id: group_id of the new library
        artifact_id: group_id of the new library
    """
    build_gradle_path = get_full_artifact_path(group_id, artifact_id) + \
                        "/build.gradle"
    owners_file_path = get_group_id_path(group_id) + "/OWNERS"
    package_docs_path = os.path.join(
        get_package_documentation_file_dir(group_id, artifact_id),
        get_package_documentation_filename(group_id, artifact_id, project_type))
    print("---\n")
    print("Created the project.  The following TODOs need to be completed by "
          "you:\n")
    print("\t1. Check that the OWNERS file is in the correct place. It is "
          "currently at:"
          "\n\t\t" + owners_file_path)
    print("\t2. Add your name (and others) to the OWNERS file:" + \
          "\n\t\t" + owners_file_path)
    print("\t3. Check that the correct library version is assigned in the "
          "build.gradle:"
          "\n\t\t" + build_gradle_path)
    print("\t4. Fill out the project/module name in the build.gradle:"
          "\n\t\t" + build_gradle_path)
    print("\t5. Update the project/module package documentation:"
          "\n\t\t" + package_docs_path)

def main(args):
    # Parse arguments and check for existence of build ID or file
    args = parser.parse_args()
    if not args.group_id or not args.artifact_id:
        parser.error("You must specify a group_id and an artifact_id")
        sys.exit(1)
    if not validate_name(args.group_id, args.artifact_id):
        sys.exit(1)
    if is_compose_project(args.group_id, args.artifact_id):
        project_type = ProjectType.KOTLIN
    else:
        project_type = ask_project_type()
    insert_new_group_id_into_library_versions_toml(
        args.group_id
    )
    create_directories(
        args.group_id,
        args.artifact_id,
        project_type,
        is_compose_project(args.group_id, args.artifact_id)
    )
    update_settings_gradle(args.group_id, args.artifact_id)
    update_docs_tip_of_tree_build_grade(args.group_id, args.artifact_id)
    print("Created directories. \nRunning updateApi for the new "
          "library, this may take a minute...", end='')
    if run_update_api(args.group_id, args.artifact_id):
        print("done.")
    else:
        print("failed.  Please investigate manually.")
    print_todo_list(args.group_id, args.artifact_id, project_type)

if __name__ == '__main__':
    main(sys.argv)
