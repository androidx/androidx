#
#  Copyright (C) 2019 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
"""A helper script for validateRefactor.sh. Should generally not be used directly.

Can be used directly if validateRefactor.sh has already created the out-old & out-new dirs.
In such a case, it can be run to compare those directories without regenerating them.
This is generally only useful when updating baselines or iterating on this script itself.
Takes baseline names as CLI arguments, which may be passed through from validateRefactor.sh.

Typical usage example:

  python validateRefactorHelper.py agpKmp
"""
import itertools
import os
import shutil
import subprocess
import sys

# noto-emoji-compat `bundleinside`s an externally-built with-timestamps jar.
# classes.jar is compared using `diffuse` instead of unzipping and diffing class files.
bannedJars = ["-x", "noto-emoji-compat-java.jar", "-x", "classes.jar"]
# java and json aren"t for unzipping, but the poor exclude-everything-but-jars regex doesn't
# exclude them. Same for exclude-non-klib and .kt/.knm
areNotZips = ["-x", r"**\.java", "-x", r"**\.json", "-x", r"**\.kt", "-x", r"**\.knm"]
# keeps making my regexes fall over :(
hasNoExtension = ["-x", "manifest", "-x", "module"]
doNotUnzip = bannedJars + areNotZips + hasNoExtension

def diff(excludes):
    return popenAndReturn(["diff", "-r", "../../out-old/dist/", "../../out-new/dist/"] + excludes)

def popenAndReturn(args):
    return subprocess.Popen(args, stdout=subprocess.PIPE).stdout.read().decode("utf-8").split("\n")

# Finds and unzips all files with old/new diff that _do not_ match the argument regex.
def findFilesMatchingWithDiffAndUnzip(regexThatMatchesEverythingElse):
    # Exclude all things that are *not* the desired zip type
    # (because diff doesn"t have an --include, only --exclude).
    zipsWithDiffs = diff(["-q", "-x", regexThatMatchesEverythingElse] + doNotUnzip)
    # Take only changed files, not new/deleted ones (the diff there is obvious)
    zipsWithDiffs = filter(lambda s: s.startswith("Files"), zipsWithDiffs)
    zipsWithDiffs = map(lambda s: s.split()[1:4:2], zipsWithDiffs)
    zipsWithDiffs = list(itertools.chain.from_iterable(zipsWithDiffs))  # flatten
    # And unzip them
    for filename in zipsWithDiffs:
        print("unzipping " + filename)
        # if os.path.exists(filename+".unzipped/"): os.rmdir(filename+".unzipped/")
        shutil.rmtree(filename+".unzipped/")
        subprocess.Popen(["unzip", "-qq", "-o", filename, "-d", filename+".unzipped/"])

diffusePath = "../../prebuilts/build-tools/diffuse-0.3.0/bin/diffuse"

def compareWithDiffuse(listOfJars):
    for jarPath in list(filter(None, listOfJars)):
        print("jarpath: " + jarPath)
        newJarPath = jarPath.replace("out-old", "out-new")
        print(popenAndReturn([diffusePath, "diff", "--jar", jarPath, newJarPath]))

# We might care to know whether .sha1 or .md5 files have changed, but changes in those files will
# always be accompanied by more meaningful changes in other files, so we don"t need to show changes
# in .sha1 or .md5 files, or in .module files showing the hashes of other files, or config names.
excludedHashes = ["-x", "*.md5*", "-x", "*.sha**", "-I", "        \"md5\".*", \
  "-I", "        \"sha.*", "-I", "        \"size\".*", "-I", "      \"name\".*"]
# Don"t care about maven-metadata files because they have timestamps in them.
excludedFiles = ["-x", "*maven-metadata.xml**", "-x", r"**\.knm"]  # temporarily ignore knms
# Also, ignore files that we already unzipped
excludedZips = ["-x", "*.zip", "-x", "*.jar", "-x", "*.aar", "-x", "*.apk", "-x", "*.klib"]

