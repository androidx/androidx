#!/usr/bin/python
#
#  Copyright (C) 2018 The Android Open Source Project
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


import datetime, filecmp, math, multiprocessing, os, shutil, subprocess, stat, sys
from collections import OrderedDict

def usage():
  print("""Usage: diff-filterer.py [--assume-no-side-effects] [--assume-input-states-are-correct] [--try-fail] [--work-path <workpath>] [--num-jobs <count>] [--debug] <passingPath> <failingPath> <shellCommand>

diff-filterer.py attempts to transform (a copy of) the contents of <passingPath> into the contents of <failingPath> subject to the constraint that when <shellCommand> is run in that directory, it returns 0

OPTIONS
  --assume-no-side-effects
    Assume that the given shell command does not make any (relevant) changes to the given directory, and therefore don't wipe and repopulate the directory before each invocation of the command
  --assume-input-states-are-correct
    Assume that <shellCommand> passes in <passingPath> and fails in <failingPath> rather than re-verifying this
  --try-fail
    Invert the success/fail status of <shellCommand> and swap <passingPath> and <failingPath>
    That is, instead of trying to transform <passingPath> into <failingPath>, try to transform <failingPath> into <passingPath>
  --work-path <filepath>
    File path to use as the work directory for testing the shell command
    This file path will be overwritten and modified as needed for testing purposes, and will also be the working directory of the shell command when it is run
  --num-jobs <count>
    The maximum number of concurrent executions of <shellCommand> to spawn at once
  --debug
    Enable some debug checks in diff-filterer.py
""")
  sys.exit(1)

debug = False

# Miscellaneous file utilities
class FileIo(object):
  def __init__(self):
    return

  def ensureDirExists(self, filePath):
    if not os.path.isdir(filePath):
      if os.path.isfile(filePath) or os.path.islink(filePath):
        os.remove(filePath)
      os.makedirs(filePath)

  def copyFile(self, fromPath, toPath):
    self.ensureDirExists(os.path.dirname(toPath))
    self.removePath(toPath)
    if os.path.islink(fromPath):
      linkText = os.readlink(fromPath)
      os.symlink(linkText, toPath)
    else:
      shutil.copy2(fromPath, toPath)

  def writeFile(self, path, text):
    f = open(path, "w+")
    f.write(text)
    f.close()

  def writeScript(self, path, text):
    self.writeFile(path, text)
    os.chmod(path, 0755)

  def removePath(self, filePath):
    if len(os.path.split(filePath)) < 2:
      raise Exception("Will not remove path at " + filePath + "; is too close to the root of the filesystem")
    if os.path.islink(filePath):
      os.remove(filePath)
    elif os.path.isdir(filePath):
      shutil.rmtree(filePath)
    elif os.path.isfile(filePath):
      os.remove(filePath)

  def join(self, path1, path2):
    return os.path.normpath(os.path.join(path1, path2))

  # tells whether <parent> either contains <child> or is <child>
  def contains(self, parent, child):
    if parent == child:
      return True
    return child.startswith(parent + "/")

  # returns the common prefix of two paths. For example, commonPrefixOf2("a/b/c", "a/b/cat") returns "a/b"
  def commonPrefixOf2(self, path1, path2):
    prefix = path2
    while True:
      if self.contains(prefix, path1):
        return prefix
      parent = os.path.dirname(prefix)
      if parent == prefix:
        return None
      prefix = parent

  # returns the common prefix of multiple paths
  def commonPrefix(self, paths):
    if len(paths) < 1:
      return None
    result = paths[0]
    for path in paths:
      prev = result
      result = self.commonPrefixOf2(result, path)
      if result is None:
        return result
    return result

fileIo = FileIo()

# Runs a shell command
class ShellScript(object):
  def __init__(self, commandText, cwd):
    self.commandText = commandText
    self.cwd = cwd

  def process(self):
    cwd = self.cwd
    print("Running '" + self.commandText + "' in " + cwd)
    try:
      subprocess.check_call(["bash", "-c", "cd " + cwd + " && " + self.commandText])
      return 0
    except subprocess.CalledProcessError as e:
      return e.returncode

# Base class that can hold the state of a file
class FileContent(object):
  def apply(self, filePath):
    pass

  def equals(self, other):
    pass

# A FileContent that refers to the content of a specific file
class FileBacked_FileContent(FileContent):
  def __init__(self, referencePath):
    super(FileBacked_FileContent, self).__init__()
    self.referencePath = referencePath
    self.isLink = os.path.islink(self.referencePath)

  def apply(self, filePath):
    fileIo.copyFile(self.referencePath, filePath)

  def equals(self, other):
    if not isinstance(other, FileBacked_FileContent):
      return False
    if self.referencePath == other.referencePath:
      return True
    if self.isLink and other.isLink:
      return os.readlink(self.referencePath) == os.readlink(other.referencePath)
    if self.isLink != other.isLink:
      return False # symlink not equal to non-symlink
    return filecmp.cmp(self.referencePath, other.referencePath)

  def __str__(self):
    return self.referencePath

