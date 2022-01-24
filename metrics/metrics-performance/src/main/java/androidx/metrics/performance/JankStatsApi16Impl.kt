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
    val metricsStateHolder = PerformanceMetricsState.getForHierarchy(view)

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
                val delegates = decorView.getOrCreateOnPreDrawListenerDelegates()
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
        val frameStates =
            metricsStateHolder.state?.getIntervalStates(startTime, startTime + uiDuration)
                ?: emptyList()
        val isJank = uiDuration > expectedDuration
        return FrameData(startTime, uiDuration, isJank, frameStates)
    }

    private fun View.removeOnPreDrawListenerDelegate(delegate: OnFrameListenerDelegate) {
        setTag(R.id.metricsDelegator, null)
        val delegator = getTag(R.id.metricsDelegator) as DelegatingOnPreDrawListener?
        with(delegator?.delegates) {
            this?.remove(delegate)
            if (this?.size == 0) {
                viewTreeObserver.removeOnPreDrawListener(delegator)
                setTag(R.id.metricsDelegator, null)
            }
        }
    }

    /**
     * This function returns the current list of OnPreDrawListener delegates.
     * If no such list exists, it will create it and add a root listener that
     * delegates to that list.
     */
    private fun View.getOrCreateOnPreDrawListenerDelegates():
        MutableList<OnFrameListenerDelegate> {
        var delegator = getTag(R.id.metricsDelegator) as DelegatingOnPreDrawListener?
        if (delegator == null) {
            val delegates = mutableListOf<OnFrameListenerDelegate>()
            delegator = createDelegatingOnDrawListener(this, choreographer, delegates)
            viewTreeObserver.addOnPreDrawListener(delegator)
            setTag(R.id.metricsDelegator, delegator)
        }
        return delegator.delegates
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

    val decorViewRef = WeakReference<View>(decorView)
    val metricsStateHolder = PerformanceMetricsState.getForHierarchy(decorView)

    override fun onPreDraw(): Boolean {
        val decorView = decorViewRef.get()
        with(decorView!!) {
            val frameStart = getFrameStartTime()
            decorView.let {
                handler.sendMessage(
                    Message.obtain(handler) {
                        val now = System.nanoTime()
                        val expectedDuration = getExpectedFrameDuration(decorView)
                        for (delegate in delegates) {
                            delegate.onFrame(frameStart, now - frameStart, expectedDuration)
                        }
                        metricsStateHolder.state?.cleanupSingleFrameStates()
                    }.apply {
                        setMessageAsynchronicity(this)
                    }
                )
            }
        }
        return true
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
