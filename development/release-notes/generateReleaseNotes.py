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

from GitClient import *
from ReleaseNoteMarkdown import *
from AndroidXMarkdown import LibraryReleaseNotes

import sys
import os
import argparse
import subprocess
import json
import datetime
from shutil import rmtree

# This script is meant as a drop in replacement until we have git tags implemented in androidx
# See b/147606199
#

# Import the JetpadClient from the parent directory
sys.path.append("..")
from JetpadClient import *

# cd into directory of script
os.chdir(os.path.dirname(os.path.abspath(__file__)))

# Set up input arguments
parser = argparse.ArgumentParser(
	description=("""Genereates AndroidX Release Notes for a given date.  This script takes in a the release date as millisecond since the epoch,
		which is the unique id for the release in Jetpad.  It queries the Jetpad db, then creates an output json file with the release information.
		Finally, it passes that json file to the gradle generateReleaseNotes task, which actually produces the release notes.
		See the ArtifactToCommitMap.kt file in the buildSrc directory for the Kotlin class that is getting serialized here."""))
parser.add_argument(
	'date',
	help='Milliseconds since epoch')
parser.add_argument(
	'--include-all-commits', action="store_true",
	help='If specified, includes all commits in the release notes regardless of the release note tag')

def print_e(*args, **kwargs):
	print(*args, file=sys.stderr, **kwargs)

def rm(path):
	if os.path.isdir(path):
		rmtree(path)
	elif os.path.exists(path):
		os.remove(path)

def isExcludedAuthorEmail(authorEmail):
	""" Check if an email address is a robot
		@param authorEmail email to check
	"""
	excludedAuthorEmails = {
		"treehugger-gerrit@google.com",
		"android-build-merger@google.com",
		"noreply-gerritcodereview@google.com"
	}
	return authorEmail in excludedAuthorEmails

def getVersionToReleaseNotesMap(releaseJsonObject, groupId):
	""" Iterates over the LibraryReleaseNotes list and creates a map from project.version to List of
		LibraryReleaseNotes. Thus, each artifactId of the same version will be collected together
		as list for that version. This is done so that release notes can be collected for all
		artifactIds of the same version.

		@param releaseJsonObject The json object containing all information about the release
		@param groupId the groupId to generate this mapping for
	"""
	versionToArtifactRNMap = {}
	for artifact in releaseJsonObject["modules"][groupId]:
		if artifact["version"] in versionToArtifactRNMap:
			versionToArtifactRNMap[artifact["version"]].append(artifact)
		else:
			versionToArtifactRNMap[artifact["version"]] = [artifact]
	return versionToArtifactRNMap

def mergeCommitListBIntoCommitListA(commitListA, commitListB):
	""" Merges CommitListB into CommitListA and removes duplicates.
	"""
	commitListAShaSet = set([])
	for commitA in commitListA:
		commitListAShaSet.add(commitA.sha)
	for commitB in commitListB:
		if commitB.sha not in commitListAShaSet:
			commitListA.append(commitB)

def getCommonPathPrefix(pathA, pathB):
	pathAList = pathA.split('/')
	pathBList = pathB.split('/')

	stringAPathLen = len(pathAList)
	stringBPathLen = len(pathBList)
	lastCommonIndex = 0
	for i in range(0, stringAPathLen):
		if i < stringBPathLen and pathAList[i] == pathBList[i]:
			lastCommonIndex = i
	return "/".join(pathAList[:lastCommonIndex + 1])

def writeArtifactIdReleaseNotesToFile(groupId, artifactId, version, releaseNotesString, channelSummary, outputDir):
	releaseNotesFileName = "%s_%s_%s_release_notes.txt" % (groupId, artifactId, version)
	groupIdDir = "%s/%s" % (outputDir, groupId)
	writeReleaseNotesToNewFile(groupIdDir, releaseNotesFileName, channelSummary + "\n" + releaseNotesString)

def writeGroupIdReleaseNotesToFile(groupId, releaseNotesString, channelSummary, outputDir):
	releaseNotesFileName = "%s_release_notes.txt" % (groupId)
	groupIdDir = "%s/%s" % (outputDir, groupId)
	writeReleaseNotesToNewFile(groupIdDir, releaseNotesFileName, channelSummary + "\n" + releaseNotesString)

def writeReleaseNotesToNewFile(groupIdDir, releaseNotesFileName, releaseNotesString):
	if not os.path.exists(groupIdDir):
		os.makedirs(groupIdDir)
	fullReleaseNotesFilePath = "%s/%s" % (groupIdDir, releaseNotesFileName)
	with open(fullReleaseNotesFilePath, 'w') as f:
		f.write(releaseNotesString)

def generateAllReleaseNotes(releaseDate, include_all_commits, outputDir):
	""" Creates all the release notes.  Creates each individual artifactId release notes, each
		individual groupId release notes, then creates an aggregrate release notes file that
		contains all of the groupId release Notes
		@param releaseDate The release date of the entire release
		@param includeAllCommits Set to true to include all commits regardless of whether or not they
				 have the release notes tag
	"""
	gitClient = GitClient(os.getcwd())
	releaseJsonObject = getJetpadRelease(releaseDate, include_all_commits)
	print("Creating release notes...")
	allReleaseNotes = ""
	allReleaseNotesSummary = ""
	for groupId in releaseJsonObject["modules"]:
		groupReleaseNotes, groupReleaseNotesSummary = generateGroupIdReleaseNotes(gitClient, releaseJsonObject, groupId, outputDir)
		allReleaseNotes += "\n\n" + groupReleaseNotes
		allReleaseNotesSummary += groupReleaseNotesSummary
	formattedReleaseDate = str(MarkdownHeader(
			HeaderType.H3,
			str(MarkdownDate(releaseJsonObject["releaseDate"])))
		) + "\n"
	allReleaseNotesSummary = formattedReleaseDate + allReleaseNotesSummary
	allReleaseNotes = allReleaseNotesSummary + "\n" + allReleaseNotes
	writeReleaseNotesToNewFile(outputDir, "all_androidx_release_notes.txt", allReleaseNotes)

