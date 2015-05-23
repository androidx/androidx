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

# Here is the final static library that apps can link against.
include $(CLEAR_VARS)

previewsdk_generate_constants_exe := $(LOCAL_PATH)/previewconstants.sh
previewsdk_gen_java_files := $(TARGET_OUT_COMMON_GEN)/previewsdk/PreviewSdkConstants.java

$(previewsdk_gen_java_files): $(previewsdk_generate_constants_exe)
	$(hide) mkdir -p $(dir $@)
	$(hide) PLATFORM_PREVIEW_SDK_VERSION="$(PLATFORM_PREVIEW_SDK_VERSION)" \
		bash $< > $@

LOCAL_MODULE := android-support-previewsdk
LOCAL_SDK_VERSION := current
LOCAL_GENERATED_SOURCES := $(previewsdk_gen_java_files)
LOCAL_SRC_FILES := $(call all-java-files-under, src)
include $(BUILD_STATIC_JAVA_LIBRARY)
