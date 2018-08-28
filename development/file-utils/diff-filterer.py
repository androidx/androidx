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


import datetime, os, shutil, subprocess, sys
from collections import OrderedDict

def usage():
  print("""Usage: diff-filterer.py [--assume-no-side-effects] <passingPath> <failingPath> <shellCommand>

diff-filterer.py attempts to transform (a copy of) the contents of <passingPath> into the contents of <failingPath> subject to the constraint that when <shellCommand> is run in that directory, it returns 0

OPTIONS
  --assume-no-side-effects
    Assume that the given shell command does not make any (relevant) changes to the given directory, and therefore don't wipe and repopulate the directory before each invocation of the command
""")
  sys.exit(1)

# Miscellaneous file utilities
class FileIo(object):
  def __init__(self):
    return

  def ensureDirExists(self, filePath):
    if os.path.isfile(filePath):
      os.remove(filePath)
    if not os.path.isdir(filePath):
      os.makedirs(filePath)

  def copyFile(self, fromPath, toPath):
    self.ensureDirExists(os.path.dirname(toPath))
    self.removePath(toPath)
    shutil.copy2(fromPath, toPath)

  def removePath(self, filePath):
    if len(os.path.split(filePath)) < 2:
      raise Exception("Will not remove path at " + filePath + "; is too close to the root of the filesystem")
    if os.path.isdir(filePath):
      shutil.rmtree(filePath)
    elif os.path.isfile(filePath):
      os.remove(filePath)

  def join(self, path1, path2):
    return os.path.normpath(os.path.join(path1, path2))
fileIo = FileIo()

# Runs a shell command
class ShellScript(object):
  def __init__(self, commandText):
    self.commandText = commandText

  def process(self, cwd):
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

  def apply(self, filePath):
    fileIo.copyFile(self.referencePath, filePath)

  def equals(self, other):
    if not isinstance(other, FileBacked_FileContent):
      return False
    return self.referencePath == other.referencePath

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

  def getContent(self, filePath):
    if filePath in self.fileStates:
      return self.fileStates[filePath]
    return None

  def containsAt(self, filePath, content):
    ourContent = self.getContent(filePath)
    if ourContent is None or content is None:
      return ourContent == content
    return ourContent.equals(content)

  # returns a FilesState resembling <self> but without the keys for which other[key] == self[key]
  def withoutDuplicatesFrom(self, other):
    result = FilesState()
    for filePath, fileState in self.fileStates.iteritems():
      if not fileState.equals(other.getContent(filePath)):
        result.add(filePath, fileState)
    return result

  # returns self[fromIndex:toIndex]
  def slice(self, fromIndex, toIndex):
    result = FilesState()
    for filePath in self.fileStates.keys()[fromIndex:toIndex]:
      result.fileStates[filePath] = self.fileStates[filePath]
    return result

  # returns a FilesState having the same keys as this FilesState, but with values taken from <other> when it has them, and <self> otherwise
  def withConflictsFrom(self, other):
    result = FilesState()
    for filePath, fileContent in self.fileStates.iteritems():
      if filePath in other.fileStates:
        result.add(filePath, other.fileStates[filePath])
      else:
        result.add(filePath, fileContent)
    return result

  # returns a set of paths to all of the dirs in <self> that are implied by any files in <self>
  def listImpliedDirs(self):
    dirs = set()
    keys = self.fileStates.keys()[:]
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

  for root, dirPaths, filePaths in os.walk(rootPath):
    if len(filePaths) == 0 and len(dirPaths) == 0:
      relPath = os.path.relpath(root, rootPath)
      paths.append(relPath)
      states[relPath] = Directory_FileContent()
    for filePath in filePaths:
      fullPath = fileIo.join(root, filePath)
      relPath = os.path.relpath(fullPath, rootPath)
      paths.append(relPath)
      states[relPath] = FileBacked_FileContent(fullPath)

  paths = sorted(paths)
  state = FilesState()
  for path in paths:
    state.add(path, states[path])
  return state

