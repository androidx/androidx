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

# Substitute LIBRARY_VERSION/LOCAL_REPO in local.properties
# It will use default values in build.gradle
replace(os.getcwd()+"/local.properties", r'(.*)LOCAL_REPO(.*)', 'LOCAL_REPO=')
replace(os.getcwd()+"/local.properties", r'(.*)LIBRARY_VERSION(.*)', 'LIBRARY_VERSION=')

# Build
print "Building SupportLeanbackShowcase app..."
subprocess.call(["./gradlew", "assembleDebug"])

#Install apk
print "Installing SupportLeanbackShowcase..."
subprocess.call(["adb", "install", "-r", "./app/build/outputs/apk/app-debug.apk"])

print "Finished installing SupportLeanbackShowcase app."

