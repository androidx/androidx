#
# Copyright 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#

cmake_minimum_required(VERSION 3.22.1)

project(arcore_test_app_jni)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror")

add_library(nativedata_jni SHARED nativedata_jni.cpp)

target_link_options(
  nativedata_jni
  PRIVATE
  "-Wl,-z,max-page-size=16384"
  "-Wl,-z,common-page-size=16384"
)
