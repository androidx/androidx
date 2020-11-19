#!/usr/bin/env python3
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

# Exports AppSearch Androidx code to Framework
#
# NOTE: This will remove and replace all files in the
# frameworks/base/apex/appsearch path.
#
# Example usage (from root dir of androidx workspace):
# $ ./frameworks/support/appsearch/exportToFramework.py "$HOME/android/master" "<jetpack changeid>"
import os
import re
import sys


JETPACK_API_ROOT = 'appsearch/src/main/java/androidx/appsearch'
JETPACK_API_TEST_ROOT = 'appsearch/src/androidTest/java/androidx/appsearch'
JETPACK_IMPL_ROOT = 'local-storage/src/main/java/androidx/appsearch'
JETPACK_IMPL_TEST_ROOT = 'local-storage/src/androidTest/java/androidx/appsearch'
FRAMEWORK_API_ROOT = 'framework/java/android/app/appsearch'
FRAMEWORK_API_TEST_ROOT = (
        '../../core/tests/coretests/src/'
        'android/app/appsearch/external')
FRAMEWORK_IMPL_ROOT = 'service/java/com/android/server/appsearch/external'
FRAMEWORK_IMPL_TEST_ROOT = (
        '../../services/tests/servicestests/src/'
        'com/android/server/appsearch/external')
CHANGEID_FILE_NAME = 'synced_jetpack_changeid.txt'


def _PruneDir(dest_dir, allow_list=None):
    all_files = []
    for walk_path, walk_folders, walk_files in os.walk(dest_dir):
        for walk_filename in walk_files:
            abs_path = os.path.join(walk_path, walk_filename)
            all_files.append(abs_path)

    for abs_path in all_files:
        rel_path = os.path.relpath(abs_path, dest_dir)
        if allow_list and rel_path in allow_list:
            print('Prune: skip "%s"' % abs_path)
        else:
            print('Prune: remove "%s"' % abs_path)
            os.remove(abs_path)


def _TransformAndCopyFile(source_path, dest_path, transform_func=None):
    with open(source_path, 'r') as fh:
        contents = fh.read()

    if '@exportToFramework:skipFile()' in contents:
        print('Skipping: "%s" -> "%s"' % (source_path, dest_path), file=sys.stderr)
        return

    print('Copy: "%s" -> "%s"' % (source_path, dest_path), file=sys.stderr)
    if transform_func:
        contents = transform_func(contents)
    os.makedirs(os.path.dirname(dest_path), exist_ok=True)
    with open(dest_path, 'w') as fh:
        fh.write(contents)


def _TransformCommonCode(contents):
    return (contents
        .replace('androidx.appsearch.app', 'android.app.appsearch')
        .replace(
                'androidx.appsearch.localstorage.',
                'com.android.server.appsearch.external.localstorage.')
        .replace('androidx.appsearch', 'android.app.appsearch')
        .replace(
                'androidx.annotation.GuardedBy',
                'com.android.internal.annotations.GuardedBy')
        .replace(
                'androidx.annotation.VisibleForTesting',
                'com.android.internal.annotations.VisibleForTesting')
        .replace('androidx.collection.ArrayMap', 'android.util.ArrayMap')
        .replace('androidx.collection.ArraySet', 'android.util.ArraySet')
        .replace(
                'androidx.core.util.ObjectsCompat',
                'java.util.Objects')
        .replace(
                'androidx.core.util.Preconditions',
                'com.android.internal.util.Preconditions')
        .replace('import androidx.annotation.RestrictTo;', '')
        .replace('@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)', '')
        .replace('ObjectsCompat.', 'Objects.')
        .replace('androidx.', 'android.')
    )


def _TransformTestCode(contents):
    contents = (contents
        .replace('org.junit.Assert.assertThrows',
                 'org.testng.Assert.expectThrows')
        .replace('assertThrows(', 'expectThrows(')
    )
    return _TransformCommonCode(contents)


def _TransformAndCopyFolder(source_dir, dest_dir, transform_func=None):
    for currentpath, folders, files in os.walk(source_dir):
        dir_rel_to_root = os.path.relpath(currentpath, source_dir)
        for filename in files:
            source_abs_path = os.path.join(currentpath, filename)
            dest_path = os.path.join(dest_dir, dir_rel_to_root, filename)
            _TransformAndCopyFile(source_abs_path, dest_path, transform_func)


