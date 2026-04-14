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
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(Parameterized::class)
class RemoteComposeFirstFrameBenchmark(val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun firstFrameRemoteCompose() {
        val metrics =
            mutableListOf<Metric>(StartupTimingMetric()).also {
                it.addAll(decodingTraces.map { TraceSectionMetric(it) })
            }

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = { pressHome() },
            measureBlock = {
                val intent = Intent()
                intent.action = FIRST_FRAME_ACTIVITY
                intent.putExtra(BENCHMARK_MODE_ARG, MODE_REMOTE_COMPOSE)
                startActivityAndWait(intent)
            },
        )
    }

    @Test
    fun firstFrameLiveCompose() {
        val metrics = listOf<Metric>(StartupTimingMetric())

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = { pressHome() },
            measureBlock = {
                val intent = Intent()
                intent.action = FIRST_FRAME_ACTIVITY
                intent.putExtra(BENCHMARK_MODE_ARG, MODE_COMPOSE)
                startActivityAndWait(intent)
            },
        )
    }

    @Test
    fun firstFrameWebView() {
        val metrics = listOf<Metric>(StartupTimingMetric())

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = { pressHome() },
            measureBlock = {
                val intent = Intent()
                intent.action = FIRST_FRAME_ACTIVITY
                intent.putExtra(BENCHMARK_MODE_ARG, MODE_WEB_VIEW)
                startActivityAndWait(intent)
            },
        )
    }

    @Test
    fun firstFrameRemoteViews() {
        val metrics = listOf<Metric>(StartupTimingMetric())

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = { pressHome() },
            measureBlock = {
                val intent = Intent()
                intent.action = FIRST_FRAME_ACTIVITY
                intent.putExtra(BENCHMARK_MODE_ARG, MODE_REMOTE_VIEW)
                startActivityAndWait(intent)
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
