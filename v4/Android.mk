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

# A helper sub-library that makes direct use of Eclair APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-eclair
LOCAL_SDK_VERSION := 5
LOCAL_SRC_FILES := $(call all-java-files-under, eclair)
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Eclair MR1 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-eclair-mr1
LOCAL_SDK_VERSION := 7
LOCAL_SRC_FILES := $(call all-java-files-under, eclair-mr1)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-eclair
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Froyo APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-froyo
LOCAL_SDK_VERSION := 8
LOCAL_SRC_FILES := $(call all-java-files-under, froyo)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-eclair-mr1
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Gingerbread APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-gingerbread
LOCAL_SDK_VERSION := 9
LOCAL_SRC_FILES := $(call all-java-files-under, gingerbread)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-froyo
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Honeycomb APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-honeycomb
LOCAL_SDK_VERSION := 11
LOCAL_SRC_FILES := $(call all-java-files-under, honeycomb)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-gingerbread
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Honeycomb MR2 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-honeycomb-mr2
LOCAL_SDK_VERSION := 13
LOCAL_SRC_FILES := $(call all-java-files-under, honeycomb_mr2)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-honeycomb
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Ice Cream Sandwich APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-ics
LOCAL_SDK_VERSION := 14
LOCAL_SRC_FILES := $(call all-java-files-under, ics)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-honeycomb-mr2
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Ice Cream Sandwich MR1 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-ics-mr1
LOCAL_SDK_VERSION := 15
LOCAL_SRC_FILES := $(call all-java-files-under, ics-mr1)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-ics
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of JellyBean APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-jellybean
LOCAL_SDK_VERSION := 16
LOCAL_SRC_FILES := $(call all-java-files-under, jellybean)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-ics-mr1
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of JellyBean MR1 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-jellybean-mr1
LOCAL_SDK_VERSION := 17
LOCAL_SRC_FILES := $(call all-java-files-under, jellybean-mr1)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-jellybean
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of JellyBean MR2 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-jellybean-mr2
LOCAL_SDK_VERSION := 18
LOCAL_SRC_FILES := $(call all-java-files-under, jellybean-mr2)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-jellybean-mr1
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of KitKat APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-kitkat
LOCAL_SDK_VERSION := 19
LOCAL_SRC_FILES := $(call all-java-files-under, kitkat)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-jellybean-mr2
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# Here is the final static library that apps can link against.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4
LOCAL_SDK_VERSION := 4
LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4-kitkat
include $(BUILD_STATIC_JAVA_LIBRARY)
