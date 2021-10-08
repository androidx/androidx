#!/usr/bin/python3

"""
Parses information about failing tests, and then generates a change to disable them.

Requires that the `bugged` command-line tool is installed, see go/bugged .
"""

import argparse, csv, os, subprocess

parser = argparse.ArgumentParser(
  description=__doc__
)
parser.add_argument("config_path", help="Path of file to process, downloaded from go/androidx-test-failures", nargs="+")

dirOfThisScript = os.path.dirname(os.path.realpath(__file__))
supportRoot = os.path.dirname(dirOfThisScript)

class LocatedFailure(object):
  def __init__(self, failure, location):
    self.failure = failure
    self.location = location

class TestFailure(object):
  def __init__(self, qualifiedClassName, methodName, testDefinitionName, consistent, branchName, testResultId):
    self.qualifiedClassName = qualifiedClassName
    self.methodName = methodName
    self.testDefinitionName = testDefinitionName
    self.consistent = consistent
    self.branchName = branchName
    self.testResultId = testResultId

  def getUrl(self):
    return "https://android-build.googleplex.com/builds/tests/view?testResultId=" + self.testResultId

class FailuresDatabase(object):
  """A collection of LocatedFailure instances, organized by their locations"""
  def __init__(self):
    self.failuresByPath = {}

  def add(self, locatedFailure):
    path = locatedFailure.location.filePath
    if path not in self.failuresByPath:
      self.failuresByPath[path] = {}
    failuresAtPath = self.failuresByPath[path]

    lineNumber = locatedFailure.location.lineNumber
    if lineNumber not in failuresAtPath:
      failuresAtPath[lineNumber] = locatedFailure
    else:
      # already have a failure at this location
      if not failuresAtPath[lineNumber].failure.consistent:
        # if the previously detected failure wasn't consistent, update with the new one
        failuresAtPath[lineNumber] = locatedFailure

  # returns Map<String, LocatedFailure> with key being filePath
  def getAll(self):
    results = {}
    for path, failuresAtPath in self.failuresByPath.items():
      lineNumbers = sorted(failuresAtPath.keys(), reverse=True)
      resultsAtPath = []
      # add failures in reverse order to make it easier to modify methods without adjusting line numbers for other methods
      for line in lineNumbers:
        resultsAtPath.append(failuresAtPath[line])
      results[path] = resultsAtPath
    return results

# parses the data file containing the failing tests
def parse():
  arguments = parser.parse_args()
  configPath = arguments.config_path[0]
  failures = []
  with open(configPath) as configFile:
    config = csv.DictReader(configFile, delimiter="\t")
    for item in config:
      # Decide whether this failure appears to be happening reliably (consistent = True)
      # or flakily (consistent = False).
      #
      # A flaky failure will probably occur a small number (probably 1) of times in a row
      # and a small fraction of times (slightly more than 0%),
      #
      # whereas a consistent failure will probably occur a large number of times (until we address
      # it, probably at least 3) and about 100% of the time
      #
      # These cutoff values are mostly arbitrary, about halfway between the expectations for these
      # two types of failures
      if int(item["consecutive_failures"]) >= 2 and float(item["failure_rate"]) > 0.5:
        consistent = True
      else:
        consistent = False
      failures.append(
        TestFailure(
          item["test_class"],
          item["method"],
          item["test_definition_name"],
          consistent,
          item["branch_name"],
          item["test_result_id"]
        )
      )
  return failures

class FileLocation(object):
  def __init__(self, filePath, lineNumber):
    self.filePath = filePath
    self.lineNumber = lineNumber

  def __str__(self):
    return self.filePath + "#" + str(self.lineNumber)

class ShellRunner(object):
  def __init__(self):
    return

  def runAndGetOutput(self, args):
    result = subprocess.run(args, capture_output=True, text=True).stdout
    return result

  def run(self, args):
    subprocess.run(args, capture_output=False)

shellRunner = ShellRunner()

class FileFinder(object):
  def __init__(self, rootPath):
    self.rootPath = rootPath
    self.resultsCache = {}

  def findIname(self, name):
    if name not in self.resultsCache:
      text = shellRunner.runAndGetOutput(["find", self.rootPath , "-type", "f", "-iname", name])
      filePaths = [path.strip() for path in text.split("\n")]
      filePaths = [path for path in filePaths if path != ""]
      self.resultsCache[name] = filePaths
    return self.resultsCache[name]
fileFinder = FileFinder(supportRoot)

