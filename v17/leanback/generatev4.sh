#!/bin/bash

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

# Generate v4 fragment related code for leanback

echo "Generate v4 fragment related code for leanback"

declare -a arr=("Background" "BaseRow" "Browse" \
    "Details" "Error" "Headers" "PlaybackOverlay" \
    "Rows" "Search" "VerticalGrid")

## now loop through the above array
for i in "${arr[@]}"
do
   echo "copy ${i}Fragment to ${i}SupportFragment"
   cp src/android/support/v17/leanback/app/${i}Fragment.java src/android/support/v17/leanback/app/${i}SupportFragment.java
   echo "Modifying code to v4 fragment"
   for j in "${arr[@]}"
   do
      sed -i "s/${j}Fragment/${j}SupportFragment/g" src/android/support/v17/leanback/app/${i}SupportFragment.java
   done
   sed -i 's/android\.app\.Fragment/android\.support\.v4\.app\.Fragment/g' src/android/support/v17/leanback/app/${i}SupportFragment.java
   sed -i 's/android\.app\.Activity/android\.support\.v4\.app\.FragmentActivity/g' src/android/support/v17/leanback/app/${i}SupportFragment.java
   sed -i "1s/^/\/* This file is auto-generated from ${i}Fragment.java.  DO NOT MODIFY. *\/\n\n/" src/android/support/v17/leanback/app/${i}SupportFragment.java
done
