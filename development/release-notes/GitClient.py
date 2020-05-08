#!/usr/bin/python3
#
# Copyright (C) 2020 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import os, sys
from enum import Enum
import subprocess
from ReleaseNoteMarkdown import *

GIT_LOG_CMD_PREFIX = "git log --name-only"

def print_e(*args, **kwargs):
	print(*args, file=sys.stderr, **kwargs)

def removePrefix(text, prefix):
	if text.startswith(prefix): return text[len(prefix):]
	return text

class GitClient:
	def __init__(self, workingDir):
		self.workingDir = workingDir
		self.gitRoot = self.findGitDirInParentFilepath(workingDir)
		if self.gitRoot == None:
			self.gitRoot = workingDir
	def findGitDirInParentFilepath(self, filepath):
		curDirectory = filepath
		while curDirectory != "/" and curDirectory != "" and curDirectory != None:
			if os.path.exists(curDirectory + "/.git"):
				return curDirectory
			curDirectory = os.path.dirname(curDirectory)
		return None
	def executeCommand(self, command):
		try:
			command_output = subprocess.check_output(command, shell=True)
		except subprocess.CalledProcessError as cmdErr:
			print_e('FAILED: The following command: \n%s\n raised error:\n%s' %  (command, cmdErr.returncode))
			return None
		# Make the output into a string, because the subprocess returns a byte object by default
		# This is necessary because when we mock the command output in tests, we use strings.  Also
		# defaulting to returning a string is just easier to reason about.
		if not isinstance(command_output, str):
			return command_output.decode()
		else:
			return command_output

	def getGitLog(self, fromExclusiveSha, untilInclusiveSha, keepMerges, subProjectDir, n=None):
		""" Converts a diff log command into a [List<Commit>]
			@param fromExclusiveSha the older Sha to include in the git log (exclusive)
			@param untilInclusiveSha the newest Sha to include in the git log (inclusive)
			@param keepMerges boolean for whether or not to add merges to the return [List<Commit>].
			@param subProjectDir a string that represents the project directory relative to the gitRoot.
			@param n the maximum number of commits to output in the git log. n==None is treated
					 equivalently to n=infinity
		"""
		commitStartDelimiter = "_CommitStart"
		commitSHADelimiter = "_CommitSHA:"
		subjectDelimiter = "_Subject:"
		authorEmailDelimiter = "_Author:"
		dateDelimiter = "_Date:"
		bodyDelimiter = "_Body:"
		if subProjectDir[0] == '/':
			raise RuntimeError("Fatal error: the subproject directory (subProjectDir) passed to " +
				"GitClient.getGitLog was an absolute filepath.  The subproject directory should " +
				"be a relative filepath to the GitClient.gitRoot")

		fullProjectDir = os.path.join(self.gitRoot, subProjectDir)

		gitLogOptions = "--pretty=format:" + \
				commitStartDelimiter + "\%n" + \
				commitSHADelimiter + "\%H\%n" + \
				authorEmailDelimiter + "\%ae\%n" + \
				dateDelimiter + "\%ad\%n" + \
				subjectDelimiter + "\%s\%n" + \
				bodyDelimiter + "\%b"
		if not keepMerges:
			gitLogOptions += " --no-merges"

		gitLogCmd = GIT_LOG_CMD_PREFIX + " " + gitLogOptions + " "
		if n is not None:
			gitLogCmd += " -n " + str(n) + " "
		if fromExclusiveSha != "":
			gitLogCmd += " " + fromExclusiveSha + ".."
		gitLogCmd += untilInclusiveSha
		gitLogCmd += " -- " + fullProjectDir

		gitLogOutputString = self.executeCommand(gitLogCmd)
		return self.parseCommitLogString(gitLogOutputString,commitStartDelimiter,commitSHADelimiter,subjectDelimiter,authorEmailDelimiter,subProjectDir)

	def parseCommitLogString(self, commitLogString, commitStartDelimiter, commitSHADelimiter, subjectDelimiter, authorEmailDelimiter, localProjectDir):
		if commitLogString == "" or commitLogString == None: return []
		# Split commits string out into individual commits (note: this removes the deliminter)
		gitLogStringList = commitLogString.split(commitStartDelimiter)
		commitLog = []
		for gitCommit in gitLogStringList:
			if gitCommit.strip() == "": continue
			commitLog.append(
				Commit(
					gitCommit,
					localProjectDir,
					commitSHADelimiter,
					subjectDelimiter,
					authorEmailDelimiter
				)
			)
		return commitLog