# Runner class that determines which diffs between two directories cause the given shell command to fail
class DiffRunner(object):
  def __init__(self, failingPath, passingPath, shellCommand, tempPath, assumeNoSideEffects):
    # some simple params
    self.tempPath = tempPath
    self.workPath = fileIo.join(tempPath, "work")
    self.bestState_path = fileIo.join(tempPath, "bestResults")
    self.shellCommand = shellCommand
    self.originalPassingPath = os.path.abspath(passingPath)
    self.originalFailingPath = os.path.abspath(failingPath)
    self.assumeNoSideEffects = assumeNoSideEffects

    # lists of all the files under the two dirs
    print("Finding files in " + passingPath)
    self.originalPassingState = filesStateFromTree(passingPath)
    print("Finding files in " + failingPath)
    self.originalFailingState = filesStateFromTree(failingPath)

    print("Identifying duplicates")
    # list of the files in the state to reset to after each test
    self.full_resetTo_state = self.originalPassingState
    # minimal description of only the files that are supposed to need to be reset after each test
    self.resetTo_state = self.originalPassingState.expandedWithEmptyEntriesFor(self.originalFailingState).withoutDuplicatesFrom(self.originalFailingState)
    self.originalNumDifferences = self.resetTo_state.size()
    # state we're trying to reach
    self.targetState = self.resetTo_state.withConflictsFrom(self.originalFailingState.expandedWithEmptyEntriesFor(self.resetTo_state))
    self.windowSize = self.resetTo_state.size()

  def test(self, filesState):
    #print("Applying state: " + str(filesState) + " to " + self.workPath)
    filesState.apply(self.workPath)
    return (self.shellCommand.process(self.workPath) == 0)

  def run(self):
    print("Testing that given failing state actually fails")
    fileIo.removePath(self.workPath)
    fileIo.ensureDirExists(self.workPath)
    if self.test(self.originalFailingState):
      print("\nGiven failing state at " + self.originalFailingPath + " does not actually fail!")
      return False

    print("Testing that given passing state actually passes")
    if self.assumeNoSideEffects:
      self.resetTo_state.apply(self.workPath)
    else:
      fileIo.removePath(self.workPath)
      fileIo.ensureDirExists(self.workPath)
    if not self.test(self.originalPassingState):
      print("\nGiven passing state at " + self.originalPassingPath + " does not actually pass!")
      return False

    print("Saving best state found so far")
    fileIo.removePath(self.bestState_path)
    self.originalPassingState.apply(self.bestState_path)

    print("Starting")
    print("(You can inspect " + self.bestState_path + " while this process runs, to observe the best state discovered so far)")
    print("")

    # decrease the window size until it reaches 0
    while self.windowSize > 0:
      # scan the state until reaching the end
      windowMax = self.resetTo_state.size()
      failedDuringThisScan = False
      succeededDuringThisScan = False
      # if we encounter only successes for this window size, then check all windows except the last (because if all other windows pass, then the last must fail)
      # if we encounter any failure for this window size, then check all windows
      while (windowMax > self.windowSize) or (windowMax > 0 and failedDuringThisScan):
        # determine which changes to test
        windowMin = max(0, windowMax - self.windowSize)
        currentWindowSize = windowMax - windowMin
        print("Analyzing " + str(self.resetTo_state.size()) + " differences with a window size of " + str(currentWindowSize))
        testState = self.resetTo_state.withConflictsFrom(self.targetState.slice(windowMin, windowMax))
        # reset state if needed
        if not self.assumeNoSideEffects:
          print("Resetting " + str(self.workPath))
          fileIo.removePath(self.workPath)
          self.full_resetTo_state.apply(self.workPath)
        # test the state
        if self.test(testState):
          print("Accepted updated state having " + str(currentWindowSize) + " changes")
          # success! keep these changes
          testState.apply(self.bestState_path)
          self.full_resetTo_state = self.full_resetTo_state.expandedWithEmptyEntriesFor(testState).withConflictsFrom(testState).withoutEmptyEntries()
          # remove these changes from the set of changes to reconsider
          self.targetState = self.targetState.withoutDuplicatesFrom(testState)
          self.resetTo_state = self.targetState.withConflictsFrom(self.resetTo_state)
          succeededDuringThisScan = True
        else:
          print("Rejected updated state having " + str(currentWindowSize) + " changes")
          failedDuringThisScan = True
        # shift the window
        windowMax -= self.windowSize
      # we checked every file once; now shrink the window
      oldWindowSize = self.windowSize
      if self.windowSize >= 3:
        self.windowSize = int(self.windowSize / 3 + 1)
      else:
        if self.windowSize > 1:
          self.windowSize = 1
        else:
          if not succeededDuringThisScan:
            # only stop if we confirmed that no files could be reverted (if one file was reverted, it's possible that that unblocks another file)
            self.windowSize = 0
      print("Decreased window size from " + str(oldWindowSize) + " to " + str(self.windowSize))
      print("")

    print("double-checking results")
    fileIo.removePath(self.workPath)
    if not self.test(filesStateFromTree(self.bestState_path)):
      message = "Error: expected best state at " + self.bestState_path + " did not pass the second time. Could the test be non-deterministic?"
      if self.assumeNoSideEffects:
        message += " (it may help to remove the --assume-no-side-effects flag)"
      print(message)
      return False

    print("")
    print("Done trying to transform the contents of passing path:\n " + self.originalPassingPath + "\ninto the contents of failing path:\n " + self.originalFailingPath)
    print("Of " + str(self.originalNumDifferences) + " differences, could not accept: " + str(self.targetState))
    print("The final accepted state can be seen at " + self.bestState_path)
    return True

def main(args):
  if len(args) < 3:
    usage()
  assumeNoSideEffects = False
  if args[0] == "--assume-no-side-effects":
    assumeNoSideEffects = True
    args = args[1:]
  if len(args) != 3:
    usage()
  passingPath = args[0]
  failingPath = args[1]
  shellCommand = ShellScript(args[2])
  tempPath = "/tmp/diff-filterer"
  startTime = datetime.datetime.now()
  success = DiffRunner(failingPath, passingPath, shellCommand, tempPath, assumeNoSideEffects).run()
  endTime = datetime.datetime.now()
  duration = endTime - startTime
  if success:
    print("Succeeded in " + str(duration))
  else:
    print("Failed in " + str(duration))
    sys.exit(1)

main(sys.argv[1:])
