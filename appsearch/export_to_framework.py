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
#   $ ./frameworks/support/appsearch/export_to_framework.py "$HOME/android/master"
import os
import re
import sys


JETPACK_API_ROOT = 'appsearch/src/main/java/androidx/appsearch'
JETPACK_API_TEST_ROOT = 'appsearch/src/androidTest/java/androidx/appsearch'
JETPACK_IMPL_ROOT = 'local-backend/src/main/java/androidx/appsearch'
JETPACK_IMPL_TEST_ROOT = 'local-backend/src/androidTest/java/androidx/appsearch'
FRAMEWORK_API_ROOT = 'framework/java/android/app/appsearch'
FRAMEWORK_API_TEST_ROOT = (
        '../../core/tests/coretests/src/'
        'android/app/appsearch/external')
FRAMEWORK_IMPL_ROOT = 'service/java/com/android/server/appsearch/external'
FRAMEWORK_IMPL_TEST_ROOT = (
        '../../services/tests/servicestests/src/'
        'com/android/server/appsearch/external')


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
    print('Copy: "%s" -> "%s"' % (source_path, dest_path), file=sys.stderr)
    with open(source_path, 'r') as fh:
      contents = fh.read()
    if transform_func:
      contents = transform_func(contents)
    os.makedirs(os.path.dirname(dest_path), exist_ok=True)
    with open(dest_path, 'w') as fh:
      fh.write(contents)


def _TransformCommonCode(contents):
    return (contents
        .replace('androidx.appsearch.app', 'android.app.appsearch')
        .replace('androidx.appsearch', 'android.app.appsearch')
        .replace(
                'androidx.annotation.GuardedBy',
                'com.android.internal.annotations.GuardedBy')
        .replace(
                'androidx.annotation.VisibleForTesting',
                'com.android.internal.annotations.VisibleForTesting')
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


def _TransformAndCopyFolder(
        source_dir, dest_dir, transform_func=None, block_list=None):
    for currentpath, folders, files in os.walk(source_dir):
        dir_rel_to_root = os.path.relpath(currentpath, source_dir)
        for filename in files:
            # Copy all files, except those in the block list
            source_abs_path = os.path.join(currentpath, filename)
            rel_to_source = os.path.relpath(source_abs_path, source_dir)
            if block_list and rel_to_source in block_list:
                print('Skipping copy: "%s"' % rel_to_source)
            else:
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
            # Copy all files, except those in the following block list
            source_abs_path = os.path.join(currentpath, filename)
            rel_to_api = os.path.relpath(source_abs_path, api_source_dir)
            if rel_to_api in (
                    'app/AppSearchBatchResult.java',
                    'app/AppSearchManager.java',
                    'app/AppSearchResult.java',
                    'app/SearchResults.java'):
                print('Skipping copy: "%s"' % rel_to_api)
                continue

            # Figure out what folder to place them into
            if dir_rel_to_root == 'app':
                # Files in the 'app' folder live in the root of the platform
                # tree
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
    _TransformAndCopyFolder(
            impl_source_dir, impl_dest_dir,
            transform_func=_TransformImplCode,
            block_list=[
                'localbackend/LocalBackend.java',
                'localbackend/util/FutureUtil.java',
            ])

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
            impl_test_source_dir, impl_test_dest_dir,
            transform_func=_TransformImplTestCode,
            block_list=[
                'localbackend/LocalBackendTest.java',
            ])


def _CopyMain(source_dir, dest_dir):
    _CopyAllApi(source_dir, dest_dir)
    _CopyAllImpl(source_dir, dest_dir)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print('Usage: %s <path/to/frameworks/base>' % sys.argv[0],
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
    _CopyMain(source_dir, dest_dir)
