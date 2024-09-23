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
# packages/modules/AppSearch path.
#
# Example usage (from root dir of androidx workspace):
# $ ./frameworks/support/appsearch/exportToFramework.py "$HOME/android/master" "<jetpack git sha>"

# Special directives supported by this script:
#
# Causes the file where it appears to not be copied at all:
#   @exportToFramework:skipFile()
#
# Causes the text appearing between startStrip() and endStrip() to be removed during export:
#   // @exportToFramework:startStrip() ... // @exportToFramework:endStrip()
#
# Replaced with @hide:
#   <!--@exportToFramework:hide-->
#
# Removes the text appearing between ifJetpack() and else(), and causes the text appearing between
# else() and --> to become uncommented, to support framework-only Javadocs:
#   <!--@exportToFramework:ifJetpack()-->
#   Jetpack-only Javadoc
#   <!--@exportToFramework:else()
#   Framework-only Javadoc
#   -->
# Note: Using the above pattern, you can hide a method in Jetpack but unhide it in Framework like
# this:
#   <!--@exportToFramework:ifJetpack()-->@hide<!--@exportToFramework:else()-->

import os
import re
import subprocess
import sys

# Jetpack paths relative to frameworks/support/appsearch
JETPACK_API_ROOT = 'appsearch/src/main/java/androidx/appsearch'
JETPACK_API_TEST_ROOT = 'appsearch/src/androidTest/java/androidx/appsearch'
JETPACK_IMPL_ROOT = 'appsearch-local-storage/src/main/java/androidx/appsearch'
JETPACK_IMPL_TEST_ROOT = 'appsearch-local-storage/src/androidTest/java/androidx/appsearch'
JETPACK_TEST_UTIL_ROOT = 'appsearch-test-util/src/main/java/androidx/appsearch'
JETPACK_TEST_UTIL_TEST_ROOT = 'appsearch-test-util/src/androidTest/java/androidx/appsearch'

# Framework paths relative to packages/modules/AppSearch
FRAMEWORK_API_ROOT = 'framework/java/external/android/app/appsearch'
FRAMEWORK_API_TEST_ROOT = 'testing/coretests/src/android/app/appsearch/external'
FRAMEWORK_IMPL_ROOT = 'service/java/com/android/server/appsearch/external'
FRAMEWORK_IMPL_TEST_ROOT = 'testing/servicestests/src/com/android/server/appsearch/external'
FRAMEWORK_TEST_UTIL_ROOT = (
    '../../../cts/tests/appsearch/testutils/src/android/app/appsearch/testutil/external')
FRAMEWORK_TEST_UTIL_TEST_ROOT = 'testing/servicestests/src/android/app/appsearch/testutil/external'
FRAMEWORK_CTS_TEST_ROOT = '../../../cts/tests/appsearch/src/com/android/cts/appsearch/external'
GOOGLE_JAVA_FORMAT = (
        '../../../prebuilts/tools/common/google-java-format/google-java-format')

# Miscellaneous constants
SHA_FILE_NAME = 'synced_jetpack_sha.txt'


