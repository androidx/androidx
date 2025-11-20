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
 * This class is used to store information about the state of an application that can be retrieved
 * later to associate state with performance timing data.
 *
 * For example, PerformanceMetricsState is used in conjunction with [JankStats] to enable JankStats
 * to report per-frame performance characteristics along with the application state that was present
 * at the time that the frame data was logged.
 *
 * There is only one PerformanceMetricsState available per view hierarchy. That instance can be
 * retrieved from the holder returned by [PerformanceMetricsState.getHolderForHierarchy]. Limiting
 * PerformanceMetricsState to a single object per hierarchy makes it possible for code outside the
 * core application logic, such as in a library, to store application state that can be useful for
 * the application to know about.
 */
class PerformanceMetricsState private constructor() {

    /**
     * Data to track UI and user state in this JankStats object.
     *
     * @see putState
     * @see markStateForRemoval
     */
    private var states = mutableListOf<StateData>()

    /**
     * Temporary per-frame to track UI and user state. Unlike the states tracked in `states`, any
     * state in this structure is only valid until the next frame, at which point it is cleared. Any
     * state data added here is automatically removed; there is no matching "remove" method for
     * [.putSingleFrameState]
     *
     * @see putSingleFrameState
     */
    private var singleFrameStates = mutableListOf<StateData>()

    /**
     * Temporary list to hold states that will be added to for any given frame in addFrameState().
     * It is used to avoid adding duplicate states by storing all data for states being considered.
     */
    private val statesHolder = mutableListOf<StateData>()
    private val statesToBeCleared = mutableListOf<Int>()
    private val pendingStatesToBeCleared = mutableListOf<Int>()

    /**
     * StateData objects are stored and retrieved from an object pool, to avoid re-allocating for
     * new state pairs, since it is expected that most states will share names/states
     */
    private val stateDataPool = mutableListOf<StateData>()

    private fun addFrameState(
        frameStartTime: Long,
        frameEndTime: Long,
        frameStates: MutableList<StateInfo>,
        activeStates: MutableList<StateData>,
    ) {
        for (i in activeStates.indices.reversed()) {
            // idea: add state if state was active during this frame
            // so state start time must be before vsync+duration
            // also, if state end time < vsync, delete it
            val item = activeStates[i]
            if (item.timeRemoved > 0 && item.timeRemoved < frameStartTime) {
                // remove states that have already been marked for removal
                returnStateDataToPool(activeStates.removeAt(i))
            } else if (item.timeAdded < frameEndTime) {
                // Only add unique state. There may be several states added in
                // a given frame (especially during heavy jank periods). Only the
                // most recently added should be logged, as it replaces the earlier ones.
                statesHolder.add(item)
                if (activeStates == singleFrameStates && item.timeRemoved == -1L) {
                    // This marks a single frame state for removal now that it has logged data.
                    // It will actually be removed on the next frame, with the removal logic
                    // above, to give it a chance to log data for multiple listeners on this frame.
                    item.timeRemoved = frameStartTime
                }
            }
        }
        // It's possible to have multiple versions with the same key active on a given
        // frame. This should result in only using the latest state added, which is what
        // this block ensures.
        if (statesHolder.size > 0) {
            for (i in 0 until statesHolder.size) {
                if (i in pendingStatesToBeCleared) {
                    statesToBeCleared.add(i)
                } else {
                    val item = statesHolder.get(i)
                    for (j in (i + 1) until statesHolder.size) {
                        val otherItem = statesHolder.get(j)
                        if (item.state.key == otherItem.state.key) {
                            // If state names are the same, remove the one added earlier.
                            // Note that we are only marking them for removal here since we
                            // cannot alter the structure while iterating through it.
                            if (item.timeAdded < otherItem.timeAdded) {
                                statesToBeCleared.add(i)
                                // If we are clearing i, we break here since j's timeAdded
                                // is the new time to compare against.
                                break
                            } else {
                                // If we are clearing j, we should not add it to
                                // statesToBeCleared until i reaches that position
                                // in order to maintain descending order
                                pendingStatesToBeCleared.add(j)
                            }
                        }
                    }
                }
            }
            // This block actually removes the duplicate items
            for (i in statesToBeCleared.size - 1 downTo 0) {
                statesHolder.removeAt(statesToBeCleared[i])
            }
            // Finally, process all items left in the holder list and add them to frameStates
            for (i in 0 until statesHolder.size) {
                frameStates.add(statesHolder[i].state)
            }
            statesHolder.clear()
            statesToBeCleared.clear()
            pendingStatesToBeCleared.clear()
        }
    }