# A FileContent describing the nonexistence of a file
class MissingFile_FileContent(FileContent):
  def __init__(self):
    super(MissingFile_FileContent, self).__init__()

  def apply(self, filePath):
    fileIo.removePath(filePath)

  def equals(self, other):
    return isinstance(other, MissingFile_FileContent)

  def __str__(self):
    return "Empty"

# A FileContent describing a directory
class Directory_FileContent(FileContent):
  def __init__(self):
    super(Directory_FileContent, self).__init__()

  def apply(self, filePath):
    fileIo.ensureDirExists(filePath)

  def equals(self, other):
    return isinstance(other, Directory_FileContent)

  def __str__(self):
    return "[empty dir]"

# A collection of many FileContent objects
class FilesState(object):
  def __init__(self):
    self.fileStates = OrderedDict()

  def apply(self, filePath):
    for relPath, state in self.fileStates.iteritems():
      state.apply(fileIo.join(filePath, relPath))

  def add(self, filePath, fileContent):
    self.fileStates[filePath] = fileContent

  def addAllFrom(self, other):
    for filePath in other.fileStates:
        self.add(filePath, other.fileStates[filePath])

  def getContent(self, filePath):
    if filePath in self.fileStates:
      return self.fileStates[filePath]
    return None

  def hasContentAt(self, filePath, content):
    ourContent = self.getContent(filePath)
    if ourContent is None:
      return (content is None)
    return ourContent.equals(content)

  # returns a FilesState resembling <self> but without the keys for which other[key] == self[key]
  def withoutDuplicatesFrom(self, other):
    result = FilesState()
    for filePath, fileState in self.fileStates.iteritems():
      otherContent = other.getContent(filePath)
      if not fileState.equals(otherContent):
        result.add(filePath, fileState)
    return result

  # returns self[fromIndex:toIndex]
  def slice(self, fromIndex, toIndex):
    result = FilesState()
    for filePath in self.fileStates.keys()[fromIndex:toIndex]:
      result.fileStates[filePath] = self.fileStates[filePath]
    return result

  def restrictedToKeysIn(self, other):
    result = FilesState()
    for filePath, fileState in self.fileStates.iteritems():
      if filePath in other.fileStates:
        result.add(filePath, fileState)
    return result

  # returns a FilesState having the same keys as this FilesState, but with values taken from <other> when it has them, and <self> otherwise
  def withConflictsFrom(self, other, listEmptyDirs = False):
    result = FilesState()
    for filePath, fileContent in self.fileStates.iteritems():
      if filePath in other.fileStates:
        result.add(filePath, other.fileStates[filePath])
      else:
        result.add(filePath, fileContent)
    if listEmptyDirs:
      oldImpliedDirs = self.listImpliedDirs()
      newImpliedDirs = result.listImpliedDirs()
      for impliedDir in oldImpliedDirs:
        if impliedDir not in newImpliedDirs and impliedDir not in result.fileStates:
          result.add(impliedDir, MissingFile_FileContent())
    return result

  def checkSameKeys(self, other):
    a = self.checkContainsKeys(other)
    b = other.checkContainsKeys(self)
    if a and b:
      return True
    if not a:
      print("a does not contain all of the keys from b")
    if not b:
      print("b does not contain all of the keys from a")
    return False

  def checkContainsKeys(self, other):
    contains = True
    for f in other.fileStates.keys():
      if f not in self.fileStates:
        print("Found in " + other.summarize() + " but not in " + self.summarize() + ": " + f)
        contains = False
    return contains

  # returns a set of paths to all of the dirs in <self> that are implied by any files in <self>
  def listImpliedDirs(self):
    dirs = set()
    empty = MissingFile_FileContent()
    keys = [key for (key, value) in self.fileStates.iteritems() if not empty.equals(value)]
    i = 0
    while i < len(keys):
      path = keys[i]
      parent, child = os.path.split(path)
      if parent == "":
        parent = "."
      if not parent in dirs:
        dirs.add(parent)
        keys.append(parent)
      i += 1
    return dirs

  # returns a FilesState having all of the entries from <self>, plus empty entries for any keys in <other> not in <self>
  def expandedWithEmptyEntriesFor(self, other):
    impliedDirs = self.listImpliedDirs()
    # now look for entries in <other> not present in <self>
    result = self.clone()
    for filePath in other.fileStates:
      if filePath not in result.fileStates and filePath not in impliedDirs:
        result.fileStates[filePath] = MissingFile_FileContent()
    return result

  def clone(self):
    result = FilesState()
    for path, content in self.fileStates.iteritems():
      result.add(path, content)
    return result

  def withoutEmptyEntries(self):
    result = FilesState()
    empty = MissingFile_FileContent()
    for path, state in self.fileStates.iteritems():
      if not empty.equals(state):
        result.add(path, state)
    return result

  def getCommonDir(self):
    result = fileIo.commonPrefix(self.fileStates.keys())
    return result

  # Returns a list of FilesState objects each containing a different subdirectory of <self>
  # If groupDirectFilesTogether == True, then all files directly under self.getCommonDir() will be assigned to the same group
  def groupByDirs(self, groupDirectFilesTogether = False):
    if len(self.fileStates) <= 1:
      if len(self.fileStates) == 1:
        return [self]
      return []

    commonDir = self.getCommonDir()
    if commonDir is None:
      prefixLength = 0
    else:
      prefixLength = len(commonDir) + 1 # skip the following '/'
    groupsByDir = {}

    for filePath, fileContent in self.fileStates.iteritems():
      subPath = filePath[prefixLength:]
      slashIndex = subPath.find("/")
      if slashIndex < 0:
        if groupDirectFilesTogether:
          firstDir = ""
        else:
          firstDir = subPath
      else:
        firstDir = subPath[:slashIndex]
      if not firstDir in groupsByDir:
        groupsByDir[firstDir] = FilesState()
      groupsByDir[firstDir].add(filePath, fileContent)
    return [group for group in groupsByDir.values()]

  # splits into multiple, smaller, FilesState objects
  def splitOnce(self, maxNumChildren = 2):
    if self.size() <= 1:
      return [self]
    children = self.groupByDirs(True)
    if len(children) == 1:
      children = children[0].groupByDirs(False)
    if len(children) > maxNumChildren:
      # If there are lots of child directories, we still want to test a smaller number of larger groups before testing smaller groups
      # So we arbitrarily recombine child directories to make a smaller number of children
      minIndex = 0
      mergedChildren = []
      for i in range(maxNumChildren):
        maxIndex = len(children) * (i + 1) / maxNumChildren
        merge = FilesState()
        for child in children[minIndex:maxIndex]:
          merge.addAllFrom(child)
        mergedChildren.append(merge)
        minIndex = maxIndex
      children = mergedChildren
    return children

  def summarize(self):
    numFiles = self.size()
    commonDir = self.getCommonDir()
    if numFiles <= 4:
      return str(self)
    if commonDir is not None:
      return str(numFiles) + " files under " + str(commonDir)
    return str(numFiles) + " files"

  def size(self):
    return len(self.fileStates)

  def __str__(self):
    if len(self.fileStates) == 0:
      return "[empty fileState]"
    entries = []
    for filePath, state in self.fileStates.iteritems():
      entries.append(filePath + " -> " + str(state))
    if len(self.fileStates) > 1:
      prefix = str(len(entries)) + " entries:\n"
    else:
      prefix = "1 entry: "
    return prefix + "\n".join(entries)

