#!/usr/bin/python
import os,sys,json

# SAMPLE USE CASE: python doAllTheBuild.py --no-daemon -PuseMaxDepVersions
#
# Runs both gradle builds, then merges the output that needs merging
# Arguments to this script are passed to both gradle builds without modification
# Exceptions to this policy:
#   if DIST_DIR=path/to/dir is not passed, it is assumed to be out/dist(/ui)
#   build variables OUT_DIR=out(/ui), ANDROID_HOME=prebuilts/fullsdk-linux
#       are set before each gradle build
#   -p frameworks/support(/ui) is passed by default
#   arguments with '=' in them (e.g. SNAPSHOT=true) are prefixed to the gradle runs

# If you want to run only one gradle build, you do not want to merge.
# So do not run this; instead run the gradle build directly

os.chdir(os.path.dirname(os.path.abspath(__file__)))
os.chdir("../../../")

projectDirArg = "-p frameworks/support"
ui = "/ui"
tasks = "buildOnServer"
gradlew = "frameworks/support/gradlew"
gradlewC = "frameworks/support/ui/gradlew"

outDirArg = "OUT_DIR=out"
androidHomeArg = "ANDROID_HOME=prebuilts/fullsdk-linux"

androidxGradleCommand = " ".join([outDirArg, androidHomeArg, gradlew, tasks, projectDirArg])
composeGradleCommand  = " ".join([outDirArg + ui, androidHomeArg, gradlewC, tasks, projectDirArg + ui])

distargs = [arg for arg in sys.argv if "DIST_DIR=" in arg]
distDir = "out/dist" if len(distargs) == 0 else distargs[0][8:]
distarg = "" if len(distargs) == 0 else " " + distargs[0]
distargC = "" if len(distargs) == 0 else " " + distargs[0] + ui
preargs = " ".join([arg for arg in sys.argv if '=' in arg and arg not in distargs]) # args of the form VAR=thing
postargs = " ".join([arg for arg in sys.argv if ".py" not in arg and arg not in distargs and arg not in preargs])
# remove "doAllTheBuild.py"

def runGradleBuilds():
    os.system(" ".join([preargs + distarg, androidxGradleCommand, postargs]))
    os.system(" ".join([preargs + distargC, composeGradleCommand, postargs]))

def mergeAggregateBuildInfoFiles() :
    N_COMMON_ARTIFACTS = 2 #the number of artifacts in both androidx and compose
    #benchmark-common and benchmark-junit4
    androidxBuildInfo = json.load(open("androidx_aggregate_build_info.txt"))["artifacts"]
    nitemsA = len(androidxBuildInfo)
    composeBuildInfo = json.load(open("ui/androidx_aggregate_build_info.txt"))["artifacts"]
    nitemsC = len(composeBuildInfo)
    resultJson = {"artifacts":androidxBuildInfo + composeBuildInfo}
    #assert len(androidxBuildInfo) == nitemsA + nitemsC - N_COMMON_ARTIFACTS
    #TODO: make this actually work, and properly

    with open("all_aggregate_build_info.txt", 'w') as outfile:
        json.dump(resultJson, outfile, sort_keys=True, indent=4, separators=(',', ': '))

def mergeBuildInfoFolders():
    os.system("cp -au ui/build-info/. build-info/") 
                        # -a = all in directory; -u = overwrite iff newer

runGradleBuilds()

def doThingsInDistDir():
    os.chdir(distDir)
    mergeAggregateBuildInfoFiles()
    mergeBuildInfoFolders()

doThingsInDistDir()