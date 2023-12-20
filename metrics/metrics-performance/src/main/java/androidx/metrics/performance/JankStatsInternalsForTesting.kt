/*
 * Copyright 2022 The Android Open Source Project
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

import android.os.Build
import android.view.FrameMetrics
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * This class exists for calling into otherwise private/internal APIs in JankStats, to allow
 * for easier, more targeted testing of those internal pieces.
 */
@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class JankStatsInternalsForTesting(val jankStats: JankStats) {
    private val impl = jankStats.implementation

    fun removeStateNow(performanceMetricsState: PerformanceMetricsState, stateName: String) {
        performanceMetricsState.removeStateNow(stateName)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun getFrameData(): FrameData? {
        when (impl) {
            is JankStatsApi16Impl -> {
                return impl.getFrameData(0, 0, 0)
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getFrameData(frameMetrics: FrameMetrics): FrameData? {
        when (impl) {
            is JankStatsApi24Impl -> {
                return impl.getFrameData(0, 0, frameMetrics)
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun logFrameData(frameData: FrameData) {
        jankStats.logFrameData(frameData)
    }
}