# These are baselined changes that we understand and know are no-ops in refactors
# "Unskippable" changes are multi-line and can't be skipped in `diff`, so post-process
baselinedChangesForAgpKmp = [
    # these are new attributes being added
    """        "org.gradle.libraryelements": "aar",""",
    """        "org.gradle.jvm.environment": "android",""",
    """        "org.gradle.jvm.environment": "non-jvm",""",
    """        "org.gradle.jvm.environment": "standard-jvm",""",
    # this attribute swap occurs alongside the above new attributes added.
    # https://chat.google.com/room/AAAAW8qmCIs/4phaNn_gsrc
    """        "org.jetbrains.kotlin.platform.type": "androidJvm\"""",
    """        "org.jetbrains.kotlin.platform.type": "jvm\"""",
    # name-only change; nothing resolves based on names
    """      "name": "releaseApiElements-published",""",
    """      "name": "androidApiElements-published",""",
    """            <pre>actual typealias""",  # open bug in dackka b/339221337
    # we are switching from our KMP sourcejars solution to the upstream one
    """        "org.gradle.docstype": "fake-sources",""",
    """        "org.gradle.docstype": "sources",""",
]
unskippableBaselinedChangesForAgpKmp = [
    """
<           },
<           "excludes": [
<             {
<               "group": "org.jetbrains.kotlin",
<               "module": "kotlin-stdlib-common"
<             },
<             {
<               "group": "org.jetbrains.kotlin",
<               "module": "kotlin-test-common"
<             },
<             {
<               "group": "org.jetbrains.kotlin",
<               "module": "kotlin-test-annotations-common"
<             }
<           ]
---
>           }
""",
"""
<       <exclusions>
<         <exclusion>
<           <groupId>org.jetbrains.kotlin</groupId>
<           <artifactId>kotlin-stdlib-common</artifactId>
<         </exclusion>
<         <exclusion>
<           <groupId>org.jetbrains.kotlin</groupId>
<           <artifactId>kotlin-test-common</artifactId>
<         </exclusion>
<         <exclusion>
<           <groupId>org.jetbrains.kotlin</groupId>
<           <artifactId>kotlin-test-annotations-common</artifactId>
<         </exclusion>
<       </exclusions>
"""
]

baselinedChanges = []
unskippableBaselinedChanges = []
arguments = sys.argv[1:]
if "agpKmp" in arguments:
    arguments.remove("agpKmp")
    print("IGNORING DIFF FOR agpKmp")
    baselinedChanges += baselinedChangesForAgpKmp
    unskippableBaselinedChanges += unskippableBaselinedChangesForAgpKmp
if arguments:
    print("invalid argument(s) for validateRefactorHelper: " + ", ".join(arguments))
    print("currently recognized arguments: agpKmp")
    exit()

# interleave "-I" to tell diffutils to 'I'gnore the baselined lines
baselinedChanges = list(itertools.chain.from_iterable(zip(["-I"]*99, baselinedChanges)))

# post-process the diff output to remove multi-line changes that can't be excluded in `diff` itself
def filterOutUnskippableBaselinedChanges(inputString):
    result = inputString
    for toRemove in unskippableBaselinedChanges:
        i = result.find(toRemove)
        while (i != -1):
            j = result.rfind("\n", 0, i-2)  # also find and remove previous line e.g. 82,96c70
            result = result[:j+1] + result[i+len(toRemove):]
            i = result.find(toRemove)
    #remove all "diff -r ..." header lines that no longer have content due to baselining
    result = result.split("\n")
    nRemoved = 0
    for i in range(len(result)):  # check for consecutive `diff -r` lines: the first has no content
        if not result[i-nRemoved].startswith("diff -r "): continue
        if not result[i+1-nRemoved].startswith("diff -r "): continue
        del result[i-nRemoved]
        nRemoved+=1
    if not result[-1]: del result[-1]  # remove possible ending blank line
    if result[-1].startswith("diff -r "): del result[-1]  # terminal `diff -r` line: has no content
    return "\n".join(result)

# print(baselinedChanges)

# Find all zip files with a diff, e.g. the tip-of-tree-repository file, and maybe the docs zip
# findFilesMatchingWithDiffAndUnzip(r"**\.[^z][a-z]*")
# Find all aar and apk files with a diff. The proper regex would be `.*\..*[^akpr]+.*`, but it
# doesn"t work in difftools exclude's very limited regex syntax.
findFilesMatchingWithDiffAndUnzip(r"**\.[^a][a-z]*")
# Find all jars and klibs and unzip them (comes after because they could be inside aars/apks).
findFilesMatchingWithDiffAndUnzip(r"**\.[^j][a-z]*")
findFilesMatchingWithDiffAndUnzip(r"**\.[^k][a-z]*")
# now find all diffs in classes.jars
classesJarsWithDiffs = popenAndReturn(["find", "../../out-old/dist/", "-name", "classes.jar"])
print("classes.jar s: " + str(classesJarsWithDiffs))
compareWithDiffuse(classesJarsWithDiffs)
# Now find all diffs in non-zipped files
finalExcludes = excludedHashes + excludedFiles + excludedZips + baselinedChanges
finalDiff = "\n".join(diff(finalExcludes))
finalDiff = filterOutUnskippableBaselinedChanges(finalDiff)
print(finalDiff)

