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

# Build the resources using the current SDK version.
# We do this here because the final static library must be compiled with an older
# SDK version than the resources.  The resources library and the R class that it
# contains will not be linked into the final static library.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v17-leanback-res
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := $(call all-java-files-under, dummy)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_AAPT_FLAGS := \
        --auto-add-overlay
LOCAL_JAR_EXCLUDE_FILES := none
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

#  Base sub-library contains classes both needed by api-level specific libraries
#  (e.g. KitKat) and final static library.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v17-leanback-common
LOCAL_SDK_VERSION := 17
LOCAL_SRC_FILES := $(call all-java-files-under, common)
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

#  A helper sub-library that makes direct use of API 21.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v17-leanback-api21
LOCAL_SDK_VERSION := 21
LOCAL_SRC_FILES := $(call all-java-files-under, api21)
LOCAL_JAVA_LIBRARIES := android-support-v17-leanback-res android-support-v17-leanback-common
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

#  A helper sub-library that makes direct use of KitKat APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v17-leanback-kitkat
LOCAL_SDK_VERSION := 19
LOCAL_SRC_FILES := $(call all-java-files-under, kitkat)
LOCAL_JAVA_LIBRARIES := android-support-v17-leanback-res android-support-v17-leanback-common
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

#  A helper sub-library that makes direct use of JBMR2 APIs.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v17-leanback-jbmr2
LOCAL_SDK_VERSION := 18
LOCAL_SRC_FILES := $(call all-java-files-under, jbmr2)
LOCAL_JAVA_LIBRARIES := android-support-v17-leanback-res android-support-v17-leanback-common
include $(BUILD_STATIC_JAVA_LIBRARY)

# -----------------------------------------------------------------------

# Here is the final static library that apps can link against.
# The R class is automatically excluded from the generated library.
# Applications that use this library must specify LOCAL_RESOURCE_DIR
# in their makefiles to include the resources in their package.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-v17-leanback
LOCAL_SDK_VERSION := 17
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v17-leanback-kitkat android-support-v17-leanback-jbmr2 \
        android-support-v17-leanback-api21 android-support-v17-leanback-common
LOCAL_JAVA_LIBRARIES := \
        android-support-v4 \
        android-support-v7-recyclerview \
        android-support-v17-leanback-res
include $(BUILD_STATIC_JAVA_LIBRARY)


# ===========================================================
# Common Droiddoc vars
leanback.docs.src_files := \
    $(call all-java-files-under, src) \
    $(call all-html-files-under, src)
leanback.docs.java_libraries := \
    framework \
    android-support-v4 \
    android-support-v7-recyclerview \
    android-support-v17-leanback-res \
    android-support-v17-leanback

# Documentation
# ===========================================================
include $(CLEAR_VARS)

LOCAL_MODULE := android-support-v17-leanback
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional

intermediates.COMMON := $(call intermediates-dir-for,$(LOCAL_MODULE_CLASS),android-support-v17-leanback,,COMMON)

LOCAL_SRC_FILES := $(leanback.docs.src_files)
LOCAL_ADDITONAL_JAVA_DIR := $(intermediates.COMMON)/src

LOCAL_SDK_VERSION := 19
LOCAL_IS_HOST_MODULE := false
LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := build/tools/droiddoc/templates-sdk

LOCAL_JAVA_LIBRARIES := $(leanback.docs.java_libraries)

LOCAL_DROIDDOC_OPTIONS := \
    -offlinemode \
    -hdf android.whichdoc offline \
    -federate Android http://developer.android.com \
    -federationapi Android prebuilts/sdk/api/17.txt \
    -hide 113

include $(BUILD_DROIDDOC)

# Stub source files
# ===========================================================

leanback_internal_api_file := $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/android-support-v17-leanback_api.txt
leanback.docs.stubpackages := android.support.v17.leanback:android.support.v17.leanback.app:android.support.v17.leanback.database:android.support.v17.leanback.widget

include $(CLEAR_VARS)

LOCAL_MODULE := android-support-v17-leanback-stubs
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(leanback.docs.src_files)
LOCAL_JAVA_LIBRARIES := $(leanback.docs.java_libraries)
LOCAL_SDK_VERSION := current

LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := build/tools/droiddoc/templates-sdk
LOCAL_UNINSTALLABLE_MODULE := true

LOCAL_DROIDDOC_OPTIONS := \
    -stubs $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/android-support-v17-leanback-stubs_intermediates/src \
    -stubpackages $(leanback.docs.stubpackages) \
    -api $(leanback_internal_api_file) \
    -hide 113 \
    -nodocs

include $(BUILD_DROIDDOC)
leanback_stubs_stamp := $(full_target)
$(leanback_internal_api_file) : $(full_target)

# Cleanup temp vars
# ===========================================================
leanback.docs.src_files :=
leanback.docs.java_libraries :=
intermediates.COMMON :=
leanback_internal_api_file :=
leanback_stubs_stamp :=
leanback.docs.stubpackages :=
