#!/usr/bin/python

# Copyright (C) 2014 The Android Open Source Project
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

cls = ['Background', 'Base', 'BaseRow', 'Browse', 'Details', 'Error', 'Headers',
      'PlaybackOverlay', 'Rows', 'Search', 'VerticalGrid', 'Branded', 'GuidedStep']

for w in cls:
    print "copy {}Fragment to {}SupportFragment".format(w, w)

    file = open('src/android/support/v17/leanback/app/{}Fragment.java'.format(w), 'r')
    outfile = open('src/android/support/v17/leanback/app/{}SupportFragment.java'.format(w), 'w')

    outfile.write("/* This file is auto-generated from {}Fragment.java.  DO NOT MODIFY. */\n\n".format(w))

    for line in file:
        line = line.replace('IS_FRAMEWORK_FRAGMENT = true', 'IS_FRAMEWORK_FRAGMENT = false');
        for w in cls:
            line = line.replace('{}Fragment'.format(w), '{}SupportFragment'.format(w))
        line = line.replace('android.app.Fragment', 'android.support.v4.app.Fragment')
        line = line.replace('android.app.Activity', 'android.support.v4.app.FragmentActivity')
        line = line.replace('activity.getFragmentManager()', 'activity.getSupportFragmentManager()')
        line = line.replace('Activity activity', 'FragmentActivity activity')
        line = line.replace('(Activity', '(FragmentActivity')
        outfile.write(line)
    file.close()
    outfile.close()

file = open('src/android/support/v17/leanback/app/PlaybackControlGlue.java', 'r')
outfile = open('src/android/support/v17/leanback/app/PlaybackControlSupportGlue.java', 'w')
outfile.write("/* This file is auto-generated from PlaybackControlGlue.java.  DO NOT MODIFY. */\n\n")
for line in file:
    line = line.replace('PlaybackControlGlue', 'PlaybackControlSupportGlue');
    line = line.replace('PlaybackOverlayFragment', 'PlaybackOverlaySupportFragment');
    outfile.write(line)
file.close()
outfile.close()
