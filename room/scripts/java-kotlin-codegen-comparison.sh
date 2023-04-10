#!/bin/bash

#
# Copyright 2023 The Android Open Source Project
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

set -eu

JAVA_GEN_TASKS=":room:integration-tests:room-testapp-kotlin:kspWithKspGenJavaDebugAndroidTestKotlin \
:room:integration-tests:room-testapp-kotlin:compileWithKspGenJavaDebugAndroidTestKotlin \
:room:integration-tests:room-testapp-kotlin:compileWithKspGenJavaDebugAndroidTestJavaWithJavac"
KOTLIN_GEN_TASKS=":room:integration-tests:room-testapp-kotlin:kspWithKspGenKotlinDebugAndroidTestKotlin \
:room:integration-tests:room-testapp-kotlin:compileWithKspGenKotlinDebugAndroidTestKotlin \
:room:integration-tests:room-testapp-kotlin:compileWithKspGenKotlinDebugAndroidTestJavaWithJavac"

kotlinc -script $(dirname $0)/tasks-comparison.kts -- \
  -t "java-codegen" $JAVA_GEN_TASKS \
  -t "kotlin-codegen" $KOTLIN_GEN_TASKS
