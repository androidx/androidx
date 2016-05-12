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

LOCAL_PATH := $(call my-dir)

# ---------------------------------------------
#
# Static vector drawable library
#
# ---------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-vectordrawable
LOCAL_SDK_VERSION := $(SUPPORT_CURRENT_SDK_VERSION)
LOCAL_SRC_FILES := $(call all-java-files-under, static/src)

LOCAL_JAVA_LIBRARIES := android-support-compat

LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

# Static API Check
support_module := $(LOCAL_MODULE)
support_module_api_dir := $(LOCAL_PATH)/static/api
support_module_src_files := $(LOCAL_SRC_FILES)
support_module_java_libraries := $(LOCAL_JAVA_LIBRARIES)
support_module_java_packages := android.support.graphics.drawable
include $(SUPPORT_API_CHECK)


# ---------------------------------------------
#
# Animated vector drawable library
#
# ---------------------------------------------
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-animatedvectordrawable
LOCAL_SDK_VERSION := $(SUPPORT_CURRENT_SDK_VERSION)
LOCAL_SRC_FILES := $(call all-java-files-under, animated/src)

LOCAL_JAVA_LIBRARIES := android-support-compat android-support-vectordrawable

LOCAL_AAPT_FLAGS := --no-version-vectors
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

# Animated API Check
support_module := $(LOCAL_MODULE)
support_module_api_dir := $(LOCAL_PATH)/animated/api
support_module_src_files := $(LOCAL_SRC_FILES) \
    static/src/android/support/graphics/drawable/VectorDrawableCommon.java
support_module_java_libraries := $(LOCAL_JAVA_LIBRARIES)
support_module_java_packages := android.support.graphics.drawable
include $(SUPPORT_API_CHECK)
