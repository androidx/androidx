#!/usr/bin/env python

# Copyright (C) 2017 The Android Open Source Project
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
import re

print("Generate framework fragment related code for leanback")

cls = ['Base', 'BaseRow', 'Browse', 'Details', 'Error', 'Headers',
      'Playback', 'Rows', 'Search', 'VerticalGrid', 'Branded',
      'GuidedStep', 'Onboarding', 'Video']

for w in cls:
    print("copy {}SupportFragment to {}Fragment".format(w, w))

    file = open('src/main/java/androidx/leanback/app/{}SupportFragment.java'.format(w), 'r')
    content = "// CHECKSTYLE:OFF Generated code\n"
    content = content + "/* This file is auto-generated from {}SupportFragment.java.  DO NOT MODIFY. */\n\n".format(w)

    for line in file:
        line = line.replace('IS_FRAMEWORK_FRAGMENT = false', 'IS_FRAMEWORK_FRAGMENT = true');
        for w2 in cls:
            line = line.replace('{}SupportFragment'.format(w2), '{}Fragment'.format(w2))
        line = line.replace('androidx.fragment.app.FragmentActivity', 'android.app.Activity')
        line = line.replace('androidx.fragment.app.Fragment', 'android.app.Fragment')
        line = line.replace('activity.getSupportFragmentManager()', 'activity.getFragmentManager()')
        line = line.replace('FragmentActivity activity', 'Activity activity')
        line = line.replace('FragmentActivity#onBackPressed', 'Activity#onBackPressed')
        line = line.replace('(FragmentActivity', '(Activity')
        line = line.replace('setEnterTransition(enterTransition)', 'setEnterTransition((android.transition.Transition) enterTransition)');
        line = line.replace('setSharedElementEnterTransition(sharedElementTransition)', 'setSharedElementEnterTransition((android.transition.Transition) sharedElementTransition)');
        line = line.replace('setExitTransition(exitTransition)', 'setExitTransition((android.transition.Transition) exitTransition)');
        line = line.replace('requestPermissions(new', 'PermissionHelper.requestPermissions(SearchFragment.this, new');

        # replace getContext() with FragmentUtil.getContext(XXXFragment.this), but dont match the case "view.getContext()"
        line = re.sub(r'([^\.])getContext\(\)', r'\1FragmentUtil.getContext({}Fragment.this)'.format(w), line);
        content = content + line
    file.close()

    # treat different Nullable for onCreateView
    content = re.sub(
        'View onCreateView\(@NonNull LayoutInflater ([a-zA-Z]+),\s*@Nullable ViewGroup ([a-zA-Z]+),\s*@Nullable Bundle ([a-zA-Z]+)',
        r'View onCreateView(LayoutInflater \1, @Nullable ViewGroup \2, Bundle \3', content)

    # treat different Nullable for onSaveInstance
    content = re.sub(
        'void onSaveInstanceState\(@NonNull Bundle ([a-zA-Z]+)\)',
        r'void onSaveInstanceState(Bundle \1)', content)

    # add deprecated tag to fragment class and inner classes/interfaces
    content = re.sub(r'\*\/\n(@.*\n|)(public |abstract public |abstract |)class', '* @deprecated use {@link ' + w + 'SupportFragment}\n */\n@Deprecated\n\\1\\2class', content)
    content = re.sub(r'\*\/\n    public (static class|interface|final static class|abstract static class)', '* @deprecated use {@link ' + w + 'SupportFragment}\n     */\n    @Deprecated\n    public \\1', content)
    outfile = open('src/main/java/androidx/leanback/app/{}Fragment.java'.format(w), 'w')
    outfile.write(content)
    outfile.close()



print("copy VideoSupportFragmentGlueHost to VideoFragmentGlueHost")
file = open('src/main/java/androidx/leanback/app/VideoSupportFragmentGlueHost.java', 'r')
content = "// CHECKSTYLE:OFF Generated code\n"
content = content + "/* This file is auto-generated from VideoSupportFragmentGlueHost.java.  DO NOT MODIFY. */\n\n"
for line in file:
    line = line.replace('androidx.fragment.app.Fragment', 'android.app.Fragment')
    line = line.replace('VideoSupportFragment', 'VideoFragment')
    line = line.replace('PlaybackSupportFragment', 'PlaybackFragment')
    content = content + line
file.close()
# add deprecated tag to class
content = re.sub(r'\*\/\npublic class', '* @deprecated use {@link VideoSupportFragmentGlueHost}\n */\n@Deprecated\npublic class', content)
outfile = open('src/main/java/androidx/leanback/app/VideoFragmentGlueHost.java', 'w')
outfile.write(content)
outfile.close()



print("copy PlaybackSupportFragmentGlueHost to PlaybackFragmentGlueHost")
file = open('src/main/java/androidx/leanback/app/PlaybackSupportFragmentGlueHost.java', 'r')
content = "// CHECKSTYLE:OFF Generated code\n"
content = content + "/* This file is auto-generated from {}PlaybackSupportFragmentGlueHost.java.  DO NOT MODIFY. */\n\n"
for line in file:
    line = line.replace('VideoSupportFragment', 'VideoFragment')
    line = line.replace('PlaybackSupportFragment', 'PlaybackFragment')
    line = line.replace('androidx.fragment.app.Fragment', 'android.app.Fragment')
    content = content + line
file.close()
# add deprecated tag to class
content = re.sub(r'\*\/\npublic class', '* @deprecated use {@link PlaybackSupportFragmentGlueHost}\n */\n@Deprecated\npublic class', content)
outfile = open('src/main/java/androidx/leanback/app/PlaybackFragmentGlueHost.java', 'w')
outfile.write(content)
outfile.close()



print("copy DetailsSupportFragmentBackgroundController to DetailsFragmentBackgroundController")
file = open('src/main/java/androidx/leanback/app/DetailsSupportFragmentBackgroundController.java', 'r')
content = "// CHECKSTYLE:OFF Generated code\n"
content = content + "/* This file is auto-generated from {}DetailsSupportFragmentBackgroundController.java.  DO NOT MODIFY. */\n\n"
for line in file:
    line = line.replace('VideoSupportFragment', 'VideoFragment')
    line = line.replace('DetailsSupportFragment', 'DetailsFragment')
    line = line.replace('RowsSupportFragment', 'RowsFragment')
    line = line.replace('androidx.fragment.app.Fragment', 'android.app.Fragment')
    line = line.replace('mFragment.getContext()', 'FragmentUtil.getContext(mFragment)')
    content = content + line
file.close()
# add deprecated tag to class
content = re.sub(r'\*\/\npublic class', '* @deprecated use {@link DetailsSupportFragmentBackgroundController}\n */\n@Deprecated\npublic class', content)
outfile = open('src/main/java/androidx/leanback/app/DetailsFragmentBackgroundController.java', 'w')
outfile.write(content)
outfile.close()