# Creates a FilesState matching the state of a directory on disk
def filesStateFromTree(rootPath):
  rootPath = os.path.abspath(rootPath)

  paths = []
  states = {}

  for root, dirPaths, filePaths in os.walk(rootPath, topdown=True):
    if len(filePaths) == 0 and len(dirPaths) == 0:
      relPath = os.path.relpath(root, rootPath)
      paths.append(relPath)
      states[relPath] = Directory_FileContent()
    # include every file and every symlink (even if the symlink points to a dir)
    leaves = filePaths
    for dirPath in dirPaths:
      fullPath = os.path.join(root, dirPath)
      if os.path.islink(fullPath):
        leaves.append(dirPath)
    for filePath in leaves:
      fullPath = fileIo.join(root, filePath)
      relPath = os.path.relpath(fullPath, rootPath)
      paths.append(relPath)
      states[relPath] = FileBacked_FileContent(fullPath)

  paths = sorted(paths)
  state = FilesState()
  for path in paths:
    state.add(path, states[path])
  return state

class FilesState_HyperBoxNode(object):
  def __init__(self, dimensions):
    self.dimensions = dimensions
    self.children = []
    if len(dimensions) > 1:
      nextDimensions = dimensions[1:]
      for i in range(dimensions[0]):
        self.children.append(FilesState_HyperBoxNode(nextDimensions))
    else:
      for i in range(dimensions[0]):
         self.children.append(FilesState_LeafBox())

  def getFiles(self, coordinates):
    child = self.children[coordinates[0]]
    return child.getFiles(coordinates[1:])

  def setFiles(self, coordinates, files):
    self.children[coordinates[0]].setFiles(coordinates[1:], files)

  def clearFiles(self, coordinates):
    self.children[coordinates[0]].clearFiles(coordinates[1:])

  def removeSlice(self, dimension, index):
    if dimension == 0:
      del self.children[index]
    else:
      for child in self.children:
        child.removeSlice(dimension - 1, index)

  def getSlice(self, dimension, index):
    result = FilesState()
    for i in range(len(self.children)):
      if dimension != 0 or i == index:
        child = self.children[i]
        childResult = child.getSlice(dimension - 1, index)
        result = result.expandedWithEmptyEntriesFor(childResult).withConflictsFrom(childResult)
    return result


