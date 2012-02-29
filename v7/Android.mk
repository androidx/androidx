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

# Note: the source code is in java/, not src/, because this code is also part of
# the framework library, and build/core/pathmap.mk expects a java/ subdirectory.

# A helper sub-library that contains the R class only. Used to compiled the final library
# without being included in it.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v7-gridlayout-r
LOCAL_SRC_FILES := $(call all-java-files-under, gridlayout/gen)
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# Here is the final static library that apps can link against.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v7-gridlayout
#LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := $(call all-java-files-under, gridlayout/src)
LOCAL_JAVA_LIBRARIES += \
        android-support-v7-gridlayout-r

include $(BUILD_STATIC_JAVA_LIBRARY)

# Include this library in the build server's output directory
$(call dist-for-goals, droidcore sdk, $(LOCAL_BUILT_MODULE):android-support-v7-gridlayout.jar)
