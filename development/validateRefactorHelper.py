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
import logging
import queue
import re
import shutil
import subprocess
import sys
import threading
from typing import Dict

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

# noto-emoji-compat `bundleinside`s an externally-built with-timestamps jar.
# classes.jar is compared using `diffuse` instead of unzipping and diffing class files.
bannedJars = ["-x", "noto-emoji-compat-java.jar", "-x", "classes.jar"]
# java and json aren't for unzipping, but the poor exclude-everything-but-jars regex doesn't
# exclude them. Same for exclude-non-klib and .kt/.knm
areNotZips = ["-x", r"**\.java", "-x", r"**\.json", "-x", r"**\.kt", "-x", r"**\.knm", "-x", r"**\.xml",
              "-x", r"**\.sha1", "-x", r"**\.sha256", "-x", r"**\.sha512", "-x", r"**\.md5",
              "-x", r"**\.module", "-x", r"**\.pom", "-x", r"**\.html"]
# keeps making my regexes fall over :(
hasNoExtension = ["-x", "manifest", "-x", "module"]
doNotUnzip = bannedJars + areNotZips + hasNoExtension

def diff(excludes):
    return popenAndReturn(["diff", "-r", "../../out-old/dist/", "../../out-new/dist/"] + excludes)

def popenAndReturn(args):
    logger.debug(" ".join(args))
    return subprocess.Popen(args, stdout=subprocess.PIPE).stdout.read().decode("utf-8").split("\n")

# Finds and unzips all files with old/new diff that _do not_ match the argument regexes.
# Because the `diff` command doesn't have an --include, only --exclude.
def findFilesNotMatchingWithDiffAndUnzip(*regexesToExclude):
    excludeArgs = list(itertools.chain.from_iterable(zip(["-x"]*9, regexesToExclude)))
    # Exclude all things that are *not* the desired zip type
    zipsWithDiffs = diff(["-q"] + excludeArgs + doNotUnzip)
    # Take only changed files, not new/deleted ones (the diff there is obvious)
    zipsWithDiffs = filter(lambda s: s.startswith("Files"), zipsWithDiffs)
    zipsWithDiffs = map(lambda s: s.split()[1:4:2], zipsWithDiffs)
    zipsWithDiffs = itertools.chain.from_iterable(zipsWithDiffs)  # flatten
    workQueueOfZips = queue.LifoQueue()
    for it in zipsWithDiffs: workQueueOfZips.put(it)
    # And unzip them
    # If we spam unzip commands without a break, the unzips start failing.
    # if we wait after every Popen, the script runs very slowly
    # So create a pool of 10 unzip workers to consume from zipsWithDiffs
    numWorkers = 10
    workers = []
    for i in range(min(numWorkers, workQueueOfZips.qsize())):
        w = threading.Thread(target=unzipWorker, args=(workQueueOfZips,))
        w.start()
        workers.append(w)
    for w in workers: w.join()

def unzipWorker(workQueueOfZips):
    while not workQueueOfZips.empty():
        zipFilePath = workQueueOfZips.get(0)
        try: shutil.rmtree(zipFilePath+".unzipped/")
        except FileNotFoundError: pass
        logger.debug("unzipping " + zipFilePath)
        subprocess.Popen(["unzip", "-qq", "-o", zipFilePath, "-d", zipFilePath+".unzipped/"]).wait()

diffusePath = "../../prebuilts/build-tools/diffuse/diffuse-0.3.0/bin/diffuser"

diffuseIsPresent = True
def compareWithDiffuse(listOfJars):
    global diffuseIsPresent
    if not diffuseIsPresent: return
    for jarPath in list(filter(None, listOfJars)):
        logger.info("jarpath: " + jarPath)
        newJarPath = jarPath.replace("out-old", "out-new")
        try: logger.info("\n".join(popenAndReturn([diffusePath, "diff", "--jar", jarPath, newJarPath])))
        except FileNotFoundError:
            logger.warning(f"https://github.com/JakeWharton/diffuse is not present on disk in expected location"
                  f" ${diffusePath}. You can install it.")
            diffuseIsPresent = False
            return