class FilesState_LeafBox(object):
  def __init__(self):
    self.files = FilesState()

  def getFiles(self, coordinates):
    return self.files

  def setFiles(self, coordinates, files):
    self.files = files

  def clearFiles(self, coordinates):
    self.files = FilesState()

  def removeSlice(self, dimensions, index):
    return

  def getSlice(self, dimension, index):
    return self.getFiles([])

class FilesState_HyperBox(object):
  def __init__(self, dimensions):
    self.dimensions = dimensions
    self.durations = []
    self.numFiles = 0
    if len(dimensions) < 1:
      raise Exception("dimensions must be nonempty: " + str(dimensions))
    for length in dimensions:
      if length < 1:
        raise Exception("Illegal dimension " + str(length) + " in " + str(dimensions))
      self.durations.append([None] * length)
    self.root = FilesState_HyperBoxNode(dimensions)

  def getNumDimensions(self):
    return len(self.dimensions)

  def getSize(self, dimension):
    return self.dimensions[dimension]

  def getDimensions(self):
    return self.dimensions

  def getSliceDuration(self, dimension, index):
    return self.durations[dimension][index]

  def setSliceDuration(self, dimension, index, value):
    durations = self.durations[dimension]
    if index >= len(durations):
      raise Exception("Index " + str(index) + " too large for durations " + str(durations) + " of length " + str(len(durations)) + ". All durations: " + str(self.durations))
    durations[index] = value

  def removeSlice(self, dimension, index):
    durations = self.durations[dimension]
    del durations[index]
    self.root.removeSlice(dimension, index)
    self.dimensions[dimension] -= 1

  def getFastestIndex(self, dimension):
    durations = self.durations[dimension]
    fastestValue = None
    fastestIndex = None
    for i in range(len(durations)):
      value = durations[i]
      if value is not None:
        if fastestValue is None or value < fastestValue:
          fastestValue = value
          fastestIndex = i
    return fastestIndex

  def getFastestIndices(self):
    return [self.getFastestIndex(dimension) for dimension in range(self.getNumDimensions())]

  def getFiles(self, coordinates):
    return self.root.getFiles(coordinates)

  def setFiles(self, dimensions, files):
    self.root.setFiles(dimensions, files)
    self.numFiles = None

  def clearFiles(self, dimensions):
    self.setFiles(dimensions, FilesState())

  def getNumFiles(self):
    if self.numFiles is None:
      numFiles = 0
      for child in self.getChildren():
        numFiles += child.size()
      self.numFiles = numFiles
    return self.numFiles

  def getSlice(self, dimension, index):
    return self.root.getSlice(dimension, index)

  def incrementCoordinates(self, coordinates):
    coordinates = coordinates[:]
    for i in range(len(coordinates)):
      coordinates[i] += 1
      if coordinates[i] >= self.dimensions[i]:
        coordinates[i] = 0
      else:
        return coordinates
    return None

  def getChildren(self):
    if len(self.dimensions) < 1 or self.dimensions[0] < 1:
      return []
    coordinates = [0] * len(self.dimensions)
    children = []
    while coordinates is not None:
      child = self.getFiles(coordinates)
      if child is not None and child.size() > 0:
        children.append(child)
      coordinates = self.incrementCoordinates(coordinates)
    return children

  def getNumChildren(self):
    return len(self.getChildren())

  def getAllFiles(self):
    files = FilesState()
    for child in self.getChildren():
      files = files.expandedWithEmptyEntriesFor(child).withConflictsFrom(child)
    return files

def boxFromList(fileStates):
  numStates = len(fileStates)
  if numStates == 1:
    dimensions = [1]
  else:
    dimensions = []
    while numStates > 1:
      if numStates == 4:
        # if there are 4 states we want to make it a 2x2
        nextDimension = 2
      else:
        nextDimension = min(3, numStates)
      dimensions.append(nextDimension)
      numStates = int(math.ceil(float(numStates) / float(nextDimension)))
  tree = FilesState_HyperBox(dimensions)
  coordinates = [0] * len(dimensions)
  for state in fileStates:
    tree.setFiles(coordinates, state)
    coordinates = tree.incrementCoordinates(coordinates)
  return tree

# runs a Job in this process
def runJobInSameProcess(shellCommand, workPath, full_resetTo_state, assumeNoSideEffects, candidateBox, twoWayPipe):
  job = Job(shellCommand, workPath, full_resetTo_state, assumeNoSideEffects, candidateBox, twoWayPipe)
  job.runAndReport()

