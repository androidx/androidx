/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.test.uiautomator.UiDevice
import androidx.testutils.createCompilationParams
import java.lang.Thread.sleep
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PositionIndicatorBenchmark(private val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Before
    fun setUp() {
        disableChargingExperience()
    }

    @After
    fun destroy() {
        enableChargingExperience()
    }

    @Test
    fun start() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(FrameTimingGfxInfoMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Last)),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = POSITION_INDICATOR_ACTIVITY
                startActivityAndWait(intent)
            }
        ) {
            val buttonVisibilityAutoHide = device.findObject(By.desc(CHANGE_VISIBILITY_AUTO_HIDE))
            val buttonVisibilityHide = device.findObject(By.desc(CHANGE_VISIBILITY_HIDE))
            val buttonVisibilityShow = device.findObject(By.desc(CHANGE_VISIBILITY_SHOW))

            buttonVisibilityShow?.click()

            // By default indicator visibility is Show
            // Increase and decrease indicator 10 times 1 direction and 10 times another
            repeatIncrementAndDecrement(device, 10, 200)

            // Switch from Show to AutoHide
            buttonVisibilityAutoHide?.click()

            // Increase and decrease indicator with delay shorter than hiding delay
            repeatIncrementAndDecrement(device, 10, 200)

            // Increase and decrease indicator with delay longer than hiding delay
            repeatIncrementAndDecrement(device, 3, 2500)

            // Switch from Autohide to Hide
            buttonVisibilityHide?.click()

            // Increase and decrease indicator 10 times 1 direction and 10 times another
            repeatIncrementAndDecrement(device, 10, 200)
        }
    }

    private fun repeatIncrementAndDecrement(
        device: UiDevice,
        times: Int,
        delayBetweenClicks: Long
    ) {
        val buttonIncrease = device.findObject(By.desc(INCREASE_POSITION))
        val buttonDecrease = device.findObject(By.desc(DECREASE_POSITION))

        repeat(times) {
            buttonIncrease?.click()
            device.waitForIdle()
            sleep(delayBetweenClicks)
        }

        repeat(times) {
            buttonDecrease?.click()
            device.waitForIdle()
            sleep(delayBetweenClicks)
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.wear.compose.integration.macrobenchmark.target"
        private const val POSITION_INDICATOR_ACTIVITY =
            "${PACKAGE_NAME}.POSITION_INDICATOR_ACTIVITY"
        private const val INCREASE_POSITION = "PI_INCREASE_POSITION"
        private const val DECREASE_POSITION = "PI_DECREASE_POSITION"
        private const val CHANGE_VISIBILITY_SHOW = "PI_VISIBILITY_SHOW"
        private const val CHANGE_VISIBILITY_HIDE = "PI_VISIBILITY_HIDE"
        private const val CHANGE_VISIBILITY_AUTO_HIDE = "PI_VISIBILITY_AUTO_HIDE"

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
