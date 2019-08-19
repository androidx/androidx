#!/usr/bin/python

import sys, re, subprocess, os

def usage():
  print("""Usage: cat <issues> | triage-guesser.py
triage-guesser.py attempts to guess the assignee based on the title of the bug

triage-guesser reads issues from stdin (issues can be copy-pasted from the hotlist)
""")
  sys.exit(1)

class Issue(object):
  def __init__(self, issueId, description):
    self.issueId = issueId
    self.description = description

class AssigneeRecommendation(object):
  def __init__(self, usernames, justification):
    self.usernames = usernames
    self.justification = justification

  def intersect(self, other):
    names = []
    for name in self.usernames:
      if name in other.usernames:
        names.append(name)
    justification = self.justification + ", " + other.justification
    return AssigneeRecommendation(names, justification)

class RecommenderRule(object):
  def __init__(self):
    return

  def recommend(self, bug):
    return

class ShellRunner(object):
  def __init__(self):
    return

  def runAndGetOutput(self, args):
    return subprocess.check_output(args)
shellRunner = ShellRunner()

class WordRule(RecommenderRule):
  def __init__(self, word, assignees):
    super(WordRule, self).__init__()
    self.word = word
    self.assignees = assignees

  def recommend(self, bug):
    if self.word.lower() in bug.description.lower():
      return AssigneeRecommendation(self.assignees, '"' + self.word + '"')
    return None

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

class InterestingFileFinder(object):
  def __init__(self):
    return

  def findInterestingWords(self, text):
    words = re.split("#| |\.", text)
    words = [word for word in words if len(word) >= 4]
    words.sort(key=len, reverse=True)
    return words
interestingFileFinder = InterestingFileFinder()

class GitLogger(object):
  def __init__(self):
    return

  def gitLog1Author(self, filePath):
    text = shellRunner.runAndGetOutput(["bash", "-c", "cd " + os.path.dirname(filePath) + " && git log --no-merges -1 --format='%ae' -- " + os.path.basename(filePath)]).strip().replace("@google.com", "")
    return text
gitLogger = GitLogger()

class LastTouchedBy_Rule(RecommenderRule):
  def __init__(self, fileFinder):
    super(LastTouchedBy_Rule, self).__init__()
    self.fileFinder = fileFinder

  def recommend(self, bug):
    interestingWords = interestingFileFinder.findInterestingWords(bug.description)
    for word in interestingWords:
      for queryString in [word + "*", word + ".*"]:
        filePaths = self.fileFinder.findIname(queryString)
        if len(filePaths) > 0 and len(filePaths) <= 4:
          candidateAuthors = []
          for path in filePaths:
            thisAuthor = gitLogger.gitLog1Author(path)
            if len(candidateAuthors) == 0 or thisAuthor != candidateAuthors[-1]:
              candidateAuthors.append(thisAuthor)
          if len(candidateAuthors) == 1:
             return AssigneeRecommendation(candidateAuthors, "last touched " + os.path.basename(filePaths[0]))
    return None

class OwnersRule(RecommenderRule):
  def __init__(self, fileFinder):
    super(OwnersRule, self).__init__()
    self.fileFinder = fileFinder

  def recommend(self, bug):
    interestingWords = interestingFileFinder.findInterestingWords(bug.description)
    for word in interestingWords:
      for queryString in [word + "*", word + ".*"]:
        filePaths = self.fileFinder.findIname(queryString)
        commonPrefix = os.path.commonprefix(filePaths)
        dirToCheck = commonPrefix
        if len(dirToCheck) < 1:
          continue
        while True:
          if dirToCheck[-1] == "/":
            dirToCheck = dirToCheck[:-1]
          if len(dirToCheck) <= len(self.fileFinder.rootPath):
            break
          ownerFilePath = os.path.join(dirToCheck, "OWNERS")
          if os.path.isfile(ownerFilePath):
            with open(ownerFilePath) as ownerFile:
              lines = ownerFile.readlines()
              names = [line.replace("@google.com", "").strip() for line in lines]
              relOwnersPath = os.path.relpath(ownerFilePath, self.fileFinder.rootPath)
              justification = relOwnersPath + " (" + os.path.basename(filePaths[0] + ' ("' + word + '")')
              if len(filePaths) > 1:
                justification += "..."
              justification += ")"
              return AssigneeRecommendation(names, justification)
          else:
            parent = os.path.dirname(dirToCheck)
            if len(parent) >= len(dirToCheck):
              break
            dirToCheck = parent