# starts a Job in a new process
def runJobInOtherProcess(shellCommand, workPath, full_resetTo_state, assumeNoSideEffects, candidateBox, queue, identifier):
  parentWriter, childReader = multiprocessing.Pipe()
  childInfo = TwoWayPipe(childReader, queue, identifier)
  process = multiprocessing.Process(target=runJobInSameProcess, args=(shellCommand, workPath, full_resetTo_state, assumeNoSideEffects, candidateBox, childInfo,))
  process.start()
  return parentWriter

class TwoWayPipe(object):
  def __init__(self, readerConnection, writerQueue, identifier):
    self.readerConnection = readerConnection
    self.writerQueue = writerQueue
    self.identifier = identifier

# Stores a subprocess for running tests and some information about which tests to run
class Job(object):
  def __init__(self, shellCommand, workPath, full_resetTo_state, assumeNoSideEffects, candidateBox, twoWayPipe):
    self.shellCommand = shellCommand
    self.workPath = workPath
    self.full_resetTo_state = full_resetTo_state
    self.assumeNoSideEffects = assumeNoSideEffects
    # all of the files that we've found so far that we can add
    self.acceptedState = FilesState()
    # HyperBox of all of the possible changes we're considering
    self.candidateBox = candidateBox
    # FilesState telling the current set of files that we're testing modifying
    self.currentTestState = None
    self.busy = False
    self.complete = False
    self.pipe = twoWayPipe

  def runAndReport(self):
    succeeded = False
    try:
      succeeded = self.run()
    finally:
      print("^" * 100)
      self.pipe.writerQueue.put((self.pipe.identifier, succeeded))

  def run(self):
    print("#" * 100)
    print("Checking " + self.candidateBox.summarize() + " (job " + str(self.pipe.identifier) + ") in " + str(self.workPath) + " at " + str(datetime.datetime.now()))
    # set file state
    if not self.assumeNoSideEffects:
      fileIo.removePath(self.workPath)
    # If the user told us that we don't have to worry about the possibility of the shell command generating files whose state matters,
    # then we don't reset any unrecognized files (they might even be caches that improve speed)
    testState = self.candidateBox
    self.full_resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState, True).apply(self.workPath)

    # run test
    start = datetime.datetime.now()
    returnCode = ShellScript(self.shellCommand, self.workPath).process()
    now = datetime.datetime.now()
    duration = (now - start).total_seconds()

    # report results
    if returnCode == 0:
      print("Passed: " + self.candidateBox.summarize() + " (job " + str(self.pipe.identifier) + ") at " + str(datetime.datetime.now()) + " in " + str(duration))
      return True
    else:
      print("Failed: " + self.candidateBox.summarize() + " (job " + str(self.pipe.identifier) + ") at " + str(datetime.datetime.now()) + " in " + str(duration))
      return False


