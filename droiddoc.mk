#
# Copyright (C) 2017 The Android Open Source Project
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

# API Level information for the Support Library, which is currently
# included as part of the core framework docs build.
SUPPORT_PATH := $(call my-dir)

framework_docs_LOCAL_DROIDDOC_OPTIONS += \
    -since $(SUPPORT_PATH)/api/22.0.0.txt 22.0.0 \
    -since $(SUPPORT_PATH)/api/22.1.0.txt 22.1.0 \
    -since $(SUPPORT_PATH)/api/22.2.0.txt 22.2.0 \
    -since $(SUPPORT_PATH)/api/22.2.1.txt 22.2.1 \
    -since $(SUPPORT_PATH)/api/23.0.0.txt 23.0.0 \
    -since $(SUPPORT_PATH)/api/23.1.0.txt 23.1.0 \
    -since $(SUPPORT_PATH)/api/23.1.1.txt 23.1.1 \
    -since $(SUPPORT_PATH)/api/23.2.0.txt 23.2.0 \
    -since $(SUPPORT_PATH)/api/23.2.1.txt 23.2.1 \
    -since $(SUPPORT_PATH)/api/23.4.0.txt 23.4.0 \
    -since $(SUPPORT_PATH)/api/24.0.0.txt 24.0.0 \
    -since $(SUPPORT_PATH)/api/24.1.0.txt 24.1.0 \
    -since $(SUPPORT_PATH)/api/24.2.0.txt 24.2.0 \
    -since $(SUPPORT_PATH)/api/25.0.0.txt 25.0.0 \
    -since $(SUPPORT_PATH)/api/25.1.0.txt 25.1.0 \
    -since $(SUPPORT_PATH)/api/25.2.0.txt 25.2.0 \
    -since $(SUPPORT_PATH)/api/25.3.0.txt 25.3.0 \
    -since $(SUPPORT_PATH)/api/25.4.0.txt 25.4.0 \
    -since $(SUPPORT_PATH)/api/26.0.0-alpha1.txt 26.0.0-alpha1 \
    -since $(SUPPORT_PATH)/api/26.0.0-beta1.txt 26.0.0-beta1 \
    -since $(SUPPORT_PATH)/api/26.0.0-beta2.txt 26.0.0-beta2
