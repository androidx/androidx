#!/usr/bin/python3
 #
 # Copyright (C) 2016 The Android Open Source Project
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
import os,sys,json
from constants_and_utils import *

def usage():
    print("""
    Merges certain results of the gradle build.
    This script recognizes the following arguments in any order:
        mergeBuildInfo   (merge the buildInfo files (used by jetpad) produced by the most recent (compose and androidx) builds)
        mergeExecutionData        (merge the coverage execution data (recorded by jacoco) produced by the most recent builds)
        mergeSourceJars           (merge the source jars (used by jacoco/coverage ui) produced by the most recent builds)
            (these args are case sensitive)

        DIST_DIR=<a path to a directory>    if this is not passed, the default value is out/dist.
                                            Should be absolute or relative to e.g. androidx-master-dev/

    Sample usage: time busytown/merge.py mergeSourceJars DIST_DIR=out/dist
    """)
    exit(1)

def main():
    things_to_merge, dist_dir = parse_arguments()
    move_to_dist_dir(dist_dir)
    if SOURCE_JARS in things_to_merge: mergeSourceJars()
    if EXECUTION_DATA in things_to_merge: mergeCoverageExecution()
    if BUILD_INFO in things_to_merge:
        mergeAggregateBuildInfoFiles()
        mergeBuildInfoFolders()
    if LIBRARY_METRICS in things_to_merge: mergeLibraryMetrics()

def parse_arguments():
    things_to_merge = []
    for arg in sys.argv:
        if any(arg.endswith(SKIPPED_SUFFIX) for SKIPPED_SUFFIX in SKIPPED_ARG_SUFFIXES): continue
        elif arg in MERGE_COMMANDS:
            things_to_merge.append(arg)
        elif any(help_keyword in arg for help_keyword in HELP_SYNTAX_LIST):
            usage()
        else:
            print("ERROR:", arg, "is an invalid keyword")
            usage()
    dist_dir = os.environ['DIST_DIR'] if 'DIST_DIR' in os.environ else "out/dist"
    return things_to_merge, dist_dir

def move_to_dist_dir(dist_dir):
    move_to_base_dir(print_pwd=False)
    os.chdir(dist_dir)
    print("Currently in", os.getcwd())

def thisRunIsValidForCoverage():
    # Assert that AMD did not exclude any projects from being tested 
    # in either androidx or ui build (thereby invalidating this run for coverage).
    # If the AMD ran, that file will exist; if it skipped browser, that log will have:
    # "checking whether I should include :browser:browser and my answer is false"
    if (os.path.exists("affected_module_detector_log.txt") and\
        'false' in open('affected_module_detector_log.txt').read()) or\
        (os.path.exists("ui/affected_module_detector_log.txt") and\
        'false' in open('ui/affected_module_detector_log.txt').read()):
        print("WARNING: not doing coverage merging because the AMD was not a no-op")
        return False
    return True
 

def mergeSourceJars():
    if not thisRunIsValidForCoverage(): return
    # assert that the report jars exist
    if not (os.path.exists("jacoco-report-classes.jar") and os.path.exists("ui/jacoco-report-classes.jar")):
        print("WARNING: not merging jacoco source jars as the source jars are not present")
        return
    ziptmp = "ziptmp"
    run_command("rm -rf " + ziptmp)
    run_command("mkdir " + ziptmp)
    # exclude these test/sample app files which are duplicated in the source jars so that `unzip` doesn't fail
    # See b/145211240 for more context. A full solution may be blocked on b/143934485.
    run_command("unzip -quo jacoco-report-classes.jar -d ziptmp -x \"testapp-debug-androidTest-allclasses*\"")
    run_command("unzip -quo ui/jacoco-report-classes.jar -d ziptmp -x \"samples-debug-androidTest-allclasses*\"")
    run_command("rm -f jacoco-report-classes-all.jar") # -f to not fail if file doesn't exist
    run_command("jar -cf jacoco-report-classes-all.jar -C ziptmp .")
    run_command("rm -rf " + ziptmp)

def mergeCoverageExecution():
    if not thisRunIsValidForCoverage(): return
    # assert that the coverage zips exist
    if not (os.path.exists("coverage_ec_files.zip") and os.path.exists("ui/coverage_ec_files.zip")):
        print("WARNING: not merging coverage execution data as the coverage zips are not present")
        return
    ziptmp = "ziptmp"
    run_command("rm -rf " + ziptmp)
    run_command("mkdir " + ziptmp)
    run_command("unzip -quo coverage_ec_files.zip -d ziptmp") # -quo = quiet; keep newer only
    run_command("unzip -quo ui/coverage_ec_files.zip -d ziptmp")
    run_command("rm -f coverage_ec_files_all.zip") # -f to not fail if file doesn't exist
    run_command("zip -rq coverage_ec_files_all.zip ziptmp")
    run_command("rm -rf " + ziptmp)

def mergeAggregateBuildInfoFiles() :
    print("merging aggregate build info files")
    androidx_buildInfo = json.load(open("androidx_aggregate_build_info.txt"))["artifacts"]
    compose_buildInfo = json.load(open("ui/androidx_aggregate_build_info.txt"))["artifacts"]
    duplicate_checking_dict = {}
    for buildinfo in androidx_buildInfo + compose_buildInfo:
        artifactId, groupId, sha = buildinfo["artifactId"], buildinfo["groupId"], buildinfo["sha"]
        # artifactid and groupid is the unique identifier for libraries
        if (artifactId, groupId) not in duplicate_checking_dict:
            duplicate_checking_dict[(artifactId, groupId)] = (sha, buildinfo)
        else:
            expected_hash = duplicate_checking_dict[(artifactId, groupId)][0]
            if expected_hash != sha:
                raise Exception("Build info specifies having been built from multiple commits: " + expected_hash + " and " + sha + ". Were AndroidX and Compose built from the same commit?")
        # don't allow androidx and compose to release two different versions of the same lib
    resultJson = {"artifacts":[buildinfo for sha,buildinfo in duplicate_checking_dict.values()]}

    with open("androidx_aggregate_build_info.txt", 'w') as outfile:
        json.dump(resultJson, outfile, sort_keys=True, indent=4, separators=(',', ': '))

def mergeBuildInfoFolders(): # -a = all in directory. -u = overwrite older (in case androidx build hasn't been run in a while)
    run_command("cp -au ui/build-info/. build-info/")

def mergeLibraryMetrics():
    run_command("cp -au ui/librarymetrics/. librarymetrics/")

if __name__ == "__main__": main()
