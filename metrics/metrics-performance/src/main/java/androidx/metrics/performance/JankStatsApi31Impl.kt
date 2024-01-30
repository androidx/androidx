/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.metrics.performance

import android.view.FrameMetrics
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi

@RequiresApi(31)
internal class JankStatsApi31Impl(
    jankStats: JankStats,
    view: View,
    window: Window
) : JankStatsApi26Impl(jankStats, view, window) {

    // Reuse the same frameData on every frame to avoid allocating per-frame objects
    val frameData = FrameDataApi31(0, 0, 0, 0, 0, false, stateInfo)

    override fun getFrameData(
        startTime: Long,
        expectedDuration: Long,
        frameMetrics: FrameMetrics
    ): FrameDataApi31 {
        val uiDuration = frameMetrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION) +
            frameMetrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION) +
            frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION) +
            frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) +
            frameMetrics.getMetric(FrameMetrics.DRAW_DURATION) +
            frameMetrics.getMetric(FrameMetrics.SYNC_DURATION)
        prevEnd = startTime + uiDuration
        metricsStateHolder.state?.getIntervalStates(startTime, prevEnd, stateInfo)
        val isJank = uiDuration > expectedDuration
        val totalDuration = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
        // SWAP is counted for both CPU and GPU metrics, so add it back in after subtracting GPU
        val cpuDuration = totalDuration -
            frameMetrics.getMetric(FrameMetrics.GPU_DURATION) +
            frameMetrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)
        val overrun = totalDuration -
            frameMetrics.getMetric(FrameMetrics.DEADLINE)
        frameData.update(startTime, uiDuration, cpuDuration, totalDuration, overrun, isJank)
        return frameData
    }

    override fun getExpectedFrameDuration(metrics: FrameMetrics): Long {
        return metrics.getMetric(FrameMetrics.DEADLINE)
    }
}
