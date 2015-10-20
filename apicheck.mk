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

#
# Input variables
#
# $(support_module) - name of the support library module
# $(support_module_api_dir) - dir to store API files
# $(support_module_java_libraries) - dependent libraries
# $(support_module_java_packages) - list of package names containing public classes
# $(support_module_src_files) - list of source files
# $(support_module_aidl_includes) - list of aidl files
# $(api_check_current_msg_file) - file containing error message for current API check
# $(api_check_last_msg_file) - file containing error message for last SDK API check
# ---------------------------------------------

#
# Generate the stub source files
# ---------------------------------------------
include $(CLEAR_VARS)

support_module_api_file := \
    $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/$(support_module)_api.txt
support_module_removed_file := \
    $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/$(support_module)_removed.txt

LOCAL_MODULE := $(support_module)-stubs
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(support_module_src_files)
LOCAL_AIDL_INCLUDES := $(support_module_aidl_includes)
LOCAL_JAVA_LIBRARIES := $(support_module_java_libraries)
LOCAL_ADDITIONAL_JAVA_DIR := \
    $(call intermediates-dir-for,$(LOCAL_MODULE_CLASS),$(support_module),,COMMON)/src
LOCAL_SDK_VERSION := current

LOCAL_DROIDDOC_OPTIONS:= \
    -stubs $(TARGET_OUT_COMMON_INTERMEDIATES)/$(LOCAL_MODULE_CLASS)/$(LOCAL_MODULE)_intermediates/src \
    -stubpackages "$(subst $(space),:,$(support_module_java_packages))" \
    -api $(support_module_api_file) \
    -removedApi $(support_module_removed_file) \
    -nodocs

LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := build/tools/droiddoc/templates-sdk
LOCAL_UNINSTALLABLE_MODULE := true

include $(BUILD_DROIDDOC)
support_stub_stamp := $(full_target)
$(support_module_api_file) : $(full_target)

#
# Check API
# ---------------------------------------------
last_released_sdk_$(support_module) := $(lastword $(call numerically_sort, \
    $(filter-out current, \
        $(patsubst $(support_module_api_dir)/%.txt,%, $(wildcard $(support_module_api_dir)/*.txt)) \
    )))

# Check that the API we're building hasn't broken the last-released SDK version
# if it exists
ifneq ($(last_released_sdk_$(support_module)),)
$(eval $(call check-api, \
    $(support_module)-checkapi-last, \
    $(support_module_api_dir)/$(last_released_sdk_$(support_module)).txt, \
    $(support_module_api_file), \
    $(support_module_api_dir)/removed.txt, \
    $(support_module_removed_file), \
    -hide 2 -hide 3 -hide 4 -hide 5 -hide 6 -hide 24 -hide 25 -hide 26 -hide 27 \
        -warning 7 -warning 8 -warning 9 -warning 10 -warning 11 -warning 12 \
        -warning 13 -warning 14 -warning 15 -warning 16 -warning 17 -warning 18, \
    cat $(api_check_last_msg_file), \
    check-support-api, \
    $(support_stub_stamp)))
endif

# Check that the API we're building hasn't changed from the not-yet-released
# SDK version.
$(eval $(call check-api, \
    $(support_module)-checkapi-current, \
    $(support_module_api_dir)/current.txt, \
    $(support_module_api_file), \
    $(support_module_api_dir)/removed.txt, \
    $(support_module_removed_file), \
    -error 2 -error 3 -error 4 -error 5 -error 6 -error 7 -error 8 -error 9 -error 10 -error 11 \
        -error 12 -error 13 -error 14 -error 15 -error 16 -error 17 -error 18 -error 19 -error 20 \
        -error 21 -error 23 -error 24 -error 25, \
    cat $(api_check_current_msg_file), \
    check-support-api, \
    $(support_stub_stamp)))

.PHONY: update-$(support_module)-api
update-$(support_module)-api: PRIVATE_API_DIR := $(support_module_api_dir)
update-$(support_module)-api: PRIVATE_MODULE := $(support_module)
update-$(support_module)-api: PRIVATE_REMOVED_API_FILE := $(support_module_removed_file)
update-$(support_module)-api: $(support_module_api_file) | $(ACP)
	@echo Copying $(PRIVATE_MODULE) current.txt
	$(hide) $(ACP) $< $(PRIVATE_API_DIR)/current.txt
	@echo Copying $(PRIVATE_MODULE) removed.txt
	$(hide) $(ACP) $(PRIVATE_REMOVED_API_FILE) $(PRIVATE_API_DIR)/removed.txt

# Run this update API task on the update-support-api task
update-support-api: update-$(support_module)-api

#
# Clear variables
# ---------------------------------------------
support_module :=
support_module_api_dir :=
support_module_src_files :=
support_module_aidl_includes :=
support_module_java_libraries :=
support_module_java_packages :=
support_module_api_file :=
support_module_removed_file :=
support_stub_stamp :=