    /**
     * This method doesn't actually remove it from the given list of states, but instead logs the
     * time at which removal was requested. This enables more accurate sync'ing of states with
     * specific frames, depending on when states are added/removed and when frames start/end. States
     * will actually be removed from the list later, as they fall out of the current frame start
     * times and stop being a factor in logging.
     *
     * @param key The name used for this state, should match the name used when [putting][putState]
     *   the state previously.
     * @param states The list of states to remove this from (either the regular state info or the
     *   singleFrame info)
     * @param removalTime The timestamp of this request. This will be used to log the time that this
     *   state stopped being active, which will be used later to sync states with frame boundaries.
     */
    private fun markStateForRemoval(key: String, states: List<StateData>, removalTime: Long) {
        synchronized(singleFrameStates) {
            for (i in 0 until states.size) {
                val item = states[i]
                if (item.state.key == key && item.timeRemoved < 0) {
                    item.timeRemoved = removalTime
                }
            }
        }
    }

    /**
     * Adds information about the state of the application that may be useful in future JankStats
     * report logs.
     *
     * State information can be about UI elements that are currently active (such as the current
     * [Activity] or layout) or a user interaction like flinging a list. If the
     * PerformanceMetricsState object already contains an entry with the same key, the old value is
     * replaced by the new one. Note that this means apps with several instances of similar objects
     * (such as multipe `RecyclerView`s) should therefore use unique keys for these instances to
     * avoid clobbering state values for other instances and to provide enough information for later
     * analysis which allows for disambiguation between these objects. For example, using
     * "RVHeaders" and "RVContent" might be more helpful than just "RecyclerView" for a messaging
     * app using `RecyclerView` objects for both a headers list and a list of message contents.
     *
     * Some state may be provided automatically by other AndroidX libraries. But applications are
     * encouraged to add user state specific to those applications to provide more context and more
     * actionable information in JankStats logs.
     *
     * For example, an app that wanted to track jank data about a specific transition in a
     * picture-gallery view might provide state like this:
     *
     * `state.putState("GalleryTransition", "Running")`
     *
     * @param key An arbitrary name used for this state, used as a key for storing the state value.
     * @param value The value of this state.
     * @see removeState
     */
    fun putState(key: String, value: String) {
        synchronized(singleFrameStates) {
            val nowTime = System.nanoTime()
            markStateForRemoval(key, states, nowTime)
            states.add(getStateData(nowTime, -1, StateInfo(key, value)))
        }
    }

    /**
     * [putSingleFrameState] is like [putState], except the state persists only for the current
     * frame and will be automatically removed after it is logged for that frame.
     *
     * This method can be used for very short-lived state, or state for which it may be difficult to
     * determine when it should be removed (leading to erroneous data if state is left present long
     * after it actually stopped happening in the app).
     *
     * @param key An arbitrary name used for this state, used as a key for storing the state value.
     * @param value The value of this state.
     * @see putState
     */
    fun putSingleFrameState(key: String, value: String) {
        synchronized(singleFrameStates) {
            val nowTime = System.nanoTime()
            markStateForRemoval(key, singleFrameStates, nowTime)
            singleFrameStates.add(getStateData(nowTime, -1, StateInfo(key, value)))
        }
    }

    private fun markStateForRemoval(key: String) {
        markStateForRemoval(key, states, System.nanoTime())
    }

    internal fun removeStateNow(stateName: String) {
        synchronized(singleFrameStates) {
            for (i in 0 until states.size) {
                val item = states[i]
                if (item.state.key == stateName) {
                    states.remove(item)
                    returnStateDataToPool(item)
                }
            }
        }
    }

    /**
     * Internal representation of state information. timeAdded/Removed allows synchronizing states
     * with frame boundaries during the FrameMetrics callback, when we can compare which states were
     * active during any given frame start/end period.
     */
    internal class StateData(var timeAdded: Long, var timeRemoved: Long, var state: StateInfo)

    internal fun getStateData(timeAdded: Long, timeRemoved: Long, state: StateInfo): StateData {
        synchronized(stateDataPool) {
            if (stateDataPool.isEmpty()) {
                // This new item will be added to the pool when it is removed, later
                return StateData(timeAdded, timeRemoved, state)
            } else {
                val stateData = stateDataPool.removeAt(0)
                stateData.timeAdded = timeAdded
                stateData.timeRemoved = timeRemoved
                stateData.state = state
                return stateData
            }
        }
    }

