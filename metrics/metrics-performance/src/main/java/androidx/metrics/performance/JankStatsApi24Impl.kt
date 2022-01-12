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

import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi

/**
 * Subclass of JankStatsBaseImpl records frame timing data for API 24 and later,
 * using FrameMetrics (which was introduced in API 24). Jank data is collected by
 * setting a [Window.addOnFrameMetricsAvailableListener]
 * on the Window associated with the Activity being tracked.
 */
@RequiresApi(24)
internal open class JankStatsApi24Impl(
    jankStats: JankStats,
    view: View,
    private val window: Window
) : JankStatsApi22Impl(jankStats, view) {

    // Workaround for situation like b/206956036, where platform would sometimes send completely
    // duplicate events through FrameMetrics. When that occurs, simply ignore the latest event
    // that has the exact same start time.
    var prevStart = 0L

    // Used to avoid problems with data gathered before things are set up
    var listenerAddedTime: Long = 0

    private val frameMetricsAvailableListener: Window.OnFrameMetricsAvailableListener =
        Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
            val startTime = getFrameStartTime(frameMetrics)
            // ignore historical data gathered before we started listening
            if (startTime >= listenerAddedTime && startTime != prevStart) {
                val expectedDuration = getExpectedFrameDuration(frameMetrics) *
                    jankStats.jankHeuristicMultiplier
                jankStats.logFrameData(
                    startTime,
                    getFrameDuration(frameMetrics),
                    expectedDuration.toLong()
                )
                prevStart = startTime
            }
        }

    internal open fun getFrameDuration(frameMetrics: FrameMetrics): Long {
        return frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
    }

    internal open fun getFrameStartTime(frameMetrics: FrameMetrics): Long {
        return getFrameStartTime()
    }

    open fun getExpectedFrameDuration(metrics: FrameMetrics): Long {
        return getExpectedFrameDuration(decorViewRef.get())
    }

    override fun setupFrameTimer(enable: Boolean) {
        window.let {
            if (enable) {
                if (listenerAddedTime == 0L) {
                    if (frameMetricsHandler == null) {
                        val thread = HandlerThread("FrameMetricsAggregator")
                        thread.start()
                        frameMetricsHandler = Handler(thread.looper)
                    }
                    // Already added, no need to do it again
                    window.addOnFrameMetricsAvailableListener(
                        frameMetricsAvailableListener,
                        frameMetricsHandler
                    )
                    listenerAddedTime = System.nanoTime()
                }
            } else {
                window.removeOnFrameMetricsAvailableListener(frameMetricsAvailableListener)
                listenerAddedTime = 0
            }
        }
    }

    companion object {
        private var frameMetricsHandler: Handler? = null
    }
}