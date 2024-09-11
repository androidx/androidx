#!/usr/bin/env python3

"""
Parses information about failing tests, and then generates a change to disable them.

Requires that the `bugged` command-line tool is installed, see go/bugged .
"""

import argparse, csv, os, subprocess

parser = argparse.ArgumentParser(
  description=__doc__
)
parser.add_argument("-v", help="Verbose", action="store_true")

dirOfThisScript = os.path.dirname(os.path.realpath(__file__))
supportRoot = os.path.dirname(dirOfThisScript)

logger = None

class PrintLogger(object):
  def log(self, message):
    print(message)

class DisabledLogger(object):
  def log(self, message):
    pass

def log(message):
  logger.log(message)

class LocatedFailure(object):
  def __init__(self, failure, location, bugId):
    self.failure = failure
    self.location = location
    self.bugId = bugId

class TestFailure(object):
  def __init__(self, qualifiedClassName, methodName, testDefinitionName, branchName, testFailureUrl, bugId):
    self.qualifiedClassName = qualifiedClassName
    self.methodName = methodName
    self.testDefinitionName = testDefinitionName
    self.branchName = branchName
    self.failureUrl = testFailureUrl
    self.bugId = bugId

  def getUrl(self):
    return self.testFailureUrl

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

def parseBugLine(bugId, line):
  components = line.split(" | ")
  if len(components) < 3:
    return None
  testLink = components[1]
  # Example test link: [compose-ui-uidebugAndroidTest.xml androidx.compose.ui.window.PopupAlignmentTest#popup_correctPosition_alignmentTopCenter_rtl](https://android-build.googleplex.com/builds/tests/view?testResultId=TR96929024659298098)
  closeBracketIndex = testLink.rindex("]")
  if closeBracketIndex <= 0:
    raise Exception("Failed to parse b/" + bugId + " '" + line + "', testLink '" + testLink + "', closeBracketIndex = " + str(closeBracketIndex))
  linkText = testLink[1:closeBracketIndex]
  linkDest = testLink[closeBracketIndex + 1:]
  # Example linkText: compose-ui-uidebugAndroidTest.xml androidx.compose.ui.window.PopupAlignmentTest#popup_correctPosition_alignmentTopCenter_rtl
  # Example linkDest: (https://android-build.googleplex.com/builds/tests/view?testResultId=TR96929024659298098)
  testResultUrl = linkDest.replace("(", "").replace(")", "")
  # Example testResultUrl: https://android-build.googleplex.com/builds/tests/view?testResultId=TR96929024659298098
  spaceIndex = linkText.index(" ")
  if spaceIndex <= 0:
    raise Exception("Failed to parse b/" + bugId + " '" + line + "', linkText = '" + linkText + ", spaceIndex = " + str(spaceIndex))
  testDefinitionName = linkText[:spaceIndex]
  testPath = linkText[spaceIndex+1:]
  # Example test path: androidx.compose.ui.window.PopupAlignmentTest#popup_correctPosition_alignmentTopCenter_rtl
  testPathSplit = testPath.split("#")
  if len(testPathSplit) != 2:
    raise Exception("Failed to parse b/" + bugId + " '" + line + "', testPath = '" + testPath + "', len(testPathSplit) = " + str(len(testPathSplit)))
  testClass, testMethod = testPathSplit

  branchName = components[2].strip()
  print("  parsed test failure class=" + testClass + " method='" + testMethod + "' definition=" + testDefinitionName + " branch=" + branchName + " failureUrl=" + testResultUrl + " bugId=" + bugId)
  return TestFailure(testClass, testMethod, testDefinitionName, branchName, testResultUrl, bugId)

def parseBug(bugId):
  bugText = shellRunner.runAndGetOutput(["bugged", "show", bugId])
  log("bug text = '" + bugText + "'")
  failures = []
  bugLines = bugText.split("\n")

  stillFailing = True
  listingTests = False
  for i in range(len(bugLines)):
    line = bugLines[i]
    #log("parsing bug line " + line)
    if listingTests:
      failure = parseBugLine(bugId, line)
      if failure is not None:
        failures.append(failure)
    if "---|---|---|---|---" in line: # table start
      listingTests = True
    if " # " in line: # start of new section
      listingTests = False
    if "There are no more failing tests in this regression" in line or "ATN has not seen a failure for this regression recently." in line or "This regression has been identified as a duplicate of another one" in line:
      stillFailing = False
  if len(failures) < 1:
    raise Exception("Failed to parse b/" + bugId + ": identified 0 failures. Rerun with -v for more information")
  if not stillFailing:
    print("tests no longer failing")
    return []
  return failures

# identifies failing tests
def getFailureData():
  bugsQuery = ["bugged", "search", "hotlistid:5083126 status:open", "--columns", "issue"]
  print("Searching for bugs: " + str(bugsQuery))
  bugsOutput = shellRunner.runAndGetOutput(bugsQuery)
  bugIds = bugsOutput.split("\n")
  print("Checking " + str(len(bugIds)) + " bugs")
  failures = []
  for i in range(len(bugIds)):
    bugId = bugIds[i].strip()
    if bugId != "issue" and bugId != "":
      print("")
      print("Parsing bug " + bugId + " (" + str(i) + "/" + str(len(bugIds)) + ")")
      failures += parseBug(bugId)
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

# converts from a List<TestFailure> to a FailuresDatabase containing LocatedFailure
def locate(failures):
  db = FailuresDatabase()
  for failure in failures:
    location = classFinder.findMethod(failure.qualifiedClassName, failure.methodName)
    if location is not None:
      db.add(LocatedFailure(failure, location, failure.bugId))
    else:
      message = "Could not locate " + str(failure.qualifiedClassName) + "#" + str(failure.methodName) + " for " + str(failure.bugId)
      if failure.branchName != "aosp-androidx-main":
        message += ", should be in " + failure.branchName
      print(message)
  return db

# Given a FailureDatabase, disables all of the tests mentioned in it, by adding the appropriate
# annotations:
#  failures get annotated with @Ignore ,
# Annotations link to the associated bug if possible
def disable(failuresDatabase):
  numUpdates = 0
  failuresByPath = failuresDatabase.getAll()
  for path, failuresAtPath in failuresByPath.items():
    source = SourceFile(path)
    addedIgnore = False
    for failure in failuresAtPath:
      lineNumber = failure.location.lineNumber
      if source.hasAnnotation(lineNumber, "@Ignore"):
        continue
      bugId = failure.bugId
      bugText = '"b/' + bugId + '"'
      source.addAnnotation(lineNumber, "@Ignore(" + bugText + ")")
      addedIgnore = True
    if addedIgnore:
      source.addImport("org.junit.Ignore")
      source.save()
      numUpdates += 1
  print("Made " + str(numUpdates) + " updates")

def commit():
  print("Generating git commit per OWNERS file")
  os.chdir(supportRoot)
  commitMessage = """Autogenerated suppression of test failures

This commit was created with the help of development/suppressFailingTests.py
"""
  shellRunner.run(["development/split_change_into_owners.sh", commitMessage])


def main():
  global logger
  arguments = parser.parse_args()
  if arguments.v:
    logger = PrintLogger()
  else:
    logger = DisabledLogger()
  failures = getFailureData()
  if len(failures) < 1:
    print("Found 0 failures")
    return
  locations = locate(failures)
  disable(locations)
  commit()

if __name__ == "__main__":
  main()

