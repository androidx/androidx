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

package androidx.compose.animation.macrobenchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This benchmark measures the cost of interrupting an animation with a seekTo for
 * [SeekableTransition]. Before running this benchmark, switch the target app's active build variant
 * to "benchmark" in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class SeekableTransitionInterruptMacrobenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun clickInterrupt() {
        lateinit var button: UiObject2
        benchmarkRule.measureRepeated(
            packageName = "androidx.compose.animation.benchmark.target",
            metrics = listOf(FrameTimingMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Max)),
            iterations = 10,
            // Warm startup simulates a user interacting with an app that's already warmed up
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                val buttonText = "interrupt animation w/ seekTo"
                button = device.findObject(By.text(buttonText))
            },
        ) {
            button.click()

            device.waitForIdle()
        }
    }
}
