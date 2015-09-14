#!/usr/bin/python

# Copyright (C) 2015 The Android Open Source Project
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

import os
import sys

print "Generate v4 fragment related code for leanback"

files = ['BrowseTest']

cls = ['BrowseTest', 'Background', 'Base', 'BaseRow', 'Browse', 'Details', 'Error', 'Headers',
      'PlaybackOverlay', 'Rows', 'Search', 'VerticalGrid', 'Branded']

for w in files:
    print "copy {}Fragment to {}SupportFragment".format(w, w)

    file = open('src/android/support/v17/leanback/app/{}Fragment.java'.format(w), 'r')
    outfile = open('src/android/support/v17/leanback/app/{}SupportFragment.java'.format(w), 'w')

    outfile.write("/* This file is auto-generated from {}Fragment.java.  DO NOT MODIFY. */\n\n".format(w))

    for line in file:
        for w in cls:
            line = line.replace('{}Fragment'.format(w), '{}SupportFragment'.format(w))
        line = line.replace('android.app.Fragment', 'android.support.v4.app.Fragment')
        line = line.replace('android.app.Activity', 'android.support.v4.app.FragmentActivity')
        outfile.write(line)
    file.close()
    outfile.close()

testcls = ['Browse']

for w in testcls:
    print "copy {}FrgamentTest to {}SupportFragmentTest".format(w, w)

    file = open('src/android/support/v17/leanback/app/{}FragmentTest.java'.format(w), 'r')
    outfile = open('src/android/support/v17/leanback/app/{}SupportFragmentTest.java'.format(w), 'w')

    outfile.write("/* This file is auto-generated from {}FrgamentTest.java.  DO NOT MODIFY. */\n\n".format(w))

    for line in file:
        for w in testcls:
            line = line.replace('{}FragmentTest'.format(w), '{}SupportFragmentTest'.format(w))
            line = line.replace('{}FragmentTestActivity'.format(w), '{}SupportFragmentTestActivity'.format(w))
            line = line.replace('{}TestFragment'.format(w), '{}TestSupportFragment'.format(w))
        outfile.write(line)
    file.close()
    outfile.close()


print "copy BrowseFragmentTestActivity to BrowseSupportFragmentTestActivity"
file = open('src/android/support/v17/leanback/app/BrowseFragmentTestActivity.java', 'r')
outfile = open('src/android/support/v17/leanback/app/BrowseSupportFragmentTestActivity.java', 'w')
outfile.write("/* This file is auto-generated from BrowseFragmentTestActivity.java.  DO NOT MODIFY. */\n\n")
for line in file:
    line = line.replace('BrowseTestFragment', 'BrowseTestSupportFragment')
    line = line.replace('BrowseFragmentTestActivity', 'BrowseSupportFragmentTestActivity')
    line = line.replace('android.app.Fragment', 'android.support.v4.app.Fragment')
    line = line.replace('android.app.Activity', 'android.support.v4.app.FragmentActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('getFragmentManager', 'getSupportFragmentManager')
    outfile.write(line)
file.close()
outfile.close()