class ExportToFramework:
    def __init__(self, jetpack_appsearch_root, framework_appsearch_root):
        self._jetpack_appsearch_root = jetpack_appsearch_root
        self._framework_appsearch_root = framework_appsearch_root
        self._written_files = []

    def _PruneDir(self, dir_to_prune):
        for walk_path, walk_folders, walk_files in os.walk(dir_to_prune):
            for walk_filename in walk_files:
                abs_path = os.path.join(walk_path, walk_filename)
                print('Prune: remove "%s"' % abs_path)
                os.remove(abs_path)

    def _TransformAndCopyFile(
            self, source_path, default_dest_path, transform_func=None, ignore_skips=False):
        """
        Transforms the file located at 'source_path' and writes it into 'default_dest_path'.

        An @exportToFramework:skip() directive will skip the copy process.
        An @exportToFramework:copyToPath() directive will override default_dest_path with another
          path relative to framework_appsearch_root (which is usually packages/modules/AppSearch)
        """
        with open(source_path, 'r') as fh:
            contents = fh.read()

        if not ignore_skips and '@exportToFramework:skipFile()' in contents:
            print('Skipping: "%s" -> "%s"' % (source_path, default_dest_path), file=sys.stderr)
            return

        copy_to_path = re.search(r'@exportToFramework:copyToPath\(([^)]+)\)', contents)
        if copy_to_path:
            dest_path = os.path.join(self._framework_appsearch_root, copy_to_path.group(1))
        else:
            dest_path = default_dest_path

        self._TransformAndCopyFileToPath(source_path, dest_path, transform_func)

    def _TransformAndCopyFileToPath(self, source_path, dest_path, transform_func=None):
        """Transforms the file located at 'source_path' and writes it into 'dest_path'."""
        print('Copy: "%s" -> "%s"' % (source_path, dest_path), file=sys.stderr)
        with open(source_path, 'r') as fh:
            contents = fh.read()
        if transform_func:
            contents = transform_func(contents)
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)
        with open(dest_path, 'w') as fh:
            fh.write(contents)

        # Save file for future formatting
        self._written_files.append(dest_path)

    def _TransformCommonCode(self, contents):
        # Apply stripping
        contents = re.sub(
            r'\/\/ @exportToFramework:startStrip\(\).*?\/\/ @exportToFramework:endStrip\(\)',
            '',
            contents,
            flags=re.DOTALL)

        # Apply if/elses in javadocs
        contents = re.sub(
            r'<!--@exportToFramework:ifJetpack\(\)-->.*?<!--@exportToFramework:else\(\)(.*?)-->',
            r'\1',
            contents,
            flags=re.DOTALL)

        # Add additional imports if required
        imports_to_add = []
        for import_to_add in imports_to_add:
            contents = re.sub(
                    r'^(\s*package [^;]+;\s*)$', r'\1\nimport %s;\n' % import_to_add, contents,
                    flags=re.MULTILINE)

        # Remove all imports for stub CREATOR classes imported for SafeParcelable
        # If there are more use cases in the future we might want to add
        # imports_to_delete
        contents = re.sub(
            r'import androidx\.appsearch\.safeparcel\.stub.*?\;',
            '', contents, flags=re.MULTILINE)

        # Apply in-place replacements
        contents = (contents
            .replace('androidx.appsearch.app', 'android.app.appsearch')
            .replace(
                    'androidx.appsearch.localstorage.',
                    'com.android.server.appsearch.external.localstorage.')
            .replace('androidx.appsearch.flags.FlaggedApi', 'android.annotation.FlaggedApi')
            .replace('androidx.appsearch.flags.Flags', 'com.android.appsearch.flags.Flags')
            .replace(
                    'androidx.appsearch.annotation.CurrentTimeMillis',
                    'android.annotation.CurrentTimeMillis')
            .replace(
                    'androidx.appsearch.annotation.SystemApi',
                    'android.annotation.SystemApi')
            .replace('androidx.appsearch', 'android.app.appsearch')
            .replace(
                    'androidx.annotation.GuardedBy',
                    'com.android.internal.annotations.GuardedBy')
            .replace(
                    'androidx.annotation.VisibleForTesting',
                    'com.android.internal.annotations.VisibleForTesting')
            .replace('androidx.annotation.', 'android.annotation.')
            .replace('androidx.collection.ArrayMap', 'android.util.ArrayMap')
            .replace('androidx.collection.ArraySet', 'android.util.ArraySet')
            .replace(
                    'androidx.core.util.ObjectsCompat',
                    'java.util.Objects')
            # Preconditions.checkNotNull is replaced with Objects.requireNonNull. We add both
            # imports and let google-java-format sort out which one is unused.
            .replace(
                    'import androidx.core.util.Preconditions;',
                    'import java.util.Objects; import com.android.internal.util.Preconditions;')
            .replace('import androidx.annotation.RestrictTo;', '')
            .replace('@RestrictTo(RestrictTo.Scope.LIBRARY)', '')
            .replace('@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)', '')
            .replace('Preconditions.checkNotNull(', 'Objects.requireNonNull(')
            .replace('ObjectsCompat.', 'Objects.')
            .replace('<!--@exportToFramework:hide-->', '@hide')
            .replace('@exportToFramework:hide', '@hide')
            .replace('// @exportToFramework:skipFile()', '')
        )
        contents = re.sub(r'\/\/ @exportToFramework:copyToPath\([^)]+\)', '', contents)
        contents = re.sub(r'@RequiresFeature\([^)]*\)', '', contents, flags=re.DOTALL)

        # Jetpack methods have the Async suffix, but framework doesn't. Strip the Async suffix
        # to allow the same documentation to compile for both.
        contents = re.sub(r'(#[a-zA-Z0-9_]+)Async}', r'\1}', contents)
        contents = re.sub(
                r'(\@see [^#]+#[a-zA-Z0-9_]+)Async$', r'\1', contents, flags=re.MULTILINE)
        return contents

    def _TransformTestCode(self, contents):
        contents = (contents
            .replace(
                    'androidx.appsearch.flags.CheckFlagsRule',
                    'android.platform.test.flag.junit.CheckFlagsRule')
            .replace(
                    'androidx.appsearch.flags.DeviceFlagsValueProvider',
                    'android.platform.test.flag.junit.DeviceFlagsValueProvider')
            .replace(
                    'androidx.appsearch.flags.RequiresFlagsEnabled',
                    'android.platform.test.annotations.RequiresFlagsEnabled')
            .replace('androidx.appsearch.testutil.', 'android.app.appsearch.testutil.')
            .replace(
                    'package androidx.appsearch.testutil;',
                    'package android.app.appsearch.testutil;')
            .replace(
                    'import androidx.appsearch.localstorage.LocalStorage;',
                    'import android.app.appsearch.AppSearchManager;')
            .replace('LocalStorage.', 'AppSearchManager.')
        )
        for shim in [
                'AppSearchSession', 'GlobalSearchSession', 'EnterpriseGlobalSearchSession',
                'SearchResults']:
            contents = re.sub(r"([^a-zA-Z])(%s)([^a-zA-Z0-9])" % shim, r'\1\2Shim\3', contents)
        return self._TransformCommonCode(contents)

    def _TransformAndCopyFolder(self, source_dir, dest_dir, transform_func=None):
        for currentpath, folders, files in os.walk(source_dir):
            dir_rel_to_root = os.path.relpath(currentpath, source_dir)
            for filename in files:
                source_abs_path = os.path.join(currentpath, filename)
                dest_path = os.path.join(dest_dir, dir_rel_to_root, filename)
                self._TransformAndCopyFile(source_abs_path, dest_path, transform_func)

    def _ExportApiCode(self):
        # Prod source
        api_source_dir = os.path.join(self._jetpack_appsearch_root, JETPACK_API_ROOT)
        api_dest_dir = os.path.join(self._framework_appsearch_root, FRAMEWORK_API_ROOT)

        # Unit tests
        api_test_source_dir = os.path.join(self._jetpack_appsearch_root, JETPACK_API_TEST_ROOT)
        api_test_dest_dir = os.path.join(self._framework_appsearch_root, FRAMEWORK_API_TEST_ROOT)

        # CTS tests
        cts_test_source_dir = os.path.join(api_test_source_dir, 'cts')
        cts_test_dest_dir = os.path.join(self._framework_appsearch_root, FRAMEWORK_CTS_TEST_ROOT)

        # Test utils
        test_util_source_dir = os.path.join(self._jetpack_appsearch_root, JETPACK_TEST_UTIL_ROOT)
        test_util_dest_dir = os.path.join(self._framework_appsearch_root, FRAMEWORK_TEST_UTIL_ROOT)

        # Prune existing files
        self._PruneDir(api_dest_dir)
        self._PruneDir(api_test_dest_dir)
        self._PruneDir(cts_test_dest_dir)
        self._PruneDir(test_util_dest_dir)

        # Copy api classes. We can't use _TransformAndCopyFolder here because we
        # need to specially handle the 'app' package.
        print('~~~ Copying API classes ~~~')
        def _TransformApiCode(contents):
            contents = contents.replace(
                    'package androidx.appsearch.app;',
                    'package android.app.appsearch;')
            return self._TransformCommonCode(contents)
        for currentpath, folders, files in os.walk(api_source_dir):
            dir_rel_to_root = os.path.relpath(currentpath, api_source_dir)
            for filename in files:
                # Figure out what folder to place them into
                source_abs_path = os.path.join(currentpath, filename)
                if dir_rel_to_root == 'app':
                    # Files in the 'app' folder live in the root of the platform tree
                    dest_path = os.path.join(api_dest_dir, filename)
                else:
                    dest_path = os.path.join(api_dest_dir, dir_rel_to_root, filename)
                self._TransformAndCopyFile(source_abs_path, dest_path, _TransformApiCode)

        # Copy api unit tests. We can't use _TransformAndCopyFolder here because we need to skip the
        # 'util' and 'cts' subfolders.
        print('~~~ Copying API unit tests ~~~')
        for currentpath, folders, files in os.walk(api_test_source_dir):
            if (currentpath.startswith(cts_test_source_dir) or
                    currentpath.startswith(test_util_source_dir)):
                continue
            dir_rel_to_root = os.path.relpath(currentpath, api_test_source_dir)
            for filename in files:
                source_abs_path = os.path.join(currentpath, filename)
                dest_path = os.path.join(api_test_dest_dir, dir_rel_to_root, filename)
                self._TransformAndCopyFile(source_abs_path, dest_path, self._TransformTestCode)

        # Copy CTS tests
        print('~~~ Copying CTS tests ~~~')
        self._TransformAndCopyFolder(
                cts_test_source_dir, cts_test_dest_dir, transform_func=self._TransformTestCode)

        # Copy test utils
        print('~~~ Copying test utils ~~~')
        self._TransformAndCopyFolder(
                test_util_source_dir, test_util_dest_dir, transform_func=self._TransformTestCode)
        for iface_file in (
                'AppSearchSession.java', 'GlobalSearchSession.java',
                'EnterpriseGlobalSearchSession.java', 'SearchResults.java'):
            dest_file_name = os.path.splitext(iface_file)[0] + 'Shim.java'
            self._TransformAndCopyFile(
                    os.path.join(api_source_dir, 'app/' + iface_file),
                    os.path.join(test_util_dest_dir, dest_file_name),
                    transform_func=self._TransformTestCode,
                    ignore_skips=True)

    def _ExportImplCode(self):
        impl_source_dir = os.path.join(self._jetpack_appsearch_root, JETPACK_IMPL_ROOT)
        impl_test_source_dir = os.path.join(self._jetpack_appsearch_root, JETPACK_IMPL_TEST_ROOT)
        impl_dest_dir = os.path.join(self._framework_appsearch_root, FRAMEWORK_IMPL_ROOT)
        impl_test_dest_dir = os.path.join(self._framework_appsearch_root, FRAMEWORK_IMPL_TEST_ROOT)
        test_util_test_source_dir = os.path.join(
                self._jetpack_appsearch_root, JETPACK_TEST_UTIL_TEST_ROOT)
        test_util_test_dest_dir = os.path.join(
                self._framework_appsearch_root, FRAMEWORK_TEST_UTIL_TEST_ROOT)

        # Prune
        self._PruneDir(impl_dest_dir)
        self._PruneDir(impl_test_dest_dir)
        self._PruneDir(test_util_test_dest_dir)

        # Copy impl classes
        def _TransformImplCode(contents):
            contents = (contents
                    .replace('package androidx.appsearch',
                            'package com.android.server.appsearch.external')
                    .replace('com.google.android.icing.protobuf.', 'com.google.protobuf.')
            )
            return self._TransformCommonCode(contents)
        self._TransformAndCopyFolder(
                impl_source_dir, impl_dest_dir, transform_func=_TransformImplCode)

        # Copy servicestests
        def _TransformImplTestCode(contents):
            contents = (contents
                    .replace('package androidx.appsearch',
                            'package com.android.server.appsearch.external')
                    .replace('com.google.android.icing.proto.',
                            'com.android.server.appsearch.icing.proto.')
                    .replace('com.google.android.appsearch.proto.',
                            'com.android.server.appsearch.appsearch.proto.')
                    .replace('com.google.android.icing.protobuf.',
                            'com.android.server.appsearch.protobuf.')
            )
            return self._TransformTestCode(contents)
        self._TransformAndCopyFolder(
                impl_test_source_dir, impl_test_dest_dir, transform_func=_TransformImplTestCode)
        self._TransformAndCopyFolder(
                test_util_test_source_dir,
                test_util_test_dest_dir,
                transform_func=self._TransformTestCode)

    def _FormatWrittenFiles(self):
        google_java_format_cmd = [GOOGLE_JAVA_FORMAT, '--aosp', '-i'] + self._written_files
        print('$ ' + ' '.join(google_java_format_cmd))
        subprocess.check_call(google_java_format_cmd, cwd=self._framework_appsearch_root)

    def ExportCode(self):
        self._ExportApiCode()
        self._ExportImplCode()
        self._FormatWrittenFiles()

    def WriteShaFile(self, sha):
        """Copies the git sha of the most recent public CL into a file on the framework side.

        This file is used for tracking, to determine what framework is synced to.

        You must always provide a sha of a submitted submitted git commit. If you abandon the CL
        pointed to by this sha, the next person syncing framework will be unable to find what CL it
        is synced to.

        The previous content of the sha file, if any, is returned.
        """
        file_path = os.path.join(self._framework_appsearch_root, SHA_FILE_NAME)
        old_sha = None
        if os.path.isfile(file_path):
          with open(file_path, 'r') as fh:
              old_sha = fh.read().rstrip()
        with open(file_path, 'w') as fh:
            print(sha, file=fh)
        print('Wrote "%s"' % file_path)
        return old_sha

    def FormatCommitMessage(self, old_sha, new_sha):
        print('\nCommand to diff old version to new version:')
        print('  git log --pretty=format:"* %h %s" {}..{} -- appsearch/'.format(old_sha, new_sha))
        pretty_log = subprocess.check_output([
            'git',
            'log',
            '--pretty=format:* %h %s',
            '{}..{}'.format(old_sha, new_sha),
            '--',
            'appsearch/'
        ]).decode("utf-8")
        bug_output = subprocess.check_output([
            '/bin/sh',
            '-c',
            'git log {}..{} -- appsearch/ | grep Bug: | sort | uniq'.format(old_sha, new_sha)
        ]).decode("utf-8")

        print('\n--------------------------------------------------')
        print('Update Framework from Jetpack.\n')
        print(pretty_log)
        print()
        for line in bug_output.splitlines():
            print(line.strip())
        print('Test: Presubmit\n')
        print('--------------------------------------------------\n')


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print('Usage: %s <path/to/framework/checkout> <git sha of head jetpack commit>' % (
                  sys.argv[0]),
              file=sys.stderr)
        sys.exit(1)
    if sys.argv[2].startswith('I'):
        print('Error: Git sha "%s" looks like a changeid. Please provide a git sha instead.' % (
                  sys.argv[2]),
              file=sys.stderr)
        sys.exit(1)

    source_dir = os.path.normpath(os.path.dirname(sys.argv[0]))
    dest_dir = os.path.normpath(sys.argv[1])
    dest_dir = os.path.join(dest_dir, 'packages/modules/AppSearch')
    if not os.path.isdir(dest_dir):
        print('Destination path "%s" does not exist or is not a directory' % (
                  dest_dir),
              file=sys.stderr)
        sys.exit(1)
    exporter = ExportToFramework(source_dir, dest_dir)
    exporter.ExportCode()

    # Update the sha file
    new_sha = sys.argv[2]
    old_sha = exporter.WriteShaFile(new_sha)
    if old_sha and old_sha != new_sha:
        exporter.FormatCommitMessage(old_sha, new_sha)
