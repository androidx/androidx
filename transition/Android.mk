# Copyright (C) 2016 The Android Open Source Project
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

# Build the resources using the latest applicable SDK version.
# We do this here because the final static library must be compiled with an older
# SDK version than the resources.  The resources library and the R class that it
# contains will not be linked into the final static library.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-transition-res
LOCAL_SDK_VERSION := $(SUPPORT_CURRENT_SDK_VERSION)
LOCAL_SRC_FILES := $(call all-java-files-under, dummy)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_JAR_EXCLUDE_FILES := none
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
include $(BUILD_STATIC_JAVA_LIBRARY)

# A helper sub-library to resolve cyclic dependencies between Transition and platform dependent
# implementations
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-transition-base
LOCAL_SDK_VERSION := 14
LOCAL_SRC_FILES := $(call all-java-files-under, base)
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
LOCAL_JAVA_LIBRARIES := android-support-transition-res \
    android-support-v4
include $(BUILD_STATIC_JAVA_LIBRARY)

# A helper sub-library that makes direct use of Ice Cream Sandwich APIs
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-transition-ics
LOCAL_SDK_VERSION := 14
LOCAL_SRC_FILES := $(call all-java-files-under, ics)
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
LOCAL_STATIC_JAVA_LIBRARIES := android-support-transition-base
LOCAL_JAVA_LIBRARIES := android-support-transition-res \
    android-support-v4
include $(BUILD_STATIC_JAVA_LIBRARY)

# A helper sub-library that makes direct use of KitKat APIs
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-transition-kitkat
LOCAL_SDK_VERSION := 19
LOCAL_SRC_FILES := $(call all-java-files-under, kitkat)
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
LOCAL_STATIC_JAVA_LIBRARIES := android-support-transition-ics
LOCAL_JAVA_LIBRARIES := android-support-transition-res \
    android-support-v4
include $(BUILD_STATIC_JAVA_LIBRARY)

# A helper sub-library that makes direct use of Lollipop APIs
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-transition-api21
LOCAL_SDK_VERSION := 21
LOCAL_SRC_FILES := $(call all-java-files-under, api21)
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
LOCAL_STATIC_JAVA_LIBRARIES := android-support-transition-kitkat
LOCAL_JAVA_LIBRARIES := android-support-transition-res \
    android-support-v4
include $(BUILD_STATIC_JAVA_LIBRARY)

# A helper sub-library that makes direct use of Marshmallow APIs
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-transition-api23
LOCAL_SDK_VERSION := 23
LOCAL_SRC_FILES := $(call all-java-files-under, api23)
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
LOCAL_STATIC_JAVA_LIBRARIES := android-support-transition-api21
LOCAL_JAVA_LIBRARIES := android-support-transition-res \
    android-support-v4
include $(BUILD_STATIC_JAVA_LIBRARY)

# Here is the final static library that apps can link against.
# The R class is automatically excluded from the generated library.
# Applications that use this library must specify LOCAL_RESOURCE_DIR
# in their makefiles to include the resources in their package.
include $(CLEAR_VARS)
LOCAL_MODULE := android-support-transition
LOCAL_SDK_VERSION := $(SUPPORT_CURRENT_SDK_VERSION)
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAVA_LANGUAGE_VERSION := 1.7
LOCAL_STATIC_JAVA_LIBRARIES := android-support-transition-api23
LOCAL_JAVA_LIBRARIES := android-support-transition-res \
    android-support-v4
include $(BUILD_STATIC_JAVA_LIBRARY)
