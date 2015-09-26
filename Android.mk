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
# Don't include in unbundled build.
ifeq ($(TARGET_BUILD_APPS),)

SUPPORT_API_CHECK := $(LOCAL_PATH)/apicheck.mk
api_check_current_msg_file := $(LOCAL_PATH)/apicheck_msg_current.txt
api_check_last_msg_file := $(LOCAL_PATH)/apicheck_msg_last.txt

.PHONY: update-support-api
.PHONY: check-support-api

.PHONY: support-gradle-archive
support-gradle-archive: PRIVATE_LOCAL_PATH := $(LOCAL_PATH)
support-gradle-archive:
	$(PRIVATE_LOCAL_PATH)/gradlew -p $(PRIVATE_LOCAL_PATH) createArchive

# Run the check-support-api task on a SDK build
sdk: check-support-api
# Run the support-gradle-archive task on a SDK build
sdk: support-gradle-archive

# Build all support libraries
include $(call all-makefiles-under,$(LOCAL_PATH))

# Clear out variables
SUPPORT_API_CHECK :=
api_check_current_msg_file :=
api_check_last_msg_file :=

endif
