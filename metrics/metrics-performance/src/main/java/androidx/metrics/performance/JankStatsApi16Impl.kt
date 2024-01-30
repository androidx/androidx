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
import android.os.Message
import android.view.Choreographer
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference
import java.lang.reflect.Field

/**
 * Subclass of JankStatsBaseImpl records frame timing data for API 16 and later,
 * using Choreographer (which was introduced in API 16).
 */
@RequiresApi(16)
internal open class JankStatsApi16Impl(
    jankStats: JankStats,
    view: View
) : JankStatsBaseImpl(jankStats) {

    // TODO: decorView may change in Window, think about how to handle that
    // e.g., should we cache Window instead?
    internal val decorViewRef: WeakReference<View> = WeakReference(view)

    // Must cache this at init time, from view, since some subclasses will not receive callbacks
    // on the UI thread, so they will not have access to the appropriate Choreographer for
    // frame timing values
    val choreographer: Choreographer = Choreographer.getInstance()

    // Cache for use during reporting, to supply the FrameData states
    val metricsStateHolder = PerformanceMetricsState.getHolderForHierarchy(view)

    // stateInfo is the backing store for the list of states that are active on any given
    // frame. It is passed to the JankStats listeners as part of the FrameData structure.
    // Reusing this mutable version of it enables zero-allocation metrics reporting.
    val stateInfo = mutableListOf<StateInfo>()

    // frameData is reused every time, populated with the latest frame's data before
    // sending out to listeners. Reuse enables zero-allocation metrics reporting.
    private val frameData = FrameData(0, 0, false, stateInfo)

    /**
     * Each JankStats instance has its own listener for per-frame metric data.
     * But we use a single listener (using OnPreDraw events prior to API 24) to gather
     * the frame data, and then delegate that information to all instances.
     * OnFrameListenerDelegate is the object that the per-frame data is delegated to,
     * which forwards it to the JankStats instances.
     */
    private val onFrameListenerDelegate = object : OnFrameListenerDelegate() {
        override fun onFrame(startTime: Long, uiDuration: Long, expectedDuration: Long) {
            jankStats.logFrameData(getFrameData(startTime, uiDuration,
                (expectedDuration * jankStats.jankHeuristicMultiplier).toLong()))
        }
    }

    override fun setupFrameTimer(enable: Boolean) {
        val decorView = decorViewRef.get()
        decorView?.let {
            if (enable) {
                val delegates = decorView.getOrCreateOnPreDrawListenerDelegator()
                delegates.add(onFrameListenerDelegate)
            } else {
                decorView.removeOnPreDrawListenerDelegate(onFrameListenerDelegate)
            }
        }
    }

    internal open fun getFrameData(
        startTime: Long,
        uiDuration: Long,
        expectedDuration: Long
    ): FrameData {
        metricsStateHolder.state?.getIntervalStates(startTime, startTime + uiDuration,
            stateInfo)
        val isJank = uiDuration > expectedDuration
        frameData.update(startTime, uiDuration, isJank)
        return frameData
    }

    private fun View.removeOnPreDrawListenerDelegate(delegate: OnFrameListenerDelegate) {
        val delegator = getTag(R.id.metricsDelegator) as DelegatingOnPreDrawListener?
        delegator?.remove(delegate, viewTreeObserver)
    }

    /**
     * This function returns the current list of OnPreDrawListener delegates.
     * If no such list exists, it will create it and add a root listener that
     * delegates to that list.
     */
    private fun View.getOrCreateOnPreDrawListenerDelegator(): DelegatingOnPreDrawListener {
        var delegator = getTag(R.id.metricsDelegator) as DelegatingOnPreDrawListener?
        if (delegator == null) {
            val delegates = mutableListOf<OnFrameListenerDelegate>()
            delegator = createDelegatingOnDrawListener(this, choreographer, delegates)
            viewTreeObserver.addOnPreDrawListener(delegator)
            setTag(R.id.metricsDelegator, delegator)
        }
        return delegator
    }

    internal open fun createDelegatingOnDrawListener(
        view: View,
        choreographer: Choreographer,
        delegates: MutableList<OnFrameListenerDelegate>
    ): DelegatingOnPreDrawListener {
        return DelegatingOnPreDrawListener(view, choreographer, delegates)
    }

    internal fun getFrameStartTime(): Long {
        return DelegatingOnPreDrawListener.choreographerLastFrameTimeField.get(choreographer)
            as Long
    }

    fun getExpectedFrameDuration(view: View?): Long {
        return DelegatingOnPreDrawListener.getExpectedFrameDuration(view)
    }
}

/**
 * This class is used by DelegatingOnDrawListener, which calculates the frame timing values
 * and calls all delegate listeners with that data.
 */