# Runner class that determines which diffs between two directories cause the given shell command to fail
class DiffRunner(object):
  def __init__(self, failingPath, passingPath, shellCommand, workPath, assumeNoSideEffects, assumeInputStatesAreCorrect, tryFail, maxNumJobsAtOnce):
    # some simple params
    self.workPath = os.path.abspath(workPath)
    self.bestState_path = fileIo.join(self.workPath, "bestResults")
    self.sampleFailure_path = fileIo.join(self.workPath, "sampleFailure")
    self.testScript_path = fileIo.join(self.workPath, "test.sh")
    fileIo.ensureDirExists(os.path.dirname(self.testScript_path))
    fileIo.writeScript(self.testScript_path, shellCommand)
    self.originalPassingPath = os.path.abspath(passingPath)
    self.originalFailingPath = os.path.abspath(failingPath)
    self.assumeNoSideEffects = assumeNoSideEffects
    self.assumeInputStatesAreCorrect = assumeInputStatesAreCorrect
    self.tryFail = tryFail

    # lists of all the files under the two dirs
    print("Finding files in " + passingPath)
    self.originalPassingState = filesStateFromTree(passingPath)
    print("Found " + self.originalPassingState.summarize() + " in " + str(passingPath))
    print("")
    print("Finding files in " + failingPath)
    self.originalFailingState = filesStateFromTree(failingPath)
    print("Found " + self.originalFailingState.summarize() + " in " + str(failingPath))
    print("")
    print("Identifying duplicates")
    # list of the files in the state to reset to after each test
    self.full_resetTo_state = self.originalPassingState
    # minimal description of only the files that are supposed to need to be reset after each test
    self.resetTo_state = self.originalPassingState.expandedWithEmptyEntriesFor(self.originalFailingState).withoutDuplicatesFrom(self.originalFailingState)
    self.targetState = self.originalFailingState.expandedWithEmptyEntriesFor(self.originalPassingState).withoutDuplicatesFrom(self.originalPassingState)
    self.originalNumDifferences = self.resetTo_state.size()
    print("Processing " + str(self.originalNumDifferences) + " file differences")
    self.maxNumJobsAtOnce = maxNumJobsAtOnce

  def cleanupTempDirs(self):
    print("Clearing work directories")
    for jobId in range(self.maxNumJobsAtOnce):
      path = self.getWorkPath(jobId)
      try:
        fileIo.removePath(path)
      except Exception as e:
        print("Failed to clean up " + str(path))

  def runnerTest(self, testState, timeout = None):
    workPath = self.getWorkPath(0)
    # reset state if needed
    fileIo.removePath(workPath)
    testState.apply(workPath)
    start = datetime.datetime.now()
    returnCode = ShellScript(self.testScript_path, workPath).process()
    duration = (datetime.datetime.now() - start).total_seconds()
    print("shell command completed in " + str(duration))
    if returnCode == 0:
      return (True, duration)
    else:
      if self.assumeNoSideEffects:
        # unapply changes so that the contents of workPath should match self.resetTo_state
        testState.withConflictsFrom(self.resetTo_state).apply(workPath)
      return (False, duration)

  def onSuccess(self, testState):
    #print("Runner received success of testState: " + str(testState.summarize()))
    if debug:
      if not filesStateFromTree(self.bestState_path).checkSameKeys(self.full_resetTo_state.withoutEmptyEntries()):
        print("Contents of " + self.bestState_path + " don't match self.full_resetTo_state at beginning of onSuccess")
        sys.exit(1)
    self.targetState = self.targetState.withoutDuplicatesFrom(testState)
    self.resetTo_state = self.resetTo_state.withConflictsFrom(testState).withoutDuplicatesFrom(testState)
    delta = self.full_resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState, True)
    delta.apply(self.bestState_path)
    self.full_resetTo_state = self.full_resetTo_state.expandedWithEmptyEntriesFor(delta).withConflictsFrom(delta)
    if debug:
      if not filesStateFromTree(self.bestState_path).checkSameKeys(self.full_resetTo_state.withoutEmptyEntries()):
        print("Contents of " + self.bestState_path + " don't match self.full_resetTo_state at end of onSuccess")
        print("Applied this delta: " + str(delta))
        sys.exit(1)

  def getWorkPath(self, jobId):
    return os.path.join(self.workPath, "job-" + str(jobId))

  def run(self):
    start = datetime.datetime.now()
    numIterationsCompleted = 0
    self.cleanupTempDirs()
    workPath = self.getWorkPath(0)
    if not self.assumeInputStatesAreCorrect:
      print("Testing that the given failing state actually fails")
      fileIo.removePath(workPath)
      if self.runnerTest(self.originalFailingState)[0]:
        print("\nGiven failing state at " + self.originalFailingPath + " does not actually fail!")
        return False

      print("Testing that the given passing state actually passes")
      if not self.runnerTest(self.full_resetTo_state)[0]:
        print("\nGiven passing state at " + self.originalPassingPath + " does not actually pass!")
        return False

    print("Saving best state found so far")
    fileIo.removePath(self.bestState_path)
    self.full_resetTo_state.apply(self.bestState_path)

    print("Starting")
    print("You can inspect " + self.bestState_path + " while this process runs, to observe the best state discovered so far")
    print("You can inspect " + self.sampleFailure_path + " while this process runs, to observe a state for which the test failed. If you delete this filepath, then it will be updated later to contain a new failing state")
    print("")
    # Now we search over groups of inodes (files or dirs) in the tree
    # Every time we encounter a group of inodes, we try replacing them and seeing if the replacement passes our test
    # If it does, we accept those changes and continue searching
    # If it doesn't, we split that group into smaller groups and continue
    numFailuresDuringCurrentWindowSize = 0
    jobId = 0
    workingDir = self.getWorkPath(jobId)
    queue = multiprocessing.Queue()
    activeJobs = {}
    boxesById = {}
    pendingBoxes = self.targetState.splitOnce(max(self.maxNumJobsAtOnce, 2))
    numConsecutiveFailures = 0
    numFailuresSinceLastSplitOrSuccess = 0
    invalidatedIds = set()
    probablyAcceptableStates = []
    numCompletedTests = 2 # Already tested initial passing state and initial failing state
    # continue until all files fail and no jobs are running
    while numFailuresSinceLastSplitOrSuccess < self.resetTo_state.size() or len(activeJobs) > 0:
      if self.maxNumJobsAtOnce > self.resetTo_state.size():
        self.maxNumJobsAtOnce = self.resetTo_state.size()

      # display status message
      now = datetime.datetime.now()
      elapsedDuration = now - start
      minNumTestsRemaining = sum([math.log(box.size(), 2) + 1 for box in pendingBoxes + boxesById.values()]) - numFailuresSinceLastSplitOrSuccess
      estimatedNumTestsRemaining = max(minNumTestsRemaining, 1)
      if numConsecutiveFailures >= 4 and numFailuresSinceLastSplitOrSuccess < 1:
        # If we are splitting often and failing often, then we probably haven't yet
        # shrunken the individual boxes down to each contain only one failing file
        # During this phase, on average we've completed half of the work
        # So, we estimate that the total work remaining is double what we've completed
        estimatedNumTestsRemaining *= 2
      estimatedRemainingDuration = datetime.timedelta(seconds = elapsedDuration.total_seconds() * float(estimatedNumTestsRemaining) / float(numCompletedTests))
      message = "Elapsed duration: " + str(elapsedDuration) + ". Waiting for " + str(len(activeJobs)) + " active subprocesses (" + str(len(pendingBoxes) + len(activeJobs)) + " total available jobs). " + str(self.resetTo_state.size()) + " changes left to test, should take about " + str(estimatedNumTestsRemaining) + " tests, about " + str(estimatedRemainingDuration)
      print(message)

      if len(activeJobs) > 0:
        # wait for a response from a worker
        response = queue.get()
        identifier = response[0]
        box = boxesById[identifier]
        didAcceptState = response[1]
        numCompletedTests += 1
        if didAcceptState:
          numConsecutiveFailures = 0
          numFailuresSinceLastSplitOrSuccess = 0
          acceptedState = box #.getAllFiles()
          #print("Succeeded : " + acceptedState.summarize() + " (job " + str(identifier) + ") at " + str(datetime.datetime.now()))
          maxRunningSize = max([state.size() for state in boxesById.values()])
          maxRelevantSize = maxRunningSize / self.maxNumJobsAtOnce
          if acceptedState.size() < maxRelevantSize:
            print("Queuing a retest of response of size " + str(acceptedState.size()) + " from job " + str(identifier) + " because a much larger job of size " + str(maxRunningSize) + " is still running")
            probablyAcceptableStates.append(acceptedState)
          else:
            if identifier in invalidatedIds:
              # queue a retesting of this box
              print("Queuing a re-test of response from job " + str(identifier) + " due to previous cancellation. Successful state: " + str(acceptedState.summarize()))
              probablyAcceptableStates.append(acceptedState)
            else:
              # A worker discovered a nonempty change that can be made successfully; update our best accepted state
              self.onSuccess(acceptedState)
              if debug:
                # The files in self.bestState_path should exactly match what's in workPath[identifier], except for files that didn't originally exist
                if not filesStateFromTree(self.bestState_path).checkSameKeys(filesStateFromTree(self.getWorkPath(identifier)).restrictedToKeysIn(self.originalPassingState.expandedWithEmptyEntriesFor(self.originalFailingState))):
                  print("Successful state from work path " + str(identifier) + " wasn't correctly copied to bestState. Could the test command be deleting files that previously existed?")
                  sys.exit(1)
              # record that the results from any previously started process are no longer guaranteed to be valid
              for i in activeJobs.keys()[:]:
                connection = activeJobs[i]
                if i != identifier:
                  invalidatedIds.add(i)
        else:
          if not os.path.isdir(self.sampleFailure_path):
            # save sample failure path where user can see it
            print("Saving sample failed state to " + str(self.sampleFailure_path))
            fileIo.ensureDirExists(self.sampleFailure_path)
            self.full_resetTo_state.expandedWithEmptyEntriesFor(box).withConflictsFrom(box, True).apply(self.sampleFailure_path)
          #print("Failed : " + box.summarize() + " (job " + str(identifier) + ") at " + str(datetime.datetime.now()))
          # count failures
          numConsecutiveFailures += 1
          numFailuresSinceLastSplitOrSuccess += 1
          # find any children that failed and queue a re-test of those children
          updatedChild = box.withoutDuplicatesFrom(box.withConflictsFrom(self.resetTo_state))
          if updatedChild.size() > 0:
            if numConsecutiveFailures >= 4:
              # Suppose we are trying to identify n single-file changes that cause failures
              # Suppose we have tried c changes of size s, each one of which failed
              # We conclude that n >= c
              # A mostly unbiased estimate of c as a function of n is that c = n / 2
              # Similarly, a mostly unbiased estimate of n is that n = c * 2
              # We want to choose a new number of changes to test, c2, such that running c2 tests results in efficiently identifying the relevant n changes
              # Let's set c2 = 2 * n = 2 * 2 * c
              splitFactor = 4
            else:
              # After we reach a sufficiently small change size such that some changes start passing,
              # Then we assume that we've probably narrowed down to each individual failing change,
              # And we can increase block sizes more slowly
              splitFactor = 2
            split = updatedChild.splitOnce(splitFactor)
            if len(split) > 1:
              numFailuresSinceLastSplitOrSuccess = 0
            pendingBoxes += split
        # clear invalidation status
        if identifier in invalidatedIds:
          invalidatedIds.remove(identifier)
        del activeJobs[identifier]
        del boxesById[identifier]

      # if probablyAcceptableStates has become large enough, then retest its contents too
      if len(probablyAcceptableStates) > 0 and (len(probablyAcceptableStates) >= self.maxNumJobsAtOnce + 1 or numConsecutiveFailures >= self.maxNumJobsAtOnce or len(activeJobs) < 1):
        probablyAcceptableState = FilesState()
        for state in probablyAcceptableStates:
          probablyAcceptableState = probablyAcceptableState.expandedWithEmptyEntriesFor(state).withConflictsFrom(state)
        probablyAcceptableState = probablyAcceptableState.withoutDuplicatesFrom(self.resetTo_state)
        if probablyAcceptableState.size() > 0:
          print("Retesting " + str(len(probablyAcceptableStates)) + " previous likely successful states as a single test: " + probablyAcceptableState.summarize())
          pendingBoxes = [probablyAcceptableState] + pendingBoxes
        probablyAcceptableStates = []
      if len(pendingBoxes) < 1 and len(activeJobs) < 1:
        print("Error: no changes remain left to test. It was expected that applying all changes would fail")
        break

      # if we haven't checked everything yet, then try to queue more jobs
      if numFailuresSinceLastSplitOrSuccess < self.resetTo_state.size():
        pendingBoxes.sort(reverse=True, key=FilesState.size)
        while len(activeJobs) < self.maxNumJobsAtOnce and len(activeJobs) < self.resetTo_state.size() and len(pendingBoxes) > 0:
          # find next pending job
          box = pendingBoxes[0]
          # find next unused job id
          jobId = 0
          while jobId in activeJobs:
            jobId += 1
          # start job
          #print("Starting process " + str(jobId) + " testing " + str(box.summarize()) + " at " + str(datetime.datetime.now()))
          workingDir = self.getWorkPath(jobId)
          activeJobs[jobId] = runJobInOtherProcess(self.testScript_path, workingDir, self.full_resetTo_state, self.assumeNoSideEffects, box, queue, jobId)
          boxesById[jobId] = box
          pendingBoxes = pendingBoxes[1:]

    print("double-checking results")
    wasSuccessful = True
    if not self.runnerTest(filesStateFromTree(self.bestState_path))[0]:
      message = "Error: expected best state at " + self.bestState_path + " did not pass the second time. Could the test be non-deterministic?"
      if self.assumeNoSideEffects:
        message += " (it may help to remove the --assume-no-side-effects flag)"
      if self.assumeInputStatesAreCorrect:
        message += " (it may help to remove the --assume-input-states-are-correct flag)"
      print(message)
      wasSuccessful = False

    self.cleanupTempDirs()

    print("")
    if self.targetState.size() < 1000:
      filesDescription = str(self.targetState)
    else:
      filesDescription = str(self.targetState.summarize())
    print("Done trying to transform the contents of passing path:\n " + self.originalPassingPath + "\ninto the contents of failing path:\n " + self.originalFailingPath)
    print("Of " + str(self.originalNumDifferences) + " differences, could not accept: " + filesDescription)
    print("The final accepted state can be seen at " + self.bestState_path)
    return wasSuccessful

