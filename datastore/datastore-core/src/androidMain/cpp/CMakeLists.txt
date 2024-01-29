#
# Copyright (C) 2022 The Android Open Source Project
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

cmake_minimum_required(VERSION 3.22)

project(datastore_shared_counter)

set (CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror")

add_library(shared_counter STATIC shared_counter.cc)
add_library(datastore_shared_counter SHARED jni/androidx_datastore_core_SharedCounter.cc)

target_link_libraries(datastore_shared_counter shared_counter)
target_link_options(
    datastore_shared_counter
    PRIVATE
    "-Wl,-z,max-page-size=16384"
)
