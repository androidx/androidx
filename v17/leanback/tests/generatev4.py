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

####### generate XXXTestFragment classes #######

files = ['BrowseTest', 'GuidedStepTest', 'PlaybackTest', 'DetailsTest']

cls = ['BrowseTest', 'Background', 'Base', 'BaseRow', 'Browse', 'Details', 'Error', 'Headers',
      'PlaybackOverlay', 'Rows', 'Search', 'VerticalGrid', 'Branded',
      'GuidedStepTest', 'GuidedStep', 'RowsTest', 'PlaybackTest', 'Playback', 'Video',
      'DetailsTest']

for w in files:
    print "copy {}Fragment to {}SupportFragment".format(w, w)

    file = open('java/android/support/v17/leanback/app/{}Fragment.java'.format(w), 'r')
    outfile = open('java/android/support/v17/leanback/app/{}SupportFragment.java'.format(w), 'w')

    outfile.write("// CHECKSTYLE:OFF Generated code\n")
    outfile.write("/* This file is auto-generated from {}Fragment.java.  DO NOT MODIFY. */\n\n".format(w))

    for line in file:
        for w in cls:
            line = line.replace('{}Fragment'.format(w), '{}SupportFragment'.format(w))
        line = line.replace('android.app.Fragment', 'android.support.v4.app.Fragment')
        line = line.replace('android.app.Activity', 'android.support.v4.app.FragmentActivity')
        line = line.replace('Activity getActivity()', 'FragmentActivity getActivity()')
        outfile.write(line)
    file.close()
    outfile.close()

####### generate XXXFragmentTestBase classes #######

testcls = ['GuidedStep', 'Single']

for w in testcls:
    print "copy {}FrgamentTestBase to {}SupportFragmentTestBase".format(w, w)

    file = open('java/android/support/v17/leanback/app/{}FragmentTestBase.java'.format(w), 'r')
    outfile = open('java/android/support/v17/leanback/app/{}SupportFragmentTestBase.java'.format(w), 'w')

    outfile.write("// CHECKSTYLE:OFF Generated code\n")
    outfile.write("/* This file is auto-generated from {}FrgamentTestBase.java.  DO NOT MODIFY. */\n\n".format(w))

    for line in file:
        for w in cls:
            line = line.replace('{}Fragment'.format(w), '{}SupportFragment'.format(w))
        for w in testcls:
            line = line.replace('{}FragmentTestBase'.format(w), '{}SupportFragmentTestBase'.format(w))
            line = line.replace('{}FragmentTestActivity'.format(w), '{}SupportFragmentTestActivity'.format(w))
            line = line.replace('{}TestFragment'.format(w), '{}TestSupportFragment'.format(w))
        line = line.replace('android.app.Fragment', 'android.support.v4.app.Fragment')
        line = line.replace('android.app.Activity', 'android.support.v4.app.FragmentActivity')
        outfile.write(line)
    file.close()
    outfile.close()

####### generate XXXFragmentTest classes #######

testcls = ['Browse', 'GuidedStep', 'VerticalGrid', 'Playback', 'Video', 'Details', 'Rows', 'Headers']

for w in testcls:
    print "copy {}FrgamentTest to {}SupportFragmentTest".format(w, w)

    file = open('java/android/support/v17/leanback/app/{}FragmentTest.java'.format(w), 'r')
    outfile = open('java/android/support/v17/leanback/app/{}SupportFragmentTest.java'.format(w), 'w')

    outfile.write("// CHECKSTYLE:OFF Generated code\n")
    outfile.write("/* This file is auto-generated from {}FragmentTest.java.  DO NOT MODIFY. */\n\n".format(w))

    for line in file:
        for w in cls:
            line = line.replace('{}Fragment'.format(w), '{}SupportFragment'.format(w))
        for w in testcls:
            line = line.replace('SingleFragmentTestBase', 'SingleSupportFragmentTestBase')
            line = line.replace('SingleFragmentTestActivity', 'SingleSupportFragmentTestActivity')
            line = line.replace('{}FragmentTestBase'.format(w), '{}SupportFragmentTestBase'.format(w))
            line = line.replace('{}FragmentTest'.format(w), '{}SupportFragmentTest'.format(w))
            line = line.replace('{}FragmentTestActivity'.format(w), '{}SupportFragmentTestActivity'.format(w))
            line = line.replace('{}TestFragment'.format(w), '{}TestSupportFragment'.format(w))
        line = line.replace('android.app.Fragment', 'android.support.v4.app.Fragment')
        line = line.replace('android.app.Activity', 'android.support.v4.app.FragmentActivity')
	line = line.replace('extends Activity', 'extends FragmentActivity')
	line = line.replace('Activity.this.getFragmentManager', 'Activity.this.getSupportFragmentManager')
	line = line.replace('tivity.getFragmentManager', 'tivity.getSupportFragmentManager')
        outfile.write(line)
    file.close()
    outfile.close()


