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
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Core spine of feed and animation macrobenchmarks across the primary player lanes (`embedded`,
 * `view`, `compose`):
 * - F1: 200-card feed (`single` and `mixed` templates)
 * - A1: `live` template (`RemoteTime().ContinuousSec()`) during fling
 * - A2: `poll` template updated every 1000 ms during fling (+ eased variant)
 *
 * Secondary comparison lanes (`documentPlayer_embedded`, `documentPlayer_view`, `empty`), parse
 * strategies (F2), prefetch (F3), and cold-start placeholders (P1) live in
 * [RemoteFeedExtendedBenchmark].
 */
@LargeTest
@RunWith(Parameterized::class)
class RemoteFeedBenchmark(val player: String) {
    @get:Rule val rule = MacrobenchmarkRule()

    private val defaultCompilation =
        CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Disable,
            warmupIterations = 3,
        )

    @Test
    fun f1_single() = runFeed {
        putExtra("templates", "single")
        putExtra("count", 200)
    }

    @Test
    fun f1_mixed() = runFeed {
        putExtra("templates", "mixed")
        putExtra("count", 200)
    }

    @Test
    fun a1_live() = runFeed {
        putExtra("templates", "live")
        putExtra("count", 200)
    }

    @Test
    fun a2_poll() = runFeed {
        putExtra("templates", "poll")
        putExtra("count", 200)
        putExtra("updateMs", 1000L)
    }

    @Test
    fun a2_poll_eased() = runFeed {
        putExtra("templates", "poll")
        putExtra("count", 200)
        putExtra("updateMs", 1000L)
        putExtra("eased", true)
    }

    private fun runFeed(extras: Intent.() -> Unit = {}) =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = feedMetrics(),
            compilationMode = defaultCompilation,
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

    companion object {
        @Parameterized.Parameters(name = "player={0}")
        @JvmStatic
        fun parameters(): List<Array<Any>> =
            listOf("embedded", "view", "compose").map { arrayOf(it) }
    }
}
