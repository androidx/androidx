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

import android.app.Activity
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
    view: View
) : JankStatsApi22Impl(jankStats, view) {

    var frameMetricsHandler: Handler? = null

    private val frameMetricsAvailableListener: Window.OnFrameMetricsAvailableListener =
        Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
            val startTime = frameMetrics.getMetric(FrameMetrics.VSYNC_TIMESTAMP)
            // ignore historical data gathered before we started listening
            if (startTime >= listenerAddedTime) {
                val expectedDuration = getExpectedFrameDuration(frameMetrics) *
                    JankStats.jankHeuristicMultiplier
                jankStats.logFrameData(
                    startTime,
                    frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION),
                    expectedDuration.toLong()
                )
            }
        }

    open fun getExpectedFrameDuration(metrics: FrameMetrics): Long {
        return getExpectedFrameDuration(decorViewRef.get())
    }

    var listenerAddedTime: Long = 0

    override fun setupFrameTimer(enable: Boolean) {
        val view = decorViewRef.get()
        val window = if (view?.context is Activity) (view.context as Activity).window else null
        window?.let {
            if (enable) {
                if (frameMetricsHandler == null) {
                    val thread = HandlerThread("FrameMetricsAggregator")
                    thread.start()
                    frameMetricsHandler = Handler(thread.looper)
                }
                window.addOnFrameMetricsAvailableListener(
                    frameMetricsAvailableListener,
                    frameMetricsHandler
                )
                listenerAddedTime = System.nanoTime()
            } else {
                window.removeOnFrameMetricsAvailableListener(frameMetricsAvailableListener)
                listenerAddedTime = 0
            }
        }
    }
}