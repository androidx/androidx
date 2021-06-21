#!/usr/bin/python3
#
#  Copyright (C) 2020 The Android Open Source Project
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

import os, sys

def usage():
  print("Usage: explode.py [--consolidate-leaves] [--remove-leaves] <inputFile> <outputPath>")
  sys.exit(1)

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

fileIo = FileIo()

def countStartingSpaces(text):
  for i in range(len(text)):
    if text[i] not in (" ", "\t", "\n"):
      return i
  return len(text)

# A Block represents some section of text from a source file and stores it as a file on disk
# The text of each child block is expected to have text starting with <numSpaces> * " "
class Block(object):
  def __init__(self, numSpaces, fileName):
    self.numSpaces = numSpaces
    self.children = []
    self.fileName = fileName

  # Adds more text into this section
  def addChild(self, newChild):
    self.children.append(newChild)

  # Displays the text represented by this Block
  def display(self):
    prefix = " " * (self.numSpaces + 1)
    for i in range(len(self.children)):
      print(prefix + str(i) + ":")
      child = self.children[i]
      child.display()

  # Generates files at <filePath> representing this Block
  def apply(self, filePath):
    if not os.path.isdir(filePath):
      os.mkdir(filePath)
    for child in self.children:
      child.apply(os.path.join(filePath, child.fileName))

  def hasChildren(self):
    return len(self.children) > 0

  def startsFunction(self):
    if len(self.children) < 1:
      return False
    return self.children[0].startsFunction()

  # Removes any nodes that seem to be function bodies
  def consolidateFunctionBodies(self, emitOptionalFunctionBodies):
    consolidated = False
    for i in range(1, len(self.children)):
      prev = self.children[i - 1]
      if (not prev.hasChildren()) and prev.startsFunction():
        child = self.children[i]
        if child.hasChildren():
          child.consolidateSelf(emitOptionalFunctionBodies)
          consolidated = True
    for child in self.children:
      if child.hasChildren():
        child.consolidateFunctionBodies(emitOptionalFunctionBodies)

  def consolidateSelf(self, emitOptionalFunctionBodies):
    text = self.getText()
    if emitOptionalFunctionBodies or " return " in text:
      self.children = [TextBlock(text, "0")]
    else:
      self.children = []

  def getText(self):
    texts = [child.getText() for child in self.children]
    return "".join(texts)

# A TextBlock stores text for inclusion in a parent Block
# Together, they store the result of parsing text from a file
class TextBlock(object):
  def __init__(self, text, fileName):
    self.text = text
    self.fileName = fileName

  def getText(self):
    return self.text

  def display(self):
    print(self.text)

  def apply(self, filePath):
    fileIo.writeFile(filePath, self.text)

  def hasChildren(self):
    return False

  def startsFunction(self):
    if "}" in self.text:
      return False
    if "class " in self.text:
      return False
    parenIndex = self.text.find(")")
    curlyIndex = self.text.find("{")
    if parenIndex >= 0 and curlyIndex >= parenIndex:
      return True
    return False

def getLineName(lineNumber, numLines):
  longestLineNumber = len(str(numLines - 1))
  thisLineNumber = len(str(lineNumber))
  extraZeros = "0" * (longestLineNumber - thisLineNumber)
  return extraZeros + str(lineNumber)

def main(args):
  if len(args) < 2:
    usage()
  consolidateLeaves = False
  emitLeaves = True
  if args[0] == "--remove-leaves":
    consolidateLeaves = True
    emitLeaves = False
    args = args[1:]
  if args[0] == "--consolidate-leaves":
    consolidateLeaves = True
    args = args[1:]
  if len(args) != 2:
    usage()
  stack = [Block(0, -1)]
  inputPath = args[0]
  outputPath = args[1]
  lines = open(inputPath).readlines()
  numLines = len(lines)
  for i in range(numLines):
    lineName = getLineName(i, numLines)
    line = lines[i]
    numSpaces = countStartingSpaces(line)
    if line.strip() == "*/" and line.startswith(" "):
      numSpaces -= 1
    ignore = (numSpaces == len(line))
    if not ignore:
      # pop back to a previous scope
      while numSpaces < stack[-1].numSpaces:
        stack = stack[:-1]
      if numSpaces > stack[-1].numSpaces:
        newChild = Block(numSpaces, lineName)
        stack[-1].addChild(newChild)
        stack.append(newChild)
    stack[-1].addChild(TextBlock(line, lineName))
  if consolidateLeaves:
    # Remove the nodes that the user considers to be leaf nodes
    stack[0].consolidateFunctionBodies(emitLeaves)
  #stack[0].display()
  stack[0].apply(outputPath)


if __name__ == "__main__":
  main(sys.argv[1:])
