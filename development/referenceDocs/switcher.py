#!/usr/bin/env python
#
# Copyright 2019 - The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

###############################################################################
# This script adds a HTML snippet to the generated reference docs located at
# developer.android.com/reference.  The snippet renders HTML that adds links to
# toggle between the Java and Kotlin versions of the page.
###############################################################################

import getopt
import os
import sys


# GLOBAL FLAGS

global stubs
global java_stubs, kotlin_stubs
global work, verbose, show_solo, max_stubs
global java_source_abs_path
global kotlin_source_abs_path

verbose = False  # set True to list all files as they are stubbed (--verbose)
work = False  # set True to insert stubs, False to do a dry run for stats (--work)
show_solo = False  # set True to list files that only appear in one language, rather than both (--solo)
max_stubs = 0  # set positive to create a limited number of stubs (--max 12)


# You must run the script from the refodcs reference/ root directory

java_ref_root = os.getcwd()
kotlin_ref_root = os.path.join(java_ref_root, "kotlin")
root = os.path.split(java_ref_root)[1]
if root != "reference":
  print ("You must cd to the refocs reference/ root directoy")
  sys.exit()


# This method uses switcher2, which assumes the refdocs stay in their current
# asymmetrical dirs (ref/android and ref/kotlin/android)
# And just puts the switcher in the existing docs
def insert_stub(doc, java, both):
  global stubs
  global java_stubs, kotlin_stubs
  global verbose, work, show_solo
  global java_source_abs_path
  global kotlin_source_abs_path

  stubs = stubs+1

  if verbose:
    print "File: ", stubs, doc
  else:
    fn  = os.path.split(doc)
    print "File: ", stubs, fn[1], "\r",

  if (java):
      java_stubs = java_stubs + 1
  else:
      kotlin_stubs = kotlin_stubs + 1


  if (work):
    if (java):
      file_path = doc[len(java_ref_root)+1:]
      stub = doc.replace(java_source_abs_path, kotlin_source_abs_path)
      if (both):
        slug1 = "sed -i 's/<\/h1>/{}/' {}".format("<\/h1>\\n{% setvar page_path %}_page_path_{% endsetvar %}\\n{% setvar can_switch %}1{% endsetvar %}\\n{% include \"reference\/_java_switcher2.md\" %}",doc)
      else:
        slug1 = "sed -i 's/<\/h1>/{}/' {}".format("<\/h1>\\n{% include \"reference\/_java_switcher2.md\" %}",doc)
    else:
      file_path = doc[len(kotlin_ref_root)+1:]
      stub = doc.replace(kotlin_source_abs_path, java_source_abs_path)
      if (both):
        slug1 = "sed -i 's/<\/h1>/{}/' {}".format("<\/h1>\\n{% setvar page_path %}_page_path_{% endsetvar %}\\n{% setvar can_switch %}1{% endsetvar %}\\n{% include \"reference\/_kotlin_switcher2.md\" %}",doc)
      else:
        slug1 = "sed -i 's/<\/h1>/{}/' {}".format("<\/h1>\\n{% include \"reference\/_kotlin_switcher2.md\" %}",doc)

    os.system(slug1)
    if (both):
      page_path_slug = "sed -i 's/_page_path_/{}/' {}".format(file_path.replace("/","\/"),doc)
      os.system(page_path_slug)


