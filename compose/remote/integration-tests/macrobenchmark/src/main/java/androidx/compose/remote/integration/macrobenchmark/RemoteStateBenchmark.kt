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
 * Parameterized state update and state-layout benchmarks:
 * - S1: 1/8/32 named values × 16/100/1000 ms on one document (`embedded` vs `view`)
 * - S2a: 4-state [RemoteStateLayout] on one document
 * - S2b: 50-card feed where each card has a 3-state [RemoteStateLayout]
 */
@LargeTest
@RunWith(Parameterized::class)
class RemoteStateBenchmark(val player: String) {
    @get:Rule val rule = MacrobenchmarkRule()

    private val defaultCompilation =
        CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Disable,
            warmupIterations = 3,
        )

    @Test fun s1_1value_16ms() = runS1(valueCount = 1, intervalMs = 16L)

    @Test fun s1_1value_100ms() = runS1(valueCount = 1, intervalMs = 100L)

    @Test fun s1_8values_100ms() = runS1(valueCount = 8, intervalMs = 100L)

    @Test fun s1_32values_100ms() = runS1(valueCount = 32, intervalMs = 100L)

    @Test fun s1_32values_1000ms() = runS1(valueCount = 32, intervalMs = 1000L)

    @Test
    fun s2a_stateLayout() =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = stateMetrics(),
            compilationMode = defaultCompilation,
            iterations = 5,
            setupBlock = {
                killProcess()
                startActivityAndWait(
                    Intent(STATE_BENCH_ACTIVITY).apply {
                        putExtra("player", player)
                        putExtra("mode", "s2a")
                        putExtra("intervalMs", 100L)
                    }
                )
                assertLaneDrew(device, STATE_BENCH_CONTENT_DESCRIPTION)
                assertScreenChanged(waitMs = 300L)
            },
        ) {
            Thread.sleep(2000L)
        }

    @Test
    fun s2b_statefulFeed() =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = feedMetrics(),
            compilationMode = defaultCompilation,
            iterations = 5,
            setupBlock = {
                killProcess()
                startActivityAndWait(
                    Intent(FEED_ACTIVITY).apply {
                        putExtra("player", player)
                        putExtra("templates", "stateful")
                        putExtra("count", 50)
                        putExtra("updateMs", 100L)
                    }
                )
                assertLaneDrew(device, FEED_CONTENT_DESCRIPTION)
                assertScreenChanged(waitMs = 300L)
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

    private fun runS1(valueCount: Int, intervalMs: Long) =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = stateMetrics(),
            compilationMode = defaultCompilation,
            iterations = 5,
            setupBlock = {
                killProcess()
                startActivityAndWait(
                    Intent(STATE_BENCH_ACTIVITY).apply {
                        putExtra("player", player)
                        putExtra("mode", "s1")
                        putExtra("valueCount", valueCount)
                        putExtra("intervalMs", intervalMs)
                    }
                )
                assertLaneDrew(device, STATE_BENCH_CONTENT_DESCRIPTION)
                assertScreenChanged(waitMs = (intervalMs * 2).coerceAtLeast(250L))
            },
        ) {
            Thread.sleep(2000L)
        }

    companion object {
        @Parameterized.Parameters(name = "player={0}")
        @JvmStatic
        fun parameters(): List<Array<Any>> = listOf(arrayOf("embedded"), arrayOf("view"))
    }
}