    /**
     * Once the StateData is done being used, it can be returned to the pool for later reuse, which
     * happens in getStateData()
     */
    internal fun returnStateDataToPool(stateData: StateData) {
        synchronized(stateDataPool) {
            try {
                stateDataPool.add(stateData)
            } catch (e: OutOfMemoryError) {
                // App must either be creating more unique states than expected or is having
                // unrelated memory pressure. Clear the pool and start over.
                stateDataPool.clear()
                stateDataPool.add(stateData)
            }
        }
    }

    /**
     * Removes information about a specified state.
     *
     * [removeState] is typically called when the user stops being in that state, such as leaving a
     * container previously put in the state, or stopping some interaction that was similarly saved.
     *
     * @param key The name used for this state, should match the name used when [putting][putState]
     *   the state previously.
     * @see putState
     */
    fun removeState(key: String) {
        markStateForRemoval(key)
    }

    /**
     * Retrieve the states current in the period defined by `startTime` and `endTime`. When a state
     * is added via [putState] or [putSingleFrameState], the time at which it is added is noted when
     * storing it. This time is used later in calls to [getIntervalStates] to determine whether that
     * state was active during the given window of time.
     *
     * Note that states are also managed implicitly in this function. Specifically, states added via
     * [putSingleFrameState] are removed, since they have been used exactly once to retrieve the
     * state for this interval.
     */
    internal fun getIntervalStates(
        startTime: Long,
        endTime: Long,
        frameStates: MutableList<StateInfo>,
    ) {
        synchronized(singleFrameStates) {
            frameStates.clear()
            addFrameState(startTime, endTime, frameStates, states)
            addFrameState(startTime, endTime, frameStates, singleFrameStates)
        }
    }

    internal fun cleanupSingleFrameStates() {
        synchronized(singleFrameStates) {
            // Remove all states intended for just one frame
            for (i in singleFrameStates.size - 1 downTo 0) {
                // SFStates are marked with timeRemoved during processing so we know when
                // they have logged data and can actually be removed
                if (singleFrameStates[i].timeRemoved != -1L) {
                    returnStateDataToPool(singleFrameStates.removeAt(i))
                }
            }
        }
    }

    companion object {

        /**
         * This function gets the single PerformanceMetricsState.Holder object for the view
         * hierarchy in which `view' exists. If there is no such object yet, this function will
         * create and store one.
         *
         * Note that the function will not create a PerformanceMetricsState object if the Holder's
         * `state` is null; that object is created when a [JankStats] object is created. This is
         * done to avoid recording performance state if it is not being tracked.
         *
         * Note also that this function should only be called with a view that is added to the view
         * hierarchy, since information about the holder is cached at the root of that hierarchy.
         * The recommended approach is to set up the holder in
         * [View.OnAttachStateChangeListener.onViewAttachedToWindow].
         */
        @JvmStatic
        @UiThread
        fun getHolderForHierarchy(view: View): Holder {
            val rootView = view.getRootView()
            var metricsStateHolder = rootView.getTag(R.id.metricsStateHolder)
            if (metricsStateHolder == null) {
                metricsStateHolder = Holder()
                rootView.setTag(R.id.metricsStateHolder, metricsStateHolder)
            }
            return metricsStateHolder as Holder
        }

        /**
         * This function returns the single PerformanceMetricsState.Holder object for the view
         * hierarchy in which `view' exists. Unlike [getHolderForHierarchy], this function will
         * create the underlying [PerformanceMetricsState] object if it does not yet exist, and will
         * set it on the holder object.
         *
         * This function exists mainly for internal use by [JankStats]; most callers should use
         * [getHolderForHierarchy] instead to simply retrieve the existing state information, not to
         * create it. Creation is reserved for JankStats because there is no sense storing state
         * information if it is not being tracked by JankStats.
         */
        @JvmStatic
        @UiThread
        internal fun create(view: View): Holder {
            val holder = getHolderForHierarchy(view)
            if (holder.state == null) {
                holder.state = PerformanceMetricsState()
            }
            return holder
        }
    }

    /**
     * This class holds the current [PerformanceMetricsState] for a given view hierarchy. Callers
     * should request the holder for a hierarchy via [getHolderForHierarchy], and check the value of
     * the [state] property to see whether state is being tracked by JankStats for the hierarchy.
     */
    class Holder internal constructor() {

        /**
         * The current PerformanceMetricsState for the view hierarchy where this Holder object was
         * retrieved. A null value indicates that state is not currently being tracked (or stored).
         */
        var state: PerformanceMetricsState? = null
            internal set
    }
}
