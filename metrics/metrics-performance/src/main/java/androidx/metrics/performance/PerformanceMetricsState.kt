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
import android.view.View
import androidx.annotation.UiThread

/**
 * This class is used to store information about the state of an application that can be
 * retrieved later to associate state with performance timing data.
 *
 * For example, PerformanceMetricsState is used in conjunction with [JankStats] to enable JankStats
 * to report per-frame performance characteristics along with the application state that was
 * present at the time that the frame data was logged.
 *
 * There is only one PerformanceMetricsState available per view hierarchy. That instance can be
 * retrieved from the holder returned by [PerformanceMetricsState.getForHierarchy]. Limiting
 * PerformanceMetricsState to a single object per hierarchy makes it
 * possible for code outside the core application logic, such as in a library, to store
 * application state that can be useful for the application to know about.
 */
class PerformanceMetricsState private constructor() {

    /**
     * Data to track UI and user state in this JankStats object.
     *
     * @see addState
     * @see markStateForRemoval
     */
    private var states = mutableListOf<StateData>()

    /**
     * Temporary per-frame to track UI and user state.
     * Unlike the states tracked in `states`, any state in this structure is only valid until
     * the next frame, at which point it is cleared. Any state data added here is automatically
     * removed; there is no matching "remove" method for [.addSingleFrameState]
     *
     * @see addSingleFrameState
     */
    private var singleFrameStates = mutableListOf<StateData>()

    private fun addFrameState(
        frameStartTime: Long,
        frameEndTime: Long,
        frameStates: MutableList<StateInfo>,
        activeStates: MutableList<StateData>
    ) {
        for (i in activeStates.indices.reversed()) {
            // idea: add state if state was active during this frame
            // so state start time must be before vsync+duration
            // also, if state end time < vsync, delete it
            val item = activeStates[i]
            if (item.timeRemoved > 0 && item.timeRemoved < frameStartTime) {
                // remove states that have already been marked for removal
                activeStates.removeAt(i)
            } else if (item.timeAdded < frameEndTime) {
                // Only add unique state. There may be several states added in
                // a given frame (especially during heavy jank periods), but it is
                // only necessary/helpful to log one of those items
                if (item.state !in frameStates) {
                    frameStates.add(item.state)
                }
            }
        }
    }

    /**
     * This method doesn't actually remove it from the
     * given list of states, but instead logs the time at which removal was requested.
     * This enables more accurate sync'ing of states with specific frames, depending on
     * when states are added/removed and when frames start/end. States will actually be removed
     * from the list later, as they fall out of the current frame start times and stop being
     * a factor in logging.
     *
     * @param stateName   The name used for this state, should match the name used when
     * [adding][addState] the state previously.
     * @param states      The list of states to remove this from (either the regular state
     * info or the singleFrame info)
     * @param removalTime The timestamp of this request. This will be used to log the time that
     * this state stopped being active, which will be used later to sync
     * states with frame boundaries.
     */
    private fun markStateForRemoval(
        stateName: String,
        states: List<StateData>,
        removalTime: Long
    ) {
        synchronized(singleFrameStates) {
            for (item in states) {
                if (item.state.stateName == stateName && item.timeRemoved < 0) {
                    item.timeRemoved = removalTime
                }
            }
        }
    }

    /**
     * Adds information about the state of the application that may be useful in
     * future JankStats report logs.
     *
     * State information can be about UI elements that are currently active (such as the current
     * [Activity] or layout) or a user interaction like flinging a list.
     * Adding a state with a stateName which is already added will replace that earlier
     * `state` value with the new `state` value.
     *
     * Some state may be provided automatically by other AndroidX libraries.
     * But applications are encouraged to add user state specific to those applications
     * to provide more context and more actionable information in JankStats logs.
     *
     * For example, an app that wanted to track jank data about a specific transition
     * in a picture-gallery view might provide state like this:
     *
     * `JankStats.addState("GalleryTransition", "Running")`
     *
     * @param stateName An arbitrary name used for this state, used as a key for storing
     * the state value.
     * @param state The value of this state.
     * @see removeState
     */
    fun addState(stateName: String, state: String) {
        synchronized(singleFrameStates) {
            val nowTime = System.nanoTime()
            markStateForRemoval(stateName, states, nowTime)
            states.add(
                StateData(
                    nowTime, -1,
                    StateInfo(stateName, state)
                )
            )
        }
        // TODO: consider pooled StateInfo objects that we reuse here instead of creating new
        // ones every time
    }

    /**
     * [addSingleFrameState] is like [addState], except it persists only for the
     * current frame and will be automatically removed after it is logged for that frame.
     *
     * This method can be used for very short-lived state, or state for which it may be
     * difficult to determine when it should be removed (leading to erroneous data if state
     * is left present long after it actually stopped happening in the app).
     *
     * @param stateName An arbitrary name used for this state, used as a key for storing
     * the state value.
     * @param state The value of this state.
     * @see addState
     */
    fun addSingleFrameState(
        stateName: String,
        state: String
    ) {
        synchronized(singleFrameStates) {
            val nowTime = System.nanoTime()
            markStateForRemoval(stateName, singleFrameStates, nowTime)
            singleFrameStates.add(
                StateData(
                    nowTime, -1,
                    StateInfo(stateName, state)
                )
            )
        }
    }

