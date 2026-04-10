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

package androidx.compose.remote.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(Parameterized::class)
open class RemoteComposeDocumentTracingMacrobenchmark(val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun documentGenerationOnly() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                recordingTraces.map { TraceSectionMetric(it) } +
                    MemoryUsageMetric(MemoryUsageMetric.Mode.Max),
            iterations = 5,
            compilationMode = compilationMode,
            startupMode = StartupMode.WARM,
            setupBlock = { pressHome() },
            measureBlock = {
                val intent = Intent()
                intent.action = DOCUMENT_GENERATING_ACTIVITY
                startActivityAndWait(intent)
                device.wait(Until.hasObject(By.text(DOCUMENT_READY)), 10000)
            },
        )

    @Test
    fun documentRenderingOnly() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(StartupTimingMetric()) +
                    decodingTraces.map { TraceSectionMetric(it) } +
                    MemoryUsageMetric(MemoryUsageMetric.Mode.Max),
            iterations = 5,
            compilationMode = compilationMode,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                val intent = Intent()
                intent.action = DOCUMENT_GENERATING_ACTIVITY
                startActivityAndWait(intent)
                device.wait(Until.hasObject(By.text(DOCUMENT_READY)), 10000)
                pressHome()
            },
            measureBlock = {
                val intent = Intent()
                intent.action = DOCUMENT_TRACING_ACTIVITY
                intent.putExtra(BENCHMARK_MODE_ARG, MODE_RENDER_FROM_CACHE)
                startActivityAndWait(intent)
                device.waitForIdle()
            },
        )

    @Test
    fun startupWithDocumentGeneration() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(StartupTimingMetric()) +
                    allTraces.map { TraceSectionMetric(it) } +
                    MemoryUsageMetric(MemoryUsageMetric.Mode.Max),
            iterations = 5,
            compilationMode = compilationMode,
            startupMode = StartupMode.WARM,
            setupBlock = { pressHome() },
            measureBlock = {
                val intent = Intent()
                intent.action = DOCUMENT_TRACING_ACTIVITY
                startActivityAndWait(intent)
                device.waitForIdle()
            },
        )

    @Test
    fun startupLocal() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            compilationMode = compilationMode,
            startupMode = StartupMode.WARM,
            setupBlock = { pressHome() },
            measureBlock = {
                val intent = Intent()
                intent.action = DOCUMENT_TRACING_ACTIVITY
                intent.putExtra(BENCHMARK_MODE_ARG, MODE_LOCAL)
                startActivityAndWait(intent)
                device.waitForIdle()
            },
        )
    }

    companion object {

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() =
            listOf(
                arrayOf(CompilationMode.None()),
                arrayOf(
                    CompilationMode.Partial(
                        baselineProfileMode = BaselineProfileMode.Disable,
                        warmupIterations = 3,
                    )
                ),
            )
    }
}
