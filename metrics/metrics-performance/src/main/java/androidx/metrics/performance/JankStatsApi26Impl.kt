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
import androidx.annotation.RequiresApi

@RequiresApi(26)
internal open class JankStatsApi26Impl(
    jankStats: JankStats,
    view: View
) : JankStatsApi24Impl(jankStats, view) {

    override fun getFrameDuration(frameMetrics: FrameMetrics): Long {
        // TOTAL_DURATION is measured from intended start of frame, but
        // JankStats uses VSYNC_TIMESTAMP as start of frame, so subtract
        // that difference to use consistent metrics
        val total = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
        val intended = frameMetrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP)
        val vsync = frameMetrics.getMetric(FrameMetrics.VSYNC_TIMESTAMP)
        return total - (vsync - intended)
    }
    override fun getFrameStartTime(frameMetrics: FrameMetrics): Long {
        return frameMetrics.getMetric(FrameMetrics.VSYNC_TIMESTAMP)
    }
}