def generateGroupIdReleaseNotes(gitClient, releaseJsonObject, groupId, outputDir):
	""" Creates the groupId release notes using the list of artifactId LibraryReleaseNotes
		Groups artifactIds of the same version.

		@param libraryReleaseNotesList The list of artifactId [LibraryReleaseNotes] objects which
				 are read in from the artifactId release note .json files
		@param releaseDate The release date of the entire release
		@param includeAllCommits Set to true to include all commits regardless of whether or not they
				 have the release notes tag
	"""
	versionToArtifactRNMap = getVersionToReleaseNotesMap(releaseJsonObject, groupId)

	groupReleaseNotesStringList = []
	groupReleaseNotesSummaryList = []
	# For each version, collect and write the release notes for all artifactIds of that version
	for (version, versionRNList) in versionToArtifactRNMap.items():
		versionArtifactIds = []
		versionGroupCommitList = []
		fromSHA = ""
		untilSHA = ""
		groupIdCommonDir = versionRNList[0]["path"]
		requiresSameVersion = versionRNList[0]["requiresSameVersion"]
		for artifact in versionRNList:
			versionArtifactIds.append(artifact["artifactId"])
			## Get and merge commits lists
			artifactIdReleaseNotes = generateArtifactIdReleaseNotes(gitClient, artifact, releaseJsonObject["releaseDate"], releaseJsonObject["includeAllCommits"], outputDir)
			mergeCommitListBIntoCommitListA(
				versionGroupCommitList,
				artifactIdReleaseNotes.commitList
			)
			fromSHA = artifact["fromSHA"]
			untilSHA = artifact["untilSHA"]
			groupIdCommonDir = getCommonPathPrefix(groupIdCommonDir, artifact["path"])
		for commit in versionGroupCommitList:
			if isExcludedAuthorEmail(commit.authorEmail):
				versionGroupCommitList.remove(commit)

		releaseNotes = LibraryReleaseNotes(
			groupId,
			versionArtifactIds,
			version,
			releaseJsonObject["releaseDate"],
			fromSHA if (fromSHA != "NULL") else "",
			untilSHA,
			groupIdCommonDir,
			requiresSameVersion,
			versionGroupCommitList,
			releaseJsonObject["includeAllCommits"]
		)

		groupReleaseNotesStringList.append(str(releaseNotes))
		groupReleaseNotesSummaryList.append(str(releaseNotes.channelSummary))

	completeGroupIdReleaseNotes = "\n\n".join((groupReleaseNotesStringList))
	completeGroupReleaseNotesSummary = "".join(groupReleaseNotesSummaryList)
	writeGroupIdReleaseNotesToFile(
		groupId,
		completeGroupIdReleaseNotes,
		completeGroupReleaseNotesSummary,
		outputDir
	)
	return completeGroupIdReleaseNotes, completeGroupReleaseNotesSummary


def generateArtifactIdReleaseNotes(gitClient, artifact, releaseDate, includeAllCommits, outputDir):
	# If there are is no fromCommit specified for this artifact, then simply return because
	# we don't know how far back to query the commit log
	fromSHA = artifact["fromSHA"]
	if fromSHA == "NULL":
		fromSHA = ""

	untilSHA = artifact["untilSHA"]
	if untilSHA == "NULL" or untilSHA == "":
		untilSHA = "HEAD"

	commitList = gitClient.getGitLog(
		fromExclusiveSha = fromSHA,
		untilInclusiveSha = untilSHA,
		keepMerges = False,
		subProjectDir = artifact["path"]
	)

	if len(commitList) == 0:
		print_e("WARNING: Found no commits for %s:%s from " % (artifact["groupId"], artifact["artifactId"]) + \
				"start SHA %s to end SHA %s.  To double check, you can run " % (fromSHA, untilSHA) + \
				"`git log --no-merges %s..%s -- %s` " % (fromSHA, untilSHA, artifact["path"]) + \
				"in the root git directory")

	for commit in commitList:
		if isExcludedAuthorEmail(commit.authorEmail):
			commitList.remove(commit)

	artifactIdReleaseNotes = LibraryReleaseNotes(
		artifact["groupId"],
		[artifact["artifactId"]],
		artifact["version"],
		releaseDate,
		fromSHA,
		untilSHA,
		artifact["path"],
		False,
		commitList,
		includeAllCommits
	)
	writeArtifactIdReleaseNotesToFile(
		artifact["groupId"],
		artifact["artifactId"],
		artifact["version"],
		str(artifactIdReleaseNotes),
		str(artifactIdReleaseNotes.channelSummary),
		outputDir
	)
	return artifactIdReleaseNotes

def main(args):
	# Parse arguments and check for existence of build ID or file
	args = parser.parse_args()
	if not args.date:
		parser.error("You must specify a release date in Milliseconds since epoch")
		sys.exit(1)
	outputDir = "./out"
	# Remove the local output dir so that leftover release notes from the previous run are removed
	rm(outputDir)
	generateAllReleaseNotes(args.date, args.include_all_commits, outputDir)
	print("Successful.")
	print("Release notes have been written to %s" % outputDir)

if __name__ == '__main__':
	main(sys.argv)
