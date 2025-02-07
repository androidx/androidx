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

package androidx.wear.compose.material3.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.testutils.createCompilationParams
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
abstract class BenchmarkTestBase(
    private val macrobenchmarkScreen: MacrobenchmarkScreen,
    private val actionSuffix: String,
    private val compilationMode: CompilationMode,
    private val metrics: List<Metric> =
        listOf(
            FrameTimingGfxInfoMetric(),
            MemoryUsageMetric(MemoryUsageMetric.Mode.Last),
        ),
    private val iterations: Int = 10,
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
    open fun start() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = iterations,
            setupBlock = {
                val intent = Intent()
                intent.action = "$PACKAGE_NAME.$actionSuffix"
                startActivityAndWait(intent)
            }
        ) {
            macrobenchmarkScreen.exercise.invoke(this)
        }
    }

    companion object {
        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
