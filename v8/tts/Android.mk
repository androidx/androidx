# Copyright (C) 2013 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Ice Cream Sandwich APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-tts-ics
LOCAL_SDK_VERSION := 14
LOCAL_SRC_FILES := $(call all-java-files-under, ics)
include $(BUILD_STATIC_JAVA_LIBRARY)

# A helper sub-library that makes direct use of Ice Cream Sandwich MR1 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-tts-ics-mr1
LOCAL_SDK_VERSION := 15
LOCAL_SRC_FILES := $(call all-java-files-under, ics-mr1)
include $(BUILD_STATIC_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v8-tts
#LOCAL_SDK_VERSION := 8
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-tts-ics-mr1 android-support-tts-ics
include $(BUILD_STATIC_JAVA_LIBRARY)

include $(LOCAL_PATH)/tests/Android.mk