def main(args):
  assumeNoSideEffects = False
  assumeInputStatesAreCorrect = False
  tryFail = False
  workPath = "/tmp/diff-filterer"
  maxNumJobsAtOnce = 1
  while len(args) > 0:
    arg = args[0]
    if arg == "--assume-no-side-effects":
      assumeNoSideEffects = True
      args = args[1:]
      continue
    if arg == "--assume-input-states-are-correct":
      assumeInputStatesAreCorrect = True
      args = args[1:]
      continue
    if arg == "--try-fail":
      tryFail = True
      args = args[1:]
      continue
    if arg == "--work-path":
      if len(args) < 2:
        usage()
      workPath = args[1]
      args = args[2:]
      continue
    if arg == "--num-jobs":
      if len(args) < 2:
        usage()
      maxNumJobsAtOnce = int(args[1])
      args = args[2:]
      continue
    if arg == "--debug":
      global debug
      debug = True
      args = args[1:]
      continue
    if len(arg) > 0 and arg[0] == "-":
      print("Unrecognized argument: '" + arg + "'")
      usage()
    break
  if len(args) != 3:
    usage()
  passingPath = args[0]
  failingPath = args[1]
  shellCommand = args[2]
  startTime = datetime.datetime.now()
  if tryFail:
    temp = passingPath
    passingPath = failingPath
    failingPath = temp
  if not os.path.exists(passingPath):
    print("Specified passing path " + passingPath + " does not exist")
    sys.exit(1)
  if not os.path.exists(failingPath):
    print("Specified failing path " + failingPath + " does not exist")
    sys.exit(1)
  success = DiffRunner(failingPath, passingPath, shellCommand, workPath, assumeNoSideEffects, assumeInputStatesAreCorrect, tryFail, maxNumJobsAtOnce).run()
  endTime = datetime.datetime.now()
  duration = endTime - startTime
  if success:
    print("Succeeded in " + str(duration))
  else:
    print("Failed in " + str(duration))
    sys.exit(1)

main(sys.argv[1:])