def scan_files(stem):
  global work, verbose, show_solo, max_stubs
  global stubs
  global java_stubs, kotlin_stubs
  global java_source_abs_path
  global kotlin_source_abs_path

  java_source_abs_path = os.path.join(java_ref_root, stem)
  kotlin_source_abs_path = os.path.join(kotlin_ref_root, stem)

  # Pass 1
  # Loop over java content, create stubs for java,
  # and for corresponding Kotlin (when it exsits)

  # solo is java-only classes
  # both is java+kotlin
  stubs = 0
  java_stubs = 0
  kotlin_stubs = 0
  solo = 0
  both = 0

  print "*** PASS1 (Java) ***"
  maxed_out = False
  for root, dirs, files in os.walk(java_source_abs_path):
      if maxed_out:
        break;
      for file_ in files:
        ext = os.path.splitext(file_)
        ext = ext[1]
        if not ext:
          # this catches package-lists with no extension
          print "***", os.path.join(root, file_)
        elif ext != ".html":
          # filter out png, yaml, etc
          continue
        else:
          # we have java content
          doc = os.path.join(root, file_)



          # look for matching kotlin file
          kotlinsource = doc.replace(java_source_abs_path, kotlin_source_abs_path)
          if os.path.isfile(kotlinsource):
             # corresponding kotlin content exists
             insert_stub(doc, True, True)
             insert_stub(kotlinsource, False, True)
             both = both+1
          else:
            # no kotlin content
            if (show_solo):
              print "solo: ", doc
            insert_stub(doc, True, False)
            solo = solo+1

          if max_stubs>0 and stubs>=max_stubs:
            print
            print "max java stubs: ", max_stubs
            maxed_out = True;
            break

  print "Java+Kotlin:", both, "Only Java:", solo
  print


  # PASS 2
  # Loop over kotlin content, create stubs for Kotlin-only APIs
  print "*** PASS2 (Kotlin) ***"
  solo = 0
  both = 0
  maxed_out = False
  stubs = 0
  for root, dirs, files in os.walk(kotlin_source_abs_path):
      if maxed_out:
        break;
      for file_ in files:
        ext = os.path.splitext (file_)
        ext = ext[1]
        if not ext:
          # this catches package-lists with no extension
          print "***", os.path.join(root, file_)
        elif ext != ".html":
          # filter out png, yaml, etc
          continue
        else:
          # we have kotlin content
          doc = os.path.join(root, file_)
          javadoc = doc.replace(kotlin_source_abs_path, java_source_abs_path)
          file_name = os.path.splitext(file_)[0]
          file_path = doc[len(kotlin_source_abs_path)+1:]
          include_path = os.path.join("/reference/_kotlin", file_path)

          if os.path.isfile(javadoc):
             # corresponding java content exists
             # so we already created the kotlin stub file
             # nothing to do
             both = both+1
          else:
            # no java content
            # create the kotlin stub file
            if (show_solo):
              print "solo: ", doc
            insert_stub(doc , False, False)
            solo = solo+1

          if (max_stubs>0 and stubs>=max_stubs):
            print
            print "max koltin stubs: ", max_stubs
            maxed_out = True;
            break


  print "Java+Kotlin:", both, "Only Kotlin:", solo
  print
  print "Java: ", java_stubs, " Kotlin: ", kotlin_stubs, "Total: ", java_stubs + kotlin_stubs


def main(argv):

  global work, verbose, show_solo, max_stubs
  global java_source_abs_path
  global kotlin_source_abs_path
  stem = ""

  try:
    opts, args = getopt.getopt(argv,"",["work","verbose","solo","max="])
  except getopt.GetoptError:
    print 'USAGE: switcher --work --verbose --solo --max=<max_stubs> platform|androidx|support|chrome'
    sys.exit(2)

  for opt, arg in opts:
    if opt == '--work':
       work = True
    elif opt == "--verbose":
       print "verbose"
       verbose = True
    elif opt == "--solo":
       print "verbose"
       show_solo = True
    elif opt == "--max":
       max_stubs = int(arg)
       print "max ", max_stubs

  if len(args)>0:
    source = args[0]
    if source == "platform":
      stem = "android"
      print
      print "*** PLATFORM PAGES ***"
      print "======================"

    elif source == "androidx":
      stem = "androidx"
      print
      print "*** ANDROIDX SUPPORT LIBRARY PAGES ***"
      print "======================================"

    elif source == "support":
      stem = "android/support/v4/media"
      print
      print "*** ANDROIDX SUPPORT LIBRARY PAGES ***"
      print "======================================"

    elif source == "chrome":
      stem = "org/chromium/support_lib_boundary"
      print
      print "*** ANDROIDX CHROMIUM PAGES ***"
      print "==============================="

  if (len(stem)>0):
    scan_files(stem)
    print " *** DONE ***"
  else:
      print 'You must specify one of: platform|androidx|support|chrome'



if __name__ == "__main__":
   main(sys.argv[1:])

