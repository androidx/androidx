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

import unittest
import os
from GitClient import *
from AndroidXMarkdown import *

class GitClientTestImpl(GitClient):
	def __init__(self, workingDir):
		self.workingDir = workingDir
		self.gitRoot = self.findGitDirInParentFilepath(workingDir)
		if self.gitRoot == None:
			self.gitRoot = workingDir
		self.commandReplies = {}

	def executeCommand(self, command):
		if command in self.commandReplies:
			return self.commandReplies[command]
		else:
			print_e('FAILED: The following command was not given a reply for the mock GitClient: \n%s\n ' %  command)
			return None

	def addReply(self, command, reply):
		self.commandReplies[command] = reply

class TestGitClient(unittest.TestCase):

	def test_gitClientFindsGitDir(self):
		gitClient = GitClientTestImpl(os.getcwd())
		self.assertTrue(os.path.exists(gitClient.gitRoot + "/.git"))

	def test_parseMalformattedReleaseNoteLine(self):
		projectDir = "group/artifact"
		commitWithABugFixString = """
				_CommitStart
				Here is an explanation of my commit that changes a kotlin file

				Relnote: "Missing close quote in the release note block.
				This is the second line of the release notes.  It should not be included in the
				release notes because this commit is missing the closing quote.

				Bug: 111111, 222222
				Test: ./gradlew buildOnServer
				Change-Id: myChangeId
				""" + \
				projectDir + "/a.java"
		commitWithABugFix = Commit(
			commitWithABugFixString,
			projectDir
		)
		self.assertEqual(
			"Missing close quote in the release note block.",
			commitWithABugFix.releaseNote
		)

	def test_parseAPICommitWithMultiLineReleaseNote(self):
		commitWithApiChangeString = """
				_CommitStart
				_CommitSHA:mySha
				_Author:anemail@google.com
				_Date:Tue Aug 6 15:05:55 2019 -0700
				_Subject:Added a new API!
				_Body:Also fixed some other bugs

				Here is an explanation of my commit

				"This is a quote about why it's great!"

				Relnote: "Added a new API that does something awesome and does a whole bunch
				of other things that are also awesome and I just can't elaborate enough how
				incredible this API is"

				"This is an extra set of quoted text"

				Bug: 123456
				Bug: b/1234567
				Fixes: 123123
				Test: ./gradlew buildOnServer

				Change-Id: myChangeId

				projectdir/a.java
				projectdir/b.java
				projectdir/androidTest/c.java
				projectdir/api/some_api_file.txt
				projectdir/api/current.txt
			"""
		commitWithApiChange = Commit(commitWithApiChangeString, "/projectdir/")
		self.assertEqual("mySha", commitWithApiChange.sha)
		self.assertEqual("anemail@google.com", commitWithApiChange.authorEmail)
		self.assertEqual("myChangeId", commitWithApiChange.changeId)
		self.assertEqual("Added a new API!", commitWithApiChange.summary)
		self.assertEqual(CommitType.API_CHANGE, commitWithApiChange.changeType)
		self.assertEqual([123456, 1234567, 123123], commitWithApiChange.bugs)
		self.assertEqual([
				"projectdir/a.java",
				"projectdir/b.java",
				"projectdir/androidTest/c.java",
				"projectdir/api/some_api_file.txt",
				"projectdir/api/current.txt"
			],
			commitWithApiChange.files
		)
		self.assertEqual(
			"Added a new API that does something awesome and does a whole bunch\n" +
			"				of other things that are also awesome and I just can't elaborate " +
			"enough how\n				incredible this API is",
			commitWithApiChange.releaseNote
		)

	def test_parseAPICommitWithDefaultDelimiters(self):
		commitWithApiChangeString = """
				_CommitStart
				_CommitSHA:mySha
				_Author:anemail@google.com
				_Date:Tue Aug 6 15:05:55 2019 -0700
				_Subject:Added a new API!
				_Body:Also fixed some other bugs

				Here is an explanation of my commit

				"This is a quote about why it's great!"

				Bug: 123456
				Bug: b/1234567
				Fixes: 123123
				Test: ./gradlew buildOnServer

				Relnote: Added an awesome new API!

				Change-Id: myChangeId

				projectdir/a.java
				projectdir/b.java
				projectdir/androidTest/c.java
				projectdir/api/some_api_file.txt
				projectdir/api/current.txt
			"""
		commitWithApiChange = Commit(commitWithApiChangeString, "/projectdir/")
		self.assertEqual("mySha", commitWithApiChange.sha)
		self.assertEqual("anemail@google.com", commitWithApiChange.authorEmail)
		self.assertEqual("myChangeId", commitWithApiChange.changeId)
		self.assertEqual("Added a new API!", commitWithApiChange.summary)
		self.assertEqual(CommitType.API_CHANGE, commitWithApiChange.changeType)
		self.assertEqual([123456, 1234567, 123123], commitWithApiChange.bugs)
		self.assertEqual([
				"projectdir/a.java",
				"projectdir/b.java",
				"projectdir/androidTest/c.java",
				"projectdir/api/some_api_file.txt",
				"projectdir/api/current.txt"
			],
			commitWithApiChange.files
		)
		self.assertEqual("Added an awesome new API!", commitWithApiChange.releaseNote)

	def test_parseAPICommitWithDefaultDelimitersAndNonstandardQuoteCharacters(self):
		commitWithApiChangeString = """
				_CommitStart
				_CommitSHA:mySha
				_Author:anemail@google.com
				_Date:Tue Aug 6 15:05:55 2019 -0700
				_Subject:Added a new API!
				_Body:Also fixed some other bugs

				Here is an explanation of my commit

				"This is a quote about why it's great!"

				Bug: 123456
				Bug: b/1234567
				Fixes: 123123
				Test: ./gradlew buildOnServer

				Relnote: “Added an awesome new API!
				It will make your life easier.”

				Change-Id: myChangeId

				projectdir/a.java
				projectdir/b.java
				projectdir/androidTest/c.java
				projectdir/api/some_api_file.txt
				projectdir/api/current.txt
			"""
		commitWithApiChange = Commit(commitWithApiChangeString, "/projectdir/")
		self.assertEqual("mySha", commitWithApiChange.sha)
		self.assertEqual("anemail@google.com", commitWithApiChange.authorEmail)
		self.assertEqual("myChangeId", commitWithApiChange.changeId)
		self.assertEqual("Added a new API!", commitWithApiChange.summary)
		self.assertEqual(CommitType.API_CHANGE, commitWithApiChange.changeType)
		self.assertEqual([123456, 1234567, 123123], commitWithApiChange.bugs)
		self.assertEqual([
				"projectdir/a.java",
				"projectdir/b.java",
				"projectdir/androidTest/c.java",
				"projectdir/api/some_api_file.txt",
				"projectdir/api/current.txt"
			],
			commitWithApiChange.files
		)
		self.assertEqual("Added an awesome new API!\n" + \
			"				It will make your life easier."
			, commitWithApiChange.releaseNote)

	def test_parseAPICommitWithDefaultDelimitersAndUncapitalizedRelnoteTag(self):
		commitWithApiChangeString = """
				_CommitStart
				_CommitSHA:mySha
				_Author:anemail@google.com
				_Date:Tue Aug 6 15:05:55 2019 -0700
				_Subject:Added a new API!
				_Body:Also fixed some other bugs

				Here is an explanation of my commit

				"This is a quote about why it's great!"

				Bug: 123456
				Bug: b/1234567
				Fixes: 123123
				Test: ./gradlew buildOnServer

				relnote: Added an awesome new API!

				Change-Id: myChangeId

				projectdir/a.java
				projectdir/b.java
				projectdir/androidTest/c.java
				projectdir/api/some_api_file.txt
				projectdir/api/current.txt
			"""
		commitWithApiChange = Commit(commitWithApiChangeString, "/projectdir/")
		self.assertEqual("mySha", commitWithApiChange.sha)
		self.assertEqual("anemail@google.com", commitWithApiChange.authorEmail)
		self.assertEqual("myChangeId", commitWithApiChange.changeId)
		self.assertEqual("Added a new API!", commitWithApiChange.summary)
		self.assertEqual(CommitType.API_CHANGE, commitWithApiChange.changeType)
		self.assertEqual([123456, 1234567, 123123], commitWithApiChange.bugs)
		self.assertEqual([
				"projectdir/a.java",
				"projectdir/b.java",
				"projectdir/androidTest/c.java",
				"projectdir/api/some_api_file.txt",
				"projectdir/api/current.txt"
			],
			commitWithApiChange.files
		)
		self.assertEqual("Added an awesome new API!", commitWithApiChange.releaseNote)

	def test_parseAPICommitWithDefaultDelimitersAndCapitalizedRelnoteTag(self):
		commitWithApiChangeString = """
				_CommitStart
				_CommitSHA:mySha
				_Author:anemail@google.com
				_Date:Tue Aug 6 15:05:55 2019 -0700
				_Subject:Added a new API!
				_Body:Also fixed some other bugs

				Here is an explanation of my commit

				"This is a quote about why it's great!"

				Bug: 123456
				Bug: b/1234567
				Fixes: 123123
				Test: ./gradlew buildOnServer

				RelNotE: Added an awesome new API!

				Change-Id: myChangeId

				projectdir/a.java
				projectdir/b.java
				projectdir/androidTest/c.java
				projectdir/api/some_api_file.txt
				projectdir/api/current.txt
			"""
		commitWithApiChange = Commit(commitWithApiChangeString, "/projectdir/")
		self.assertEqual("mySha", commitWithApiChange.sha)
		self.assertEqual("anemail@google.com", commitWithApiChange.authorEmail)
		self.assertEqual("myChangeId", commitWithApiChange.changeId)
		self.assertEqual("Added a new API!", commitWithApiChange.summary)
		self.assertEqual(CommitType.API_CHANGE, commitWithApiChange.changeType)
		self.assertEqual([123456, 1234567, 123123], commitWithApiChange.bugs)
		self.assertEqual([
				"projectdir/a.java",
				"projectdir/b.java",
				"projectdir/androidTest/c.java",
				"projectdir/api/some_api_file.txt",
				"projectdir/api/current.txt"
			],
			commitWithApiChange.files
		)
		self.assertEqual("Added an awesome new API!", commitWithApiChange.releaseNote)

	def test_parseAPICommitWithNotApplicableWithSlashRelnoteTag(self):
		commitWithApiChangeString = """
				_CommitStart
				_CommitSHA:mySha
				_Author:anemail@google.com
				_Date:Tue Aug 6 15:05:55 2019 -0700
				_Subject:API Tracking Improvements
				_Body:Also fixed some other bugs

				This CL fixes some infrastructure bugs

				"This is a quote"

				Bug: 123456
				Bug: b/1234567
				Fixes: 123123
				Test: ./gradlew buildOnServer

				Relnote: N/A

				Change-Id: myChangeId

				projectdir/a.java
				projectdir/b.java
				projectdir/androidTest/c.java
				projectdir/api/some_api_file.txt
				projectdir/api/current.txt
				projectdir/api/restricted_current.txt
			"""
		commitWithApiChange = Commit(commitWithApiChangeString, "/projectdir/")
		self.assertEqual("mySha", commitWithApiChange.sha)
		self.assertEqual("anemail@google.com", commitWithApiChange.authorEmail)
		self.assertEqual("myChangeId", commitWithApiChange.changeId)
		self.assertEqual("API Tracking Improvements", commitWithApiChange.summary)
		self.assertEqual(CommitType.API_CHANGE, commitWithApiChange.changeType)
		self.assertEqual([123456, 1234567, 123123], commitWithApiChange.bugs)
		self.assertEqual([
				"projectdir/a.java",
				"projectdir/b.java",
				"projectdir/androidTest/c.java",
				"projectdir/api/some_api_file.txt",
				"projectdir/api/current.txt",
				"projectdir/api/restricted_current.txt"
			],
			commitWithApiChange.files
		)
		self.assertEqual("", commitWithApiChange.releaseNote)

	def test_parseAPICommitWithMalformattedNotApplicableRelnoteTag(self):
		commitWithApiChangeString = """
				_CommitStart
				_CommitSHA:mySha
				_Author:anemail@google.com
				_Date:Tue Aug 6 15:05:55 2019 -0700
				_Subject:API Tracking Improvements
				_Body:Also fixed some other bugs

				This CL fixes some infrastructure bugs

				"This is a quote"

				Bug: 123456
				Bug: b/1234567
				Fixes: 123123
				Test: ./gradlew buildOnServer

				Relnote: "N/A

				Change-Id: myChangeId

				projectdir/a.java
				projectdir/b.java
				projectdir/androidTest/c.java
				projectdir/api/some_api_file.txt
				projectdir/api/current.txt
				projectdir/api/restricted_current.txt
			"""
		commitWithApiChange = Commit(commitWithApiChangeString, "/projectdir/")
		self.assertEqual("mySha", commitWithApiChange.sha)
		self.assertEqual("anemail@google.com", commitWithApiChange.authorEmail)
		self.assertEqual("myChangeId", commitWithApiChange.changeId)
		self.assertEqual("API Tracking Improvements", commitWithApiChange.summary)
		self.assertEqual(CommitType.API_CHANGE, commitWithApiChange.changeType)
		self.assertEqual([123456, 1234567, 123123], commitWithApiChange.bugs)
		self.assertEqual([
				"projectdir/a.java",
				"projectdir/b.java",
				"projectdir/androidTest/c.java",
				"projectdir/api/some_api_file.txt",
				"projectdir/api/current.txt",
				"projectdir/api/restricted_current.txt"
			],
			commitWithApiChange.files
		)
		self.assertEqual("", commitWithApiChange.releaseNote)

	def test_parseAPICommitWithNotApplicableWithoutSlashRelnoteTag(self):
		commitWithApiChangeString = """
				_CommitStart
				_CommitSHA:mySha
				_Author:anemail@google.com
				_Date:Tue Aug 6 15:05:55 2019 -0700
				_Subject:API Tracking Improvements
				_Body:Also fixed some other bugs

				This CL fixes some infrastructure bugs

				"This is a quote"

				Bug: 123456
				Bug: b/1234567
				Fixes: 123123
				Test: ./gradlew buildOnServer

				Relnote: na

				Change-Id: myChangeId

				projectdir/a.java
				projectdir/b.java
				projectdir/androidTest/c.java
				projectdir/api/some_api_file.txt
				projectdir/api/current.txt
				projectdir/api/restricted_current.txt
			"""
		commitWithApiChange = Commit(commitWithApiChangeString, "/projectdir/")
		self.assertEqual("mySha", commitWithApiChange.sha)
		self.assertEqual("anemail@google.com", commitWithApiChange.authorEmail)
		self.assertEqual("myChangeId", commitWithApiChange.changeId)
		self.assertEqual("API Tracking Improvements", commitWithApiChange.summary)
		self.assertEqual(CommitType.API_CHANGE, commitWithApiChange.changeType)
		self.assertEqual([123456, 1234567, 123123], commitWithApiChange.bugs)
		self.assertEqual([
				"projectdir/a.java",
				"projectdir/b.java",
				"projectdir/androidTest/c.java",
				"projectdir/api/some_api_file.txt",
				"projectdir/api/current.txt",
				"projectdir/api/restricted_current.txt"
			],
			commitWithApiChange.files
		)
		self.assertEqual("", commitWithApiChange.releaseNote)

	def test_parseBugFixCommitWithCustomDelimiters(self):
		commitSHADelimiter = "_MyCommitSHA:"
		authorEmailDelimiter = "_MyAuthor:"
		subjectDelimiter = "_MySubject:"
		projectDir = "group/artifact"
		commitWithABugFixString = "_CommitStart\n" + \
				commitSHADelimiter + "mySha\n" + \
				authorEmailDelimiter + "anemail@google.com\n" + \
				"_Date:Tue Aug 6 15:05:55 2019 -0700\n" + \
				subjectDelimiter + "Fixed a bug!\n" + \
				"""
				_Body:Also fixed some other bugs

				Here is an explanation of my commit that changes a kotlin file

				Relnote: "Fixed a critical bug"

				Bug: 111111, 222222
				Test: ./gradlew buildOnServer

				Change-Id: myChangeId
				""" + \
				projectDir + "/a.java\n" + \
				projectDir + "/b.kt\n" + \
				projectDir + "/androidTest/c.java\n"
		commitWithABugFix = Commit(
			gitCommit = commitWithABugFixString,
			projectDir = projectDir,
			commitSHADelimiter = commitSHADelimiter,
			subjectDelimiter = subjectDelimiter,
			authorEmailDelimiter = authorEmailDelimiter
		)
		self.assertEqual("mySha", commitWithABugFix.sha)
		self.assertEqual("anemail@google.com", commitWithABugFix.authorEmail)
		self.assertEqual("myChangeId", commitWithABugFix.changeId)
		self.assertEqual("Fixed a bug!", commitWithABugFix.summary)
		self.assertEqual(CommitType.BUG_FIX, commitWithABugFix.changeType)
		self.assertEqual([111111, 222222], commitWithABugFix.bugs)
		self.assertEqual([
				projectDir + "/a.java",
				projectDir + "/b.kt",
				projectDir + "/androidTest/c.java"
			],
			commitWithABugFix.files
		)
		self.assertEqual("Fixed a critical bug", commitWithABugFix.releaseNote)

	def test_parseExternalContributorCommitWithCustomDelimiters(self):
		commitSHADelimiter = "_MyCommitSHA:"
		subjectDelimiter = "_MySubject:"
		authorEmailDelimiter = "_MyAuthor:"
		projectDir = "group/artifact"
		commitFromExternalContributorString = "_CommitStart\n" + \
				commitSHADelimiter + "mySha\n" + \
				authorEmailDelimiter + "externalcontributor@gmail.com\n" + \
				"_Date:Thurs Aug 8 15:05:55 2019 -0700\n" + \
				subjectDelimiter + "New compat API!\n" + \
				"""
				_Body:Also fixed some other bugs

				Here is an explanation of my commit that changes a java file

				Relnote: Added a new compat API!

				Bug: 111111, 222222
				Test: ./gradlew buildOnServer

				Change-Id: myChangeId
				""" + \
				projectDir + "/a.java\n" + \
				projectDir + "/b.java\n" + \
				projectDir + "/androidTest/c.java\n" + \
				projectDir + "/api/current.txt\n" + \
				projectDir + "/api/1.2.0-alpha04.txt\n"
		commitFromExternalContributor = Commit(
			commitFromExternalContributorString,
			projectDir,
			commitSHADelimiter = commitSHADelimiter,
			subjectDelimiter = subjectDelimiter,
			authorEmailDelimiter = authorEmailDelimiter
		)
		self.assertEqual("mySha", commitFromExternalContributor.sha)
		self.assertEqual("externalcontributor@gmail.com", commitFromExternalContributor.authorEmail)
		self.assertEqual("myChangeId", commitFromExternalContributor.changeId)
		self.assertEqual("New compat API!", commitFromExternalContributor.summary)
		self.assertEqual(CommitType.EXTERNAL_CONTRIBUTION, commitFromExternalContributor.changeType)
		self.assertEqual([111111, 222222], commitFromExternalContributor.bugs)
		self.assertEqual([
				projectDir + "/a.java",
				projectDir + "/b.java",
				projectDir + "/androidTest/c.java",
				projectDir + "/api/current.txt",
				projectDir + "/api/1.2.0-alpha04.txt"
			],
			commitFromExternalContributor.files
		)
		self.assertEqual("Added a new compat API!", commitFromExternalContributor.releaseNote)

	def test_parseGitLog(self):
		mockGitRootDir = "gitRoot"
		gitClient = GitClientTestImpl(mockGitRootDir)
		# This is the default set-up boilerplate from `GitClientImpl.getGitLog` to set up the
		# default git log command (gitLogCmd)
		commitStartDelimiter = "_CommitStart"
		commitSHADelimiter = "_CommitSHA:"
		subjectDelimiter = "_Subject:"
		authorEmailDelimiter = "_Author:"
		dateDelimiter = "_Date:"
		bodyDelimiter = "_Body:"
		projectDir = "group/artifact"
		gitLogOptions = "--pretty=format:" + \
				commitStartDelimiter + "\%n" + \
				commitSHADelimiter + "\%H\%n" + \
				authorEmailDelimiter + "\%ae\%n" + \
				dateDelimiter + "\%ad\%n" + \
				subjectDelimiter + "\%s\%n" + \
				bodyDelimiter + "\%b" + \
				" --no-merges"
		fullProjectDir = os.path.join(mockGitRootDir, projectDir)
		gitLogCmd = GIT_LOG_CMD_PREFIX + " " + gitLogOptions + "  sha..topSha -- " + fullProjectDir
		# Check with default delimiters
		gitLogString = """
				_CommitStart
				_CommitSHA:topSha
				_Author:anemail@google.com
				_Date:Tue Aug 6 15:05:55 2019 -0700
				_Subject:Added a new API!
				_Body:Also fixed some other bugs

				Here is an explanation of my commit

				Bug: 123456
				Bug: b/1234567
				Fixes: 123123
				Test: ./gradlew buildOnServer

				Change-Id: myChangeId\n
				""" + \
				projectDir + "/a.java\n" + \
				projectDir + "/b.java\n" + \
				projectDir + "/androidTest/c.java\n" + \
				projectDir + "/api/some_api_file.txt\n" + \
				projectDir + "/api/current.txt\n" + \
				"""
				_CommitStart
				_CommitSHA:midSha
				_Author:anemail@google.com
				_Date:Tue Aug 6 15:05:55 2019 -0700
				_Subject:Fixed a bug!
				_Body:Also fixed some other bugs

				Here is an explanation of my commit

				Bug: 111111, 222222
				Test: ./gradlew buildOnServer

				Change-Id: myChangeId\n
				""" + \
				projectDir + "/a.java\n" + \
				projectDir + "/b.java\n" + \
				projectDir + "/androidTest/c.java\n" + \
				"""
				_CommitStart
				_CommitSHA:sha
				_Author:externalcontributor@gmail.com
				_Date:Thurs Aug 8 15:05:55 2019 -0700
				_Subject:New compat API!
				_Body:Also fixed some other bugs

				Here is an explanation of my commit

				Bug: 111111, 222222
				Test: ./gradlew buildOnServer

				Change-Id: myChangeId\n
				""" + \
				projectDir + "/a.java\n" + \
				projectDir + "/b.java\n" + \
				projectDir + "/androidTest/c.java\n" + \
				projectDir + "/api/current.txt\n" + \
				projectDir + "/api/1.2.0-alpha04.txt\n"

		gitClient.addReply(
			gitLogCmd,
			gitLogString
		)

		commitWithAPIChange = Commit("", projectDir)
		commitWithAPIChange.sha = "topSha"
		commitWithAPIChange.authorEmail = "anemail@google.com"
		commitWithAPIChange.changeId = "myChangeId"
		commitWithAPIChange.summary = "Added a new API!"
		commitWithAPIChange.type = CommitType.API_CHANGE
		commitWithAPIChange.bugs = [123456, 1234567, 123123]
		commitWithAPIChange.files = [
			projectDir + "/a.java",
			projectDir + "/b.java",
			projectDir + "/androidTest/c.java",
			projectDir + "/api/some_api_file.txt",
			projectDir + "/api/current.txt"
		]

		commitWithBugFix = Commit("", projectDir)
		commitWithBugFix.sha = "midSha"
		commitWithBugFix.authorEmail = "anemail@google.com"
		commitWithBugFix.changeId = "myChangeId"
		commitWithBugFix.summary = "Fixed a bug!"
		commitWithBugFix.type = CommitType.BUG_FIX
		commitWithBugFix.bugs = [111111, 222222]
		commitWithBugFix.files = [
			projectDir + "/a.java",
			projectDir + "/b.java",
			projectDir + "/androidTest/c.java"
		]

		commitFromExternalContributor = Commit("", projectDir)
		commitFromExternalContributor.sha = "sha"
		commitFromExternalContributor.authorEmail = "externalcontributor@gmail.com"
		commitFromExternalContributor.changeId = "myChangeId"
		commitFromExternalContributor.summary = "New compat API!"
		commitFromExternalContributor.type = CommitType.EXTERNAL_CONTRIBUTION
		commitFromExternalContributor.bugs = [111111, 222222]
		commitFromExternalContributor.files = [
			projectDir + "/a.java",
			projectDir + "/b.java",
			projectDir + "/androidTest/c.java",
			projectDir + "/api/current.txt",
			projectDir + "/api/1.2.0-alpha04.txt"
		]
		# In this test case, we pass an empty string as the subProjectDir because "group/artifact"
		# is the git root dir and the git client will prepend that to the subProjectDir. 
		gitLogList = gitClient.getGitLog(
			fromExclusiveSha = "sha",
			untilInclusiveSha = "topSha",
			keepMerges = False,
			subProjectDir = projectDir
		)
		self.assertEqual(3, len(gitLogList))
		for commit in gitLogList:
			if commit.sha == "topSha":
				self.assertCommitsAreEqual(commitWithAPIChange, commit)
			elif commit.sha == "midSha":
				self.assertCommitsAreEqual(commitWithBugFix, commit)
			elif commit.sha == "sha":
				self.assertCommitsAreEqual(commitFromExternalContributor, commit)
			else:
				self.assertFalse("Unable to parse commit")

	def test_checkLatestCommitExists(self):
		# Do not use the MockCommandRunner because it's a better test to check the validity of
		# the git command against the actual git in the repo
		gitClient = GitClient(os.getcwd())
		subProjectDir = os.getcwd().split("frameworks/support/")[1]
		commitList = gitClient.getGitLog(
			fromExclusiveSha = "",
			untilInclusiveSha = "HEAD",
			keepMerges = False,
			subProjectDir = subProjectDir,
			n = 1
		)
		self.assertEqual(1, len(commitList))

	def test_checkLatestNCommitExists(self):
		# Do not use the MockCommandRunner because it's a better test to check the validity of
		# the git command against the actual git in the repo
		gitClient = GitClient(os.getcwd())
		subProjectDir = os.getcwd().split("frameworks/support/")[1]
		commitList = gitClient.getGitLog(
			fromExclusiveSha = "",
			untilInclusiveSha = "HEAD",
			keepMerges = False,
			subProjectDir = subProjectDir,
			n = 0
		)
		self.assertEqual(0, len(commitList))
		commitList = gitClient.getGitLog(
			fromExclusiveSha = "",
			untilInclusiveSha = "HEAD",
			keepMerges = False,
			subProjectDir = subProjectDir,
			n = 1
		)
		self.assertEqual(1, len(commitList))
		commitList = gitClient.getGitLog(
			fromExclusiveSha = "",
			untilInclusiveSha = "HEAD",
			keepMerges = False,
			subProjectDir = subProjectDir,
			n = 2
		)
		self.assertEqual(2, len(commitList))
		commitList = gitClient.getGitLog(
			fromExclusiveSha = "HEAD~3",
			untilInclusiveSha = "HEAD",
			keepMerges = False,
			subProjectDir = subProjectDir,
			n = 1
		)
		self.assertEqual(1, len(commitList))

	def test_gitLogFailsWhenPassedAnAbsolutePath(self):
		# Tests that you cannot pass an absolute file path to git log
		gitClient = GitClient(os.getcwd())
		subProjectDir = os.getcwd().split("frameworks/support/")[1]
		subProjectDir = '/' + subProjectDir
		self.assertRaises(RuntimeError, gitClient.getGitLog,
			fromExclusiveSha = "",
			untilInclusiveSha = "HEAD",
			keepMerges = False,
			subProjectDir = subProjectDir,
			n = 1
		)

	def assertCommitsAreEqual(self, commitA, commitB):
		self.assertEqual(commitA.summary, commitB.summary)
		self.assertEqual(commitA.files, commitB.files)
		self.assertEqual(commitA.sha, commitB.sha)
		self.assertEqual(commitA.changeId, commitB.changeId)
		self.assertEqual(commitA.authorEmail, commitB.authorEmail)
		self.assertEqual(commitA.type, commitB.changeType)
		self.assertEqual(commitA.projectDir, commitB.projectDir)
		self.assertEqual(commitA.summary, commitB.summary)
		self.assertEqual(commitA.releaseNote, commitB.releaseNote)

