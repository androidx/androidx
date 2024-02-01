#!/usr/bin/env python3

import argparse, os, sys

# List of directories we want to exclude when traversing the project files. This list should
# contain directories related to tests, documentation/samples, API, resources, etc.
EXCLUDED_DIRS = ['androidTest', 'androidAndroidTest', 'api', 'docs', 'res', 'samples', 'test',
                 'androidInstrumentedTest']
# List of packages that should be excluded when traversing the project files. These packages might
# include Java/Kotlin source files that contain Composable, but we're not interested in including
# them on the output list because it's unlikely that developers will use them in their code.
# For example, this list should contain (test) utility and tooling-related packages.
EXCLUDED_PACKAGES = ['benchmark-utils', 'compiler', 'integration-tests', 'test-utils',
                     'ui-android-stubs', 'ui-tooling', 'ui-tooling-data', 'ui-tooling-preview']
# Set of directories that will be excluded when traversing the project files. Excluding a directory
# means our search won't look into its subdirectories, so this list should be populated
# with caution.
EXCLUDED_FROM_FILE_SEARCH = set(EXCLUDED_DIRS + EXCLUDED_PACKAGES)

# The directory containing this script, relative to androidx-main root.
SCRIPT_DIR_PATH = 'frameworks/support/compose/ui/ui-inspection/generate-packages/'
# The file name of this script.
SCRIPT_NAME = 'generate_compose_packages.py'
# File containing an ordered list of packages that contain at least one Composable.
# The file is formatted as one package per line.
COMPOSE_PACKAGES_LIST_FILE = 'compose_packages_list.txt'

# `frameworks/support/compose/` and `frameworks/support/navigation/`, relative to this script
# directory, should be the root directories where we search for composables.
TARGET_DIRECTORIES = ['../../..', '../../../../navigation']

# Reads a source file with the given file_path and adds its package to the current set of packages
# if the file contains at least one Composable.
def add_package_if_composable(file_path, packages):
    with open(file_path, 'r') as file:
        lines = file.readlines()
        for line in lines:
            if line.startswith('package '):
                package = line.lstrip('package').strip().strip(';')
                # Early return to prevent reading the rest of the file.
                if package in packages: return
            if line.lstrip().startswith('@Composable') and package:
                packages.add(package)
                return

# Iterates on a directory recursively, looking for Java/Kotlin source files that contain Composable
# functions, and add their corresponding packages to a set that will be returned when the traversal
# is complete.
def extract_packages_from_directory(directory):
    packages = set()
    for root, dirs, files in os.walk(directory, topdown=True):
        dirs[:] = [d for d in dirs if d not in EXCLUDED_FROM_FILE_SEARCH]
        for filename in files:
            if filename.endswith('.java') or filename.endswith('.kt'):
                add_package_if_composable(os.path.join(root, filename), packages)
    return packages

# Searches the given directories and returns a sorted list of all the packages that contain
# Composable functions.
def sorted_packages_from_directories(directories):
    packages = []
    for directory in directories:
        packages.extend(extract_packages_from_directory(directory))
    return sorted(packages)

# Verifies that the given the list of packages match the ones currently listed on the
# compose_packages_list.txt file
def verify_packages(packages):
    with open(COMPOSE_PACKAGES_LIST_FILE, 'r') as file:
        file_packages = file.readlines()
        if len(file_packages) != len(packages): report_failure_and_exit()
        for i in range(len(file_packages)):
            if packages[i] != file_packages[i].strip('\n'): report_failure_and_exit()

def report_failure_and_exit():
    print(
        'Compose packages mismatch\n The current list of Compose packages does not match the list '
        'stored in %s%s. If the current list of packages have changed, please regenerate the list '
        'by running the following command:\n\t%s%s --regenerate' % (
            SCRIPT_DIR_PATH,
            COMPOSE_PACKAGES_LIST_FILE,
            SCRIPT_DIR_PATH,
            SCRIPT_NAME
        ),
        file=sys.stderr
    )
    sys.exit(1)

# Regenerates the compose_packages_list.txt file, given the list of packages.
def regenerate_packages_file(packages):
    with open(COMPOSE_PACKAGES_LIST_FILE, 'w') as file:
        file.write('\n'.join(packages))

# Regenerates the PackageHashes.kt, given the list of packages. The file format is:
# 1) Header indicating the file should not be edited manually
# 2) Package definition
# 3) Required imports
# 4) packageNameHash function
# 5) systemPackages val, which is a list containing the result of the packageNameHash
#    function applied to each package name of the given packages list.
def regenerate_packages_kt_file(packages):
    kt_file = '../src/main/java/androidx/compose/ui/inspection/inspector/PackageHashes.kt'
    header = (
        '// WARNING: DO NOT EDIT THIS FILE MANUALLY. It\'s automatically generated by running:\n'
        '//    %s%s -r\n' % (SCRIPT_DIR_PATH, SCRIPT_NAME)
    )
    package = 'package androidx.compose.ui.inspection.inspector\n\n'
    imports = (
        'import androidx.annotation.VisibleForTesting\n'
        'import kotlin.math.absoluteValue\n\n'
    )
    package_name_hash_function = (
        '@VisibleForTesting\n'
        'fun packageNameHash(packageName: String) =\n'
        '    packageName.fold(0) { hash, char -> hash * 31 + char.code }.absoluteValue\n\n'
    )
    system_packages_val = (
        'val systemPackages = setOf(\n'
        '    -1,\n'
        '%s\n'
        ')\n' % (
            '\n'.join(['    packageNameHash("' + package + '"),' for package in packages])
        )
    )
    with open(kt_file, 'w') as file:
        file.write(
            header + package + imports + package_name_hash_function + system_packages_val
        )

if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='This script is invoked to check whether the current list of packages '
                    'containing Composables is up-to-date. This list is used by Layout Inspector '
                    'and Compose Preview to filter out framework Composables.'
    )
    parser.add_argument(
        '-r',
        '--regenerate',
        action='store_true',
        help='this argument should be used to regenerate the list of packages'
    )
    args = parser.parse_args()

    # cd into directory of script
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    current_packages = sorted_packages_from_directories(TARGET_DIRECTORIES)

    if args.regenerate:
        regenerate_packages_file(current_packages)
        regenerate_packages_kt_file(current_packages)
    else:
        verify_packages(current_packages)
