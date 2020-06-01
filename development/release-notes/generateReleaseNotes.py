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

def get_jetpad_release_info(date):
	try:
		raw_jetpad_release_output = subprocess.check_output('span sql /span/global/androidx-jetpad:prod_instance \"SELECT GroupId, ArtifactId, ReleaseVersion, PreviousReleaseSHA, ReleaseSHA, Path, RequireSameVersionGroupBuild FROM LibraryReleases WHERE ReleaseDate = %s\"' % date, shell=True)
	except subprocess.CalledProcessError:
		print_e('FAIL: Failed to get jetpad release info for  %s' %  date)
		return None
	raw_jetpad_release_output_lines = raw_jetpad_release_output.splitlines()
	if len(raw_jetpad_release_output_lines) <= 2:
		print_e("Error: Date %s returned zero results from Jetpad.  Please check your date" % args.date)
		return None
	jetpad_release_output = iter(raw_jetpad_release_output_lines)
	return jetpad_release_output

def get_release_note_object(date, include_all_commits, jetpad_release_info):
	releaseDateTime = datetime.datetime.fromtimestamp(float(date)/1000.0)
	release_json_object = {}
	release_json_object["releaseDate"] = "%02d-%02d-%s" % (releaseDateTime.month, releaseDateTime.day, releaseDateTime.year)
	release_json_object["includeAllCommits"] = include_all_commits
	release_json_object["modules"] = {}
	for line in jetpad_release_info:
		if "androidx" not in line.decode(): continue
		# Remove all white space and split line based on '|'
		artifactId_release_line = line.decode().replace(" ", "").split('|')
		groupId = artifactId_release_line[1]
		artifactId = artifactId_release_line[2]
		version = artifactId_release_line[3]
		fromSHA = artifactId_release_line[4]
		untilSHA = artifactId_release_line[5]
		path = artifactId_release_line[6]
		if path[0] == '/': path = path[1:]
		requiresSameVersion = False
		if artifactId_release_line[7] == "true":
			requiresSameVersion = True
		if groupId in release_json_object["modules"]:
			release_json_object["modules"][groupId].append({
				"groupId": groupId,
				"artifactId": artifactId,
				"version": version,
				"fromSHA": fromSHA,
				"untilSHA": untilSHA,
				"requiresSameVersion": requiresSameVersion,
				"path": path
			})
		else:
			release_json_object["modules"][groupId] = [{
				"groupId": groupId,
				"artifactId": artifactId,
				"version": version,
				"fromSHA": fromSHA,
				"untilSHA": untilSHA,
				"requiresSameVersion": requiresSameVersion,
				"path": path
			}]
	return release_json_object

def generate_release_json_file(date, include_all_commits, jetpad_release_info):
	release_json_object = get_release_note_object(date, include_all_commits, jetpad_release_info)
	# Serialize the json release_json_object into a json file for reading from gradle
	output_json_filename = "release_info_%s.json" % date
	with open(output_json_filename, 'w') as f:
		json.dump(release_json_object, f)
		output_json_filepath = os.path.abspath(f.name)
	return output_json_filepath


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

def writeArtifactIdReleaseNotesToFile(groupId, artifactId, version, releaseNotesString, outputDir):
	releaseNotesFileName = "%s_%s_%s_release_notes.txt" % (groupId, artifactId, version)
	groupIdDir = "%s/%s" % (outputDir, groupId)
	writeReleaseNotesToNewFile(groupIdDir, releaseNotesFileName, releaseNotesString)

def writeGroupIdReleaseNotesToFile(groupId, releaseNotesString, outputDir):
	releaseNotesFileName = "%s_release_notes.txt" % (groupId)
	groupIdDir = "%s/%s" % (outputDir, groupId)
	writeReleaseNotesToNewFile(groupIdDir, releaseNotesFileName, releaseNotesString)

def writeReleaseNotesToNewFile(groupIdDir, releaseNotesFileName, releaseNotesString):
	if not os.path.exists(groupIdDir):
		os.makedirs(groupIdDir)
	fullReleaseNotesFilePath = "%s/%s" % (groupIdDir, releaseNotesFileName)
	with open(fullReleaseNotesFilePath, 'w') as f:
		f.write(releaseNotesString)

def generateAllReleaseNotes(releaseDate, include_all_commits, jetpad_release_info, outputDir):
	""" Creates all the release notes.  Creates each individual artifactId release notes, each
		individual groupId release notes, then creates an aggregrate release notes file that
		contains all of the groupId release Notes
		@param releaseDate The release date of the entire release
		@param includeAllCommits Set to true to include all commits regardless of whether or not they
				 have the release notes tag
		@param jetpad_release_info The raw output of information from Jetpad
	"""
	gitClient = GitClient(os.getcwd())
	releaseJsonObject = get_release_note_object(releaseDate, include_all_commits, jetpad_release_info)
	allReleaseNotes = ""
	for groupId in releaseJsonObject["modules"]:
		allReleaseNotes += "\n\n" + generateGroupIdReleaseNotes(gitClient, releaseJsonObject, groupId, outputDir)
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

	completeGroupIdReleaseNotes = "\n\n".join((groupReleaseNotesStringList))
	writeGroupIdReleaseNotesToFile(
		groupId,
		completeGroupIdReleaseNotes,
		outputDir
	)
	return completeGroupIdReleaseNotes


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
		outputDir
	)
	return artifactIdReleaseNotes

def main(args):
	# Parse arguments and check for existence of build ID or file
	args = parser.parse_args()
	if not args.date:
		parser.error("You must specify a release date in Milliseconds since epoch")
		sys.exit(1)
	print("Getting the release info from Jetpad...")
	jetpad_release_info = get_jetpad_release_info(args.date)
	if not jetpad_release_info:
		exit(1)
	print("Successful")
	print("Creating release notes...")
	outputDir = "./out"
	# Remove the local output dir so that leftover release notes from the previous run are removed
	rm(outputDir)
	generateAllReleaseNotes(args.date, args.include_all_commits, jetpad_release_info, outputDir)
	print("Successful.")
	print("Release notes have been written to %s" % outputDir)

if __name__ == '__main__':
	main(sys.argv)