####### generate XXXTestActivity classes #######
testcls = ['Browse', 'GuidedStep', 'Single']

for w in testcls:
    print "copy {}FragmentTestActivity to {}SupportFragmentTestActivity".format(w, w)
    file = open('java/android/support/v17/leanback/app/{}FragmentTestActivity.java'.format(w), 'r')
    outfile = open('java/android/support/v17/leanback/app/{}SupportFragmentTestActivity.java'.format(w), 'w')
    outfile.write("// CHECKSTYLE:OFF Generated code\n")
    outfile.write("/* This file is auto-generated from {}FragmentTestActivity.java.  DO NOT MODIFY. */\n\n".format(w))
    for line in file:
        line = line.replace('{}TestFragment'.format(w), '{}TestSupportFragment'.format(w))
        line = line.replace('{}FragmentTestActivity'.format(w), '{}SupportFragmentTestActivity'.format(w))
        line = line.replace('android.app.Fragment', 'android.support.v4.app.Fragment')
        line = line.replace('android.app.Activity', 'android.support.v4.app.FragmentActivity')
        line = line.replace('extends Activity', 'extends FragmentActivity')
        line = line.replace('getFragmentManager', 'getSupportFragmentManager')
        outfile.write(line)
    file.close()
    outfile.close()

####### generate Float parallax test #######

print "copy ParallaxIntEffectTest to ParallaxFloatEffectTest"
file = open('java/android/support/v17/leanback/widget/ParallaxIntEffectTest.java', 'r')
outfile = open('java/android/support/v17/leanback/widget/ParallaxFloatEffectTest.java', 'w')
outfile.write("// CHECKSTYLE:OFF Generated code\n")
outfile.write("/* This file is auto-generated from ParallaxIntEffectTest.java.  DO NOT MODIFY. */\n\n")
for line in file:
    line = line.replace('IntEffect', 'FloatEffect')
    line = line.replace('IntParallax', 'FloatParallax')
    line = line.replace('IntProperty', 'FloatProperty')
    line = line.replace('intValue()', 'floatValue()')
    line = line.replace('int screenMax', 'float screenMax')
    line = line.replace('assertEquals((int)', 'assertFloatEquals((float)')
    line = line.replace('(int)', '(float)')
    line = line.replace('int[', 'float[')
    line = line.replace('Integer', 'Float');
    outfile.write(line)
file.close()
outfile.close()


print "copy ParallaxIntTest to ParallaxFloatTest"
file = open('java/android/support/v17/leanback/widget/ParallaxIntTest.java', 'r')
outfile = open('java/android/support/v17/leanback/widget/ParallaxFloatTest.java', 'w')
outfile.write("// CHECKSTYLE:OFF Generated code\n")
outfile.write("/* This file is auto-generated from ParallaxIntTest.java.  DO NOT MODIFY. */\n\n")
for line in file:
    line = line.replace('ParallaxIntTest', 'ParallaxFloatTest')
    line = line.replace('IntParallax', 'FloatParallax')
    line = line.replace('IntProperty', 'FloatProperty')
    line = line.replace('verifyIntProperties', 'verifyFloatProperties')
    line = line.replace('intValue()', 'floatValue()')
    line = line.replace('int screenMax', 'float screenMax')
    line = line.replace('assertEquals((int)', 'assertFloatEquals((float)')
    line = line.replace('(int)', '(float)')
    outfile.write(line)
file.close()
outfile.close()

