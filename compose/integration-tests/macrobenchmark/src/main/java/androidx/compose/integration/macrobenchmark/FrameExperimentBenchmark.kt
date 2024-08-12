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

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.createCompilationParams
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmark for experimenting with synthetic frame patterns/durations and how they show up in
 * metrics
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FrameExperimentBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    // NOTE: Keep in sync with FrameExperimentActivity!!
    private enum class FrameMode(val id: Int) {
        Fast(0),
        PrefetchEveryFrame(1),
        WorkDuringEveryFrame(2),
        PrefetchSomeFrames(3),
    }

    @Test fun fast() = benchmark(FrameMode.Fast)

    @Test fun prefetchEveryFrame() = benchmark(FrameMode.PrefetchEveryFrame)

    @Test fun workDuringEveryFrame() = benchmark(FrameMode.WorkDuringEveryFrame)

    @Test fun prefetchSomeFrames() = benchmark(FrameMode.PrefetchSomeFrames)

    @OptIn(ExperimentalMetricApi::class)
    private fun benchmark(mode: FrameMode) {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric(), FrameTimingGfxInfoMetric()),
            compilationMode = CompilationMode.DEFAULT,
            iterations = 1,
            setupBlock = {
                val intent = Intent()
                intent.action = ACTION
                intent.putExtra("FRAME_MODE", mode.id)
                startActivityAndWait(intent)
            }
        ) {
            device.click(device.displayWidth / 2, device.displayHeight / 2)
            Thread.sleep(4_000) // empirically enough to produce expected frames
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.compose.integration.macrobenchmark.target"
        private const val ACTION =
            "androidx.compose.integration.macrobenchmark.target.FRAME_EXPERIMENT_ACTIVITY"

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
