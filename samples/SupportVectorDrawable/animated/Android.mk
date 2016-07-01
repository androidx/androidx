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

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_SDK_VERSION := current

LOCAL_MIN_SDK_VERSION := 11

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := SupportAnimatedVectorDrawable

LOCAL_STATIC_JAVA_LIBRARIES := android-support-animatedvectordrawable \
        android-support-vectordrawable \
        android-support-v4

LOCAL_AAPT_FLAGS += --auto-add-overlay \
        --extra-packages android.support.graphics.drawable \
        --no-version-vectors

include $(BUILD_PACKAGE)

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
