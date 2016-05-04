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

# A helper sub-library that makes direct use of Donut APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-donut
LOCAL_SDK_VERSION := 4
LOCAL_SRC_FILES := $(call all-java-files-under, donut)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-annotations android-support-compat
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files := $(LOCAL_SRC_FILES)
support_module_java_libraries := android-support-annotations android-support-compat

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Eclair APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-eclair
LOCAL_SDK_VERSION := 5
LOCAL_SRC_FILES := $(call all-java-files-under, eclair)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-donut
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Froyo APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-froyo
LOCAL_SDK_VERSION := 8
LOCAL_SRC_FILES := $(call all-java-files-under, froyo)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-eclair
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Gingerbread APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-gingerbread
LOCAL_SDK_VERSION := 9
LOCAL_SRC_FILES := $(call all-java-files-under, gingerbread)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-froyo
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Honeycomb APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-honeycomb
LOCAL_SDK_VERSION := 11
LOCAL_SRC_FILES := $(call all-java-files-under, honeycomb)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-gingerbread
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Ice Cream Sandwich APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-ics
LOCAL_SDK_VERSION := 14
LOCAL_SRC_FILES := $(call all-java-files-under, ics)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-honeycomb
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of JellyBean APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-jellybean
LOCAL_SDK_VERSION := 16
LOCAL_SRC_FILES := $(call all-java-files-under, jellybean)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-ics
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of JellyBean MR2 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-jellybean-mr2
LOCAL_SDK_VERSION := 18
LOCAL_SRC_FILES := $(call all-java-files-under, jellybean-mr2)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-jellybean
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of KitKat APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-kitkat
LOCAL_SDK_VERSION := 19
LOCAL_SRC_FILES := $(call all-java-files-under, kitkat)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-jellybean-mr2
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of V20 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-api20
LOCAL_SDK_VERSION := 20
LOCAL_SRC_FILES := $(call all-java-files-under, api20)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-kitkat
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Lollipop APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-api21
LOCAL_SDK_VERSION := 21
LOCAL_SRC_FILES := $(call all-java-files-under, api21)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-api20
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of V22 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-api22
LOCAL_SDK_VERSION := 22
LOCAL_SRC_FILES := $(call all-java-files-under, api22)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-api21
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of V23 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-api23
LOCAL_SDK_VERSION := 23
LOCAL_SRC_FILES := $(call all-java-files-under, api23)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-api22
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of V24 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v4-api24
LOCAL_SDK_VERSION := $(SUPPORT_CURRENT_SDK_VERSION)
LOCAL_SRC_FILES := $(call all-java-files-under, api24)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-api23
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# Here is the final static library that apps can link against.
include $(CLEAR_VARS)
LOCAL_USE_AAPT2 := true
LOCAL_MODULE := android-support-core
LOCAL_SDK_VERSION := 4
LOCAL_AIDL_INCLUDES := frameworks/support/v4/java
LOCAL_SRC_FILES := $(call all-java-files-under, java) \
    $(call all-Iaidl-files-under, java)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4-api24
LOCAL_SHARED_ANDROID_LIBRARIES := \
    android-support-compat \
    android-support-annotations
LOCAL_JAR_EXCLUDE_FILES := none
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)
support_module_src_files += $(call all-java-files-under, ../compat/java)
support_module_src_files += $(call all-java-files-under, ../compat/donut)
support_module_src_files += $(call all-java-files-under, ../compat/honeycomb_mr2)
support_module_src_files += $(call all-java-files-under, ../compat/ics)
support_module_aidl_includes := $(LOCAL_AIDL_INCLUDES)

# API Check
# ---------------------------------------------
support_module := $(LOCAL_MODULE)
support_module_api_dir := $(LOCAL_PATH)/api
support_module_java_libraries := $(LOCAL_JAVA_LIBRARIES)
support_module_java_packages := android.support.v4.*
include $(SUPPORT_API_CHECK)
