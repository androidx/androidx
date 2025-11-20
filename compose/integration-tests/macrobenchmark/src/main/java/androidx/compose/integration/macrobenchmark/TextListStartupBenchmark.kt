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

@file:OptIn(ExperimentalMetricApi::class)

package androidx.compose.integration.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.testutils.getStartupMetrics
import androidx.testutils.measureStartup
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class TextListStartupBenchmark(
    private val startupMode: StartupMode,
    private val compilationMode: CompilationMode,
    private val styled: Boolean,
    private val prefetch: Boolean,
) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Test
    fun startup() {
        benchmarkRule.measureStartup(
            compilationMode = compilationMode,
            startupMode = startupMode,
            metrics = getStartupMetrics(),
            iterations = 3,
            packageName = PackageName,
        ) {
            action = Action
            putExtra(BenchmarkConfig.Prefetch, prefetch)
            putExtra(BenchmarkConfig.Styled, styled)
            putExtra(BenchmarkConfig.WordCount, 2)
            putExtra(BenchmarkConfig.TextCount, 16)
            putExtra(BenchmarkConfig.WordLength, 8)
            tryFreeTextLayoutCache()
        }
    }

    companion object {
        private const val PackageName = "androidx.compose.integration.macrobenchmark.target"
        private const val Action =
            "androidx.compose.integration.macrobenchmark.target.TEXT_LIST_ACTIVITY"

        object BenchmarkConfig {
            val WordCount = "word_count" // Integer
            val TextCount = "text_count" // Integer
            val WordLength = "word_length" // Integer
            val Styled = "styled" // Boolean
            val Prefetch = "prefetch" // Boolean
        }

        @Parameterized.Parameters(name = "styled={2}, prefetch={3}")
        @JvmStatic
        fun parameters() =
            cartesian(
                arrayOf(StartupMode.COLD),
                arrayOf(CompilationMode.Full()),
                arrayOf(false), // styled
                arrayOf(true, false), // prefetch
            )
    }
}
