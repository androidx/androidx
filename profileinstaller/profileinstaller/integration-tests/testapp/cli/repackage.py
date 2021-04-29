#!/usr/bin/env python3

import argparse
from pathlib import Path
import os
import re
import shutil
import sys
import shutil
import subprocess
import tempfile
from zipfile import ZipFile

SCRIPT_PATH = Path(__file__).parent.absolute()
SUPPORT_PATH = (SCRIPT_PATH / Path("../../../../..")).resolve()
ROOT_DIR = (SUPPORT_PATH / Path("../..")).resolve()
BUILD_OUT_DIR = (Path(SUPPORT_PATH) / Path(
    "../../out/androidx/profileinstaller/profileinstaller/integration-tests/"
    "testapp/build/outputs/apk/")).resolve()

APK_PREFIX = "testapp"
APK_PROFILE_FILE = "baseline.prof"

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--variant', '-v', required=False, choices=['debug', 'release'],
                        default='debug', help='apk build variant to annotate from default '
                                              'build output directory')
    parser.add_argument('--profile', '-p', required=False, default=str(Path(SCRIPT_PATH) / Path(
        "all_compose_profile.txt")))
    parser.add_argument('--apk-path', '-f', required=False, help='apk path to for processing a '
                                                                 'single apk')
    parser.add_argument('--jar', '-j', required=False, default=str(Path(SCRIPT_PATH) /
                                                                   Path("profgen-cli.jar")))
    parser.add_argument('--output', '-o', required=False, default="out.apk")
    args = parser.parse_args()
    return args

def dir_for_buildtype(type, path):
    if (path is not None):
        return Path(path).absolute()
    newpath = BUILD_OUT_DIR / Path(type) / Path(APK_PREFIX + "-" + type + ".apk")
    return newpath.resolve()

def profile_from(pathStr):
    return Path(pathStr)

def jar_from(jarPathStr):
    return Path(jarPathStr)

def output_apk_from(outPathStr):
    return Path(outPathStr)

def check_env(apk_src, profile, jar, out_apk):
    if not apk_src.exists():
        print("ERROR: APK source does not exist, build it using gradle.")
        print(apk_src)
        sys.exit(-1)

    if not profile.exists():
        print("ERROR: Profile path does not exist")
        print(profile)
        sys.exit(-1)

    if not jar.exists():
        print("ERROR: Jar file does not exist")
        print(jar)
        sys.exit(-1)

    if shutil.which('apksigner') is None:
        print("ERROR: missing command line tool `apksigner`")
        print("please install it on your system")
        sys.exit(-1)

    if out_apk.exists():
        print("WARNING: Output apk already exists, overwriting")

    print(f"Apk source: //{apk_src.relative_to(ROOT_DIR)}")
    print(f"Profile:    //{profile.relative_to(ROOT_DIR)}")
    print(f"Profgen:    {jar.absolute()}")
    print(f"Output:     {output_apk.absolute()}")

def run_profgen(tmpDirName, apk_src, profile, jar, output_file):
    jar_command = [
        'java',
        '-jar',
        str(jar.absolute()),
        'bin',
        str(profile.absolute()),
        '--apk',
        str(apk_src.absolute()),
        '--output',
        str(output_file.absolute())
    ]
    subprocess.check_output(jar_command)
    if not output_file.exists():
        print(f"Failed to generate output file from profgen")
        print(" ".join(jar_command))
        sys.exit(-1)

def repackage_jar(apk_src, profile, apk_dest, tmpDir):
    working_dir = tmpDir / Path("working/")
    working_dir.mkdir()
    working_apk = working_dir / Path("working.apk")
    shutil.copyfile(apk_src, working_apk)
    with ZipFile(working_apk, 'a') as zip:
        profile_destination = Path('assets/dexopt/') / Path(APK_PROFILE_FILE)
        if str(profile_destination) in [it.filename for it in zip.infolist()]:
            print("ERROR: profile already in apk, aborting")
            print(profile_destination)
            sys.exit(-1)
        zip.write(profile, profile_destination)

    keystore = Path.home() / Path(".android/debug.keystore")
    apksigner_command = [
        'apksigner',
        'sign',
        '-ks',
        str(keystore.absolute()),
        '--ks-pass',
        'pass:android',
        str(working_apk.absolute())
    ]
    subprocess.check_output(apksigner_command)

    shutil.copyfile(working_apk, apk_dest)

def generate_apk(apk_src, profile, jar, out_apk):
    check_env(apk_src, profile, jar, out_apk)
    with tempfile.TemporaryDirectory() as tmpDirName:
        output_profile = Path(tmpDirName) / Path("out.prof")
        run_profgen(tmpDirName, apk_src, profile, jar, output_profile)
        repackage_jar(apk_src, output_profile, out_apk, Path(tmpDirName))

if __name__ == "__main__":
    args = parse_args()
    apk_src = dir_for_buildtype(args.variant, args.apk_path)
    profile = profile_from(args.profile)
    jar = jar_from(args.jar)
    output_apk = output_apk_from(args.output)
    generate_apk(apk_src, profile, jar, output_apk)