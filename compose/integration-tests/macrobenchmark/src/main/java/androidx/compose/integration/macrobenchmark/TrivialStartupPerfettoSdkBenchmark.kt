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

package androidx.compose.integration.macrobenchmark

import androidx.benchmark.Arguments
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.testutils.measureStartup
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(Parameterized::class)
class TrivialStartupPerfettoSdkBenchmark(
    private val startupMode: StartupMode,
    private val compilationMode: CompilationMode,
    private val isPerfettoSdkEnabled: Boolean
) {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = try {
        Arguments.perfettoSdkTracingEnableOverride = isPerfettoSdkEnabled
        assertThat(Arguments.perfettoSdkTracingEnable, `is`(isPerfettoSdkEnabled))

        try {
            val perfettoSdkTraceSection = TraceSectionMetric(
                "%TrivialStartupTracingActivity.onCreate%" +
                    " (TrivialStartupTracingActivity.kt:%)"
            )
            benchmarkRule.measureStartup(
                compilationMode = compilationMode,
                startupMode = startupMode,
                iterations = 1, // we are only verifying presence of entries (not the timing data)
                metrics = listOf(perfettoSdkTraceSection),
                packageName = "androidx.compose.integration.macrobenchmark.target"
            ) {
                action = "androidx.compose.integration.macrobenchmark.target." +
                    "TRIVIAL_STARTUP_TRACING_ACTIVITY"
            }
        } catch (e: IllegalArgumentException) {
            if (!isPerfettoSdkEnabled &&
                e.message?.contains("Unable to read any metrics during benchmark") == true
            ) {
                // We are relying on the fact that Macrobenchmark will throw an exception when it
                // cannot find any metrics, and given we are looking for one specific metric
                // (a Composable function emitted by Compose Tracing), we are able to tell if
                // Compose Tracing is working (enabled) or not, both of which we want to verify in
                // this test.
            } else throw e // this is a legitimate failure
        }
    } finally {
        Arguments.perfettoSdkTracingEnableOverride = null
    }

    companion object {
        @Parameterized.Parameters(name = "startup={0},compilation={1},perfettoSdk={2}")
        @JvmStatic
        fun parameters() = listOf(
            arrayOf(StartupMode.COLD, CompilationMode.DEFAULT, /* perfettoSdk = */ true),
            arrayOf(StartupMode.COLD, CompilationMode.DEFAULT, /* perfettoSdk = */ false),
            arrayOf(StartupMode.WARM, CompilationMode.DEFAULT, /* perfettoSdk = */ true),
            arrayOf(StartupMode.WARM, CompilationMode.DEFAULT, /* perfettoSdk = */ false),
        )
    }
}
