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
import getopt

def write_java_head(tofile, name):
    tofile.write("// CHECKSTYLE:OFF Generated code\n")
    tofile.write("/* This file is auto-generated from {}.java.  DO NOT MODIFY. */\n\n".format(name))

def replace_xml_head(line, name):
    return line.replace('<?xml version="1.0" encoding="utf-8"?>', '<?xml version="1.0" encoding="utf-8"?>\n<!-- This file is auto-generated from {}.xml.  DO NOT MODIFY. -->\n'.format(name))

file = open('src/main/java/com/example/android/leanback/GuidedStepActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/GuidedStepSupportActivity.java', 'w')
write_java_head(outfile, "GuidedStepActivity")
for line in file:
    line = line.replace('android.app.Fragment', 'androidx.fragment.app.Fragment')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('GuidedStepFragment', 'GuidedStepSupportFragment')
    line = line.replace('GuidedStepActivity', 'GuidedStepSupportActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/GuidedStepHalfScreenActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/GuidedStepSupportHalfScreenActivity.java', 'w')
write_java_head(outfile, "GuidedStepHalfScreenActivity")
for line in file:
    line = line.replace('android.app.Fragment', 'androidx.fragment.app.Fragment')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('GuidedStepFragment', 'GuidedStepSupportFragment')
    line = line.replace('GuidedStepActivity', 'GuidedStepSupportActivity')
    line = line.replace('GuidedStepHalfScreenActivity', 'GuidedStepSupportHalfScreenActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/BrowseFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/BrowseSupportFragment.java', 'w')
write_java_head(outfile, "BrowseFragment")
for line in file:
    line = line.replace('android.app.Fragment', 'androidx.fragment.app.Fragment')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('BrowseFragment', 'BrowseSupportFragment')
    line = line.replace('GuidedStepFragment', 'GuidedStepSupportFragment')
    line = line.replace('GuidedStepActivity', 'GuidedStepSupportActivity')
    line = line.replace('getActivity().getFragmentManager()', 'getActivity().getSupportFragmentManager()')
    line = line.replace('BrowseActivity', 'BrowseSupportActivity')
    line = line.replace('DetailsActivity', 'DetailsSupportActivity')
    line = line.replace('SearchActivity', 'SearchSupportActivity')
    line = line.replace('RowsActivity', 'RowsSupportActivity')
    line = line.replace('RowsFragment', 'RowsSupportFragment')
    line = line.replace('GuidedStepHalfScreenActivity', 'GuidedStepSupportHalfScreenActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/BrowseActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/BrowseSupportActivity.java', 'w')
write_java_head(outfile, "BrowseActivity")
for line in file:
    line = line.replace('BrowseActivity', 'BrowseSupportActivity')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('R.layout.browse', 'R.layout.browse_support')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/res/layout/browse.xml', 'r')
outfile = open('src/main/res/layout/browse_support.xml', 'w')
for line in file:
    line = replace_xml_head(line, "browse")
    line = line.replace('com.example.android.leanback.BrowseFragment', 'com.example.android.leanback.BrowseSupportFragment')
    outfile.write(line)
file.close()
outfile.close()


file = open('src/main/java/com/example/android/leanback/DetailsFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/DetailsSupportFragment.java', 'w')
write_java_head(outfile, "DetailsFragment")
for line in file:
    line = line.replace('android.app.Fragment', 'androidx.fragment.app.Fragment')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('DetailsFragment', 'DetailsSupportFragment')
    line = line.replace('DetailsActivity', 'DetailsSupportActivity')
    line = line.replace('PlaybackOverlayActivity', 'PlaybackOverlaySupportActivity')
    line = line.replace('SearchActivity', 'SearchSupportActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/NewDetailsFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/NewDetailsSupportFragment.java', 'w')
write_java_head(outfile, "NewDetailsFragment")
for line in file:
    line = line.replace('android.app.Fragment', 'androidx.fragment.app.Fragment')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('DetailsFragment', 'DetailsSupportFragment')
    line = line.replace('DetailsSupportFragmentVideoHelper', 'DetailsFragmentVideoHelper')
    line = line.replace('VideoFragment', 'VideoSupportFragment')
    line = line.replace('PlaybackFragmentGlueHost', 'PlaybackSupportFragmentGlueHost')
    line = line.replace('DetailsActivity', 'DetailsSupportActivity')
    line = line.replace('PlaybackOverlayActivity', 'PlaybackOverlaySupportActivity')
    line = line.replace('SearchActivity', 'SearchSupportActivity')
    line = line.replace('SearchActivity', 'SearchSupportActivity')
    line = line.replace('getRowsFragment', 'getRowsSupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/DetailsActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/DetailsSupportActivity.java', 'w')
