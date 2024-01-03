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
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.testutils.createCompilationParams
import java.lang.Thread.sleep
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PositionIndicatorBenchmark(
    private val compilationMode: CompilationMode
) {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Test
    fun start() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(
                FrameTimingMetric()
            ),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = ACTION
                startActivityAndWait(intent)
            }
        ) {
            val buttonShow = device.findObject(By.desc(CHANGE_VISIBILITY))
            val buttonIncrease = device.findObject(By.desc(INCREASE_POSITION))
            val buttonDecrease = device.findObject(By.desc(DECREASE_POSITION))

            // Setting a gesture margin is important otherwise gesture nav is triggered.
            repeat(10) {
                buttonIncrease?.let { it.click() }
                device.waitForIdle()
                sleep(500)
            }

            repeat(10) {
                buttonDecrease?.let { it.click() }
                device.waitForIdle()
                sleep(500)
            }

            repeat(4) {
                buttonShow?.let { it.click() }
                sleep(3000)
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.wear.compose.integration.macrobenchmark.target"
        private const val ACTION =
            "androidx.wear.compose.integration.macrobenchmark.target" +
                ".POSITION_INDICATOR_ACTIVITY"
        private const val INCREASE_POSITION = "PI_INCREASE_POSITION"
        private const val DECREASE_POSITION = "PI_DECREASE_POSITION"
        private const val CHANGE_VISIBILITY = "PI_VISIBILITY"

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
