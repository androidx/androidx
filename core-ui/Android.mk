# Copyright (C) 2011 The Android Open Source Project
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

# A helper sub-library that makes direct use of Honeycomb APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-core-ui-honeycomb
LOCAL_SDK_VERSION := 11
LOCAL_SRC_FILES := $(call all-java-files-under, honeycomb)
LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-annotations \
    android-support-compat
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Ice Cream Sandwich APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-core-ui-ics
LOCAL_SDK_VERSION := 14
LOCAL_SRC_FILES := $(call all-java-files-under, ics)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-core-ui-honeycomb
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of JellyBean MR2 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-core-ui-jellybean-mr2
LOCAL_SDK_VERSION := 18
LOCAL_SRC_FILES := $(call all-java-files-under, jellybean-mr2)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-core-ui-ics
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Lollipop APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-core-ui-api21
LOCAL_SDK_VERSION := 21
LOCAL_SRC_FILES := $(call all-java-files-under, api21)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-core-ui-jellybean-mr2
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# Here is the final static library that apps can link against.
include $(CLEAR_VARS)
LOCAL_USE_AAPT2 := true
LOCAL_MODULE := android-support-core-ui
LOCAL_SDK_VERSION := 9
LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_STATIC_JAVA_LIBRARIES := android-support-core-ui-api21
LOCAL_SHARED_ANDROID_LIBRARIES := \
    android-support-compat \
    android-support-annotations
LOCAL_JAR_EXCLUDE_FILES := none
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)