write_java_head(outfile, "DetailsActivity")
for line in file:
    line = line.replace('DetailsActivity', 'DetailsSupportActivity')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('getFragmentManager()', 'getSupportFragmentManager()')
    line = line.replace('DetailsFragment', 'DetailsSupportFragment')
    line = line.replace('NewDetailsFragment', 'NewDetailsSupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/SearchDetailsActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/SearchDetailsSupportActivity.java', 'w')
write_java_head(outfile, "SearchDetailsActivity")
for line in file:
    line = line.replace('DetailsActivity', 'DetailsSupportActivity')
    outfile.write(line)
file.close()
outfile.close()


file = open('src/main/java/com/example/android/leanback/SearchFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/SearchSupportFragment.java', 'w')
write_java_head(outfile, "SearchFragment")
for line in file:
    line = line.replace('SearchFragment', 'SearchSupportFragment')
    line = line.replace('DetailsActivity', 'DetailsSupportActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/SearchActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/SearchSupportActivity.java', 'w')
write_java_head(outfile, "SearchActivity")
for line in file:
    line = line.replace('SearchActivity', 'SearchSupportActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('R.layout.search', 'R.layout.search_support')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('getFragmentManager()', 'getSupportFragmentManager()')
    line = line.replace('SearchFragment', 'SearchSupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/res/layout/search.xml', 'r')
outfile = open('src/main/res/layout/search_support.xml', 'w')
for line in file:
    line = replace_xml_head(line, "search")
    line = line.replace('com.example.android.leanback.SearchFragment', 'com.example.android.leanback.SearchSupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/VerticalGridFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/VerticalGridSupportFragment.java', 'w')
write_java_head(outfile, "VerticalGridFragment")
for line in file:
    line = line.replace('VerticalGridFragment', 'VerticalGridSupportFragment')
    line = line.replace('DetailsActivity', 'DetailsSupportActivity')
    line = line.replace('SearchActivity', 'SearchSupportActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/VerticalGridActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/VerticalGridSupportActivity.java', 'w')
write_java_head(outfile, "VerticalGridActivity")
for line in file:
    line = line.replace('VerticalGridActivity', 'VerticalGridSupportActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('R.layout.vertical_grid', 'R.layout.vertical_grid_support')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('getFragmentManager()', 'getSupportFragmentManager()')
    line = line.replace('VerticalGridFragment', 'VerticalGridSupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/res/layout/vertical_grid.xml', 'r')
outfile = open('src/main/res/layout/vertical_grid_support.xml', 'w')
for line in file:
    line = replace_xml_head(line, "vertical_grid")
    line = line.replace('com.example.android.leanback.VerticalGridFragment', 'com.example.android.leanback.VerticalGridSupportFragment')
    outfile.write(line)
file.close()
outfile.close()


file = open('src/main/java/com/example/android/leanback/ErrorFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/ErrorSupportFragment.java', 'w')
write_java_head(outfile, "ErrorFragment")
for line in file:
    line = line.replace('ErrorFragment', 'ErrorSupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/BrowseErrorActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/BrowseErrorSupportActivity.java', 'w')
write_java_head(outfile, "BrowseErrorActivity")
for line in file:
    line = line.replace('BrowseErrorActivity', 'BrowseErrorSupportActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('R.layout.browse', 'R.layout.browse_support')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('getFragmentManager()', 'getSupportFragmentManager()')
    line = line.replace('ErrorFragment', 'ErrorSupportFragment')
    line = line.replace('SpinnerFragment', 'SpinnerSupportFragment')
    line = line.replace('android.app.Fragment', 'androidx.fragment.app.Fragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/RowsFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/RowsSupportFragment.java', 'w')
write_java_head(outfile, "RowsFragment")
for line in file:
    line = line.replace('RowsFragment', 'RowsSupportFragment')
    line = line.replace('DetailsActivity', 'DetailsSupportActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/RowsActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/RowsSupportActivity.java', 'w')
write_java_head(outfile, "RowsActivity")
for line in file:
    line = line.replace('RowsActivity', 'RowsSupportActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('R.layout.rows', 'R.layout.rows_support')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('RowsFragment', 'RowsSupportFragment')
    line = line.replace('getFragmentManager()', 'getSupportFragmentManager()')
    line = line.replace('SearchActivity', 'SearchSupportActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/res/layout/rows.xml', 'r')
outfile = open('src/main/res/layout/rows_support.xml', 'w')
for line in file:
    line = replace_xml_head(line, "rows")
    line = line.replace('com.example.android.leanback.RowsFragment', 'com.example.android.leanback.RowsSupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/PlaybackFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/PlaybackSupportFragment.java', 'w')
write_java_head(outfile, "PlaybackFragment")
for line in file:
    line = line.replace('PlaybackFragment', 'PlaybackSupportFragment')
    line = line.replace('PlaybackActivity', 'PlaybackSupportActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/PlaybackActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/PlaybackSupportActivity.java', 'w')
write_java_head(outfile, "PlaybackActivity")
for line in file:
    line = line.replace('PlaybackActivity', 'PlaybackSupportActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('R.layout.playback_activity', 'R.layout.playback_activity_support')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/res/layout/playback_activity.xml', 'r')
outfile = open('src/main/res/layout/playback_activity_support.xml', 'w')
for line in file:
    line = replace_xml_head(line, "playback_controls")
    line = line.replace('com.example.android.leanback.PlaybackFragment', 'com.example.android.leanback.PlaybackSupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/PlaybackTransportControlFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/PlaybackTransportControlSupportFragment.java', 'w')
write_java_head(outfile, "PlaybackTransportControlFragment")
for line in file:
    line = line.replace('PlaybackFragment', 'PlaybackSupportFragment')
    line = line.replace('PlaybackTransportControlFragment', 'PlaybackTransportControlSupportFragment')
    line = line.replace('PlaybackTransportControlActivity', 'PlaybackTransportControlSupportActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/PlaybackTransportControlActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/PlaybackTransportControlSupportActivity.java', 'w')
write_java_head(outfile, "PlaybackTransportControlActivity")
for line in file:
    line = line.replace('PlaybackTransportControlActivity', 'PlaybackTransportControlSupportActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('R.layout.playback_transportcontrol_activity', 'R.layout.playback_transportcontrol_activity_support')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/res/layout/playback_transportcontrol_activity.xml', 'r')
outfile = open('src/main/res/layout/playback_transportcontrol_activity_support.xml', 'w')
for line in file:
    line = replace_xml_head(line, "playback_transportcontrols")
    line = line.replace('com.example.android.leanback.PlaybackTransportControlFragment', 'com.example.android.leanback.PlaybackTransportControlSupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/res/layout/playback_controls.xml', 'r')
outfile = open('src/main/res/layout/playback_controls_support.xml', 'w')
for line in file:
    line = replace_xml_head(line, "playback_controls")
    line = line.replace('com.example.android.leanback.PlaybackOverlayFragment', 'com.example.android.leanback.PlaybackOverlaySupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/OnboardingActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/OnboardingSupportActivity.java', 'w')
write_java_head(outfile, "OnboardingActivity")
for line in file:
    line = line.replace('android.app.Fragment', 'androidx.fragment.app.Fragment')
    line = line.replace('android.app.Activity', 'androidx.fragment.app.FragmentActivity')
    line = line.replace('OnboardingActivity', 'OnboardingSupportActivity')
    line = line.replace('OnboardingDemoFragment', 'OnboardingDemoSupportFragment')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('getFragmentManager()', 'getSupportFragmentManager()')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/OnboardingDemoFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/OnboardingDemoSupportFragment.java', 'w')
write_java_head(outfile, "OnboardingDemoFragment")
for line in file:
    line = line.replace('android.app.Fragment', 'androidx.fragment.app.Fragment')
    line = line.replace('import android.app.Activity', 'import androidx.fragment.app.FragmentActivity')
    line = line.replace('OnboardingDemoFragment', 'OnboardingDemoSupportFragment')
    line = line.replace('OnboardingFragment', 'OnboardingSupportFragment')
    line = line.replace('OnboardingActivity', 'OnboardingSupportActivity')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/SampleVideoFragment.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/SampleVideoSupportFragment.java', 'w')
write_java_head(outfile, "OnboardingDemoFragment")
for line in file:
    line = line.replace('android.app.Fragment', 'androidx.fragment.app.Fragment')
    line = line.replace('import android.app.Activity', 'import androidx.fragment.app.FragmentActivity')
    line = line.replace('SampleVideoFragment', 'SampleVideoSupportFragment')
    line = line.replace('VideoFragment', 'VideoSupportFragment')
    outfile.write(line)
file.close()
outfile.close()

file = open('src/main/java/com/example/android/leanback/VideoActivity.java', 'r')
outfile = open('src/main/java/com/example/android/leanback/VideoSupportActivity.java', 'w')
write_java_head(outfile, "OnboardingDemoFragment")
for line in file:
    line = line.replace('android.app.Fragment', 'androidx.fragment.app.Fragment')
    line = line.replace('import android.app.Activity', 'import androidx.fragment.app.FragmentActivity')
    line = line.replace('VideoActivity', 'VideoSupportActivity')
    line = line.replace('extends Activity', 'extends FragmentActivity')
    line = line.replace('getFragmentManager()', 'getSupportFragmentManager()')
    line = line.replace('SampleVideoFragment', 'SampleVideoSupportFragment')
    outfile.write(line)
file.close()
outfile.close()
