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

    private val frameMetricsAvailableListenerDelegate: Window.OnFrameMetricsAvailableListener =
        Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
            val startTime = getFrameStartTime(frameMetrics)
            // ignore historical data gathered before we started listening
            if (startTime >= listenerAddedTime && startTime != prevStart) {
                val expectedDuration = getExpectedFrameDuration(frameMetrics) *
                    jankStats.jankHeuristicMultiplier
                jankStats.logFrameData(getFrameData(startTime, expectedDuration.toLong(),
                    frameMetrics))
                prevStart = startTime
            }
        }

    internal open fun getFrameData(
        startTime: Long,
        expectedDuration: Long,
        frameMetrics: FrameMetrics
    ): FrameDataApi24 {
        val uiDuration = frameMetrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION) +
            frameMetrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION) +
            frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION) +
            frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) +
            frameMetrics.getMetric(FrameMetrics.DRAW_DURATION) +
            frameMetrics.getMetric(FrameMetrics.SYNC_DURATION)
        val frameStates =
            metricsStateHolder.state?.getIntervalStates(startTime, startTime + uiDuration)
                ?: emptyList()
        val isJank = uiDuration > expectedDuration
        val cpuDuration = uiDuration +
            frameMetrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION) +
            frameMetrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)
        return FrameDataApi24(startTime, uiDuration, cpuDuration, isJank, frameStates)
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
                    val delegates = window.getOrCreateFrameMetricsListenerDelegates()
                    delegates.add(frameMetricsAvailableListenerDelegate)
                    listenerAddedTime = System.nanoTime()
                }
            } else {
                window.removeFrameMetricsListenerDelegate(frameMetricsAvailableListenerDelegate)
                listenerAddedTime = 0
            }
        }
    }

    companion object {
        // Need a Handler for FrameMetricsListener; just use a singleton, no need for Thread
        // overhead per JankStats instance
        internal var frameMetricsHandler: Handler? = null
    }

    @RequiresApi(24)
    private fun Window.removeFrameMetricsListenerDelegate(
        delegate: Window.OnFrameMetricsAvailableListener
    ) {
        val delegator = decorView.getTag(R.id.metricsDelegator) as
            DelegatingFrameMetricsListener?
        with(delegator?.delegates) {
            this?.remove(delegate)
            if (this?.size == 0) {
                removeOnFrameMetricsAvailableListener(delegator)
                decorView.setTag(R.id.metricsDelegator, null)
            }
        }
    }

    /**
     * This function returns the current list of FrameMetricsListener delegates.
     * If no such list exists, it will create it, and add a root listener which
     * delegates to that list.
     */
    @RequiresApi(24)
    private fun Window.getOrCreateFrameMetricsListenerDelegates():
        MutableList<Window.OnFrameMetricsAvailableListener> {
        var delegator = decorView.getTag(R.id.metricsDelegator) as
            DelegatingFrameMetricsListener?
        if (delegator == null) {
            val delegates = mutableListOf<Window.OnFrameMetricsAvailableListener>()
            delegator = DelegatingFrameMetricsListener(delegates)
            // First listener for this window; create the delegates list and
            // add a listener to the window
            if (frameMetricsHandler == null) {
                val thread = HandlerThread("FrameMetricsAggregator")
                thread.start()
                frameMetricsHandler = Handler(thread.looper)
            }
            addOnFrameMetricsAvailableListener(delegator,
                frameMetricsHandler
            )
            decorView.setTag(R.id.metricsDelegator, delegator)
        }
        return delegator.delegates
    }
}

/**
 * To avoid having multiple frame metrics listeners for a given window (if the client
 * creates multiple JankStats instances on that window), we use a single listener and
 * delegate out to the multiple listeners provided by the client. This single instance
 * and the list of delegates are cached in view tags in the DecorView for the window.
 */
@RequiresApi(24)
private class DelegatingFrameMetricsListener(
    val delegates: MutableList<Window.OnFrameMetricsAvailableListener>
) : Window.OnFrameMetricsAvailableListener {
    override fun onFrameMetricsAvailable(
        window: Window?,
        frameMetrics: FrameMetrics?,
        dropCount: Int
    ) {
        for (delegate in delegates) {
            delegate.onFrameMetricsAvailable(window, frameMetrics, dropCount)
        }
        // Remove singleFrame states now that we are done processing this frame
        if (window?.decorView != null) {
            val holder = PerformanceMetricsState.getForHierarchy(window.decorView)
            holder.state?.cleanupSingleFrameStates()
        }
    }
}
