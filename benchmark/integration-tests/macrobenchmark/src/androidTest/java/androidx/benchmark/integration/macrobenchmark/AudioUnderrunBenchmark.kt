/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.AudioUnderrunMetric
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23)
@OptIn(ExperimentalMetricApi::class)
class AudioUnderrunBenchmark() {
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
            metrics = listOf(AudioUnderrunMetric()),
            compilationMode = CompilationMode.Full(),
            startupMode = StartupMode.WARM,
            iterations = 1,
            setupBlock = {
                val intent = Intent()
                intent.action = ACTION
                startActivityAndWait(intent)
            }
        ) {
            // audio is played for a half of duration, ~50% of the frames would be zero
            Thread.sleep(DURATION_MS.toLong())
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"
        private const val ACTION = "$PACKAGE_NAME.AUDIO_ACTIVITY"
        private const val DURATION_MS = 2000
    }
}