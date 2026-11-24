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

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.traceprocessor.ExperimentalTraceProcessorApi
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Cost of the `sharedBounds` crossfade during a container transform.
 *
 * Drives `SharedBoundsFadeActivity` list -> detail -> list and reports frame timing plus
 * RenderThread and GPU slices from the trace. `content` is `image` (one draw call) or `page` (a
 * detail page with text, shapes, images).
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(Parameterized::class)
class SharedBoundsFadeBenchmark(private val content: String) {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun containerTransform() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingMetric(), RenderThreadMetric()),
            compilationMode = CompilationMode.Full(),
            iterations = 8,
            setupBlock = {
                val intent =
                    Intent().apply {
                        action = Action
                        putExtra("content", content)
                    }
                startActivityAndWait(intent)
                device.wait(Until.hasObject(By.desc(ToggleDescription)), 5000)
            },
        ) {
            // Four transitions per iteration: list -> detail -> list -> detail -> list.
            repeat(4) {
                val toggle =
                    checkNotNull(device.wait(Until.findObject(By.desc(ToggleDescription)), 5000)) {
                        "Unable to find toggle button"
                    }
                toggle.click()
                device.wait(Until.hasObject(By.desc(ComposeBusy)), 1000)
                device.wait(Until.hasObject(By.desc(ComposeIdle)), 5000)
            }
        }
    }

    companion object {
        private const val PackageName = "androidx.compose.integration.macrobenchmark.target"
        private const val Action =
            "androidx.compose.integration.macrobenchmark.target.SHARED_BOUNDS_FADE_ACTIVITY"
        const val ToggleDescription = "toggle-shared-bounds"
        const val ComposeBusy = "COMPOSE-BUSY"
        const val ComposeIdle = "COMPOSE-IDLE"

        @Parameterized.Parameters(name = "content={0}")
        @JvmStatic
        fun parameters(): List<Array<Any>> = listOf(arrayOf("image"), arrayOf("page"))
    }
}

/**
 * RenderThread and GPU work of the target process, per iteration.
 * - `drawFrameCount`, `drawFrameSumMs`, `drawFrameAvgMs`: HWUI `DrawFrames*` slices on
 *   RenderThread. CPU time to record and submit GPU work.
 * - `gpuCompletionSumMs`, `gpuCompletionAvgMs`: `waiting for GPU completion*` slices on HWUI's `GPU
 *   completion` thread. A proxy for GPU time per frame.
 * - `saveLayerCount`: Skia `saveLayer` slices on RenderThread.
 * - `flushLayersCount`, `flushLayersSumMs`: HWUI `flush layers` slices. One per frame that rendered
 *   at least one offscreen layer.
 */
@OptIn(ExperimentalMetricApi::class, ExperimentalTraceProcessorApi::class)
class RenderThreadMetric : TraceMetric() {
    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: TraceProcessor.Session,
    ): List<Measurement> {
        val pkg = captureInfo.targetPackageName

        fun agg(threadLike: String, nameLike: String): Triple<Long, Double, Double> {
            val row =
                traceSession
                    .query(
                        """
                        SELECT COUNT(*) AS cnt,
                            CAST(COALESCE(SUM(slice.dur), 0) AS REAL) AS sum
                        FROM slice
                            INNER JOIN thread_track ON slice.track_id = thread_track.id
                            INNER JOIN thread USING(utid)
                            INNER JOIN process USING(upid)
                        WHERE process.name LIKE '$pkg'
                            AND thread.name LIKE '$threadLike'
                            AND slice.name LIKE '$nameLike'
                            AND slice.dur > 0
                        """
                            .trimIndent()
                    )
                    .first()
            val cnt = row.long("cnt")
            val sumMs = row.double("sum") / 1_000_000.0
            return Triple(cnt, sumMs, if (cnt > 0) sumMs / cnt else 0.0)
        }

        val draw = agg("RenderThread", "DrawFrame%")
        val gpu = agg("GPU completion", "waiting for GPU completion%")
        val saveLayer = agg("RenderThread", "%aveLayer%")
        val flushLayers = agg("RenderThread", "flush layers")

        return listOf(
            Measurement("drawFrameCount", draw.first.toDouble()),
            Measurement("drawFrameSumMs", draw.second),
            Measurement("drawFrameAvgMs", draw.third),
            Measurement("gpuCompletionSumMs", gpu.second),
            Measurement("gpuCompletionAvgMs", gpu.third),
            Measurement("saveLayerCount", saveLayer.first.toDouble()),
            Measurement("flushLayersCount", flushLayers.first.toDouble()),
            Measurement("flushLayersSumMs", flushLayers.second),
        )
    }
}