class Triager(object):
  def __init__(self, fileFinder):
    self.recommenderRules = self.parseKnownOwners({
      "fragment": ["ilake", "mount", "adamp"],
      "animation": ["chet", "mount", "tianlu"],
      "transition": ["chet", "mount"],
      "theme": ["alanv"],
      "style": ["alanv"],
      "preferences": ["pavlis", "lpf"],
      "ViewPager": ["jgielzak", "aurimas"],
      "DrawerLayout": ["kirillg"],
      "RecyclerView": ["shepshapard", "yboyar"],
      "Loaders": ["ilake"],
      "VectorDrawableCompat": ["tianliu"],
      "AppCompat": ["kirillg"],
      "Design Library": ["dcarlsson"],
      "android.support.design": ["dcarlsson"],
      "RenderThread": ["jreck"],
      "VectorDrawable": ["tianliu"],
      "drawable": ["alanv"],
      "colorstatelist": ["alanv"],
      "multilocale": ["nona", "mnita"],
      "TextView": ["siyamed", "clarabayarri"],
      "Linkify": ["siyamed", "toki"],
      "Spannable": ["siyamed"],
      "Minikin": ["nona"],
      "Fonts": ["nona", "dougfelt"],
      "freetype": ["nona", "junkshik"],
      "harfbuzz": ["nona", "junkshik"],
      "slice": ["jmonk", "madym"]
    })
    self.recommenderRules.append(OwnersRule(fileFinder))
    self.recommenderRules.append(LastTouchedBy_Rule(fileFinder))

  def parseKnownOwners(self, ownersDict):
    rules = []
    keywords = sorted(ownersDict.keys())
    for keyword in keywords:
      assignees = ownersDict[keyword]
      rules.append(WordRule(keyword, assignees))
    return rules

  def process(self, lines):
    issues = self.parseIssues(lines)
    outputs = []
    print("Analyzing " + str(len(issues)) + " issues")
    for issue in issues:
      print(".")
      assigneeRecommendation = self.recommendAssignees(issue)
      recommendationText = "?"
      if assigneeRecommendation is not None:
        usernames = assigneeRecommendation.usernames
        if len(usernames) > 2:
          usernames = usernames[:2]
        recommendationText = str(usernames) + " (" + assigneeRecommendation.justification + ")"
      outputs.append(("(" + issue.issueId + ") " + issue.description.replace("\t", "...."), recommendationText, ))
    maxColumnWidth = 0
    for item in outputs:
      maxColumnWidth = max(maxColumnWidth, len(item[0]))
    for item in outputs:
      print(str(item[0]) + (" " * (maxColumnWidth - len(item[0]))) + " -> " + str(item[1]))

  def parseIssues(self, lines):
    priority = ""
    issueType = ""
    description = ""
    when = ""

    lines = [line.strip() for line in lines]
    fields = [line for line in lines if line != ""]
    linesPerIssue = 4
    if len(fields) % linesPerIssue != 0:
      raise Exception("Parse error, number of lines must be divisible by " + str(linesPerIssue) + ", not " + str(len(fields)) + ". Last line: " + fields[-1])
    issues = []
    while len(fields) > 0:
      priority = fields[0]
      issueType = fields[1]

      middle = fields[2].split("\t")
      expectedNumTabComponents = 5
      if len(middle) != expectedNumTabComponents:
        raise Exception("Parse error: wrong number of tabs in " + str(middle) + ", got " + str(len(middle) - 1) + ", expected " + str(expectedNumTabComponents - 1))
      description = middle[0]
      currentAssignee = middle[1]
      status = middle[2]
      issueId = middle[4]

      when = fields[3]

      issues.append(Issue(issueId, description))
      fields = fields[linesPerIssue:]
    return issues

  def recommendAssignees(self, issue):
    overallRecommendation = None
    for rule in self.recommenderRules:
      thisRecommendation = rule.recommend(issue)
      if thisRecommendation is not None:
        if overallRecommendation is None:
          overallRecommendation = thisRecommendation
        else:
          newRecommendation = overallRecommendation.intersect(thisRecommendation)
          count = len(newRecommendation.usernames)
          if count > 0 and count < len(overallRecommendation.usernames):
            overallRecommendation = newRecommendation
    return overallRecommendation



def main(args):
  if len(args) != 1:
    usage()
  fileFinder = FileFinder(os.path.dirname(args[0]))
  print("Reading issues (copy-paste from the hotlist) from stdin")
  lines = sys.stdin.readlines()
  triager = Triager(fileFinder)
  triager.process(lines)




main(sys.argv)