# We might care to know whether .sha1 or .md5 files have changed, but changes in those files will
# always be accompanied by more meaningful changes in other files, so we don"t need to show changes
# in .sha1 or .md5 files, or in .module files showing the hashes of other files, or config names.
excludedHashes = ["-x", "*.md5*", "-x", "*.sha**", "-I", "        \"md5\".*",
"-I", "        \"sha.*", "-I", "        \"size\".*", "-I", "      \"name\".*"]
# Don"t care about maven-metadata files because they have timestamps in them.
# temporarily ignore knm files
# If changes to the dackka args json are meaningful, they will affect the generated docs and show diff there
# kotlin-project-structure-metadata.json is not used by Kotlin or Android Studio, and changes to it
# are dependent on the plugin and targets that are applied to the project
excludedFiles = ["-x", "*maven-metadata.xml**", "-x", r"**\.knm", "-x", "dackkaArgs-docs-tip-of-tree.json", "-x", "**kotlin-project-structure-metadata.json"]
# Also, ignore files that we already unzipped
excludedZips = ["-x", "*.zip", "-x", "*.jar", "-x", "*.aar", "-x", "*.apk", "-x", "*.klib"]

# These are baselined changes that we understand and know are no-ops in refactors
# "Unskippable" changes are multi-line and can't be skipped in `diff`, so post-process
baselinedChangesForAgpKmp = [
    # these are new attributes being added
    """>         "org.gradle.libraryelements": "aar",""",
    """>         "org.gradle.jvm.environment": "android",""",
    """>         "org.gradle.jvm.environment": "non-jvm",""",
    """>         "org.gradle.jvm.environment": "standard-jvm",""",
    """>       <type>aar</type>""",
    # this attribute swap occurs alongside the above new attributes added.
    # https://chat.google.com/room/AAAAW8qmCIs/4phaNn_gsrc
    """<         "org.jetbrains.kotlin.platform.type": "androidJvm\"""",
    """>         "org.jetbrains.kotlin.platform.type": "jvm\"""",
    # name-only change; nothing resolves based on names
    """<      "name": "releaseApiElements-published",""",
    """>      "name": "androidApiElements-published",""",
    """             <pre>actual typealias""",  # open bug in dackka b/339221337
    # we are switching from our KMP sourcejars solution to the upstream one
    """<         "org.gradle.docstype": "fake-sources",""",
    """>         "org.gradle.docstype": "sources",""",
]
unskippableBaselinedChangesForAgpKmp = [
# This was an AGP workaround for a dependency resolution issue for kotlin stdlib
# https://chat.google.com/room/AAAAW8qmCIs/4phaNn_gsrc
re.compile(r"""
[0-9]+,[0-9]+c[0-9]+
<           \},
<           "excludes": \[
<             \{
<               "group": "org.jetbrains.kotlin",
<               "module": "kotlin-stdlib-common"
<             \},
<             \{
<               "group": "org.jetbrains.kotlin",
<               "module": "kotlin-test-common"
<             \},
<             \{
<               "group": "org.jetbrains.kotlin",
<               "module": "kotlin-test-annotations-common"
<             \}
<           \]
---
>           \}"""),
# This was an AGP workaround for a dependency resolution issue for kotlin stdlib
# https://chat.google.com/room/AAAAW8qmCIs/4phaNn_gsrc
re.compile(r"""
[0-9]+,[0-9]+c[0-9]+
<           "module": "kotlin-stdlib",
<           "excludes": \[
<             \{
<               "group": "org.jetbrains.kotlin",
<               "module": "kotlin-stdlib-common"
<             \},
<             \{
<               "group": "org.jetbrains.kotlin",
<               "module": "kotlin-test-common"
<             \},
<             \{
<               "group": "org.jetbrains.kotlin",
<               "module": "kotlin-test-annotations-common"
<             \}
<           \]
---
>           "module": "kotlin-stdlib"
"""),
re.compile(r"""
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
<       </exclusions>"""),
# .module files[] blocks aren't ordered; baseline reordering of samples-sources b/374956513
re.compile(r"""
[0-9]+,[0-9]+d[0-9]+
<           "name": "[a-z3\-]+-[0-9].[0-9].[0-9](-[a-z0-9]+)?-samples-sources.jar",
<           "url": "[a-z3\-]+-[0-9].[0-9].[0-9](-[a-z0-9]+)?-samples-sources.jar",
<           "size": [0-9]+,
<           "sha512": "[0-9a-z]+",
<           "sha256": "[0-9a-z]+",
<           "sha1": "[0-9a-z]+",
<           "md5": "[0-9a-z]+"
<         \},
<         \{
[0-9]+a[0-9]+,[0-9]+
>         \},
>         \{
>           "name": "[a-z3\-]+-[0-9].[0-9].[0-9](-[a-z0-9]+)?-samples-sources.jar",
>           "url": "[a-z3\-]+-[0-9].[0-9].[0-9](-[a-z0-9]+)?-samples-sources.jar",
>           "size": [0-9]+,
>           "sha512": "[0-9a-z]+",
>           "sha256": "[0-9a-z]+",
>           "sha1": "[0-9a-z]+",
>           "md5": "[0-9a-z]+"
"""),
# This one is okay because the common pom expresses a dependency on the jvm pom
# https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.7.3/kotlinx-coroutines-core-1.7.3.pom
re.compile(r"""[0-9]+c[0-9]+
<       <artifactId>kotlinx-coroutines-core-jvm</artifactId>
---
>       <artifactId>kotlinx-coroutines-core</artifactId>"""),
# AGP-KMP adds a new default sourceSet, which in itself doesn't do anything
re.compile(r"""(11,17d10|12,18d11)
<       "name": "androidRelease",
<       "dependencies": \[
<         "commonMain"
<       \],
<       "analysisPlatform": "jvm"
<     \},
<     \{
"""),
]