def _CopyAllApi(source_dir, dest_dir):
    api_source_dir = os.path.join(source_dir, JETPACK_API_ROOT)
    api_test_source_dir = os.path.join(source_dir, JETPACK_API_TEST_ROOT)
    api_dest_dir = os.path.join(dest_dir, FRAMEWORK_API_ROOT)
    api_test_dest_dir = os.path.join(dest_dir, FRAMEWORK_API_TEST_ROOT)

    # Prune existing files
    _PruneDir(api_dest_dir, allow_list=[
        'AppSearchBatchResult.java',
        'AppSearchManager.java',
        'AppSearchManagerFrameworkInitializer.java',
        'AppSearchResult.java',
        'IAppSearchManager.aidl',
        'SearchResults.java',
    ])
    _PruneDir(api_test_dest_dir)

    # Copy api classes. We can't use _TransformAndCopyFolder here because we
    # need to specially handle the 'app' package.
    def _TransformApiCode(contents):
        contents = contents.replace(
                'package androidx.appsearch.app;',
                'package android.app.appsearch;')
        return _TransformCommonCode(contents)
    for currentpath, folders, files in os.walk(api_source_dir):
        dir_rel_to_root = os.path.relpath(currentpath, api_source_dir)
        for filename in files:
            # Figure out what folder to place them into
            source_abs_path = os.path.join(currentpath, filename)
            if dir_rel_to_root == 'app':
                # Files in the 'app' folder live in the root of the platform tree
                dest_path = os.path.join(api_dest_dir, filename)
            else:
                dest_path = os.path.join(
                        api_dest_dir, dir_rel_to_root, filename)
            _TransformAndCopyFile(source_abs_path, dest_path, _TransformApiCode)

    # Copy api test classes.
    _TransformAndCopyFolder(
            api_test_source_dir, api_test_dest_dir,
            transform_func=_TransformTestCode)


def _CopyAllImpl(source_dir, dest_dir):
    impl_source_dir = os.path.join(source_dir, JETPACK_IMPL_ROOT)
    impl_test_source_dir = os.path.join(source_dir, JETPACK_IMPL_TEST_ROOT)
    impl_dest_dir = os.path.join(dest_dir, FRAMEWORK_IMPL_ROOT)
    impl_test_dest_dir = os.path.join(dest_dir, FRAMEWORK_IMPL_TEST_ROOT)

    # Prune
    _PruneDir(impl_dest_dir)
    _PruneDir(impl_test_dest_dir)

    # Copy impl classes
    def _TransformImplCode(contents):
        contents = (contents
                .replace('package androidx.appsearch',
                         'package com.android.server.appsearch.external')
                .replace('com.google.android.icing.protobuf.', 'com.google.protobuf.')
        )
        return _TransformCommonCode(contents)
    _TransformAndCopyFolder(impl_source_dir, impl_dest_dir, transform_func=_TransformImplCode)

    # Copy servicestests
    def _TransformImplTestCode(contents):
        contents = (contents
                .replace('package androidx.appsearch',
                         'package com.android.server.appsearch.external')
                .replace('com.google.android.icing.proto.',
                         'com.android.server.appsearch.proto.')
                .replace('com.google.android.icing.protobuf.',
                         'com.android.server.appsearch.protobuf.')
        )
        return _TransformTestCode(contents)
    _TransformAndCopyFolder(
            impl_test_source_dir, impl_test_dest_dir, transform_func=_TransformImplTestCode)


def _CopyChangeIdFile(changeid, dest_dir):
    """Copies the changeid of the most recent public CL into a file on the framework side.

    This file is used for tracking, to determine what framework is synced to.

    You must always provide a changeid of an exported, preferably even submitted CL. If you abandon
    the CL pointed to by this changeid, the next person syncing framework will be unable to find
    what CL it is synced to.
    """
    file_path = os.path.join(dest_dir, CHANGEID_FILE_NAME)
    with open(file_path, 'w') as fh:
        print(changeid, file=fh)
    print('Wrote "%s"' % file_path)


def _CopyMain(source_dir, dest_dir, changeid):
    _CopyAllApi(source_dir, dest_dir)
    _CopyAllImpl(source_dir, dest_dir)
    _CopyChangeIdFile(changeid, dest_dir)


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print('Usage: %s <path/to/frameworks/base> <changeId of head jetpack commit>' % sys.argv[0],
              file=sys.stderr)
        sys.exit(1)
    source_dir = os.path.normpath(os.path.dirname(sys.argv[0]))
    dest_dir = os.path.normpath(sys.argv[1])
    if os.path.basename(dest_dir) == 'appsearch':
        pass
    elif os.path.basename(dest_dir) == 'base':
        dest_dir = os.path.join(dest_dir, 'apex/appsearch')
    else:
        dest_dir = os.path.join(dest_dir, 'frameworks/base/apex/appsearch')
    if not os.path.isdir(dest_dir):
        print('Destination path "%s" does not exist or is not a directory' % (
                dest_dir),
              file=sys.stderr)
        sys.exit(1)
    _CopyMain(source_dir, dest_dir, sys.argv[2])
