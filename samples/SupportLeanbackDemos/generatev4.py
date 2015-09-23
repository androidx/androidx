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

file = open('src/com/example/android/leanback/GuidedStepActivity.java', 'r')
outfile = open('src/com/example/android/leanback/GuidedStepSupportActivity.java', 'w')

outfile.write("/* This file is auto-generated from GuidedStepActivity.  DO NOT MODIFY. */\n\n")

for line in file:
    line = line.replace('android.app.Fragment', 'android.support.v4.app.Fragment')
    line = line.replace('android.app.Activity', 'android.support.v4.app.FragmentActivity')
    line = line.replace('GuidedStepFragment', 'GuidedStepSupportFragment')
    line = line.replace('GuidedStepActivity', 'GuidedStepSupportActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('Activity activity', 'FragmentActivity activity')
    outfile.write(line)
file.close()
outfile.close()