baselines = []
baselinedChanges = []
unskippableBaselinedChanges = []
arguments = sys.argv[1:]
if "agpKmp" in arguments:
    arguments.remove("agpKmp"); baselines += ["agpKmp"]
    logger.info("IGNORING DIFF FOR agpKmp")
    baselinedChanges += baselinedChangesForAgpKmp
    unskippableBaselinedChanges += unskippableBaselinedChangesForAgpKmp
    excludedFiles += ["-x", r"**\.aar.unzipped/res"]  # agp-kmp may add this empty
if arguments:
    logger.error("invalid argument(s) for validateRefactorHelper: " + ", ".join(arguments))
    logger.error("currently recognized arguments: agpKmp")
    exit()

# interleave "-I" to tell diffutils to 'I'gnore the baselined lines
baselinedChangesArgs = list(itertools.chain.from_iterable(zip(["-I"]*99, [it.removeprefix(">").removeprefix("<") for it in baselinedChanges])))

def removeLinesStartingWith(listOfStrings, listOfStringsToMatchAgainst):
    return [line for line in listOfStrings if not any(line.startswith(it) for it in listOfStringsToMatchAgainst)]

# removeLinesWithChangedSuffixes(["foo"], ["foo-bar"], "-bar") returns [], []
def removeLinesWithChangedSuffixes(newStrings, oldStrings, newSuffix, oldSuffix=""):
    possibleIndices = [i for i, string in enumerate(newStrings) if string.endswith(newSuffix)]
    convertedMap: Dict[int, str] = {i: newStrings[i].replace(newSuffix, oldSuffix) for i in possibleIndices}
    confirmedIndicesNew = [i for i, converted in convertedMap.items() if converted in oldStrings]
    confirmedIndicesOld = [oldStrings.index(convertedMap[i]) for i in confirmedIndicesNew]
    resultNew = [string for i, string in enumerate(newStrings) if i not in confirmedIndicesNew]
    resultOld = [string for i, string in enumerate(oldStrings) if i not in confirmedIndicesOld]
    return resultNew, resultOld

