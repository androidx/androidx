#
# Copyright (C) 2012 The Android Open Source Project
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

# Don't build the library in unbundled branches.
ifeq (,$(TARGET_BUILD_APPS))

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS += -std=c++11

LOCAL_MODULE := android-support-v8-renderscript
LOCAL_SDK_VERSION := 23
LOCAL_SRC_FILES := $(call all-java-files-under, java/src)
LOCAL_JAVA_LIBRARIES := android-support-annotations

# Disable JACK when RSTest_Compatlib is used for updating RS prebuilts.
ifneq (,$(UPDATE_RS_PREBUILTS_DISABLE_JACK))
LOCAL_JACK_ENABLED := disabled
endif

include $(BUILD_STATIC_JAVA_LIBRARY)

# API Check
# ---------------------------------------------
support_module := $(LOCAL_MODULE)
support_module_api_dir := $(LOCAL_PATH)/api
support_module_src_files := $(LOCAL_SRC_FILES)
support_module_java_libraries := $(LOCAL_MODULE)
support_module_java_packages := android.support.v8.renderscript
include $(SUPPORT_API_CHECK)

# TODO: Build the tests as an APK here

include $(call all-makefiles-under, $(LOCAL_PATH))

endif
