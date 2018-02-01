#
# Copyright (C) 2016 The Android Open Source Project
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
#

# The list of support library modules made available to the platform docs build.
FRAMEWORKS_SUPPORT_JAVA_LIBRARIES := \
    android-support-animatedvectordrawable \
    android-support-annotations \
    android-support-emoji \
    android-support-emoji-appcompat \
    android-support-emoji-bundled \
    android-support-compat \
    android-support-core-ui \
    android-support-core-utils \
    android-support-customtabs \
    android-support-design \
    android-support-dynamic-animation \
    android-support-exifinterface \
    android-support-fragment \
    android-support-media-compat \
    android-support-percent \
    android-support-recommendation \
    android-support-transition \
    android-support-tv-provider \
    android-support-v7-appcompat \
    android-support-v7-cardview \
    android-support-v7-gridlayout \
    android-support-v7-mediarouter \
    android-support-v7-palette \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    android-support-v13 \
    android-support-v14-preference \
    android-support-v17-leanback \
    android-support-v17-preference-leanback \
    android-support-vectordrawable \
    android-support-wear

# List of all Design transitive dependencies. Use this instead of android-support-design.
ANDROID_SUPPORT_DESIGN_TARGETS := \
    android-support-design \
    android-support-compat \
    android-support-core-ui \
    android-support-core-utils \
    android-support-fragment \
    android-support-transition \
    android-support-v7-appcompat \
    android-support-v7-recyclerview \

# List of all Car transitive dependencies. Use this instead of android-support-car.
ANDROID_SUPPORT_CAR_TARGETS := \
    android-support-car \
    $(ANDROID_SUPPORT_DESIGN_TARGETS) \
    android-support-media-compat \
    android-support-v7-cardview