# remove baselined elements from a single diff segment, starting with a location-in-file element like 223c220
def processDiffSegment(segment, fileExtension):
    if segment == "": return ""
    lines = segment.split("\n")
    lines = removeLinesStartingWith(lines, baselinedChanges)
    removed = [line[1:] for line in lines if line.startswith("<")]
    added = [line[1:] for line in lines if line.startswith(">")]
    if (fileExtension == "pom") and "agpKmp" in baselines:
        # Ignore artifactIds' new -jvm and -android suffixes in poms b/356612738
        added, removed = removeLinesWithChangedSuffixes(added, removed, "-jvm</artifactId>", "</artifactId>")
        added, removed = removeLinesWithChangedSuffixes(added, removed, "-android</artifactId>", "</artifactId>")
    keptContentLines = set(">" + it for it in added).union(set("<" + it for it in removed))
    # Do not keep any formatting lines or the header if there is no content
    if len(keptContentLines) == 0: return ""
    # return value is based on `lines` because we want to retain ordering we may have lost during processing
    # We want to keep keptContentLines, and formatting lines like "---" and the header (which don't start with <>).
    return "\n".join([line for line in lines if (line != "") and ((not line[0] in "<>") or line in keptContentLines)])

# The output of diff entails multiple files, and multiple segments per file
# This function removes baselined changes from the entire diff output
def processMegaDiff(inputString):
    perFileDiffs = inputString.split("diff -r")
    processedPerFileDiffs = []
    for i in range(1, len(perFileDiffs)):
        diffStatement, _, diffContent = perFileDiffs[i].partition("\n")
        newFilePath = diffStatement.rpartition(" ")[2]
        fileExtension = newFilePath.rpartition(".")[2]
        for multilineBaselinedElement in unskippableBaselinedChanges:
            diffContent = multilineBaselinedElement.sub("", diffContent)
        diffSegments = re.split(r'(^[0-9]+[0-9acd,]*\n)', diffContent, flags=re.MULTILINE)
        result = []
        # every other segment is a segment header like 99,112d87; 0th is ""
        for j in range(1, len(diffSegments)-1, 2):
            # a complete segment is a location-in-file header and everything until the next header. E.g.
            # 83c70
            # <       <artifactId>kotlinx-coroutines-core-jvm</artifactId>
            # ---
            # >       <artifactId>kotlinx-coroutines-core</artifactId>
            segment = diffSegments[j] + diffSegments[j+1]
            processedSegment = processDiffSegment(segment, fileExtension)
            if processedSegment != "": result.append(processedSegment)
        if len(result) != 0: processedPerFileDiffs += [newFilePath + "\n" + "\n".join(result)]
    return "\ndiff ".join(processedPerFileDiffs)

# We unzip multiple times in this order because e.g. zips can contain apks.
# Find all zip files with a diff, e.g. the tip-of-tree-repository file, and maybe the docs zip
logger.info("UNZIPPING ZIP FILES");
findFilesNotMatchingWithDiffAndUnzip(r"**\.[^z][a-z]*")
# Find all aar and apk files with a diff. The proper regex would be `.*\..*[^akpr]+.*`, but it
# doesn"t work in difftools exclude's very limited regex syntax.
logger.info("UNZIPPING AAR/APK FILES");
findFilesNotMatchingWithDiffAndUnzip(r"**\.zip", r"**\.jar", r"**\.klib")
# Find all jars and klibs and unzip them (comes after because they could be inside aars/apks).
logger.info("UNZIPPING JAR/KLIB FILES");
findFilesNotMatchingWithDiffAndUnzip(r"**\.zip", r"**\.aar", r"**\.apk")

# now find all diffs in classes.jars
# TODO(375636734) Disabled because this tracks internal methods' diffs
# classesJarsWithDiffs = popenAndReturn(["find", "../../out-old/dist/", "-name", "classes.jar"])
# logger.info("classes.jar s: " + str(classesJarsWithDiffs))
# compareWithDiffuse(classesJarsWithDiffs)

# Now find all diffs in non-zipped files
finalExcludes = excludedHashes + excludedFiles + excludedZips + baselinedChangesArgs
finalDiff = "\n".join(diff(finalExcludes))
finalDiff = processMegaDiff(finalDiff)
print(finalDiff)
