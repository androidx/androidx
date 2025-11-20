#!/bin/bash

set -eu

KSP_TASKS=":room3:integration-tests:room-testapp-kotlin:kspWithKspGenJavaDebugAndroidTestKotlin"
KAPT_TASKS=":room3:integration-tests:room-testapp-kotlin:kaptGenerateStubsWithKaptDebugAndroidTestKotlin \
:room3:integration-tests:room-testapp-kotlin:kaptWithKaptDebugAndroidTestKotlin"

kotlinc -script $(dirname $0)/tasks-comparison.kts -- \
  -t "ksp" $KSP_TASKS \
  -t "kapt" $KAPT_TASKS
