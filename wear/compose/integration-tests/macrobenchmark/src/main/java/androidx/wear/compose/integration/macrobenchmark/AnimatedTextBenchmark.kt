/*
 * Copyright 2024 The Android Open Source Project
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
import android.os.Build
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class AnimatedTextBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Before
    fun setUp() {
        disableChargingExperience()
    }

    @After
    fun destroy() {
        enableChargingExperience()
    }

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun start() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(
                    FrameTimingGfxInfoMetric(),
                    MemoryUsageMetric(MemoryUsageMetric.Mode.Last),
                ),
            compilationMode = CompilationMode.DEFAULT,
            iterations = 10,
            setupBlock = {
                val intent = Intent()
                intent.action = ANIMATED_TEXT_ACTIVITY
                startActivityAndWait(intent)
            }
        ) {
            runBlocking {
                val plusButton = device.findObject(By.desc("plusContentDescription"))
                val minusButton = device.findObject(By.desc("minusContentDescription"))
                repeat(3) {
                    plusButton.click()
                    delay(250L)
                }
                repeat(3) {
                    minusButton.click()
                    delay(250L)
                }
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.wear.compose.integration.macrobenchmark.target"
        private const val ANIMATED_TEXT_ACTIVITY = "$PACKAGE_NAME.ANIMATED_TEXT_ACTIVITY"
    }
}
