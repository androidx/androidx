/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.scenecore.openxr.smoketest

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class SceneCoreOpenXrSmokeTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.scenecore.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(SceneCoreOpenXrSmokeTestActivity::class.java)

    @Test
    fun executeSceneCoreOpenXrSmokeTest_verifiesPhase1TopDownOpenXrCapabilities() {
        activityRule.scenario.onActivity { activity ->
            val results = activity.getResults()

            assertThat(results).isNotEmpty()

            // Verify Step 1: Native Prebuilt & Handle Creation
            val step1 = results.first { it.stepNumber == 1 }
            assertThat(step1.status).isEqualTo(SceneCoreOpenXrSmokeTestActivity.StepStatus.PASSED)

            // Verify Step 2: OpenXrSceneRuntimeFactory Requirements Query
            val step2 = results.first { it.stepNumber == 2 }
            assertThat(step2.status).isEqualTo(SceneCoreOpenXrSmokeTestActivity.StepStatus.PASSED)

            // Verify Step 3: Top-Down OpenXrSceneRuntime Lifecycle & Teardown Protocol
            val step3 = results.first { it.stepNumber == 3 }
            assertThat(step3.status).isEqualTo(SceneCoreOpenXrSmokeTestActivity.StepStatus.PASSED)

            // Verify Step 4: Extension Negotiation & Spatial Container Capability
            val step4 = results.first { it.stepNumber == 4 }
            assertThat(step4.status).isEqualTo(SceneCoreOpenXrSmokeTestActivity.StepStatus.PASSED)

            // Ensure zero unexpected step failures occurred across the entire checklist
            val failedSteps =
                results.filter { it.status == SceneCoreOpenXrSmokeTestActivity.StepStatus.FAILED }
            assertThat(failedSteps).isEmpty()
        }
    }
}