internal abstract class OnFrameListenerDelegate {
    abstract fun onFrame(startTime: Long, uiDuration: Long, expectedDuration: Long)
}

/**
 * There is only a single listener for OnPreDraw events, which are used to calculate frame
 * timing details. This listener delegates to a list of OnFrameListenerDelegate objects,
 * which do the work of sending that data to JankStats instance clients.
 */
@RequiresApi(16)
internal open class DelegatingOnPreDrawListener(
    decorView: View,
    val choreographer: Choreographer,
    val delegates: MutableList<OnFrameListenerDelegate>
) : ViewTreeObserver.OnPreDrawListener {

    // Track whether the delegate list is being iterated, used to prevent concurrent modification
    var iterating = false

    // These lists cache add/remove requests to be handled after the current iteration loop
    val toBeAdded = mutableListOf<OnFrameListenerDelegate>()
    val toBeRemoved = mutableListOf<OnFrameListenerDelegate>()

    val decorViewRef = WeakReference<View>(decorView)
    val metricsStateHolder = PerformanceMetricsState.getHolderForHierarchy(decorView)

    /**
     * It is possible for the delegates list to be modified concurrently (adding/removing items
     * while also iterating through the list). To prevent this, we synchronize on this instance.
     * It is also possible for the same thread to do both operations, causing reentrance into
     * that synchronization block. However, the only way that should happen is if the list is
     * being iterated on (which is called from the UI thread) and, in any of those delegate
     * listeners, the delegates list is modified
     * (by calling JankStats.isTrackingEnabled()). In this case, we cache the request in one of the
     * toBeAdded/Removed lists and return. When iteration is complete, we handle those requests.
     * This would not be sufficient if those operations could happen randomly on the same thread,
     * but the order should also be as described above (with add/remove nested inside iteration).
     *
     * Iteration and add/remove could also happen randomly and concurrently on different threads,
     * but in that case the synchronization block around both accesses should suffice.
     */

    override fun onPreDraw(): Boolean {
        val decorView = decorViewRef.get()
        decorView?.let {
            val frameStart = getFrameStartTime()
            with(decorView) {
                handler.sendMessageAtFrontOfQueue(Message.obtain(handler) {
                    val now = System.nanoTime()
                    val expectedDuration = getExpectedFrameDuration(decorView)
                    // prevent concurrent modification of delegates list by synchronizing on
                    // this delegator object while iterating and modifying
                    synchronized(this@DelegatingOnPreDrawListener) {
                        iterating = true
                        for (delegate in delegates) {
                            delegate.onFrame(frameStart, now - frameStart, expectedDuration)
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
                                viewTreeObserver.removeOnPreDrawListener(
                                    this@DelegatingOnPreDrawListener
                                )
                                setTag(R.id.metricsDelegator, null)
                            }
                        }
                        iterating = false
                    }
                    metricsStateHolder.state?.cleanupSingleFrameStates()
                }.apply {
                    setMessageAsynchronicity(this)
                })
            }
        }
        return true
    }

    fun add(delegate: OnFrameListenerDelegate) {
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

    fun remove(delegate: OnFrameListenerDelegate, viewTreeObserver: ViewTreeObserver) {
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
                    viewTreeObserver.removeOnPreDrawListener(this)
                    val decorView = decorViewRef.get()
                    decorView?.setTag(R.id.metricsDelegator, null)
                } else {
                    // noop - compiler requires else{} clause here for some strange reason
                }
            }
        }
    }
    private fun getFrameStartTime(): Long {
        return choreographerLastFrameTimeField.get(choreographer) as Long
    }

    // Noop prior to API 22 - overridden in 22Impl subclass
    internal open fun setMessageAsynchronicity(message: Message) {}

    companion object {
        val choreographerLastFrameTimeField: Field =
            Choreographer::class.java.getDeclaredField("mLastFrameTimeNanos")

        init {
            choreographerLastFrameTimeField.isAccessible = true
        }

        @Suppress("deprecation") /* defaultDisplay */
        fun getExpectedFrameDuration(view: View?): Long {
            if (JankStatsBaseImpl.frameDuration < 0) {
                var refreshRate = 60f
                val window = if (view?.context is Activity)
                    (view.context as Activity).window else null
                if (window != null) {
                    val display = window.windowManager.defaultDisplay
                    refreshRate = display.refreshRate
                }
                if (refreshRate < 30f || refreshRate > 200f) {
                    // Account for faulty return values (including 0)
                    refreshRate = 60f
                }
                JankStatsBaseImpl.frameDuration =
                    (1000 / refreshRate * JankStatsBaseImpl.NANOS_PER_MS).toLong()
            }
            return JankStatsBaseImpl.frameDuration
        }
    }
}
