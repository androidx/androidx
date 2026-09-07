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
import android.graphics.Point
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.testutils.defaultComposeScrollingMetrics
import androidx.testutils.defaultMemoryMetrics
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Macrobenchmark suite comparing the Compose-embedded remote player ([RcPlayer]) against the
 * View-based Java player ([RemoteComposePlayer] wrapped in Compose as [RemoteDocumentPlayer])
 * across 6 critical performance scenarios:
 *
 * 1) Load time (startup & initial display)
 * 2) Constant animation (continuous indefinite rotating spinner)
 * 3) Publishing state every 100ms (updating text)
 * 4) State Layout changes every 100ms (state machine conditional layouts)
 * 5) Image loading with simulated network latency
 * 6) Scrolling components (500-item scrollable list)
 */
@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(Parameterized::class)
class RemotePlayerMacrobenchmark(
    val compilationMode: CompilationMode,
    val playerMode: String,
) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    // -------------------------------------------------------------------------
    // 1) Load Time
    // -------------------------------------------------------------------------

    @Test
    fun loadTime() {
        val metrics = listOf<Metric>(StartupTimingMetric()) + defaultMemoryMetrics()

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = { pressHome() },
            measureBlock = {
                val intent =
                    Intent().apply {
                        action = LOAD_TIME_ACTIVITY
                        putExtra(BENCHMARK_MODE_ARG, playerMode)
                    }
                startActivityAndWait(intent)
            },
        )
    }

    // -------------------------------------------------------------------------
    // 2) Constant Animation (Indefinite Spinner)
    // -------------------------------------------------------------------------

    @Test
    fun constantAnimation() {
        val metrics = listOf<Metric>(FrameTimingMetric())

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent =
                    Intent().apply {
                        action = ANIMATION_ACTIVITY
                        putExtra(BENCHMARK_MODE_ARG, playerMode)
                    }
                startActivityIntent(intent)
                device.wait(Until.hasObject(By.desc(SPINNER_CONTENT_DESCRIPTION)), 10_000)
            },
        ) {
            Thread.sleep(2000L)
        }
    }

    // -------------------------------------------------------------------------
    // 3) Publishing State Every 100ms (Updating Text)
    // -------------------------------------------------------------------------

    @Test
    fun stateUpdate() {
        val metrics = listOf<Metric>(FrameTimingMetric())

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent =
                    Intent().apply {
                        action = STATE_UPDATE_ACTIVITY
                        putExtra(BENCHMARK_MODE_ARG, playerMode)
                    }
                startActivityIntent(intent)
                device.wait(Until.hasObject(By.desc(STATE_UPDATE_CONTENT_DESCRIPTION)), 10_000)
            },
        ) {
            Thread.sleep(2000L)
        }
    }

    // -------------------------------------------------------------------------
    // 4) State Layout Changes Every 100ms
    // -------------------------------------------------------------------------

    @Test
    fun stateLayout() {
        val metrics = listOf<Metric>(FrameTimingMetric())

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent =
                    Intent().apply {
                        action = STATE_LAYOUT_ACTIVITY
                        putExtra(BENCHMARK_MODE_ARG, playerMode)
                    }
                startActivityIntent(intent)
                device.wait(Until.hasObject(By.desc(STATE_LAYOUT_CONTENT_DESCRIPTION)), 10_000)
            },
        ) {
            Thread.sleep(2000L)
        }
    }

    // -------------------------------------------------------------------------
    // 5) Image Loading With Simulated Network Latency
    // -------------------------------------------------------------------------

    @Test
    fun imageLoading() {
        val metrics = listOf<Metric>(StartupTimingMetric()) + defaultMemoryMetrics()

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = { pressHome() },
            measureBlock = {
                val intent =
                    Intent().apply {
                        action = IMAGE_LOADING_ACTIVITY
                        putExtra(BENCHMARK_MODE_ARG, playerMode)
                    }
                startActivityAndWait(intent)
            },
        )
    }

    // -------------------------------------------------------------------------
    // 6) Scrolling Components
    // -------------------------------------------------------------------------

    @Test
    fun scrolling() {
        val metrics =
            defaultComposeScrollingMetrics() + FrameTimingGfxInfoMetric() + defaultMemoryMetrics()

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent =
                    Intent().apply {
                        action = SCROLL_ACTIVITY
                        putExtra(BENCHMARK_MODE_ARG, playerMode)
                    }
                startActivityIntent(intent)
                device.wait(Until.hasObject(By.desc(LIST_CONTENT_DESCRIPTION)), 10_000)
            },
        ) {
            val list = device.findObject(By.desc(LIST_CONTENT_DESCRIPTION))
            if (list != null) {
                repeat(5) {
                    list.drag(Point(list.visibleCenter.x, list.visibleCenter.y / 3))
                    device.waitForIdle()
                }
            } else {
                fail("Remote list not found")
            }
        }
    }

    companion object {
        @Parameterized.Parameters(name = "compilation={0},player={1}")
        @JvmStatic
        fun parameters(): List<Array<Any>> =
            listOf(
                    CompilationMode.None(),
                    CompilationMode.Partial(
                        baselineProfileMode = BaselineProfileMode.Disable,
                        warmupIterations = 3,
                    ),
                )
                .flatMap { compilationMode ->
                    listOf(
                        arrayOf(compilationMode, MODE_EMBEDDED_PLAYER),
                        arrayOf(compilationMode, MODE_JAVA_PLAYER),
                    )
                }
    }
}
