#!/usr/bin/env python3

# Copyright (C) 2022 The Android Open Source Project
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

import os
import glob
import argparse
import pathlib
import shutil
import subprocess
import re


def sed(pattern, replacement, file):
    """ Performs an in-place string replacement of pattern in a target file

    Args:
        pattern: pattern to replace
        replacement: replacement for the pattern matches
        file: target file

    Returns:
        Nothing
    """

    with open(file) as reader:
        file_contents = reader.read()
    new_file_contents = re.sub(pattern, replacement, file_contents)
    with open(file, "w") as writer:
        writer.write(new_file_contents)


def single(items):
    """ Returns the only item from a list of just one item

    Raises a ValueError if the list does not contain exactly one element

    Args:
        items: a list of one item

    Returns:
        The only item from a single-item-list
    """

    if len(items) != 1:
        raise ValueError('Expected a list of size 1. Found: %s' % items)
    return items[0]


def update_tracing_perfetto(old_version, new_version, core_path, force_unstripped_binaries=False):
    """Updates tracing-perfetto version and artifacts (including building new binaries)

    Args:
        old_version: old version of the existing library
        new_version: new version of the library; defaults to incrementing the old_version
        core_path: path to frameworks/support directory
        force_unstripped_binaries: flag allowing to force unstripped variant of binaries
    Returns:
        Nothing
    """

    print("Updating tracing-perfetto, this can take a while...")

    # update version in code
    sed('tracingPerfettoVersion = "%s"' % old_version,
        'tracingPerfettoVersion = "%s"' % new_version,
        os.path.join(core_path, 'benchmark/benchmark-macro/src/androidTest/java/androidx/benchmark/'
                                'macro/perfetto/PerfettoSdkHandshakeTest.kt'))
    sed('TRACING_PERFETTO = "%s"' % old_version,
        'TRACING_PERFETTO = "%s"' % new_version,
        os.path.join(core_path, 'libraryversions.toml'))
    sed('#define VERSION "%s"' % old_version,
        '#define VERSION "%s"' % new_version,
        os.path.join(core_path, 'tracing/tracing-perfetto-binary/src/main/cpp/tracing_perfetto.cc'))
    sed('const val libraryVersion = "%s"' % old_version,
        'const val libraryVersion = "%s"' % new_version,
        os.path.join(core_path, 'tracing/tracing-perfetto/src/androidTest/java/androidx/tracing/'
                                'perfetto/jni/test/PerfettoNativeTest.kt'))
    sed('const val version = "%s"' % old_version,
        'const val version = "%s"' % new_version,
        os.path.join(core_path, 'tracing/tracing-perfetto/src/main/java/androidx/tracing/perfetto/'
                                'jni/PerfettoNative.kt'))

    # build new binaries
    subprocess.check_call(["./gradlew",
                           ":tracing:tracing-perfetto-binary:createProjectZip",
                           "-DTRACING_PERFETTO_REUSE_PREBUILTS_AAR=false"],
                          cwd=core_path)

    # copy binaries to prebuilts
    project_zip_dir = os.path.join(core_path, '../../out/dist/per-project-zips')
    project_zip_file = os.path.join(
        project_zip_dir,
        single(glob.glob('%s/*tracing*perfetto*binary*%s*.zip' % (project_zip_dir, new_version))))
    dst_dir = pathlib.Path(os.path.join(
        core_path,
        "../../prebuilts/androidx/internal/androidx/tracing/tracing-perfetto-binary",
        new_version))
    if dst_dir.exists():
        shutil.rmtree(dst_dir)
    dst_dir.mkdir()
    subprocess.check_call(
        ["unzip", "-xjqq", project_zip_file, '**/%s/**' % new_version, "-d", dst_dir])

    # force unstripped binaries if the flag is enabled
    if force_unstripped_binaries:
        # locate unstripped binaries
        out_dir = pathlib.Path(core_path, "../../out")
        arm64_lib_file = out_dir.joinpath(single(subprocess.check_output(
            'find . -type f -name "libtracing_perfetto.so"'
            ' -and -path "*RelWithDebInfo/*/obj/arm64*"'
            ' -exec stat -c "%Y %n" {} \\; |'
            ' sort | tail -1 | cut -d " " -f2-',
            cwd=out_dir,
            shell=True).splitlines()).decode())
        base_dir = arm64_lib_file.parent.parent.parent
        obj_dir = base_dir.joinpath('obj')
        if not obj_dir.exists():
            raise RuntimeError('Expected path %s to exist' % repr(obj_dir))
        jni_dir = base_dir.joinpath('jni')

        # prepare a jni folder to inject into the destination aar
        if jni_dir.exists():
            shutil.rmtree(jni_dir)
        shutil.copytree(obj_dir, jni_dir)

        # inject the jni folder into the aar
        dst_aar = os.path.join(dst_dir, 'tracing-perfetto-binary-%s.aar' % new_version)
        subprocess.check_call(['zip', '-r', dst_aar, 'jni'], cwd=base_dir)

        # clean up
        if jni_dir.exists():
            shutil.rmtree(jni_dir)

    # update SHA
    for arch in ['armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64']:
        checksum = subprocess.check_output(
            'unzip -cxqq "*tracing*binary*%s*.aar" "**/%s/libtracing_perfetto.so" | shasum -a256 |'
            ' awk \'{print $1}\' | tr -d "\n"' % (new_version, arch),
            cwd=dst_dir,
            shell=True
        ).decode()
        if not re.fullmatch('^[0-9a-z]{64}$', checksum):
            raise ValueError('Expecting a sha256 sum. Got: %s' % checksum)
        sed(
            '"%s" to "[0-9a-z]{64}"' % arch,
            '"%s" to "%s"' % (arch, checksum),
            os.path.join(core_path, 'tracing/tracing-perfetto/src/main/java/androidx/tracing/'
                                    'perfetto/jni/PerfettoNative.kt'))

    print("Updated tracing-perfetto.")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Updates tracing-perfetto in the source code, which involves:'
                    ' 1) updating hardcoded version references in code'
                    ' 2) building binaries and updating them in the prebuilts folder'
                    ' 3) updating SHA checksums hardcoded in code.')
    parser.add_argument('-f', '--frameworks-support-dir',
                        required=True,
                        help='Path to frameworks/support directory')
    parser.add_argument('-c', '--current-version',
                        required=True,
                        help='Current version, e.g. 1.0.0-alpha07')
    parser.add_argument('-t', '--target-version',
                        required=True,
                        help='Target version, e.g. 1.0.0-alpha08')
    parser.add_argument('-k', '--keep-binary-debug-symbols',
                        required=False,
                        default=False,
                        action='store_true',
                        help='Keeps debug symbols in the built binaries. Useful when profiling '
                             'performance of the library. ')
    args = parser.parse_args()
    core_path_abs = pathlib.Path(args.frameworks_support_dir).resolve()
    update_tracing_perfetto(args.current_version, args.target_version, core_path_abs,
                            args.keep_binary_debug_symbols)