class ClassFinder(object):
  """Locates the file path and line number for classes and methods"""
  def __init__(self):
    self.classToFile_cache = {}
    self.methodLocations_cache = {}

  def findMethod(self, qualifiedClassName, methodName):
    bracketIndex = methodName.find("[")
    if bracketIndex >= 0:
      methodName = methodName[:bracketIndex]
    fullName = qualifiedClassName + "." + methodName
    containingFile = self.findFileContainingClass(qualifiedClassName)
    if containingFile is None:
      return None
    if fullName not in self.methodLocations_cache:
      index = -1
      foundLineNumber = None
      with open(containingFile) as f:
        for line in f:
          index += 1
          if (" " + methodName + "(") in line:
            if foundLineNumber is not None:
              # found two matches, can't choose one
              foundLineNumber = None
              break
            foundLineNumber = index
      result = None
      if foundLineNumber is not None:
        result = FileLocation(containingFile, foundLineNumber)
      self.methodLocations_cache[fullName] = result
    return self.methodLocations_cache[fullName]


  def findFileContainingClass(self, qualifiedName):
    if qualifiedName not in self.classToFile_cache:
      lastDotIndex = qualifiedName.rindex(".")
      if lastDotIndex >= 0:
        packageName = qualifiedName[:lastDotIndex]
        className = qualifiedName[lastDotIndex + 1:]
      else:
        packageName = ""
        className = qualifiedName
      options = fileFinder.findIname(className + ".*")
      possibleContainingFiles = sorted(options)
      result = None
      for f in possibleContainingFiles:
        if self.getPackage(f) == packageName:
          result = f
          break
      self.classToFile_cache[qualifiedName] = result
    return self.classToFile_cache[qualifiedName]

  def getPackage(self, filePath):
    prefix = "package "
    with open(filePath) as f:
      for line in f:
        line = line.strip()
        if line.startswith(prefix):
          suffix = line[len(prefix):]
          if suffix.endswith(";"):
            return suffix[:-1]
          return suffix
    return None

classFinder = ClassFinder()

def readFile(path):
  f = open(path)
  text = f.read()
  f.close()
  return text

def writeFile(path, text):
  f = open(path, "w")
  f.write(text)
  f.close()

def extractIndent(text):
  indentSize = 0
  for c in text:
    if c == " ":
      indentSize += 1
    else:
      break
  return " " * indentSize

class SourceFile(object):
  """An in-memory model of a source file (java, kotlin) that can be manipulated and saved"""
  def __init__(self, path):
    text = readFile(path)
    self.lines = text.split("\n")
    self.path = path

  def isKotlin(self):
    return self.path.endswith(".kt")

  def maybeSemicolon(self):
    if self.isKotlin():
      return ""
    return ";"

  def addAnnotation(self, methodLineNumber, annotation):
    parenIndex = annotation.find("(")
    if parenIndex > 0:
      baseName = annotation[:parenIndex]
    else:
      baseName = annotation
    if self.findAnnotationLine(methodLineNumber, baseName) is not None:
      # already have an annotation, don't need to add another
      return
    indent = extractIndent(self.lines[methodLineNumber])
    self.insertLine(methodLineNumber, indent + annotation)

  # Adds an import to this file
  # Attempts to preserve alphabetical import ordering:
  #  If two consecutive imports can be found such that one should precede this import and
  #   one should follow this import, inserts between those two imports
  #  Otherwise attempts to add this import after the last import or before the first import
  # (Note that imports might be grouped into multiple blocks, each separated by a blank line)
  def addImport(self, symbolText):
    insertText = "import " + symbolText + self.maybeSemicolon()
    if insertText in self.lines:
      return # already added
    # set of lines that the insertion could immediately precede
    beforeLineNumbers = set()
    # set of lines that the insertion could immediately follow
    afterLineNumbers = set()
    for i in range(len(self.lines)):
      line = self.lines[i]
      if line.startswith("import"):
        # found an import. Should our import be before or after?
        if insertText < line:
          beforeLineNumbers.add(i)
        else:
          afterLineNumbers.add(i)
    # search for two adjacent lines that the line can be inserted between
    insertionLineNumber = None
    for i in range(len(self.lines) - 1):
      if i in afterLineNumbers and (i + 1) in beforeLineNumbers:
        insertionLineNumber = i + 1
        break
    # search for a line we can insert after
    if insertionLineNumber is None:
      for i in range(len(self.lines) - 1):
        if i in afterLineNumbers and (i + 1) not in afterLineNumbers:
          insertionLineNumber = i + 1
          break
    # search for a line we can insert before
    if insertionLineNumber is None:
      for i in range(len(self.lines) - 1, 0, -1):
        if i in beforeLineNumbers and (i - 1) not in beforeLineNumbers:
          insertionLineNumber = i
          break

    if insertionLineNumber is not None:
      self.insertLine(insertionLineNumber, insertText)

  def insertLine(self, beforeLineNumber, text):
    self.lines = self.lines[:beforeLineNumber] + [text] + self.lines[beforeLineNumber:]

  def findAnnotationLine(self, methodLineNumber, annotationText):
    lineNumber = methodLineNumber
    while True:
      if lineNumber < 0:
        return None
      if annotationText in self.lines[lineNumber]:
        return lineNumber
      if self.lines[lineNumber].strip() == "":
        return None
      lineNumber -= 1

  def removeLine(self, index):
    self.lines = self.lines[:index] + self.lines[index + 1:]

  def hasAnnotation(self, methodLineNumber, annotation):
    return self.findAnnotationLine(methodLineNumber, annotation) is not None

  def save(self):
    writeFile(self.path, "\n".join(self.lines))

