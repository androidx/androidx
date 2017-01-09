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

LOCAL_PATH:= $(call my-dir)

# Build the samples.
# We need to add some special AAPT flags to generate R classes
# for resources that are included from the libraries.
include $(CLEAR_VARS)
LOCAL_USE_AAPT2 := true
LOCAL_PACKAGE_NAME := SupportLeanbackDemos
LOCAL_MODULE_TAGS := samples tests
LOCAL_SDK_VERSION := current
LOCAL_MIN_SDK_VERSION := 17
LOCAL_DEX_PREOPT := false
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_ANDROID_LIBRARIES := \
        android-support-compat \
        android-support-core-ui \
        android-support-media-compat \
        android-support-fragment \
        android-support-v7-recyclerview \
        android-support-v17-leanback \
        android-support-v17-preference-leanback \
        android-support-v7-preference \
        android-support-v14-preference

include $(BUILD_PACKAGE)
