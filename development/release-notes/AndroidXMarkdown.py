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

from ReleaseNoteMarkdown import *
from GitClient import CommitType, getTitleFromCommitType

class CommitMarkdownList:
	"""Generates the markdown list of commits with sections defined by enum [CommitType], in the format:

		**New Features**
		- <[Commit.summary]> <[getChangeIdAOSPLink]> <[getBuganizerLink] 1> <[getBuganizerLink] 2>...
		**API Changes**
		- <[Commit.summary]> <[getChangeIdAOSPLink]> <[getBuganizerLink] 1> <[getBuganizerLink] 2>...
		**Bug Fixes**
		- <[Commit.summary]> <[getChangeIdAOSPLink]> <[getBuganizerLink] 1> <[getBuganizerLink] 2>...
		**External Contribution**
		- <[Commit.summary]> <[getChangeIdAOSPLink]> <[getBuganizerLink] 1> <[getBuganizerLink] 2>...
	"""
	def __init__(self, commits=[], forceIncludeAllCommits=False):
		self.forceIncludeAllCommits = forceIncludeAllCommits
		self.commits = commits

	def add(self, commit):
		self.commits.append(commit)

	def getListItemStr(self):
		return "- "

	def makeReleaseNotesSection(self, sectionCommitType):
		sectionHeader = MarkdownBoldText(getTitleFromCommitType(sectionCommitType))
		markdownStringSection = ""
		for commit in self.commits:
			if commit.changeType != sectionCommitType: continue
			if commit.releaseNote != "":
				commitString = self.getListItemStr() + commit.getReleaseNoteString()
			else:
				commitString = self.getListItemStr() + str(commit)
			if self.forceIncludeAllCommits or commit.releaseNote != "":
				markdownStringSection = markdownStringSection + commitString
				if markdownStringSection[-1] != '\n':
					markdownStringSection += '\n'
		markdownStringSection = "\n%s\n\n%s" % (sectionHeader, markdownStringSection)
		return markdownStringSection

	def __str__(self):
		markdownString = ""
		for commitType in CommitType:
			markdownString += self.makeReleaseNotesSection(commitType)
		return markdownString

class GitilesDiffLogLink(MarkdownLink):
    def __str__(self):
        return "[%s](%s)" % (self.linkText, self.linkUrl)

def getGitilesDiffLogLink(version, startSHA, endSHA, projectDir):
	"""
	@param startSHA the SHA at which to start the diff log (exclusive)
	@param endSHA the last SHA to include in the diff log (inclusive)
	@param projectDir the local directory of the project, in relation to frameworks/support

	@return A [MarkdownLink] to the public Gitiles diff log
	"""
	baseGitilesUrl = "https://android.googlesource.com/platform/frameworks/support/+log/"
	# The root project directory is already existent in the url path, so the directory here
	# should be relative to frameworks/support/.
	if ("frameworks/support" in projectDir):
		print_e("Gitiles directory should be relative to frameworks/support; received incorrect directory: $projectDir")
		exit(1)
	if startSHA != "":
		return GitilesDiffLogLink("Version %s contains these commits." % version, "%s%s..%s/%s" % (baseGitilesUrl, startSHA, endSHA, projectDir))
	else:
		return GitilesDiffLogLink("Version %s contains these commits." % version, "%s%s/%s" % (baseGitilesUrl, endSHA, projectDir))

class LibraryHeader(MarkdownHeader):
	"""
	Markdown class for a Library Header in the format:

	### Version <version> {:#<artifactIdTag-version>}

	An artifactId tag is required because artifactIds may be can be grouped by version, in which case the tag is not obvious
	"""
	def __init__(self, groupId="", version="", artifactIdTag=""):
		self.markdownType = HeaderType.H3
		self.text = "%s Version %s {:#%s%s}" % (groupId, version, artifactIdTag, version)

