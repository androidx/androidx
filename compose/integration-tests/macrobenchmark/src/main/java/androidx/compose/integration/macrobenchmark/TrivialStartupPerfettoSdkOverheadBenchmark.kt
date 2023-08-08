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
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.testutils.createStartupCompilationParams
import androidx.testutils.measureStartup
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class TrivialStartupPerfettoSdkOverheadBenchmark(
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

        benchmarkRule.measureStartup(
            compilationMode = compilationMode,
            startupMode = startupMode,
            packageName = "androidx.compose.integration.macrobenchmark.target"
        ) {
            action = "androidx.compose.integration.macrobenchmark.target." +
                "TRIVIAL_STARTUP_TRACING_ACTIVITY"
        }
    } finally {
        Arguments.perfettoSdkTracingEnableOverride = null
    }

    companion object {
        // intended for local testing of all possible configurations
        private const val exhaustiveMode = false

        @Parameterized.Parameters(name = "startup={0},compilation={1},perfettoSdk={2}")
        @JvmStatic
        fun parameters() =
            when {
                exhaustiveMode ->
                    // complete set for testing locally
                    createStartupCompilationParams()
                        .flatMap { listOf(it + true, it + false) } /* perfetto sdk enabled */
                else ->
                    // subset for testing in CI:
                    // compilation isn't expected to affect this, so we just look at startup time
                    // for cold and not, since the behavior is very different in those scenarios
                    createStartupCompilationParams(
                        listOf(StartupMode.COLD, StartupMode.WARM),
                        listOf(CompilationMode.DEFAULT)
                    ).map { it + true } /* perfetto sdk enabled */
            }
    }
}
