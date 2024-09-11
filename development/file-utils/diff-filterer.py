#!/usr/bin/env python3
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


import datetime, filecmp, math, multiprocessing, os, shutil, subprocess, stat, sys, time
from collections import OrderedDict

def usage():
  print("""Usage: diff-filterer.py [--assume-input-states-are-correct] [--allow-goal-passing] [--work-path <workpath>] [--num-jobs <count>] [--timeout <seconds>] [--debug] <passingPath> <goalPath> <shellCommand>

diff-filterer.py attempts to transform (a copy of) the contents of <passingPath> into the contents of <goalPath> subject to the constraint that when <shellCommand> is run in that directory, it returns 0

OPTIONS
  --assume-input-states-are-correct
    Assume that <shellCommand> passes in <passingPath> and fails in <goalPath> rather than re-verifying this
  --allow-goal-passing
    If <goalPath> passes the test, report it as the best result rather than reporting an error.
    Usually it's a mistake to pass a passing state as the goal path, because then the passing state is the best result. This usually means the inputs might be reversed or mistaken. It also means that the binary search is very short.
    In some cases this can make sense if the caller hasn't already checked whether the goal state passes.
  --work-path <filepath>
    File path to use as the work directory for testing the shell command
    This file path will be overwritten and modified as needed for testing purposes, and will also be the working directory of the shell command when it is run
  --num-jobs <count>
    The maximum number of concurrent executions of <shellCommand> to spawn at once
    Specify 'auto' to have diff-filterer.py dynamically adjust the number of jobs based on system load
  --timeout <seconds>
    Approximate maximum amount of time to run. If diff-filterer.py expects that running a test would exceed this timeout, then it will skip running the test, terminate early, and report what it did find.
    diff-filterer.py doesn't terminate any child processes that have already started, so it is still possible that diff-filterer.py might exceed this timeout by the amount of time required to run one test.
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

  def hardLink(self, oldPath, newPath):
    self.ensureDirExists(os.path.dirname(newPath))
    self.removePath(newPath)
    os.link(oldPath, newPath)

  def writeFile(self, path, text):
    f = open(path, "w+")
    f.write(text)
    f.close()

  def writeScript(self, path, text):
    self.writeFile(path, text)
    os.chmod(path, stat.S_IRWXU)

  def removePath(self, filePath):
    if len(os.path.split(filePath)) < 2:
      raise Exception("Will not remove path at " + filePath + "; is too close to the root of the filesystem")
    if os.path.islink(filePath):
      os.remove(filePath)
    elif os.path.isdir(filePath):
      shutil.rmtree(filePath)
    elif os.path.isfile(filePath):
      os.remove(filePath)

  def move(self, fromPath, toPath):
    os.rename(fromPath, toPath)

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
    result = None
    for path in paths:
      if result is None:
        # first iteration
        result = path
      else:
        prev = result
        result = self.commonPrefixOf2(result, path)
        if result is None:
          # the common prefix of two paths was nothing
          return result
    return result

  # returns the time at which <path> was last modified, without following symlinks
  def getModificationTime(self, path):
    if os.path.exists(path):
      if os.path.islink(path):
        # for a symlink, the last time the link itself was modified is the ctime (mtime for a broken link is undefined)
        return os.path.getctime(path)
      else:
        # for a file, the last time its content was modified is the mtime
        return os.path.getmtime(path)
    return None

fileIo = FileIo()

# Returns cpu usage
class CpuStats(object):

  def cpu_times_percent(self):
    # We wait to attempt to import psutil in case we don't need it and it doesn't exist on this system
    import psutil
    return psutil.cpu_times_percent(interval=None)

cpuStats = CpuStats()

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

  def equals(self, other, checkWithFileSystem=False):
    pass

# A FileContent that refers to the content of a specific file
class FileBacked_FileContent(FileContent):
  def __init__(self, referencePath):
    super(FileBacked_FileContent, self).__init__()
    self.referencePath = referencePath
    self.isLink = os.path.islink(self.referencePath)

  def apply(self, filePath):
    fileIo.copyFile(self.referencePath, filePath)

  def equals(self, other, checkWithFileSystem=False):
    if not isinstance(other, FileBacked_FileContent):
      return False
    if self.referencePath == other.referencePath:
      return True
    if not checkWithFileSystem:
      return False
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

  def equals(self, other, checkWithFileSystem=False):
    return isinstance(other, MissingFile_FileContent)

  def __str__(self):
    return "Empty"

# A FileContent describing a directory
class Directory_FileContent(FileContent):
  def __init__(self):
    super(Directory_FileContent, self).__init__()

  def apply(self, filePath):
    fileIo.ensureDirExists(filePath)

  def equals(self, other, checkWithFileSystem=False):
    return isinstance(other, Directory_FileContent)

  def __str__(self):
    return "[empty dir]"

# A collection of many FileContent objects
class FilesState(object):
  def __init__(self):
    self.fileStates = OrderedDict()

  def apply(self, filePath):
    for relPath, state in self.fileStates.items():
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

  def getKeys(self):
    return self.fileStates.keys()

  # returns a FilesState resembling <self> but without the keys for which other[key] == self[key]
  def withoutDuplicatesFrom(self, other, checkWithFileSystem=False):
    result = FilesState()
    for filePath, fileState in self.fileStates.items():
      otherContent = other.getContent(filePath)
      if not fileState.equals(otherContent, checkWithFileSystem):
        result.add(filePath, fileState)
    return result

  # returns self[fromIndex:toIndex]
  def slice(self, fromIndex, toIndex):
    result = FilesState()
    for filePath in list(self.fileStates.keys())[fromIndex:toIndex]:
      result.fileStates[filePath] = self.fileStates[filePath]
    return result

  def restrictedToKeysIn(self, other):
    result = FilesState()
    for filePath, fileState in self.fileStates.items():
      if filePath in other.fileStates:
        result.add(filePath, fileState)
    return result

  # returns a FilesState having the same keys as this FilesState, but with values taken from <other> when it has them, and <self> otherwise
  def withConflictsFrom(self, other, listEmptyDirs = False):
    result = FilesState()
    for filePath, fileContent in self.fileStates.items():
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
    keys = [key for (key, value) in self.fileStates.items() if not empty.equals(value)]
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
  def expandedWithEmptyEntriesFor(self, other, listEmptyDirs=False):
    impliedDirs = self.listImpliedDirs()
    # now look for entries in <other> not present in <self>
    result = self.clone()
    for filePath in other.fileStates:
      if filePath not in result.fileStates and filePath not in impliedDirs:
        result.fileStates[filePath] = MissingFile_FileContent()
    if listEmptyDirs:
      newImpliedDirs = other.listImpliedDirs()
      oldImpliedDirs = result.listImpliedDirs()
      for impliedDir in newImpliedDirs:
        if impliedDir not in oldImpliedDirs and impliedDir not in result.fileStates:
          result.add(impliedDir, MissingFile_FileContent())
    return result

  def clone(self):
    result = FilesState()
    for path, content in self.fileStates.items():
      result.add(path, content)
    return result

  def withoutEmptyEntries(self):
    result = FilesState()
    empty = MissingFile_FileContent()
    for path, state in self.fileStates.items():
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

    for filePath, fileContent in self.fileStates.items():
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
        maxIndex = len(children) * (i + 1) // maxNumChildren
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
    for filePath, state in self.fileStates.items():
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

# runs a Test in this process
def runJobInSameProcess(shellCommand, workPath, previousTestState, clean, fullTestState, description, twoWayPipe):
  job = Test(shellCommand, workPath, previousTestState, clean, fullTestState, description, twoWayPipe)
  job.runAndReport()

# starts a Test in a new process
def runJobInOtherProcess(shellCommand, workPath, previousTestState, clean, fullTestState, description, queue, identifier):
  parentWriter, childReader = multiprocessing.Pipe()
  childInfo = TwoWayPipe(childReader, queue, identifier)
  process = multiprocessing.Process(target=runJobInSameProcess, args=(shellCommand, workPath, previousTestState, clean, fullTestState, description, childInfo,))
  process.start()
  return parentWriter

class TwoWayPipe(object):
  def __init__(self, readerConnection, writerQueue, identifier):
    self.readerConnection = readerConnection
    self.writerQueue = writerQueue
    self.identifier = identifier

# Stores a subprocess for running tests and some information about which tests to run
class Test(object):
  def __init__(self, shellCommand, workPath, previousTestState, clean, fullTestState, description, twoWayPipe):
    # the test to run
    self.shellCommand = shellCommand
    # directory to run the test in
    self.workPath = workPath
    # The previous state that we were asked to test. If the test command didn't modify any files, then our working directly would exactly match this state
    self.previousTestState = previousTestState
    # whether to reset the worker's state to match the target state exactly
    self.clean = clean
    # the state to test
    self.fullTestState = fullTestState
    # description of changes
    self.description = description
    self.pipe = twoWayPipe

  def runAndReport(self):
    succeeded = False
    postState = None
    try:
      succeeded = self.run()
    finally:
      print("^" * 100)
      self.pipe.writerQueue.put((self.pipe.identifier, succeeded, self.clean))

  def run(self):
    print("#" * 100)
    print("Checking " + self.description + " at " + str(datetime.datetime.now()))

    if self.clean:
      # update all files to match the target state
      currentState = filesStateFromTree(self.workPath)
      self.fullTestState.expandedWithEmptyEntriesFor(currentState, True).withoutDuplicatesFrom(currentState).apply(self.workPath)
    else:
      # just apply the difference from previousTestState to full_resetTo_state
      self.fullTestState.expandedWithEmptyEntriesFor(self.previousTestState).withoutDuplicatesFrom(self.previousTestState).apply(self.workPath)

    # run test
    testStartSeconds = time.time()
    testStart = datetime.datetime.now()
    returnCode = ShellScript(self.shellCommand, self.workPath).process()
    testEnd = datetime.datetime.now()
    duration = (testEnd - testStart).total_seconds()

    # report results
    if returnCode == 0:
      print("Passed: " + self.description + " at " + str(datetime.datetime.now()) + " in " + str(duration) + "s")
      return True
    else:
      print("Failed: " + self.description + " at " + str(datetime.datetime.now()) + " in " + str(duration) + "s")
      return False

# keeps track of a plan for running a Test
class Job(object):
  def __init__(self, testState, ancestorSucceeded):
    self.testState = testState
    self.ancestorSucceeded = ancestorSucceeded

  def size(self):
    return self.testState.size()

# Runner class that determines which diffs between two directories cause the given shell command to fail
class DiffRunner(object):
  def __init__(self, failingPath, passingPath, shellCommand, workPath, assumeInputStatesAreCorrect, allowGoalPassing, maxNumJobsAtOnce, timeoutSeconds):
    # some simple params
    self.workPath = os.path.abspath(workPath)
    self.bestState_path = fileIo.join(self.workPath, "bestResults")
    self.sampleFailure_path = fileIo.join(self.workPath, "sampleFailure")
    self.testScript_path = fileIo.join(self.workPath, "test.sh")
    fileIo.ensureDirExists(os.path.dirname(self.testScript_path))
    fileIo.writeScript(self.testScript_path, shellCommand)
    self.originalPassingPath = os.path.abspath(passingPath)
    self.originalFailingPath = os.path.abspath(failingPath)
    self.assumeInputStatesAreCorrect = assumeInputStatesAreCorrect
    self.allowGoalPassing = allowGoalPassing
    self.timeoutSeconds = timeoutSeconds

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
    self.resetTo_state = self.originalPassingState.expandedWithEmptyEntriesFor(self.originalFailingState).withoutDuplicatesFrom(self.originalFailingState, True)
    self.targetState = self.originalFailingState.expandedWithEmptyEntriesFor(self.originalPassingState).withoutDuplicatesFrom(self.originalPassingState, True)
    self.originalNumDifferences = self.resetTo_state.size()
    print("Processing " + str(self.originalNumDifferences) + " file differences")
    self.maxNumJobsAtOnce = maxNumJobsAtOnce

  def cleanupTempDirs(self):
    print("Clearing work directories")
    numAttempts = 3
    for attempt in range(numAttempts):
      if os.path.isdir(self.workPath):
        for child in os.listdir(self.workPath):
          if child.startswith("job-"):
            path = os.path.join(self.workPath, child)
            try:
              fileIo.removePath(path)
            except IOError as e:
              if attempt >= numAttempts - 1:
                raise Exception("Failed to remove " + path, e)

  def runnerTest(self, testState, timeout = None):
    self.cleanupTempDirs()
    workPath = self.getWorkPath(0)
    testState.apply(workPath)
    start = datetime.datetime.now()
    returnCode = ShellScript(self.testScript_path, workPath).process()
    duration = (datetime.datetime.now() - start).total_seconds()
    print("shell command completed in " + str(duration))
    if returnCode == 0:
      return (True, duration)
    else:
      return (False, duration)

  def onSuccess(self, testState):
    #print("Runner received success of testState: " + str(testState.summarize()))
    if debug:
      if not filesStateFromTree(self.bestState_path).checkSameKeys(self.full_resetTo_state.withoutEmptyEntries()):
        print("Contents of " + self.bestState_path + " don't match self.full_resetTo_state at beginning of onSuccess")
        sys.exit(1)
    self.targetState = self.targetState.withoutDuplicatesFrom(testState)
    self.resetTo_state = self.resetTo_state.withConflictsFrom(testState).withoutDuplicatesFrom(testState)
    delta = self.full_resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState, True).withoutDuplicatesFrom(self.full_resetTo_state)
    self.full_resetTo_state = self.full_resetTo_state.expandedWithEmptyEntriesFor(delta).withConflictsFrom(delta)
    # Update results path for the user to look at
    if os.path.exists(self.bestState_path):
      # The previous results are still there, so we just apply the difference between the previous and new best results
      delta.apply(self.bestState_path)
    else:
      # The previous results are missing (most likely moved/deleted by the user) so we save them again
      self.full_resetTo_state.apply(self.bestState_path)
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
        if self.allowGoalPassing:
          print("\nGiven goal state at " + self.originalFailingPath + " passes, so it is the best result")
          self.cleanupTempDirs()
          fileIo.removePath(self.bestState_path)
          self.originalFailingState.apply(self.bestState_path)
          return True
        print("\nGiven goal state at " + self.originalFailingPath + " does not fail! Pass --allow-goal-passing if this is intentional")
        return False
      # clean up temporary dirs in case any daemons remain running
      self.cleanupTempDirs()

      print("Testing that the given passing state actually passes")
      if not self.runnerTest(self.full_resetTo_state)[0]:
        print("\nGiven passing state at " + self.originalPassingPath + " does not actually pass!")
        return False
      # clean up temporary dirs in case any daemons remain running
      self.cleanupTempDirs()

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
    jobId = 0
    workingDir = self.getWorkPath(jobId)
    queue = multiprocessing.Queue()
    activeJobsById = {}
    workerStatesById = {}
    consecutiveIncrementalBuildsById = {}
    initialSplitSize = 2
    if self.maxNumJobsAtOnce != "auto" and self.maxNumJobsAtOnce > 2:
      initialSplitSize = self.maxNumJobsAtOnce
    availableJobs = [Job(testState, False) for testState in self.targetState.splitOnce(initialSplitSize)]
    numConsecutiveFailures = 0
    numFailuresSinceLastSplitOrSuccess = 0
    numCompletionsSinceLastPoolSizeChange = 0
    invalidatedIds = set()
    probablyAcceptableStates = []
    numCompletedTests = 2 # Already tested initial passing state and initial failing state
    numJobsAtFirstSuccessAfterMerge = None
    timedOut = False
    summaryLogPath = os.path.join(self.workPath, "diff-filterer.log")
    summaryLog = open(summaryLogPath, "w")
    summaryLog.write("diff-filterer.py starting at " + str(datetime.datetime.now()))
    summaryLog.flush()
    # continue until all files fail and no jobs are running
    while (numFailuresSinceLastSplitOrSuccess < self.resetTo_state.size() and not timedOut) or len(activeJobsById) > 0:
      # display status message
      now = datetime.datetime.now()
      elapsedDuration = now - start
      minNumTestsRemaining = sum([math.log(job.testState.size(), 2) + 1 for job in availableJobs + list(activeJobsById.values())]) - numFailuresSinceLastSplitOrSuccess
      estimatedNumTestsRemaining = max(minNumTestsRemaining, 1)
      if numConsecutiveFailures >= 4 and numFailuresSinceLastSplitOrSuccess < 1:
        # If we are splitting often and failing often, then we probably haven't yet
        # shrunken the individual boxes down to each contain only one failing file
        # During this phase, on average we've completed half of the work
        # So, we estimate that the total work remaining is double what we've completed
        estimatedNumTestsRemaining *= 2
      estimatedRemainingDuration = datetime.timedelta(seconds = elapsedDuration.total_seconds() * float(estimatedNumTestsRemaining) / float(numCompletedTests))
      message = "Elapsed duration: " + str(elapsedDuration) + ". Waiting for " + str(len(activeJobsById)) + " active subprocesses (" + str(len(availableJobs) + len(activeJobsById)) + " total available jobs). " + str(self.resetTo_state.size()) + " changes left to test, should take about " + str(estimatedNumTestsRemaining) + " tests, about " + str(estimatedRemainingDuration)
      print(message)
      if self.timeoutSeconds is not None:
        # what fraction of the time is left
        remainingTimeFraction = 1.0 - (elapsedDuration.total_seconds() / self.timeoutSeconds)
        # how many jobs there will be if we add another one
        possibleNumPendingJobs = len(activeJobsById) + 1
        if possibleNumPendingJobs / (numCompletedTests + possibleNumPendingJobs) > remainingTimeFraction:
          # adding one more job would be likely to cause us to exceed our time limit
          timedOut = True

      if len(activeJobsById) > 0:
        # wait for a response from a worker
        identifier, didAcceptState, clean = queue.get()
        job = activeJobsById[identifier]
        numCompletedTests += 1
        numCompletionsSinceLastPoolSizeChange += 1
        if didAcceptState:
          numConsecutiveFailures = 0
          numFailuresSinceLastSplitOrSuccess = 0
          acceptedState = job.testState
          maxRunningSize = max([job.testState.size() for job in activeJobsById.values()])
          maxRelevantSize = maxRunningSize / len(activeJobsById)
          if acceptedState.size() < maxRelevantSize:
            print("Queuing a retest of response of size " + str(acceptedState.size()) + " from job " + str(identifier) + " because a much larger job of size " + str(maxRunningSize) + " is still running")
            probablyAcceptableStates.append(acceptedState)
          else:
            if identifier in invalidatedIds:
              print("Queuing a retest of response from job " + str(identifier) + " due to previous invalidation. Successful state: " + str(acceptedState.summarize()))
              probablyAcceptableStates.append(acceptedState)
            else:
              if not clean:
                print("Queuing a clean retest of incremental success from job " + str(identifier))
                probablyAcceptableStates.append(acceptedState)
              else:
                print("Accepting clean success from job " + str(identifier))
                summaryLog.write("Succeeded : " + acceptedState.summarize() + " (job " + str(identifier) + ") at " + str(datetime.datetime.now()) + "\n")
                summaryLog.flush()
                # A worker discovered a nonempty change that can be made successfully; update our best accepted state
                self.onSuccess(acceptedState)
                if debug:
                  # The files in self.bestState_path should exactly match what's in workPath[identifier], except for files that didn't originally exist
                  if not filesStateFromTree(self.bestState_path).checkSameKeys(filesStateFromTree(self.getWorkPath(identifier)).restrictedToKeysIn(self.originalPassingState.expandedWithEmptyEntriesFor(self.originalFailingState))):
                    print("Successful state from work path " + str(identifier) + " wasn't correctly copied to bestState. Could the test command be deleting files that previously existed?")
                    sys.exit(1)
                # record that the results from any previously started process are no longer guaranteed to be valid
                for i in activeJobsById.keys():
                  if i != identifier:
                    invalidatedIds.add(i)
                # record our first success
                if numJobsAtFirstSuccessAfterMerge is None:
                  numJobsAtFirstSuccessAfterMerge = len(availableJobs) + len(activeJobsById)
        else:
          testState = job.testState
          if not os.path.isdir(self.sampleFailure_path):
            # save sample failure path where user can see it
            print("Saving sample failed state to " + str(self.sampleFailure_path))
            # write to a temporary directory so if a user looks at this path while we're writing, they don't see incomplete results
            tempPath = self.sampleFailure_path + ".temp"
            fileIo.removePath(tempPath)
            fileIo.ensureDirExists(tempPath)
            self.full_resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState, True).apply(tempPath)
            # rename temporary directory
            if os.path.exists(tempPath):
              fileIo.move(tempPath, self.sampleFailure_path)
          # count failures
          numConsecutiveFailures += 1
          numFailuresSinceLastSplitOrSuccess += 1
          # find any children that failed and queue a re-test of those children
          updatedChild = testState.withoutDuplicatesFrom(testState.withConflictsFrom(self.resetTo_state))
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
            for testState in split:
              availableJobs.append(Job(testState, job.ancestorSucceeded))
        # clear invalidation status
        if identifier in invalidatedIds:
          invalidatedIds.remove(identifier)
        del activeJobsById[identifier]
        # Check whether we've had enough failures lately to warrant checking for the possibility of dependencies among files
        if numJobsAtFirstSuccessAfterMerge is not None:
          if len(availableJobs) > 3 * numJobsAtFirstSuccessAfterMerge:
            # It's plausible that every file in one directory depends on every file in another directory
            # If this happens, then after we delete the dependent directory, we can delete the dependency directory too
            # To make sure that we consider deleting the dependency directory, we recombine all of our states and start splitting from there
            print("#############################################################")
            print("#                                                           #")
            print("# Lots of failures (" + str(len(availableJobs)) + " available jobs) since first success (" + str(numJobsAtFirstSuccessAfterMerge) + ")!")
            print("# Recombining all states in case we uncovered a dependency! #")
            print("#                                                           #")
            print("#############################################################")
            rejoinedState = FilesState()
            for job in availableJobs:
              state = job.testState
              rejoinedState = rejoinedState.expandedWithEmptyEntriesFor(state).withConflictsFrom(state)
            rejoinedState = rejoinedState.withoutDuplicatesFrom(self.resetTo_state)
            availableJobs = [Job(testState, False) for testState in rejoinedState.splitOnce(initialSplitSize)]
            numFailuresSinceLastSplitOrSuccess = 0
            numJobsAtFirstSuccessAfterMerge = None
            numCompletionsSinceLastPoolSizeChange = 0

      # if probablyAcceptableStates has become large enough, then retest its contents too
      if len(probablyAcceptableStates) > 0 and (len(probablyAcceptableStates) >= len(activeJobsById) + 1 or numConsecutiveFailures >= len(activeJobsById) or len(activeJobsById) < 1):
        probablyAcceptableState = FilesState()
        for state in probablyAcceptableStates:
          probablyAcceptableState = probablyAcceptableState.expandedWithEmptyEntriesFor(state).withConflictsFrom(state)
        probablyAcceptableState = probablyAcceptableState.withoutDuplicatesFrom(self.resetTo_state)
        if probablyAcceptableState.size() > 0:
          print("Retesting " + str(len(probablyAcceptableStates)) + " previous likely successful states as a single test: " + probablyAcceptableState.summarize())
          availableJobs = [Job(probablyAcceptableState, True)] + availableJobs
        probablyAcceptableStates = []
      if len(availableJobs) < 1 and len(activeJobsById) < 1:
        print("Error: no changes remain left to test. It was expected that applying all changes would fail")
        break

      # if we haven't checked everything yet, then try to queue more jobs
      if numFailuresSinceLastSplitOrSuccess < self.resetTo_state.size():
        availableJobs.sort(reverse=True, key=Job.size)

        if self.maxNumJobsAtOnce != "auto":
          targetNumJobs = self.maxNumJobsAtOnce
        else:
          # If N jobs are running then wait for all N to fail before increasing the number of running jobs
            # Recalibrate the number of processes based on the system load
            systemUsageStats = cpuStats.cpu_times_percent()
            systemIdleFraction = systemUsageStats.idle / 100
            if systemIdleFraction >= 0.5:
              if numCompletionsSinceLastPoolSizeChange <= len(activeJobsById):
                # Not much time has passed since the previous time we changed the pool size
                targetNumJobs = len(activeJobsById) + 1 # just replace existing job
              else:
                # We've been using less than the target capacity for a while, so add another job
                targetNumJobs = len(activeJobsById) + 2 # replace existing job and add a new one
                numCompletionsSinceLastPoolSizeChange = 0
            else:
              targetNumJobs = len(activeJobsById) # don't replace existing job
              numCompletionsSinceLastPoolSizeChange = 0

              if targetNumJobs < 1:
                targetNumJobs = 1
            print("System idle = " + str(systemIdleFraction) + ", current num jobs = " + str(len(activeJobsById) + 1) + ", target num jobs = " + str(targetNumJobs))

        if timedOut:
          print("Timeout reached, not starting new jobs")
        else:
          while len(activeJobsById) < targetNumJobs and len(activeJobsById) < self.resetTo_state.size() and len(availableJobs) > 0:
            # find next pending job
            job = availableJobs[0]
            # find next unused job id
            workerId = 0
            while workerId in activeJobsById:
              workerId += 1
            # start job
            workingDir = self.getWorkPath(workerId)
            if workerId in workerStatesById:
              workerPreviousState = workerStatesById[workerId]
            else:
              workerPreviousState = FilesState()
            testState = job.testState

            # If job.ancestorSucceeded, then this job came from another job that succeeded (it's either the union of several jobs that succeeded, or a piece of a job that succeeded).
            # However, if we get here, then this job failed.
            # So, joining or splitting this job's successful ancestor(s) created a failed job (this one).
            # So, in the future it's also likely that we'll find jobs that succeed on their own but if joined will fail.
            # So, in the future we don't want to join all successful jobs (because that could be likely to introduce a failure).
            # Any successful jobs in the future that we don't accept, we join together.
            # So, we want to accept a successful job soon.
            # We can only accept the results of clean builds (because for incremental builds we're not sure that the results are reliable)
            # So, if job.ancestorSucceeded, we make the descendants of this job be clean
            #
            # Also, we want each worker to occasionally use a new state in case so that incremental errors can't remain forever
            clean = job.ancestorSucceeded
            if workerId in consecutiveIncrementalBuildsById:
              consecutiveIncrementalBuilds = consecutiveIncrementalBuildsById[workerId]
              if consecutiveIncrementalBuilds >= 10:
                clean = True
                consecutiveIncrementalBuilds = 0
            else:
              consecutiveIncrementalBuilds = 0
              # Also, if this worker hasn't run any jobs yet, then we don't expect it to have any leftover files, so an incremental test is essentially equivalent to a clean test anyway
              # We ask the worker to run a clean test so that if it succeeds, we can detect that the success started from a clean state
              clean = True
            consecutiveIncrementalBuildsById[workerId] = 0
            fullTestState = self.full_resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState)
            description = testState.summarize() + " (job " + str(workerId) + ", "
            if clean:
              description += "clean"
            else:
              description += "incremental"
            description += ")"
            runJobInOtherProcess(self.testScript_path, workingDir, workerPreviousState, clean, fullTestState, description, queue, workerId)
            activeJobsById[workerId] = job
            workerStatesById[workerId] = fullTestState
            availableJobs = availableJobs[1:]

    if timedOut:
      wasSuccessful = False
    else:
      print("double-checking results")
      wasSuccessful = True
      if not self.runnerTest(filesStateFromTree(self.bestState_path))[0]:
        message = "Error: expected best state at " + self.bestState_path + " did not pass the second time. Could the test be non-deterministic?"
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
    print("Ran " + str(numCompletedTests) + " tests")
    if timedOut:
      print("Note that these results might not be optimal due to reaching the timeout of " + str(self.timeoutSeconds) + " seconds")
    return wasSuccessful

def main(args):
  assumeInputStatesAreCorrect = False
  allowGoalPassing = False
  workPath = "/tmp/diff-filterer"
  timeoutSeconds = None
  maxNumJobsAtOnce = 1
  while len(args) > 0:
    arg = args[0]
    if arg == "--assume-input-states-are-correct":
      assumeInputStatesAreCorrect = True
      args = args[1:]
      continue
    if arg == "--allow-goal-passing":
      allowGoalPassing = True
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
      val = args[1]
      if val == "auto":
        maxNumJobsAtOnce = val
      else:
        maxNumJobsAtOnce = int(val)
      args = args[2:]
      continue
    if arg == "--timeout":
      if len(args) < 2:
        usage()
      val = args[1]
      timeoutSeconds = float(val)
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
  if not os.path.exists(passingPath):
    print("Specified passing path " + passingPath + " does not exist")
    sys.exit(1)
  if not os.path.exists(failingPath):
    print("Specified failing path " + failingPath + " does not exist")
    sys.exit(1)
  success = DiffRunner(failingPath, passingPath, shellCommand, workPath, assumeInputStatesAreCorrect, allowGoalPassing, maxNumJobsAtOnce, timeoutSeconds).run()
  endTime = datetime.datetime.now()
  duration = endTime - startTime
  if success:
    print("Succeeded in " + str(duration))
  else:
    print("Failed in " + str(duration))
    sys.exit(1)

main(sys.argv[1:])
