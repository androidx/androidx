#!/usr/bin/python

# This script does package renamings on source code (and .xml files and build files, etc), as part of migrating code to Jetpack.
# For example, this script may replace the text
#   import android.support.annotation.RequiresApi;
# with
#   import androidx.annotation.RequiresApi;

# See also b/74074903
import json
import os.path
import subprocess

HARDCODED_RULES_REVERSE = [
  "s|androidx.core.os.ResultReceiver|android.support.v4.os.ResultsReceiver|g\n",
  "s|androidx.core.media.MediaBrowserCompat|android.support.v4.media.MediaBrowserCompat|g\n",
  "s|androidx.core.media.MediaDescriptionCompat|android.support.v4.media.MediaDescriptionCompat|g\n",
  "s|androidx.core.media.MediaMetadataCompat|android.support.v4.media.MediaMetadataCompat|g\n",
  "s|androidx.core.media.RatingCompat|android.support.v4.media.RatingCompat|g\n",
  "s|androidx.core.media.session.MediaControllerCompat|android.support.v4.media.session.MediaControllerCompat|g\n",
  "s|androidx.core.media.session.MediaSessionCompat|android.support.v4.media.session.MediaSessionCompat|g\n",
  "s|androidx.core.media.session.ParcelableVolumeInfo|android.support.v4.media.session.ParcelableVolumeInfo|g\n",
  "s|androidx.core.media.session.PlaybackStateCompat|android.support.v4.media.session.PlaybackStateCompat|g\n",
]

class StringBuilder(object):
  def __init__(self, item=None):
    self.items = []
    if item is not None:
      self.add(item)

  def add(self, item):
    self.items.append(str(item))
    return self

  def __str__(self):
    return "".join(self.items)

class ExecutionConfig(object):
  """Stores configuration about this execution of the package renaming.
     For example, file paths of the source code.
     This config could potentially be affected by command-line arguments.
  """
  def __init__(self, jetifierConfig, sourceRoots, excludeDirs):
    self.jetifierConfig = jetifierConfig
    self.sourceRoots = sourceRoots
    self.excludeDirs = excludeDirs


class SourceRewriteRule(object):
  def __init__(self, fromName, toName):
    self.fromName = fromName
    self.toName = toName

  def serialize(self):
    return self.fromName + ":" + self.toName

class JetifierConfig(object):
  """Stores configuration about the renaming itself, such as package rename rules.
     This config isn't supposed to be affected by command-line arguments.
  """
  @staticmethod
  def parse(filePath):
    with open(filePath) as f:
      lines = f.readlines()
      nonCommentLines = [line for line in lines if not line.strip().startswith("#")]
      parsed = json.loads("".join(nonCommentLines))
      return JetifierConfig(parsed)
    
  def __init__(self, parsedJson):
    self.json = parsedJson

  def getTypesMap(self):
    rules = []
    for rule in self.json["rules"]:
      fromName = rule["from"].replace("/", ".").replace("(.*)", "")
      toName = rule["to"].replace("/", ".").replace("{0}", "")
      if not toName.startswith("ignore"):
        rules.append(SourceRewriteRule(fromName, toName))

    return rules


def createRewriteCommand(executionConfig):
  # create command to find source files
  finderTextBuilder = StringBuilder("find")
  for sourceRoot in executionConfig.sourceRoots:
    finderTextBuilder.add(" ").add(sourceRoot)
  for exclusion in executionConfig.excludeDirs:
    finderTextBuilder.add(" -name ").add(exclusion).add(" -prune -o")
  finderTextBuilder.add(" -iregex '.*\.java\|.*\.xml\|.*\.cfg\|.*\.flags' -print")

  # create command to rewrite one source
  print("Building sed instructions")
  rewriterTextBuilder = StringBuilder()
  rewriteRules = executionConfig.jetifierConfig.getTypesMap()
  for rule in rewriteRules:
    rewriterTextBuilder.add("s|").add(rule.fromName.replace(".", "\.")).add("|").add(rule.toName).add("|g\n")
  for rule in HARDCODED_RULES_REVERSE:
    rewriterTextBuilder.add(rule)
  scriptPath = "/tmp/jetifier-sed-script.txt"
  print("Writing " + scriptPath)
  with open(scriptPath, 'w') as scriptFile:
    scriptFile.write(str(rewriterTextBuilder))
  
  # create the command to do the rewrites
  fullCommand = "time " + str(finderTextBuilder) + " | xargs -n 1 --no-run-if-empty -P 64 sed -i -f " + scriptPath

  return fullCommand  

def processConfig(executionConfig):
  print("Building rewrite command")
  rewriteCommand = createRewriteCommand(executionConfig)
  commandLength = len(rewriteCommand)
  print("""
Will run command:

""" + rewriteCommand + """

""")
  response = raw_input("Ok? [y/n]")
  if response == "y":
    subprocess.check_output(rewriteCommand, shell=True)  


def main():
  pathOfThisFile = os.path.realpath(__file__)
  jetifierPath = os.path.abspath(os.path.join(pathOfThisFile, "..", ".."))

  jetifierConfigPath = os.path.join(jetifierPath, "source-transformer", "default.config")
  print("Parsing " + jetifierConfigPath)
  jetifierConfig = JetifierConfig.parse(jetifierConfigPath)

  sourceRoot = os.getcwd()
  excludeDirs = ["out", ".git", ".repo"]

  executionConfig = ExecutionConfig(jetifierConfig, [sourceRoot], excludeDirs)

  processConfig(executionConfig)

main()


