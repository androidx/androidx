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

# A helper sub-library that makes direct use of Gingerbread APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-fragment-gingerbread
LOCAL_SDK_VERSION := 9
LOCAL_SRC_FILES := $(call all-java-files-under, gingerbread)
LOCAL_JAVA_LIBRARIES := \
    android-support-annotations \
    android-support-compat \
    android-support-core-utils \
    android-support-media-compat \
    android-support-core-ui
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files := $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Honeycomb APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-fragment-honeycomb
LOCAL_SDK_VERSION := 11
LOCAL_SRC_FILES := $(call all-java-files-under, honeycomb)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-fragment-gingerbread
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of JellyBean APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-fragment-jellybean
LOCAL_SDK_VERSION := 16
LOCAL_SRC_FILES := $(call all-java-files-under, jellybean)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-fragment-honeycomb
LOCAL_JAVA_LIBRARIES := \
    android-support-annotations \
    android-support-compat \
    android-support-media-compat \
    android-support-core-ui \
    android-support-core-utils
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# A helper sub-library that makes direct use of Lollipop APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-fragment-api21
LOCAL_SDK_VERSION := 21
LOCAL_SRC_FILES := $(call all-java-files-under, api21)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-fragment-jellybean
LOCAL_JAVA_LIBRARIES := \
    android-support-annotations \
    android-support-compat \
    android-support-media-compat \
    android-support-core-ui \
    android-support-core-utils
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)

# -----------------------------------------------------------------------

# Here is the final static library that apps can link against.
include $(CLEAR_VARS)
LOCAL_USE_AAPT2 := true
LOCAL_MODULE := android-support-fragment
LOCAL_SDK_VERSION := 9
LOCAL_AIDL_INCLUDES := frameworks/support/fragment/java
LOCAL_SRC_FILES := $(call all-java-files-under, java) \
    $(call all-Iaidl-files-under, java)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_STATIC_JAVA_LIBRARIES := android-support-fragment-api21
LOCAL_JAVA_LIBRARIES := \
    android-support-annotations \
    android-support-compat \
    android-support-media-compat \
    android-support-core-ui \
    android-support-core-utils
LOCAL_JAR_EXCLUDE_FILES := none
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
LOCAL_AAPT_FLAGS := --add-javadoc-annotation doconly
include $(BUILD_STATIC_JAVA_LIBRARY)

support_module_src_files += $(LOCAL_SRC_FILES)
support_module_aidl_includes := $(LOCAL_AIDL_INCLUDES)

# We're asking doclava to generate stubs for android.support.v4.app
# so we'll have to point doclava at the sources for additional classes
# in that package. Note that this API definition will overlap with that
# of those classes in compat module.
support_module_src_files += \
  ../compat/java/android/support/v4/app/SharedElementCallback.java

# API Check
# ---------------------------------------------
support_module := $(LOCAL_MODULE)
support_module_api_dir := $(LOCAL_PATH)/api
support_module_java_libraries := $(LOCAL_JAVA_LIBRARIES)
support_module_java_packages := android.support.v4.app
include $(SUPPORT_API_CHECK)