    private fun markStateForRemoval(stateName: String) {
        markStateForRemoval(stateName, states, System.nanoTime())
    }

    /**
     * Internal representation of state information. timeAdded/Removed allows synchronizing states
     * with frame boundaries during the FrameMetrics callback, when we can compare which states
     * were active during any given frame start/end period.
     */
    internal class StateData(
        var timeAdded: Long,
        var timeRemoved: Long,
        var state: StateInfo
    )

    /**
     * Removes information about a specified state.
     *
     * [removeState] is typically called when
     * the user stops being in that state, either leaving a container previously added to
     * the state, or stopping some interaction that was added.
     *
     * @param stateName The name used for this state, should match the name used when
     * [adding][addState] the state previously.
     * @see addState
     */
    fun removeState(stateName: String) {
        markStateForRemoval(stateName)
    }

    /**
     * Retrieve the states current in the period defined by `startTime` and `endTime`.
     * When a state is added via [addState] or [addSingleFrameState], the time at which
     * it is added is noted when storing it. This time is used later in calls to
     * [getIntervalStates] to determine whether that state was active during the
     * given window of time.
     *
     * Note that states are also managed implicitly in this function. Specifically,
     * states added via [addSingleFrameState] are removed, since they have been used
     * exactly once to retrieve the state for this interval.
     */
    internal fun getIntervalStates(startTime: Long, endTime: Long): List<StateInfo> {
        var frameStates: MutableList<StateInfo>
        synchronized(singleFrameStates) {
            frameStates = ArrayList<StateInfo>(
                states.size +
                    singleFrameStates.size
            )
            addFrameState(startTime, endTime, frameStates, states)
            addFrameState(startTime, endTime, frameStates, singleFrameStates)
        }
        return frameStates
    }

    internal fun cleanupSingleFrameStates() {
        synchronized(singleFrameStates) {
            // Remove all states intended for just one frame
            singleFrameStates.clear()
        }
    }

    companion object {

        /**
         * This function gets the single PerformanceMetricsStateHolder object for the view hierarchy
         * in which `view' exists. If there is no PerformanceMetricsState object yet, this function
         * will create and store one.
         *
         * Note that the function will not create a PerformanceMetricsState object if the
         * MetricsStateHolder's `state` is null; that object is created when a [JankStats]
         * object is created. This is done to avoid recording performance state if it is
         * not being tracked.
         *
         * Note also that this function should only be called with a view that is added to the
         * view hierarchy, since information about the holder is cached at the root of that
         * hierarchy. The recommended approach is to set up the holder in
         * [View.OnAttachStateChangeListener.onViewAttachedToWindow].
         */
        @JvmStatic
        @UiThread
        fun getForHierarchy(view: View): MetricsStateHolder {
            val rootView = getRootView(view)
            var metricsStateHolder = rootView.getTag(R.id.metricsStateHolder)
            if (metricsStateHolder == null) {
                metricsStateHolder = MetricsStateHolder()
                rootView.setTag(R.id.metricsStateHolder, metricsStateHolder)
            }
            return metricsStateHolder as MetricsStateHolder
        }

        /**
         * This function returns the single PerformanceMetricsStateHolder object for the view
         * hierarchy in which `view' exists. Unlike [getForHierarchy], this function will create
         * the underlying [PerformanceMetricsState] object if it does not yet exist, and will
         * set it on the holder object.
         *
         * This function exists mainly for internal use by [JankStats]; most callers should use
         * [getForHierarchy] instead to simply retrieve the existing state information, not to
         * create it. Creation is reserved for JankStats because there is no sense storing state
         * information if it is not being tracked by JankStats.
         */
        @JvmStatic
        @UiThread
        internal fun create(view: View): MetricsStateHolder {
            val holder = getForHierarchy(view)
            if (holder.state == null) {
                holder.state = PerformanceMetricsState()
            }
            return holder
        }

        internal fun getRootView(view: View): View {
            var rootView = view
            var parent = rootView.parent
            while (parent is View) {
                rootView = parent
                parent = rootView.parent
            }
            return rootView
        }
    }

    /**
     * This class holds the current [PerformanceMetricsState] for a given view hierarchy.
     * Callers should request the holder for a hierarchy via [getForHierarchy], and check
     * the value of the [state] property to see whether state is being tracked by JankStats
     * for the hierarchy.
     */
    class MetricsStateHolder internal constructor() {

        /**
         * The current PerformanceMetricsState for the view hierarchy where this
         * MetricsStateHolder object was retrieved. A null value indicates that state
         * is not currently being tracked (or stored).
         */
        var state: PerformanceMetricsState? = null
            internal set
    }
}