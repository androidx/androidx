#!/usr/bin/env python3

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from zipfile import ZipFile

# CHANGEME:
# PATH_TO_APKSIGNER = '/Users/lelandr/Library/Android/sdk/build-tools/30.0.3/apksigner'
PATH_TO_APKSIGNER = 'apksigner'

SCRIPT_PATH = Path(__file__).parent.absolute()
SUPPORT_PATH = (SCRIPT_PATH / Path("../../../..")).resolve()
ROOT_DIR = (SUPPORT_PATH / Path("../..")).resolve()
BUILD_OUT_DIR = (Path(SUPPORT_PATH) / Path(
    "../../out/androidx/profileinstaller/integration-tests/"
    "testapp/build/outputs/apk/")).resolve()
MAPPING_OUT_PATH = (Path(SUPPORT_PATH) / Path(
    "../../out/androidx/profileinstaller/integration-tests/"
    "testapp/build/outputs/mapping/release/mapping.txt")).resolve()

APK_PREFIX = "testapp"
APK_PROFILE_FILE = "baseline.prof"

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--profile', '-p', required=False, default=str(Path(SCRIPT_PATH) / Path(
        "all_compose_profile.txt")))
    parser.add_argument('--apk-path', '-f', required=False, help='apk path to for processing a '
                                                                 'single apk')
    parser.add_argument('--jar', '-j', required=False, default=str(Path(SCRIPT_PATH) /
                                                                   Path("profgen-cli.jar")))
    parser.add_argument('--output', '-o', required=False, default="out.apk")
    parser.add_argument('--debug', type=bool, required=False, default=False)
    parser.add_argument('--apk-signer', required=False, default=PATH_TO_APKSIGNER)
    args = parser.parse_args()
    return args

def dir_for_buildtype(debug, path):
    if (path is not None):
        return Path(path).absolute()
    type = 'debug' if debug else 'release'
    newpath = BUILD_OUT_DIR / Path(type) / Path(APK_PREFIX + "-" + type + ".apk")
    return newpath.resolve()

def profile_from(pathStr):
    return Path(pathStr)

def jar_from(jarPathStr):
    return Path(jarPathStr)

def output_apk_from(outPathStr):
    return Path(outPathStr)

def check_env(apk_src, profile, jar, out_apk, apk_signer):
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

    if shutil.which(apk_signer) is None:
        print("ERROR: missing command line tool `apksigner`")
        print("please install it on your system or modify the constant PATH_TO_APKSIGNER")
        sys.exit(-1)

    if out_apk.exists():
        print("WARNING: Output apk already exists, overwriting")

    print(f"Apk source: //{apk_src.relative_to(ROOT_DIR)}")
    print(f"Profile:    //{profile.relative_to(ROOT_DIR)}")
    print(f"Profgen:    {jar.absolute()}")
    print(f"Output:     {output_apk.absolute()}")

def run_profgen(tmpDirName, apk_src, profile, jar, output_file, debug):
    print(f"Running profgen:")
    print(f"Profile: {profile.absolute()}")
    print(f"Apk: {apk_src.absolute()}")
    print(f"Output: {output_file.absolute()}")
    jar_command = [
        'java',
        '-jar',
        str(jar.absolute()),
        'generate',
        str(profile.absolute()),
        '--apk',
        str(apk_src.absolute()),
        '--output',
        str(output_file.absolute()),
        '--verbose'
    ] + ([] if debug else [
        '--map',
        str(MAPPING_OUT_PATH.absolute())
    ])
    subprocess.run(jar_command, stdout=sys.stdout)
    if not output_file.exists():
        print(f"Failed to generate output file from profgen")
        print(" ".join(jar_command))
        sys.exit(-1)

    output_size = os.stat(output_file.absolute()).st_size
    print(f"Successfully created profile. Size: {output_size}")

def repackage_jar(apk_src, profile, apk_dest, tmp_dir, apksigner):
    working_dir = tmp_dir / Path("working/")
    working_dir.mkdir()
    working_apk = working_dir / Path("working.apk")
    shutil.copyfile(apk_src, working_apk)
    print(f"copying to {SUPPORT_PATH / Path('baseline.prof')}")
    shutil.copyfile(profile, SUPPORT_PATH / Path("baseline.prof"))
    with ZipFile(working_apk, 'a') as zip:
        profile_destination = Path('assets/dexopt/') / Path(APK_PROFILE_FILE)
        if str(profile_destination) in [it.filename for it in zip.infolist()]:
            print("ERROR: profile already in apk, aborting")
            print(profile_destination)
            sys.exit(-1)
        zip.write(profile, profile_destination)

    keystore = Path.home() / Path(".android/debug.keystore")
    apksigner_command = [
        apksigner,
        'sign',
        '-ks',
        str(keystore.absolute()),
        '--ks-pass',
        'pass:android',
        str(working_apk.absolute())
    ]
    subprocess.check_output(apksigner_command)

    shutil.copyfile(working_apk, apk_dest)

def generate_apk(apk_src, profile, jar, out_apk, debug, apk_signer):
    check_env(apk_src, profile, jar, out_apk, apk_signer)
    with tempfile.TemporaryDirectory() as tmpDirName:
        output_profile = Path(tmpDirName) / Path("out.prof")
        print(f"Output profile: {output_profile.absolute()}")
        run_profgen(tmpDirName, apk_src, profile, jar, output_profile, debug)
        repackage_jar(apk_src, output_profile, out_apk, Path(tmpDirName), apk_signer)

if __name__ == "__main__":
    args = parse_args()
    apk_src = dir_for_buildtype(args.debug, args.apk_path)
    profile = profile_from(args.profile)
    jar = jar_from(args.jar)
    output_apk = output_apk_from(args.output)
    generate_apk(apk_src, profile, jar, output_apk, args.debug, args.apk_signer)
