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
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Extended and secondary-lane feed scenarios:
 * - F1 quick comparison lanes (`documentPlayer_embedded`, `documentPlayer_view`, `empty`)
 * - F1 compilation sensitivity (`None`, `Full`, `BaselineProfileMode.Require`)
 * - F2 parse strategies (`ahead`, `prepared`)
 * - F3 prefetch options (`LazyLayoutCacheWindow`)
 * - P1 cold start into F1 with simulated per-card fetch delay
 */
@LargeTest
@RunWith(JUnit4::class)
class RemoteFeedExtendedBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    private val defaultCompilation =
        CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Disable,
            warmupIterations = 3,
        )

    @Test
    fun f1_mixed_documentPlayer_embedded() =
        runFeed("documentPlayer_embedded", defaultCompilation) { putExtra("templates", "mixed") }

    @Test
    fun f1_mixed_documentPlayer_view() =
        runFeed("documentPlayer_view", defaultCompilation) { putExtra("templates", "mixed") }

    @Test
    fun f1_mixed_empty() = runFeed("empty", defaultCompilation) { putExtra("templates", "mixed") }

    @Test
    fun f1_embeddedDoc_embedded() =
        runFeed("embedded", defaultCompilation) {
            putExtra("templates", "embeddedDoc")
            putExtra("count", 200)
        }

    @Test
    fun f1_embeddedDoc_view() =
        runFeed("view", defaultCompilation) {
            putExtra("templates", "embeddedDoc")
            putExtra("count", 200)
        }

    @Test
    fun f1_mixed_embedded_none() =
        runFeed("embedded", CompilationMode.None()) { putExtra("templates", "mixed") }

    @Test
    fun f1_mixed_embedded_full() =
        runFeed("embedded", CompilationMode.Full()) { putExtra("templates", "mixed") }

    @Test
    fun f1_mixed_embedded_baselineProfile() =
        runFeed(
            "embedded",
            CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
        ) {
            putExtra("templates", "mixed")
        }

    @Test
    fun f2_embedded_parseAhead() =
        runFeed("embedded", defaultCompilation) {
            putExtra("templates", "mixed")
            putExtra("parse", "ahead")
            putExtra("ahead", 8)
        }

    @Test
    fun f2_view_prepared() =
        runFeed("view", defaultCompilation) {
            putExtra("templates", "mixed")
            putExtra("parse", "prepared")
            putExtra("ahead", 8)
        }

    @Test
    fun f3_embedded_cacheWindow() =
        runFeed("embedded", defaultCompilation) {
            putExtra("templates", "mixed")
            putExtra("cacheWindow", true)
        }

    @Test
    fun f3_compose_cacheWindow() =
        runFeed("compose", defaultCompilation) {
            putExtra("templates", "mixed")
            putExtra("cacheWindow", true)
        }

    @Test fun p1_coldStart_delay20ms_embedded() = runColdStart("embedded", parse = "ahead")

    @Test fun p1_coldStart_delay20ms_compose() = runColdStart("compose", parse = "composition")

    private fun runColdStart(player: String, parse: String) =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()) + feedMetrics(),
            compilationMode = defaultCompilation,
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = {
                killProcess()
                pressHome()
            },
            measureBlock = {
                startActivityAndWait(
                    Intent(FEED_ACTIVITY).apply {
                        putExtra("player", player)
                        putExtra("templates", "mixed")
                        putExtra("count", 200)
                        putExtra("parse", parse)
                        putExtra("ahead", 8)
                        putExtra("fetchDelayMs", 20L)
                    }
                )
                assertLaneDrew(device, FEED_CONTENT_DESCRIPTION)
            },
        )

    private fun runFeed(
        player: String,
        compilationMode: CompilationMode,
        extras: Intent.() -> Unit = {},
    ) =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = feedMetrics(),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                killProcess()
                startActivityIntent(Intent(FEED_ACTIVITY).putExtra("player", player).apply(extras))
                assertLaneDrew(device, FEED_CONTENT_DESCRIPTION)
            },
        ) {
            val feed = device.findObject(By.desc(FEED_CONTENT_DESCRIPTION))
            assertNotNull("Feed list not found", feed)
            feed.setGestureMargin(device.displayWidth / 5)
            repeat(4) {
                feed.fling(Direction.DOWN)
                device.waitForIdle()
            }
        }
}
