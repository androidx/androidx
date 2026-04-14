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
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(Parameterized::class)
class RemoteComposeScrollBenchmark(val compilationMode: CompilationMode, val useLocal: Boolean) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scroll() {
        val metrics =
            mutableListOf(
                FrameTimingGfxInfoMetric(),
                MemoryUsageMetric(MemoryUsageMetric.Mode.Last),
            )

        if (!useLocal) {
            metrics.addAll(decodingTraces.map { TraceSectionMetric(it) })
        }

        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = metrics,
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = SCROLL_ACTIVITY
                if (useLocal) intent.putExtra(BENCHMARK_MODE_ARG, MODE_LOCAL)
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

        @Parameterized.Parameters(name = "compilation={0},local={1}")
        @JvmStatic
        fun parameters() =
            listOf(
                arrayOf(CompilationMode.None(), true),
                arrayOf(CompilationMode.None(), false),
                arrayOf(
                    CompilationMode.Partial(
                        baselineProfileMode = BaselineProfileMode.Disable,
                        warmupIterations = 3,
                    ),
                    true,
                ),
                arrayOf(
                    CompilationMode.Partial(
                        baselineProfileMode = BaselineProfileMode.Disable,
                        warmupIterations = 3,
                    ),
                    false,
                ),
            )
    }
}