class CommitType(Enum):
	NEW_FEATURE = 1
	API_CHANGE = 2
	BUG_FIX = 3
	EXTERNAL_CONTRIBUTION = 4
def getTitleFromCommitType(commitType):
	if commitType == CommitType.NEW_FEATURE: return "New Features"
	if commitType == CommitType.API_CHANGE: return "API Changes"
	if commitType == CommitType.BUG_FIX: return "Bug Fixes"
	if commitType == CommitType.EXTERNAL_CONTRIBUTION: return "External Contribution"

class Commit:
	def __init__(self, gitCommit, projectDir, commitSHADelimiter="_CommitSHA:", subjectDelimiter="_Subject:", authorEmailDelimiter="_Author:"):
		self.gitCommit = gitCommit
		self.projectDir = projectDir
		self.commitSHADelimiter = commitSHADelimiter
		self.subjectDelimiter = subjectDelimiter
		self.authorEmailDelimiter = authorEmailDelimiter
		self.changeIdDelimiter = "Change-Id:"
		self.bugs = []
		self.files = []
		self.sha = ""
		self.authorEmail = ""
		self.changeId = ""
		self.summary = ""
		self.changeType = CommitType.BUG_FIX
		self.releaseNote = ""
		self.releaseNoteDelimiter = "relnote:"
		self.formatGitCommitRelnoteTag()
		listedCommit = self.gitCommit.split('\n')
		for line in listedCommit:
			if line.strip() == "": continue
			if self.commitSHADelimiter in line:
				self.getSHAFromGitLine(line)
			if self.subjectDelimiter in line:
				self.getSummary(line)
			if self.changeIdDelimiter in line:
				self.getChangeIdFromGitLine(line)
			if self.authorEmailDelimiter in line:
				self.getAuthorEmailFromGitLine(line)
			if ("Bug:" in line) or ("b/" in line) or ("bug:" in line) or ("Fixes:" in line) or ("fixes b/" in line):
				self.getBugsFromGitLine(line)
			if self.releaseNoteDelimiter in line:
				self.getReleaseNotesFromGitLine(line, self.gitCommit)
			if self.projectDir.strip('/') in line:
				self.getFileFromGitLine(line)

	def formatGitCommitRelnoteTag(self):
		""" This method accounts for the fact that the releaseNoteDelimiter is case insensitive
			To do this, we just replace it with the tag we expect and can easily parse
		"""
		relnoteIndex = self.gitCommit.lower().find(self.releaseNoteDelimiter)
		if relnoteIndex > -1:
			self.gitCommit = self.gitCommit[:relnoteIndex] + \
						self.releaseNoteDelimiter + \
						self.gitCommit[relnoteIndex + len(self.releaseNoteDelimiter):]
		# Provide support for other types of quotes around the Relnote message
		self.gitCommit = self.gitCommit.replace('“','"')
		self.gitCommit = self.gitCommit.replace('”','"')

	def isExternalAuthorEmail(self, authorEmail):
		return "@google.com" not in self.authorEmail

	def getSHAFromGitLine(self, line):
		""" Parses SHAs from git commit line, with the format:
			[Commit.commitSHADelimiter] <commitSHA>
		"""
		self.sha = line.split(self.commitSHADelimiter, 1)[1].strip()

	def getSummary(self, line):
		""" Parses subject from git commit line, with the format:
			[Commit.subjectDelimiter]<commit subject>
		"""
		self.summary = line.split(self.subjectDelimiter, 1)[1].strip()

	def getChangeIdFromGitLine(self, line):
		"""	Parses commit Change-Id lines, with the format:
			`commit.changeIdDelimiter` <changeId>
		"""
		self.changeId = line.split(self.changeIdDelimiter, 1)[1].strip()

	def getAuthorEmailFromGitLine(self, line):
		"""	Parses commit author lines, with the format:
			[Commit.authorEmailDelimiter]email@google.com
		"""
		self.authorEmail = line.split(self.authorEmailDelimiter, 1)[1].strip()
		if self.isExternalAuthorEmail(self.authorEmail):
			self.changeType = CommitType.EXTERNAL_CONTRIBUTION

	def getFileFromGitLine(self, filepath):
		"""	Parses filepath to get changed files from commit, with the format:
			{project_directory}/{filepath}
		"""
		self.files.append(filepath.strip())
		if "current.txt" in filepath and self.changeType != CommitType.EXTERNAL_CONTRIBUTION:
			self.changeType = CommitType.API_CHANGE

	def getBugsFromGitLine(self, line):
		""" Parses bugs from a git commit message line
		"""
		punctuationChars = ["b/", ":", ",", ".", "(", ")", "!", "\\"]
		formattedLine = line
		for punctChar in punctuationChars:
			formattedLine = formattedLine.replace(punctChar, " ")
		words = formattedLine.split(' ')
		possibleBug = 0
		for word in words:
			try:
				possibleBug = int(word)
			except ValueError:
				# Do nothing, it's not a bug number
				pass
			if possibleBug > 1000 and possibleBug not in self.bugs:
				self.bugs.append(possibleBug)

	def getReleaseNotesFromGitLine(self, line, gitCommit):
		""" Reads in the release notes field from the git commit message line
		They can have a couple valid formats:
		`Relnote: This is a one-line release note`
		`Relnote: "This is a multi-line release note.  This accounts for the use case where
						 the commit cannot be explained in one line"
		`Relnote: "This is a one-line release note.  The quotes can be used this way too"`
		"""

		releaseNote = ""

		# Account for the use of quotes in a release note line
		# No quotes in the Release Note line means it's a one-line release note
		# If there are quotes, assume it's a multi-line release note
		quoteCountInRelNoteLine = 0
		for character in line:
			if character == '"': quoteCountInRelNoteLine += 1
		if quoteCountInRelNoteLine == 0:
			releaseNote = self.getOneLineReleaseNotesFromGitLine(line)
		else:
			if self.releaseNoteDelimiter in line:
				# Find the starting quote of the release notes quote block
				releaseNoteStartIndexInit = gitCommit.rfind(self.releaseNoteDelimiter) + len(self.releaseNoteDelimiter)
				try:
					releaseNoteStartIndex = gitCommit.index('"', releaseNoteStartIndexInit)
				except ValueError:
					releaseNoteStartIndex = releaseNoteStartIndexInit
				# Move to the character after the first quote
				if gitCommit[releaseNoteStartIndex] == '"':
					releaseNoteStartIndex += 1
				# Find the ending quote of the release notes quote block
				releaseNoteEndIndex = releaseNoteStartIndex + 1
				try:
					releaseNoteEndIndex = gitCommit.index('"', releaseNoteEndIndex)
					releaseNote = gitCommit[releaseNoteStartIndex:releaseNoteEndIndex].strip()
				except ValueError:
					# If there is no closing quote, just use the first line
					releaseNote = self.getOneLineReleaseNotesFromGitLine(line)

		# Finally, set the release note to be an empty string if the Relnote says the commit
		# is not applicable for release notes.
		if self.isIgnoredChange(releaseNote):
			self.releaseNote = ""
		else:
			self.releaseNote = releaseNote

	def getOneLineReleaseNotesFromGitLine(self, line):
		if self.releaseNoteDelimiter in line:
			releaseNoteStartIndex = line.index(self.releaseNoteDelimiter) + len(self.releaseNoteDelimiter)
			return line[releaseNoteStartIndex:].strip(' "')
		return ""

	def getReleaseNoteString(self):
		releaseNoteString = self.releaseNote
		releaseNoteString += " " + str(getChangeIdAOSPLink(self.changeId))
		for bug in self.bugs:
			releaseNoteString += " " + str(getBuganizerLink(bug))
		return releaseNoteString

	def isIgnoredChange(self, releaseNote):
		notApplicableStringOptions = ['na', 'n/a', 'n a']
		return releaseNote.lower().strip() in notApplicableStringOptions

	def __str__(self):
		commitString = self.summary
		commitString += " " + str(getChangeIdAOSPLink(self.changeId))
		for bug in self.bugs:
			commitString += " " + str(getBuganizerLink(bug))
		return commitString

def getChangeIdAOSPLink(changeId):
	""" @param changeId The Gerrit Change-Id to link to
		@return A [MarkdownLink] to AOSP Gerrit
	"""
	baseAOSPUrl = "https://android-review.googlesource.com/#/q/"
	return MarkdownLink(changeId[:6], "%s%s" % (baseAOSPUrl, changeId))

def getBuganizerLink(bugId):
	""" @param bugId the Id of the buganizer issue
		@return A [MarkdownLink] to the public buganizer issue tracker

		Note: This method does not check if the bug is public
	"""
	baseBuganizerUrl = "https://issuetracker.google.com/issues/"
	return MarkdownLink("b/%d" % bugId, "%s%d" % (baseBuganizerUrl, bugId))