# searches for bugs matching certain criteria, using the `bugged` CLI tool
class BugFinder(object):
  def __init__(self):
    self.bugsByQuery = {}

  def findForFailure(self, testFailure):
    qualifiedName = testFailure.qualifiedClassName
    text = ["title:" + qualifiedName, "status:open", "--columns=issue"]
    return self.query(text)

  def query(self, args):
    text = " ".join(args)
    if text not in self.bugsByQuery:
      response = None
      try:
        response = shellRunner.runAndGetOutput(["bugged", "search"] + args)
      except FileNotFoundError as e:
        raise FileNotFoundError("The `bugged` command-line tool is required but was not found. See go/bugged to install.")
      lines = response.split("\n")
      result = None
      for line in response.split("\n"):
        if line != "issue":
          result = line
          break
      if result == "":
        result = None
      self.bugsByQuery[text] = result
    return self.bugsByQuery[text]

bugFinder = BugFinder()

# converts from a List<TestFailure> to a FailuresDatabase containing LocatedFailure
def locate(failures):
  db = FailuresDatabase()
  for failure in failures:
    location = classFinder.findMethod(failure.qualifiedClassName, failure.methodName)
    if location is not None:
      db.add(LocatedFailure(failure, location))
    else:
      message = "Could not locate " + str(failure.qualifiedClassName) + "#" + str(failure.methodName)
      if failure.branchName != "aosp-androidx-main":
        message += ", should be in " + failure.branchName
      print(message)
  return db

# removes test result urls from the commit
def uncommitTestResultUrls():
  # first, remove test results urls from the files
  shellRunner.run(["bash", "-c", "git log -1 --name-only | grep -v ' ' | xargs sed -i 's| // .*testResultId.*||g'"])
  # commit the removal of these test result urls
  shellRunner.run(["git", "add", "."])
  shellRunner.run(["git", "commit", "-q", "--amend", "--no-edit"])
  # restore the previous versions of the files
  shellRunner.run(["git", "checkout", "-q", "HEAD@{1}", "--", "."])
  shellRunner.run(["git", "reset", "-q"])

# Given a FailureDatabase, disables all of the tests mentioned in it, by adding the appropriate
# annotations:
#  consistent failures get annotated with @Ignore ,
#  flaky failures get annotated with @FlakyTest.
# Annotations link to the associated bug if possible
def disable(failuresDatabase):
  mentionedBugs = set()
  numUpdates = 0
  failuresByPath = failuresDatabase.getAll()
  for path, failuresAtPath in failuresByPath.items():
    source = SourceFile(path)
    addedIgnore = False
    addedFlaky = False
    for failure in failuresAtPath:
      lineNumber = failure.location.lineNumber
      if source.hasAnnotation(lineNumber, "@FlakyTest") or source.hasAnnotation(lineNumber, "@Ignore"):
        continue
      bug = bugFinder.findForFailure(failure.failure)
      if bug is not None:
        mentionedBugs.add(bug)
      if failure.failure.consistent:
        if bug is not None:
          bugText = '"b/' + bug + '"'
        else:
          bugText = '"why"'
        source.addAnnotation(lineNumber, "@Ignore(" + bugText + ") // " + failure.failure.getUrl())
        addedIgnore = True
      else:
        if bug is not None:
          bugText = "bugId = " + bug
        else:
          bugText = "bugId = num"
        source.addAnnotation(lineNumber, "@FlakyTest(" + bugText + ") // " + failure.failure.getUrl())
        addedFlaky = True
    if addedIgnore:
      source.addImport("org.junit.Ignore")
    if addedFlaky:
      source.addImport("androidx.test.filters.FlakyTest")
    if addedIgnore or addedFlaky:
      # save file
      source.save()
      numUpdates += 1
  # make git commit
  commitHeader = """Mostly autogenerated suppression of test failures

This commit was created with the help of development/suppressFailingTests.py

"""

  bugStanzas = "\n".join(["Bug: " + bug for bug in sorted(mentionedBugs)])
  commitMessage = commitHeader + bugStanzas

  # make git commit containing the suppressions
  os.chdir(supportRoot)
  shellRunner.run(["git", "add", "."])
  shellRunner.run(["git", "commit", "-q", "--no-edit", "-m", commitMessage])

  # Remove test result urls from the git commit but keep them in the tree
  uncommitTestResultUrls()
  print("")
  print("Committed updates to " + str(numUpdates) + " files. Inspect/fix as needed.")
  print("")
  print("Additional context (test failure urls) has been added but not committed.")
  print("You can manually remove this information or you can run `git checkout -- <path>` to discard uncommitted changes under <path>")

def main():
  failures = parse()
  locations = locate(failures)
  disable(locations)

if __name__ == "__main__":
  main()

