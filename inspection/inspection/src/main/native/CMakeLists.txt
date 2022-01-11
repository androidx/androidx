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

cmake_minimum_required(VERSION 3.4.1)

project(compose_inspection_inspection)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -lz -Wall -Werror -Wno-unused-command-line-argument -Wno-shift-count-overflow")

include_directories(
        ../../../../../../../external/dexter/slicer/export
        jvmti
        util
)

aux_source_directory(../../../../../../../external/dexter/slicer SLICER)

add_library(
        art_tooling SHARED
        inspection/array_params_entry_hook.cc
        inspection/art_tooling_impl.cc
        inspection/art_tooling_java_jni.cc
        jvmti/hidden_api_silencer.cc
        jvmti/jvmti_helper.cc
        util/properties.cc
        ${SLICER}
)

find_library(android-lib android)
find_library(log-lib log)

# jvmti.h isn't distributed with android ndk, so we need to compile against jdk's one.
# Unfortunately we can't simply use it, because of "include "jni.h"" in it.
# Include with quotes priorities file's directory, so it resolves jdk's jni instead of ndk's one.
# To avoid jdk's jni jvmti.h is copied in another folder.
configure_file($ENV{JAVA_HOME}/include/jvmti.h external_jvmti/jvmti.h COPYONLY)

target_include_directories(art_tooling
        PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/external_jvmti)
target_link_libraries(art_tooling ${log-lib})
