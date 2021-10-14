/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fully drawn benchmark, used to create sample startup
 * traces with reportFullyDrawn ~500ms after resume
 *
 * As this is just used to provide sample / test traces, it's only ever one iteration with no
 * parameterization beyond startup mode
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class TrivialStartupFullyDrawnBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    fun startup(startupMode: StartupMode) = benchmarkRule.measureRepeated(
        compilationMode = CompilationMode.None,
        packageName = TARGET_PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        startupMode = startupMode,
        iterations = 1
    ) {
        pressHome()
        startActivityAndWait(Intent().apply {
            setPackage(TARGET_PACKAGE_NAME)
            action = "androidx.benchmark.integration.macrobenchmark.target" +
                ".TRIVIAL_STARTUP_FULLY_DRAWN_ACTIVITY"
        })
        val fullDisplayComplete = UiDevice
            .getInstance(InstrumentationRegistry.getInstrumentation())
            .wait(Until.findObject(By.text("FULL DISPLAY")), 3000) != null
        check(fullDisplayComplete)
    }

    @Test
    fun hot() = startup(StartupMode.HOT)

    @Test
    fun warm() = startup(StartupMode.WARM)

    @Test
    fun cold() = startup(StartupMode.COLD)

    companion object {
        const val TARGET_PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"
    }
}
