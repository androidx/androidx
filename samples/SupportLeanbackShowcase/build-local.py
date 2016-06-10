# BUILDING SupportLeanbackShowcase app using local library.
import sys
import subprocess
import os
import fileinput
import re

# Does an inplace substitution of the pattern with newVal in inputFile
def replace(inputFile, pattern, newVal, ):
  print 'About to replace repo path to {0} in {1}'.format(newVal, inputFile)
  replaced = False
  if os.path.exists(inputFile):
    for line in fileinput.input(inputFile, inplace = 1):
      if re.match(pattern, line, re.I|re.M):
        line = re.sub(pattern, newVal, line)
        replaced = True
      print line,

  if not replaced:
    file = open(inputFile, "a")
    file.write(newVal + "\n")

# Finds the local leanback library version based on leanback-v17/maven-metadata.xml
def lookup_local_library_version(repo_path):
  leanback_maven_metadata_path = repo_path + "/out/host/gradle/frameworks/support/build/support_repo/com/android/support/leanback-v17/maven-metadata.xml"
  if not os.path.exists(leanback_maven_metadata_path):
    print "ERROR: Missing leanback-v17 library {} in local repo".format(leanback_maven_metadata_path)

  file = open(leanback_maven_metadata_path, "r")
  for line in file:
    matchObj = re.match(r'\s*<version>(.*)</version>', line)
    if matchObj:
      return matchObj.group(1).strip(' \t\n\r')

# Get repo path
current_path = os.getcwd()
index = current_path.find("frameworks/support/samples/SupportLeanbackShowcase")
if index < 0:
  print "ERROR: Invalid repo {0}".format(current_path)
  exit(0)

repo_path = current_path[:index]
support_frameworks_path = repo_path + "/frameworks/support"
if not (os.path.isdir(repo_path) or os.path.isdir(support_frameworks_path)):
  print 'ERROR : Repo "{0}" does not exist'.format(repo_path)
  print 'Please run gradlew uploadArchives inside frameworks/support'
  exit(0)

# Substitute LIBRARY_VERSION/LOCAL_REPO in local.properties
library_version = lookup_local_library_version(repo_path)
replace(os.getcwd()+"/local.properties", r'(.*)LOCAL_REPO(.*)', 'LOCAL_REPO='+repo_path)
replace(os.getcwd()+"/local.properties", r'(.*)LIBRARY_VERSION(.*)', 'LIBRARY_VERSION='+library_version)

# Build
print "Building SupportLeanbackShowcase app..."
subprocess.call(["./gradlew", "assembleDebug"])

#Install apk
print "Installing SupportLeanbackShowcase..."
subprocess.call(["adb", "install", "-r", "./app/build/outputs/apk/app-debug.apk"])

print "Finished installing SupportLeanbackShowcase app."

