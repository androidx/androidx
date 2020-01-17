#!/usr/bin/python3

import sys
import os
import argparse
import subprocess
import json
import datetime

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
		raw_jetpad_release_output = subprocess.check_output('span sql /span/global/androidx-jetpad:prod_instance \"SELECT GroupId, ArtifactId, ReleaseVersion, PreviousReleaseSHA, ReleaseSHA, RequireSameVersionGroupBuild FROM LibraryReleases WHERE ReleaseDate = %s\"' % date, shell=True)
	except subprocess.CalledProcessError:
		print_e('FAIL: Failed to get jetpad release info for  %s' %  date)
		return None
	raw_jetpad_release_output_lines = raw_jetpad_release_output.splitlines()
	if len(raw_jetpad_release_output_lines) <= 2:
		print_e("Error: Date %s returned zero results from Jetpad.  Please check your date" % args.date)
		return None
	jetpad_release_output = iter(raw_jetpad_release_output_lines)
	return jetpad_release_output

def generate_release_json_file(date, include_all_commits, jetpad_release_info):
	releaseDateTime = datetime.datetime.fromtimestamp(float(date)/1000.0)
	release_json_object = {}
	release_json_object["releaseDate"] = "%02d-%02d-%s" % (releaseDateTime.month, releaseDateTime.day, releaseDateTime.year)
	release_json_object["includeAllCommits"] = include_all_commits
	release_json_object["modules"] = []
	for line in jetpad_release_info:
		if not ("androidx" in line.decode()): continue
		# Remove all white space and split line based on '|'
		artifactId_release_line = line.decode().replace(" ", "").split('|')
		groupId = artifactId_release_line[1]
		artifactId = artifactId_release_line[2]
		version = artifactId_release_line[3]
		fromSHA = artifactId_release_line[4]
		untilSHA = artifactId_release_line[5]
		requiresSameVersion = False
		if artifactId_release_line[6] == "true":
			requiresSameVersion = True
		release_json_object['modules'].append({
			"groupId": groupId,
			"artifactId": artifactId,
			"version": artifactId,
			"fromCommit": fromSHA,
			"untilCommit": untilSHA,
			"requiresSameVersion": requiresSameVersion
		})

	# Serialize the json release_json_object into a json file for reading from gradle
	output_json_filename = "release_info_%s.json" % date
	with open(output_json_filename, 'w') as f:
		json.dump(release_json_object, f)
		output_json_filepath = os.path.abspath(f.name)
	return output_json_filepath

def run_release_notes_command(output_json_filepath):
	try:
		subprocess.check_call('cd ../.. && ./gradlew generateReleaseNotes -PartifactToCommitMap=%s' % output_json_filepath, shell=True)
	except subprocess.CalledProcessError:
		print_e('FAIL: Failed to run the gradle task generateReleaseNotes for file %s' %  output_json_filepath)
		return False
	print("Success! Release notes have been generated at ../../../../out/dist/release-notes/androidx_aggregate_release_notes.txt \n")
	return True

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
	print("Generating the release information json file...")
	output_json_filepath = generate_release_json_file(args.date, args.include_all_commits, jetpad_release_info)
	print("Successful")
	print("This will take a while...")
	print('cd ../.. && ./gradlew generateReleaseNotes -PartifactToCommitMap=%s' % output_json_filepath)
	if not run_release_notes_command(output_json_filepath):
		rm(output_json_filepath)
		exit(1)
	rm(output_json_filepath)

main(sys.argv)
