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
import android.view.Window.OnFrameMetricsAvailableListener
import androidx.annotation.RequiresApi
import kotlin.math.max

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
) : JankStatsApi16Impl(jankStats, view) {

    // Workaround for situation like b/206956036, where platform would sometimes send completely
    // duplicate events through FrameMetrics. When that occurs, simply ignore the latest event
    // that has the exact same start time.
    var prevStart = 0L

    // Used to avoid problems with data gathered before things are set up
    var listenerAddedTime: Long = 0

    // Constrain startTime to be >= previous frame's end time to avoid incorrect
    // overlap of state information during janky times when intended frame times
    // overlapped due to jank
    var prevEnd: Long = 0

    // Reuse the same frameData on every frame to avoid allocating per-frame objects
    private val frameData = FrameDataApi24(0, 0, 0, false, stateInfo)

    private val frameMetricsAvailableListenerDelegate =
        OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
            val startTime = max(getFrameStartTime(frameMetrics), prevEnd)
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
        prevEnd = startTime + uiDuration
        metricsStateHolder.state?.getIntervalStates(startTime, prevEnd, stateInfo)
        val isJank = uiDuration > expectedDuration
        val cpuDuration = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
        frameData.update(startTime, uiDuration, cpuDuration, isJank)
        return frameData
    }

    internal open fun getFrameStartTime(frameMetrics: FrameMetrics): Long {
        return getFrameStartTime()
    }

    open fun getExpectedFrameDuration(metrics: FrameMetrics): Long {
        return getExpectedFrameDuration(decorViewRef.get())
    }

    override fun setupFrameTimer(enable: Boolean) {
        // prevent concurrent modification of delegates list by synchronizing on
        // Window, whose DecorView holds the delegator object
        synchronized(window) {
            if (enable) {
                if (listenerAddedTime == 0L) {
                    val delegates = window.getOrCreateFrameMetricsListenerDelegator()
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
        with(delegator) {
            this?.remove(delegate, this@removeFrameMetricsListenerDelegate)
        }
    }

    /**
     * This function returns the current list of FrameMetricsListener delegates.
     * If no such list exists, it will create it, and add a root listener which
     * delegates to that list.
     */
    @RequiresApi(24)
    private fun Window.getOrCreateFrameMetricsListenerDelegator():
        DelegatingFrameMetricsListener {
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
            addOnFrameMetricsAvailableListener(delegator, frameMetricsHandler)
            decorView.setTag(R.id.metricsDelegator, delegator)
        }
        return delegator
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

    // Track whether the delegate list is being iterated, used to prevent concurrent modification
    var iterating = false

    // These lists cache add/remove requests to be handled after the current iteration loop
    val toBeAdded = mutableListOf<Window.OnFrameMetricsAvailableListener>()
    val toBeRemoved = mutableListOf<Window.OnFrameMetricsAvailableListener>()

    /**
     * It is possible for the delegates list to be modified concurrently (adding/removing items
     * while also iterating through the list). To prevent this, we synchronize on this instance.
     * It is also possible for the same thread to do both operations, causing reentrance into
     * that synchronization block. However, the only way that should happen is if the list is
     * being iterated on (which is called from the FrameMetrics thread, not accessible to the
     * JankStats client) and, in any of those delegate listeners, the delegates list is modified
     * (by calling JankStats.isTrackingEnabled()). In this case, we cache the request in one of the
     * toBeAdded/Removed lists and return. When iteration is complete, we handle those requests.
     * This would not be sufficient if those operations could happen randomly on the same thread,
     * but the order should also be as described above (with add/remove nested inside iteration).
     *
     * Iteration and add/remove could also happen randomly and concurrently on different threads,
     * but in that case the synchronization block around both accesses should suffice.
     */

    override fun onFrameMetricsAvailable(
        window: Window?,
        frameMetrics: FrameMetrics?,
        dropCount: Int
    ) {
        // prevent concurrent modification of delegates list by synchronizing on
        // this delegator object while iterating and modifying
        synchronized(this) {
            iterating = true
            for (delegate in delegates) {
                delegate.onFrameMetricsAvailable(window, frameMetrics, dropCount)
            }
            if (toBeAdded.isNotEmpty()) {
                for (delegate in toBeAdded) {
                    delegates.add(delegate)
                }
                toBeAdded.clear()
            }
            if (toBeRemoved.isNotEmpty()) {
                val delegatesNonEmpty = delegates.isNotEmpty()
                for (delegate in toBeRemoved) {
                    delegates.remove(delegate)
                }
                toBeRemoved.clear()
                // Only remove delegator if we emptied the list here
                if (delegatesNonEmpty && delegates.isEmpty()) {
                    window?.removeOnFrameMetricsAvailableListener(this)
                    window?.decorView?.setTag(R.id.metricsDelegator, null)
                }
            }
            iterating = false
        }
        if (window != null) {
            // Remove singleFrame states now that we are done processing this frame
            val holder = PerformanceMetricsState.getHolderForHierarchy(window.decorView)
            holder.state?.cleanupSingleFrameStates()
        }
    }

    fun add(delegate: Window.OnFrameMetricsAvailableListener) {
        // prevent concurrent modification of delegates list by synchronizing on
        // this delegator object while iterating and modifying
        synchronized(this) {
            if (iterating) {
                toBeAdded.add(delegate)
            } else {
                delegates.add(delegate)
            }
        }
    }

    fun remove(delegate: OnFrameMetricsAvailableListener, window: Window) {
        // prevent concurrent modification of delegates list by synchronizing on
        // this delegator object while iterating and modifying
        synchronized(this) {
            if (iterating) {
                toBeRemoved.add(delegate)
            } else {
                val delegatesNonEmpty = delegates.isNotEmpty()
                delegates.remove(delegate)
                // Only remove delegator if we emptied the list here
                if (delegatesNonEmpty && delegates.isEmpty()) {
                    window.removeOnFrameMetricsAvailableListener(this)
                    window.decorView.setTag(R.id.metricsDelegator, null)
                } else {
                    // noop - compiler requires else{} clause here for some strange reason
                }
            }
        }
    }
}
