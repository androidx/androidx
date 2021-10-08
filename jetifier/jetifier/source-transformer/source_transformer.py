#!/usr/bin/env python

# This script does package renamings on source code (and .xml files and build files, etc), as part of migrating code to Jetpack.
# For example, this script may replace the text
#   import android.support.annotation.RequiresApi;
# with
#   import androidx.annotation.RequiresApi;

# See also b/74074903
import argparse
import json
import os.path
import subprocess

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

class SourceRewriteRule(object):
  def __init__(self, fromName, toName):
    self.fromName = fromName
    self.toName = toName

  def serialize(self):
    return self.fromName + ":" + self.toName

class JetifierConfig(object):
  """
  Stores configuration about the renaming itself, such as package rename rules.
  """
  @staticmethod
  def parse(filePath):
    with open(filePath) as f:
      lines = f.readlines()
      nonCommentLines = [line for line in lines if not line.strip().startswith("#")]
      uncommentedPath = "/tmp/config.json"
      with open(uncommentedPath, 'w') as uncommentedFile:
        uncommentedFile.writelines(nonCommentLines)
      try:
        parsed = json.loads("".join(nonCommentLines))
        return JetifierConfig(parsed)
      except ValueError as e:
        print("Failed to parse " + uncommentedPath)
        raise e

  def __init__(self, parsedJson):
    self.json = parsedJson

  def getTypesMap(self, reverse):
    rules = []
    for rule in self.json["rules"]:
      fromName = rule["from"].replace("/", ".").replace("(.*)", "")
      toName = rule["to"].replace("/", ".").replace("{0}", "")
      if not toName.startswith("ignore"):
        if reverse:
          # Dejetify instead, so toName becomes fromName and vice versa.
          rules.append(SourceRewriteRule(toName, fromName))
        else:
          rules.append(SourceRewriteRule(fromName, toName))

    return rules


def createSourceJetificationSedCommand(args, jetifierConfig):
  rewriterTextBuilder = StringBuilder()
  rewriteRules = jetifierConfig.getTypesMap(args.reverse)
  # Append substitution rules, this will most probably never exceed the shell
  # characters per line limit which should be 131072.
  # In the weird case where the user somehow modified their ARG_MAX,
  # they'll know what to do when they see a /bin/sed: Argument list too long
  # error.
  for rule in rewriteRules:
    rewriterTextBuilder.add("-e \'s|").add(rule.fromName.replace(".", "\.")).add("|").add(rule.toName).add("|g\' ")

  # sed command containing substitutions and applied to the output file.
  sedCommand = "sed %s %s > %s" % (rewriterTextBuilder, args.infile, args.outfile)
  return sedCommand

def jetifySource(args):
  # If config file is not specified, look for the config file in the
  # same folder.
  jetifierConfigPath = args.config
  if not jetifierConfigPath:
    jetifierConfigPath = os.path.join(os.path.realpath(__file__), "default.config")
  jetifierConfig = JetifierConfig.parse(jetifierConfigPath)
  command = createSourceJetificationSedCommand(args, jetifierConfig)
  subprocess.check_output(command, shell=True)


def main():
  # Set up input arguments
  parser = argparse.ArgumentParser()
  parser.add_argument("-c", "--config", help="path to optional custom config file.")
  parser.add_argument("-r", "--reverse", help="operate in reverse mode (\"de-jetification\")",
        action="store_true")
  parser.add_argument("-i", "--infile",
        required=True, help="path to input source (java or xml)")
  parser.add_argument("-o", "--outfile",
        required=True, help="path to the output file, overriden if exists.")
  args = parser.parse_args()
  jetifySource(args)

if __name__ == "__main__":
    main()
