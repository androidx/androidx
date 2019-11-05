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
tasks = "buildOnServer ktlint"
gradlew = "frameworks/support/gradlew"
gradlewC = "frameworks/support/ui/gradlew"

outDirArg = "OUT_DIR=out"
androidHomeArg = "ANDROID_HOME=" + os.path.realpath("prebuilts/fullsdk-linux")

androidxGradleCommand = " ".join([outDirArg, androidHomeArg, gradlew, tasks, projectDirArg])
composeGradleCommand  = " ".join([outDirArg + ui, androidHomeArg, gradlewC, tasks,
                                projectDirArg + ui])
# the DIST_DIR arg
distargs = [arg for arg in sys.argv if "DIST_DIR=" in arg]
distDir = "out/dist" if len(distargs) == 0 else distargs[0][9:]
distarg = "" if len(distargs) == 0 else " " + distargs[0]
distargC = "" if len(distargs) == 0 else " " + distargs[0] + ui
# args of the form VAR=thing
preargs = " ".join([arg for arg in sys.argv if '=' in arg and arg not in distargs])
postargs = " ".join([arg for arg in sys.argv if ".py" not in arg and arg not in distargs
                                                            and arg not in preargs])
# remove "doAllTheBuild.py"

def runCommand(commandText):
    print('Running "' + commandText + '"')
    result = os.system(commandText)
    assert(os.WEXITSTATUS(result)) == 0

def runGradleBuilds():
    runCommand(" ".join([preargs + distarg, androidxGradleCommand, postargs]))
    runCommand(" ".join([preargs + distarg, androidxGradleCommand, "-PdisallowExecution", postargs]))
    runCommand(" ".join([preargs + distargC, composeGradleCommand, postargs]))
    runCommand(" ".join([preargs + distargC, composeGradleCommand, "-PdisallowExecution", postargs]))

def mergeAggregateBuildInfoFiles() :
    androidxBuildInfo = json.load(open("androidx_aggregate_build_info.txt"))["artifacts"]
    nitemsA = len(androidxBuildInfo)
    composeBuildInfo = json.load(open("ui/androidx_aggregate_build_info.txt"))["artifacts"]
    nitemsC = len(composeBuildInfo)
    duplicatecheckingdict = {}
    for buildinfo in androidxBuildInfo + composeBuildInfo:
        artifactId, groupId, sha = buildinfo["artifactId"], buildinfo["groupId"], buildinfo["sha"]
        # artifactid and groupid is the unique identifier for libraries
        if (artifactId, groupId) not in duplicatecheckingdict:
            duplicatecheckingdict[(artifactId, groupId)] = (sha, buildinfo)
        else: assert duplicatecheckingdict[(artifactId, groupId)][0] == sha
        # androidx and compose requiring two different versions of the same lib breaks everything
    resultJson = {"artifacts":[buildinfo for sha,buildinfo in duplicatecheckingdict.values()]}

    with open("androidx_aggregate_build_info.txt", 'w') as outfile:
        json.dump(resultJson, outfile, sort_keys=True, indent=4, separators=(',', ': '))

def mergeBuildInfoFolders():
    os.system("cp -a ui/build-info/. build-info/")
                        # -a = all in directory; -u = overwrite iff newer

runGradleBuilds()

def doMergesInDistDir():
    os.chdir(distDir)
    mergeAggregateBuildInfoFiles()
    mergeBuildInfoFolders()

doMergesInDistDir()