class TestMarkdown(unittest.TestCase):

	def test_markdownCorrectlyFormatsForOneArtifactWithNoCommit(self):
		releaseNotes = LibraryReleaseNotes(
			groupId = "groupId",
			artifactIds = ["artifactId"],
			version = "version",
			releaseDate = "01-01-1970",
			fromSHA = "fromSHA",
			untilSHA = "untilSHA",
			projectDir = "projectDir",
			requiresSameVersion = True,
			commitList = [],
			forceIncludeAllCommits = False
		)
		self.assertEqual(releaseNotes.groupId, "groupId")
		self.assertEqual(releaseNotes.artifactIds, ["artifactId"])
		self.assertEqual(releaseNotes.version, "version")
		self.assertEqual(str(releaseNotes.releaseDate), "January 1, 1970")
		self.assertEqual(releaseNotes.fromSHA, "fromSHA")
		self.assertEqual(releaseNotes.untilSHA, "untilSHA")
		self.assertEqual(releaseNotes.projectDir, "projectDir")
		self.assertEqual(releaseNotes.requiresSameVersion, True)
		self.assertEqual(releaseNotes.forceIncludeAllCommits, False)
		self.assertEqual(str(releaseNotes.header),
			"### Groupid Version version {:#version}")
		self.assertEqual(str(releaseNotes.diffLogLink),
			"[Version version contains" + \
			" these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/fromSHA..untilSHA/projectDir)"
		)
		self.assertEqual(releaseNotes.commits, [])
		self.assertEqual(str(releaseNotes.commitMarkdownList),
			"\n**New Features**\n\n" + \
			"\n**API Changes**\n\n" + \
			"\n**Bug Fixes**\n\n" + \
			"\n**External Contribution**\n\n"
		)
		self.assertEqual(releaseNotes.getFormattedReleaseSummary(),
			"`groupId:artifactId:version` is released." + \
			" [Version version contains these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/fromSHA..untilSHA/projectDir)\n"
		)
		self.assertEqual(releaseNotes.bugsFixed, [])

	def test_markdownCorrectlyFormatsForTwoArtifactsWithFalseRequiresSameVersion(self):
		releaseNotes = LibraryReleaseNotes(
			groupId = "groupId",
			artifactIds = ["artifactId1", "artifactId2"],
			version = "version",
			releaseDate = "01-01-1970",
			fromSHA = "fromSHA",
			untilSHA = "untilSHA",
			projectDir = "projectDir",
			requiresSameVersion = False,
			commitList = [],
			forceIncludeAllCommits = False
		)
		self.assertEqual(releaseNotes.groupId, "groupId")
		self.assertEqual(releaseNotes.artifactIds, ["artifactId1", "artifactId2"])
		self.assertEqual(releaseNotes.version, "version")
		self.assertEqual(str(releaseNotes.releaseDate), "January 1, 1970")
		self.assertEqual(releaseNotes.fromSHA, "fromSHA")
		self.assertEqual(releaseNotes.untilSHA, "untilSHA")
		self.assertEqual(releaseNotes.projectDir, "projectDir")
		self.assertEqual(releaseNotes.requiresSameVersion, False)
		self.assertEqual(releaseNotes.forceIncludeAllCommits, False)
		self.assertEqual(str(releaseNotes.header),
			"### Artifactid1 Artifactid2 Version version {:#version}")
		self.assertEqual(str(releaseNotes.diffLogLink),
			"[Version version contains" + \
			" these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/fromSHA..untilSHA/projectDir)"
		)
		self.assertEqual(releaseNotes.commits, [])
		self.assertEqual(str(releaseNotes.commitMarkdownList),
			"\n**New Features**\n\n" + \
			"\n**API Changes**\n\n" + \
			"\n**Bug Fixes**\n\n" + \
			"\n**External Contribution**\n\n"
		)
		self.assertEqual(releaseNotes.getFormattedReleaseSummary(),
			"`groupId:artifactId1:version` and `groupId:artifactId2:version` are released." + \
			" [Version version contains these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/fromSHA..untilSHA/projectDir)\n"
		)
		self.assertEqual(releaseNotes.bugsFixed, [])

	def test_markdownCorrectlyFormatsForThreeArtifactsWithFalseRequiresSameVersion(self):
		releaseNotes = LibraryReleaseNotes(
			groupId = "groupId",
			artifactIds = ["artifactId1", "artifactId2", "artifactId3"],
			version = "version",
			releaseDate = "01-01-1970",
			fromSHA = "fromSHA",
			untilSHA = "untilSHA",
			projectDir = "projectDir",
			requiresSameVersion = False,
			commitList = [],
			forceIncludeAllCommits = False
		)
		self.assertEqual(releaseNotes.groupId, "groupId")
		self.assertEqual(releaseNotes.artifactIds,
			["artifactId1", "artifactId2", "artifactId3"])
		self.assertEqual(releaseNotes.version, "version")
		self.assertEqual(str(releaseNotes.releaseDate), "January 1, 1970")
		self.assertEqual(releaseNotes.fromSHA, "fromSHA")
		self.assertEqual(releaseNotes.untilSHA, "untilSHA")
		self.assertEqual(releaseNotes.projectDir, "projectDir")
		self.assertEqual(releaseNotes.requiresSameVersion, False)
		self.assertEqual(releaseNotes.forceIncludeAllCommits, False)
		self.assertEqual(str(releaseNotes.header),
			"### Artifactid1 Artifactid2 Artifactid3 Version version {:#version}")
		self.assertEqual(str(releaseNotes.diffLogLink),
			"[Version version contains" + \
			" these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/fromSHA..untilSHA/projectDir)"
		)
		self.assertEqual(releaseNotes.commits, [])
		self.assertEqual(str(releaseNotes.commitMarkdownList),
			"\n**New Features**\n\n" + \
			"\n**API Changes**\n\n" + \
			"\n**Bug Fixes**\n\n" + \
			"\n**External Contribution**\n\n"
		)
		self.assertEqual(releaseNotes.getFormattedReleaseSummary(),
			"`groupId:artifactId1:version`, `groupId:artifactId2:version`, and " + \
			"`groupId:artifactId3:version` are released. " + \
			"[Version version contains these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/fromSHA..untilSHA/projectDir)\n"
		)
		self.assertEqual(releaseNotes.bugsFixed, [])

	def test_markdownCorrectlyFormatsForFiveArtifactsWithFalseRequiresSameVersion(self):
		releaseNotes = LibraryReleaseNotes(
			groupId = "groupId",
			artifactIds = ["artifact-Id1", "artifact-Id2", "artifact-Id3",
						   "artifact-Id4", "artifact-Id5"],
			version = "version",
			releaseDate = "01-01-1970",
			fromSHA = "fromSHA",
			untilSHA = "untilSHA",
			projectDir = "projectDir",
			requiresSameVersion = False,
			commitList = [],
			forceIncludeAllCommits = False
		)
		self.assertEqual(releaseNotes.groupId, "groupId")
		self.assertEqual(releaseNotes.artifactIds,
			["artifact-Id1", "artifact-Id2", "artifact-Id3",
			 "artifact-Id4", "artifact-Id5"])
		self.assertEqual(releaseNotes.version, "version")
		self.assertEqual(str(releaseNotes.releaseDate), "January 1, 1970")
		self.assertEqual(releaseNotes.fromSHA, "fromSHA")
		self.assertEqual(releaseNotes.untilSHA, "untilSHA")
		self.assertEqual(releaseNotes.projectDir, "projectDir")
		self.assertEqual(releaseNotes.requiresSameVersion, False)
		self.assertEqual(releaseNotes.forceIncludeAllCommits, False)
		self.assertEqual(str(releaseNotes.header),
			"### Groupid Version version {:#version}")
		self.assertEqual(str(releaseNotes.diffLogLink),
			"[Version version contains" + \
			" these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/fromSHA..untilSHA/projectDir)"
		)
		self.assertEqual(releaseNotes.commits, [])
		self.assertEqual(str(releaseNotes.commitMarkdownList),
			"\n**New Features**\n\n" + \
			"\n**API Changes**\n\n" + \
			"\n**Bug Fixes**\n\n" + \
			"\n**External Contribution**\n\n"
		)
		self.assertEqual(releaseNotes.getFormattedReleaseSummary(),
			"`groupId:artifact-*:version` is released. " + \
			"[Version version contains these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/fromSHA..untilSHA/projectDir)\n"
		)
		self.assertEqual(releaseNotes.bugsFixed, [])

	def test_markdownCorrectlyCapitalizesGroupIds(self):
		releaseNotes = LibraryReleaseNotes(
			groupId = "androidx.recyclerview",
			artifactIds = ["recyclerview"],
			version = "version",
			releaseDate = "01-01-1970",
			fromSHA = "fromSHA",
			untilSHA = "untilSHA",
			projectDir = "projectDir",
			requiresSameVersion = False,
			commitList = [],
			forceIncludeAllCommits = False
		)
		self.assertEqual(releaseNotes.groupId, "androidx.recyclerview")
		self.assertEqual(str(releaseNotes.header),
			"### RecyclerView Version version {:#recyclerview-version}")
		self.assertEqual(releaseNotes.getFormattedReleaseSummary(),
			"`androidx.recyclerview:recyclerview:version` is released. " + \
			"[Version version contains these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/fromSHA..untilSHA/projectDir)\n"
		)

		releaseNotes = LibraryReleaseNotes(
			groupId = "androidx.swiperefreshlayout",
			artifactIds = ["swiperefreshlayout"],
			version = "version",
			releaseDate = "01-01-1970",
			fromSHA = "fromSHA",
			untilSHA = "untilSHA",
			projectDir = "projectDir",
			requiresSameVersion = True,
			commitList = [],
			forceIncludeAllCommits = False
		)
		self.assertEqual(releaseNotes.groupId, "androidx.swiperefreshlayout")
		self.assertEqual(str(releaseNotes.header),
			"### SwipeRefreshLayout Version version {:#version}")
		self.assertEqual(releaseNotes.getFormattedReleaseSummary(),
			"`androidx.swiperefreshlayout:swiperefreshlayout:version` is released. " + \
			"[Version version contains these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/fromSHA..untilSHA/projectDir)\n"
		)

	def test_markdownCorrectlyFormatsGittilesLinkWithNoFromSHA(self):
		releaseNotes = LibraryReleaseNotes(
			groupId = "groupId",
			artifactIds = ["artifactId"],
			version = "version",
			releaseDate = "01-01-1970",
			fromSHA = "",
			untilSHA = "untilSHA",
			projectDir = "projectDir",
			requiresSameVersion = True,
			commitList = [],
			forceIncludeAllCommits = False
		)
		self.assertEqual(releaseNotes.fromSHA, "")
		self.assertEqual(releaseNotes.untilSHA, "untilSHA")
		self.assertEqual(releaseNotes.projectDir, "projectDir")
		self.assertEqual(str(releaseNotes.diffLogLink),
			"[Version version contains" + \
			" these commits.](https://android.googlesource.com/" + \
			"platform/frameworks/support/+log/untilSHA/projectDir)"
		)

if __name__ == '__main__':
	unittest.main()