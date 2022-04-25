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

project(tracing_perfetto)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror")

find_package(Threads)

add_library(perfetto STATIC perfetto/perfetto.cc)
add_library(tracing_perfetto SHARED tracing_perfetto.cc trace_categories.cc jni/androidx_tracing_perfetto_jni_PerfettoNative.cc)

find_library(android-lib android)
find_library(log-lib log)

target_link_libraries(tracing_perfetto ${android-lib} ${log-lib} perfetto ${CMAKE_THREAD_LIBS_INIT})
