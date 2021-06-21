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

import subprocess
import datetime

def getJetpadReleaseInfo(date):
	try:
		rawJetpadReleaseOutput = subprocess.check_output('span sql /span/global/androidx-jetpad:prod_instance \"SELECT GroupId, ArtifactId, ReleaseVersion, PreviousReleaseSHA, ReleaseSHA, Path, RequireSameVersionGroupBuild, ReleaseBuildId, ReleaseBranch FROM LibraryReleases WHERE ReleaseDate = %s\"' % date, shell=True)
	except subprocess.CalledProcessError:
		print_e('FAIL: Failed to get jetpad release info for  %s' %  date)
		return None
	rawJetpadReleaseOutputLines = rawJetpadReleaseOutput.splitlines()
	if len(rawJetpadReleaseOutputLines) <= 2:
		print_e("Error: Date %s returned zero results from Jetpad.  Please check your date" % args.date)
		return None
	jetpadReleaseOutput = iter(rawJetpadReleaseOutputLines)
	return jetpadReleaseOutput

def getReleaseInfoObject(date, includeAllCommits, jetpadReleaseInfo):
	releaseDateTime = datetime.datetime.fromtimestamp(float(date)/1000.0)
	releaseJsonObject = {}
	releaseJsonObject["releaseDate"] = "%02d-%02d-%s" % (releaseDateTime.month, releaseDateTime.day, releaseDateTime.year)
	releaseJsonObject["includeAllCommits"] = includeAllCommits
	releaseJsonObject["modules"] = {}
	for line in jetpadReleaseInfo:
		if "androidx" not in line.decode(): continue
		# Remove all white space and split line based on '|'
		artifactIdReleaseLine = line.decode().replace(" ", "").split('|')
		groupId = artifactIdReleaseLine[1]
		artifactId = artifactIdReleaseLine[2]
		version = artifactIdReleaseLine[3]
		fromSHA = artifactIdReleaseLine[4]
		untilSHA = artifactIdReleaseLine[5]
		path = artifactIdReleaseLine[6]
		if path and path[0] == '/': path = path[1:]
		requiresSameVersion = False
		if artifactIdReleaseLine[7] == "true":
			requiresSameVersion = True
		buildId = artifactIdReleaseLine[8]
		branch = artifactIdReleaseLine[9]
		if groupId in releaseJsonObject["modules"]:
			releaseJsonObject["modules"][groupId].append({
				"groupId": groupId,
				"artifactId": artifactId,
				"version": version,
				"fromSHA": fromSHA,
				"untilSHA": untilSHA,
				"requiresSameVersion": requiresSameVersion,
				"path": path,
				"buildId": buildId,
				"branch": branch,
			})
		else:
			releaseJsonObject["modules"][groupId] = [{
				"groupId": groupId,
				"artifactId": artifactId,
				"version": version,
				"fromSHA": fromSHA,
				"untilSHA": untilSHA,
				"requiresSameVersion": requiresSameVersion,
				"path": path,
				"buildId": buildId,
				"branch": branch,
			}]
	return releaseJsonObject

def getJetpadRelease(date, includeAllCommits):
	print("Getting the release info from Jetpad...")
	jetpadReleaseInfo = getJetpadReleaseInfo(date)
	if not jetpadReleaseInfo:
		exit(1)
	print("Successful")
	return getReleaseInfoObject(date, includeAllCommits, jetpadReleaseInfo)

