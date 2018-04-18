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
    print "copy {}SupportFragment to {}Fragment".format(w, w)

    file = open('java/android/support/v17/leanback/app/{}SupportFragment.java'.format(w), 'r')
    outfile = open('java/android/support/v17/leanback/app/{}Fragment.java'.format(w), 'w')

    outfile.write("// CHECKSTYLE:OFF Generated code\n")
    outfile.write("/* This file is auto-generated from {}SupportFragment.java.  DO NOT MODIFY. */\n\n".format(w))

    for line in file:
        for w in cls:
            line = line.replace('{}SupportFragment'.format(w), '{}Fragment'.format(w))
        line = line.replace('androidx.fragment.app.FragmentActivity', 'android.app.Activity')
        line = line.replace('androidx.fragment.app.Fragment', 'android.app.Fragment')
        line = line.replace('FragmentActivity getActivity()', 'Activity getActivity()')
        outfile.write(line)
    file.close()
    outfile.close()

####### generate XXXFragmentTestBase classes #######

testcls = ['GuidedStep', 'Single']

for w in testcls:
    print "copy {}SupportFrgamentTestBase to {}FragmentTestBase".format(w, w)

    file = open('java/android/support/v17/leanback/app/{}SupportFragmentTestBase.java'.format(w), 'r')
    outfile = open('java/android/support/v17/leanback/app/{}FragmentTestBase.java'.format(w), 'w')

    outfile.write("// CHECKSTYLE:OFF Generated code\n")
    outfile.write("/* This file is auto-generated from {}SupportFrgamentTestBase.java.  DO NOT MODIFY. */\n\n".format(w))

    for line in file:
        for w in cls:
            line = line.replace('{}SupportFragment'.format(w), '{}Fragment'.format(w))
        for w in testcls:
            line = line.replace('{}SupportFragmentTestBase'.format(w), '{}FragmentTestBase'.format(w))
            line = line.replace('{}SupportFragmentTestActivity'.format(w), '{}FragmentTestActivity'.format(w))
            line = line.replace('{}TestSupportFragment'.format(w), '{}TestFragment'.format(w))
        line = line.replace('androidx.fragment.app.FragmentActivity', 'android.app.Activity')
        line = line.replace('androidx.fragment.app.Fragment', 'android.app.Fragment')
        outfile.write(line)
    file.close()
    outfile.close()

####### generate XXXFragmentTest classes #######

testcls = ['Browse', 'GuidedStep', 'VerticalGrid', 'Playback', 'Video', 'Details', 'Rows', 'Headers']

for w in testcls:
    print "copy {}SupporFrgamentTest to {}tFragmentTest".format(w, w)

    file = open('java/android/support/v17/leanback/app/{}SupportFragmentTest.java'.format(w), 'r')
    outfile = open('java/android/support/v17/leanback/app/{}FragmentTest.java'.format(w), 'w')

    outfile.write("// CHECKSTYLE:OFF Generated code\n")
    outfile.write("/* This file is auto-generated from {}SupportFragmentTest.java.  DO NOT MODIFY. */\n\n".format(w))

    for line in file:
        for w in cls:
            line = line.replace('{}SupportFragment'.format(w), '{}Fragment'.format(w))
        for w in testcls:
            line = line.replace('SingleSupportFragmentTestBase', 'SingleFragmentTestBase')
            line = line.replace('SingleSupportFragmentTestActivity', 'SingleFragmentTestActivity')
            line = line.replace('{}SupportFragmentTestBase'.format(w), '{}FragmentTestBase'.format(w))
            line = line.replace('{}SupportFragmentTest'.format(w), '{}FragmentTest'.format(w))
            line = line.replace('{}SupportFragmentTestActivity'.format(w), '{}FragmentTestActivity'.format(w))
            line = line.replace('{}TestSupportFragment'.format(w), '{}TestFragment'.format(w))
        line = line.replace('androidx.fragment.app.FragmentActivity', 'android.app.Activity')
        line = line.replace('androidx.fragment.app.Fragment', 'android.app.Fragment')
	line = line.replace('extends FragmentActivity', 'extends Activity')
	line = line.replace('Activity.this.getSupportFragmentManager', 'Activity.this.getFragmentManager')
	line = line.replace('tivity.getSupportFragmentManager', 'tivity.getFragmentManager')
        outfile.write(line)
    file.close()
    outfile.close()


####### generate XXXTestActivity classes #######
testcls = ['Browse', 'GuidedStep', 'Single']

for w in testcls:
    print "copy {}SupportFragmentTestActivity to {}FragmentTestActivity".format(w, w)
    file = open('java/android/support/v17/leanback/app/{}SupportFragmentTestActivity.java'.format(w), 'r')
    outfile = open('java/android/support/v17/leanback/app/{}FragmentTestActivity.java'.format(w), 'w')
    outfile.write("// CHECKSTYLE:OFF Generated code\n")
    outfile.write("/* This file is auto-generated from {}SupportFragmentTestActivity.java.  DO NOT MODIFY. */\n\n".format(w))
    for line in file:
        line = line.replace('{}TestSupportFragment'.format(w), '{}TestFragment'.format(w))
        line = line.replace('{}SupportFragmentTestActivity'.format(w), '{}FragmentTestActivity'.format(w))
        line = line.replace('androidx.fragment.app.FragmentActivity', 'android.app.Activity')
        line = line.replace('androidx.fragment.app.Fragment', 'android.app.Fragment')
        line = line.replace('extends FragmentActivity', 'extends Activity')
        line = line.replace('getSupportFragmentManager', 'getFragmentManager')
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

