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

package androidx.wear.compose.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.testutils.createCompilationParams
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(Parameterized::class)
class CurvedLayoutBenchmark(private val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    private fun test(action: String) {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(FrameTimingGfxInfoMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Last)),
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = {
                val intent = Intent()
                intent.action = action
                startActivityAndWait(intent)
            },
        ) {
            repeat(10) {
                device
                    .wait(Until.findObject(By.desc(TOGGLE_DISPLAY)), FIND_OBJECT_TIMEOUT_MS)
                    .click()
                device.waitForIdle()
            }
        }
    }

    @Before
    fun setUp() {
        disableChargingExperience()
        pressHome()
    }

    @After
    fun destroy() {
        enableChargingExperience()
    }

    @Test
    fun startSimple() {
        test(SIMPLE_CURVED_LAYOUT_ACTIVITY)
    }

    @Test
    fun startComplex() {
        test(COMPLEX_CURVED_LAYOUT_ACTIVITY)
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.wear.compose.integration.macrobenchmark.target"
        private const val SIMPLE_CURVED_LAYOUT_ACTIVITY =
            "${PACKAGE_NAME}.SIMPLE_CURVED_LAYOUT_ACTIVITY"
        private const val COMPLEX_CURVED_LAYOUT_ACTIVITY =
            "${PACKAGE_NAME}.COMPLEX_CURVED_LAYOUT_ACTIVITY"

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}

private const val TOGGLE_DISPLAY = "TOGGLE_DISPLAY"
private const val FIND_OBJECT_TIMEOUT_MS = 10_000L
