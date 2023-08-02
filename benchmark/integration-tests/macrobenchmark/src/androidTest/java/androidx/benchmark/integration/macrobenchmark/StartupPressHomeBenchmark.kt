/*
 * Copyright 2022 The Android Open Source Project
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
import android.os.Build
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.testutils.STARTUP_MODES
import androidx.testutils.getStartupMetrics
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Fully drawn benchmark, used to check the difference between
 * calling pressHome() in setupBlock vs measureBlock.
 *
 * It uses more iterations to verify the behavior for Hot startupMode.
 */
@LargeTest
@RunWith(Parameterized::class)
class StartupPressHomeBenchmark(
    private val pressHomeInMeasure: Boolean,
    private val startupMode: StartupMode,
) {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        compilationMode = if (Build.VERSION.SDK_INT >= 24) {
            CompilationMode.None()
        } else {
            CompilationMode.Full()
        },
        packageName = TARGET_PACKAGE_NAME,
        metrics = getStartupMetrics(),
        startupMode = startupMode,
        iterations = 3,
        setupBlock = {
            if (!pressHomeInMeasure) {
                pressHome()
            }
        }
    ) {
        if (pressHomeInMeasure) {
            pressHome()
        }

        startActivityAndWait(Intent().apply {
            setPackage(TARGET_PACKAGE_NAME)
            action = "androidx.benchmark.integration.macrobenchmark.target" +
                ".TRIVIAL_STARTUP_FULLY_DRAWN_ACTIVITY"
        })

        val fullDisplayComplete = device.wait(Until.hasObject(By.text("FULL DISPLAY")), 3000)
        check(fullDisplayComplete)
    }

    companion object {
        const val TARGET_PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"

        @Parameterized.Parameters(name = "pressHomeInMeasure={0},startup={1}")
        @JvmStatic
        fun parameters(): List<Array<Any>> = listOf(true, false).flatMap { pressHomeInMeasure ->
            STARTUP_MODES.map { arrayOf(pressHomeInMeasure, it) }
        }
    }
}
