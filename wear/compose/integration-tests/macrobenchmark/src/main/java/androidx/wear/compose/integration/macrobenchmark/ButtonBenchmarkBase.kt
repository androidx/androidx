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
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

abstract class ButtonBenchmarkBase(
    private val compilationMode: CompilationMode,
    private val activityAction: String
) {
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
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = {
                val intent = Intent()
                intent.action = activityAction
                startActivityAndWait(intent)
            }
        ) {
            val buttons = buildList {
                repeat(4) { add(device.findObject(By.desc(numberedContentDescription(it)))) }
            }
            repeat(3) {
                for (button in buttons) {
                    button.click(50)
                    device.waitForIdle()
                }
                Thread.sleep(500)
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.wear.compose.integration.macrobenchmark.target"
    }
}
