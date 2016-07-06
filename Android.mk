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

SUPPORT_CURRENT_SDK_VERSION := current

###########################################################
# Find all of the files in the given subdirs that match the
# specified pattern but do not match another pattern. This
# function uses $(1) instead of LOCAL_PATH as the base.
# $(1): the base dir, relative to the root of the source tree.
# $(2): the file name pattern to match.
# $(3): the file name pattern to exclude.
# $(4): a list of subdirs of the base dir.
# Returns: a list of paths relative to the base dir.
###########################################################

define find-files-in-subdirs-exclude
$(sort $(patsubst ./%,%, \
  $(shell cd $(1) ; \
          find -L $(4) -name $(2) -and -not -name $(3) -and -not -name ".*") \
 ))
endef

###########################################################
## Find all of the files under the named directories where
## the file name matches the specified pattern but does not
## match another pattern. Meant to be used like:
##    SRC_FILES := $(call all-named-files-under,.*\.h,src tests)
###########################################################

define all-named-files-under-exclude
$(call find-files-in-subdirs-exclude,$(LOCAL_PATH),"$(1)","$(2)",$(3))
endef

###########################################################
## Find all of the files under the current directory where
## the file name matches the specified pattern but does not
## match another pattern.
###########################################################

define all-subdir-named-files-exclude
$(call all-named-files-under-exclude,$(1),$(2),.)
endef

# Proxy to gradle task for updating API
.PHONY: update-support-api
update-support-api: PRIVATE_LOCAL_PATH := $(LOCAL_PATH)
update-support-api:
	$(PRIVATE_LOCAL_PATH)/gradlew -p $(PRIVATE_LOCAL_PATH) updateApi

# Proxy to gradle task for checking API
.PHONY: check-support-api
check-support-api: PRIVATE_LOCAL_PATH := $(LOCAL_PATH)
check-support-api:
	$(PRIVATE_LOCAL_PATH)/gradlew -p $(PRIVATE_LOCAL_PATH) checkApi

# Proxy to gradle task for generating docs
.PHONY: support-docs
support-docs: PRIVATE_LOCAL_PATH := $(LOCAL_PATH)
support-docs:
	$(PRIVATE_LOCAL_PATH)/gradlew -p $(PRIVATE_LOCAL_PATH) generateDocs

# Pre-process support library AIDLs
aidl_files := $(addprefix $(LOCAL_PATH)/, $(call all-subdir-named-files-exclude,*.aidl,I*.aidl))
support-aidl := $(TARGET_OUT_COMMON_INTERMEDIATES)/support.aidl
$(support-aidl): $(aidl_files) | $(AIDL)
	$(AIDL) --preprocess $@ $(aidl_files)

# Check APIs and generate support AIDL file for SDK build
sdk: check-support-api $(support-aidl)

# Build all support libraries
include $(call all-makefiles-under,$(LOCAL_PATH))

# Clear out variables
SUPPORT_CURRENT_SDK_VERSION :=

endif