class LibraryReleaseNotes:
	""" Structured release notes class, that connects all parts of the release notes.  Creates release
	notes in the format:
		<pre>
		<[LibraryHeader]>
		<Date>

		`androidx.<groupId>:<artifactId>:<version>` is released. The commits included in this version
		can be found <[MarkdownLink]>.

		 <[CommitMarkdownList]>
		</pre>
	"""
	def __init__(self, groupId, artifactIds, version, releaseDate, fromSHA, untilSHA, projectDir, requiresSameVersion, commitList=[], forceIncludeAllCommits=False):
		"""
		@property groupId Library GroupId.
		@property artifactIds List of ArtifactIds included in these release notes.
		@property version Version of the library, assuming all artifactIds have the same version.
		@property releaseDate Date the release will go live.  Defaults to the current date.
		@property fromSHA The oldest SHA to which to query for release notes.  It will be
					excluded from release notes, but the next newer SHA will be included.
		@property untilSHA The newest SHA to be included in the release notes.
		@property projectDir The filepath relative to the parent directory of the .git directory.
		@property requiresSameVersion True if the groupId of this module requires the same version for
				  all artifactIds in the groupId.  When true, uses the GroupId for the release notes
				  header.  When false, uses the list of artifactIds for the header.
		@property commitList The initial list of Commits to include in these release notes.  Defaults to an
				  empty list.  Users can always add more commits with [LibraryReleaseNotes.addCommit]
		@param forceIncludeAllCommits Set to true to include all commits, both with and without a
				 release note field in the commit message.  Defaults to false, which means only commits
				 with a release note field are included in the release notes.
		"""
		self.groupId = groupId
		self.artifactIds = artifactIds
		self.version = version
		self.releaseDate = MarkdownDate(releaseDate)
		self.fromSHA = fromSHA
		self.untilSHA = untilSHA
		self.projectDir = projectDir
		self.commitList = commitList
		self.requiresSameVersion = requiresSameVersion
		self.forceIncludeAllCommits = forceIncludeAllCommits
		self.diffLogLink = MarkdownLink()
		self.commits = commitList
		self.commitMarkdownList = CommitMarkdownList(commitList, forceIncludeAllCommits)
		self.summary = ""
		self.bugsFixed = []

		if version == "" or groupId == "":
			raise RuntimeError("Tried to create Library Release Notes Header without setting " +
					"the groupId or version!")
		if requiresSameVersion:
			formattedGroupId = groupId.replace("androidx.", "")
			formattedGroupId = self.capitalizeTitle(formattedGroupId)
			self.header = LibraryHeader(formattedGroupId, version)
		else:
			artifactIdTag = artifactIds[0] + "-" if len(artifactIds) == 1 else ""
			formattedArtifactIds = (" ".join(artifactIds))
			if len(artifactIds) > 3:
				formattedArtifactIds = groupId.replace("androidx.", "")
			formattedArtifactIds = self.capitalizeTitle(formattedArtifactIds)
			self.header = LibraryHeader(formattedArtifactIds, version, artifactIdTag)
		self.diffLogLink = getGitilesDiffLogLink(version, fromSHA, untilSHA, projectDir)

	def getFormattedReleaseSummary(self):
		numberArtifacts = len(self.artifactIds)
		for i in range(0, numberArtifacts):
			currentArtifactId = self.artifactIds[i]
			if numberArtifacts == 1:
				self.summary = "`%s:%s:%s` is released. " % (self.groupId, currentArtifactId, self.version)
			elif numberArtifacts == 2:
				if i == 0: self.summary = "`%s:%s:%s` and " % (self.groupId, currentArtifactId, self.version)
				if i == 1: self.summary += "`%s:%s:%s` are released. " % (self.groupId, currentArtifactId, self.version)
			elif numberArtifacts == 3:
				if (i < numberArtifacts - 1):
					self.summary += "`%s:%s:%s`, " % (self.groupId, currentArtifactId, self.version)
				else:
					self.summary += "and `%s:%s:%s` are released. " % (self.groupId, currentArtifactId, self.version)
			else:
				commonArtifactIdSubstring = self.artifactIds[0].split('-')[0]
				self.summary = "`%s:%s-*:%s` is released. " % (
					self.groupId,
					commonArtifactIdSubstring,
					self.version
				)
		self.summary += "%s\n" % self.diffLogLink
		return self.summary

	def capitalizeTitle(self, artifactWord):
		artifactWord = artifactWord.title()
		keywords = ["Animated", "Animation", "Callback", "Compat", "Drawable", "File", "Layout",
					"Pager", "Pane", "Parcelable", "Provider", "Refresh", "SQL", "State", "TV",
					"Target", "View", "Inflater"]
		for keyword in keywords:
			artifactWord = artifactWord.replace(keyword.lower(), keyword)
		return artifactWord

	def addCommit(self, newCommit):
		for bug in newCommit.bugs:
			bugsFixed.append(bug)
		commits.append(newCommit)
		commitMarkdownList.add(newCommit)

	def __str__(self):
		return "%s\n%s\n\n%s%s" % (self.header, self.releaseDate, self.getFormattedReleaseSummary(), self.commitMarkdownList)
