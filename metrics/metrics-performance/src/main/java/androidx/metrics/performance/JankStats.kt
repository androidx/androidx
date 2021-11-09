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
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import androidx.annotation.UiThread
import java.lang.IllegalStateException
import java.util.concurrent.Executor

/**
 * This class is used to both accumulate and report information about UI "jank" (runtime
 * performance problems) in an application.
 *
 * There are three major components at work in JankStats:
 *
 * **Identifying Jank**: This library uses internal heuristics to determine when
 * jank has occurred, and uses that information to know when to issue jank reports so
 * that developers have information on those problems to help analyze and fix the issues.
 *
 * **Providing UI Context**: To make the jank reports more useful and actionable,
 * the system provides a mechanism to help track the current state of the UI and user.
 * This information is provided whenever reports are logged, so that developers can
 * understand not only when problems occurred, but what the user was doing at the time,
 * to help identify problem areas in the application that can then be addressed. Some
 * of this state is provided automatically, and internally, by various AndroidX libraries.
 * But developers are encouraged to provide their own app-specific state as well.
 *
 * **Reporting Results**: On every frame, the JankStats client is notified via a listener
 * with information about that frame, including how long the frame took to
 * complete, whether it was considered jank, and what the UI context was during that frame.
 * Clients are encouraged to aggregate and upload the data as they see fit for analysis that
 * can help debug overall performance problems.
 *
 * Note that the behavior of JankStats varies according to API level, because it is dependent
 * upon underlying capabilities in the platform to determine frame timing information.
 * Below API level 16, JankStats does nothing, because there is no way to derive dependable
 * frame timing data. Starting at API level 16, JankStats uses rough frame timing information
 * that can at least provide estimates of how long frames should have taken, compared to how
 * long they actually took. Starting with API level 24, frame durations are more dependable,
 * using platform timing APIs that are available in that release. And starting in API level
 * 31, there is even more underlying platform information which helps provide more accurate
 * timing still. On all of these releases (starting with API level 16), the base functionality
 * of JankStats should at least provide useful information about performance problems, along
 * with the state of the application during those frames, but the timing data will be necessarily
 * more accurate for later releases, as described above.
 */
@Suppress("SingletonConstructor")
class JankStats private constructor(
    view: View,
    private val executor: Executor,
    private val frameListener: OnFrameListener
) {

    /**
     * Whether this JankStats instance is enabled for tracking and reporting jank data.
     * Enabling tracking causes JankStats to listen to system frame-timing information and
     * record data on a per-frame basis that can later be reported to the JankStats listener.
     * Tracking is enabled by default at creation time.
     */
    var isTrackingEnabled: Boolean = false
        /**
         * Enabling tracking causes JankStats to listen to system frame-timing information and
         * record data on a per-frame basis that can later be reported to the JankStats listener.
         * Tracking is enabled by default at creation time.
         */
        @UiThread
        set(value) {
            implementation.setupFrameTimer(value)
            field = value
        }

    /**
     * This multiplier is used to determine when frames are exhibiting jank.
     *
     * The factor is multiplied by the current refresh rate to calculate a frame
     * duration beyond which frames are considered, and reported, as having jank.
     * By default, the multiplier is 2.
     */
    var jankHeuristicMultiplier: Float = 2.0f
        set(value) {
            // reset calculated value to force recalculation based on new heuristic
            JankStatsBaseImpl.frameDuration = -1
            field = value
        }

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

    /**
     * JankStats uses the platform FrameMetrics API internally when it is available to track frame
     * timings. It turns this data into "jank" metrics. Prior to API 24, it uses other mechanisms
     * to derive frame durations (not as dependable as FrameMetrics, but better than nothing).
     *
     * Because of this platform version limitation, most of the functionality of
     * JankStats is in the impl class, which is instantiated when necessary
     * based on the runtime OS version. The JankStats API is basically a think wrapper around
     * the implementations in these classes.
     */
    private val implementation =
        when {
            Build.VERSION.SDK_INT >= 31 -> {
                JankStatsApi31Impl(this, view)
            }
            Build.VERSION.SDK_INT >= 26 -> {
                JankStatsApi26Impl(this, view)
            }
            Build.VERSION.SDK_INT >= 24 -> {
                JankStatsApi24Impl(this, view)
            }
            Build.VERSION.SDK_INT >= 22 -> {
                JankStatsApi22Impl(this, view)
            }
            Build.VERSION.SDK_INT >= 16 -> {
                JankStatsApi16Impl(this, view)
            }
            else -> {
                JankStatsBaseImpl(this)
            }
        }

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
                activeStates.removeAt(i)
            } else if (item.timeAdded < frameEndTime) {
                // Only add unique state. There may be several states added in
                // a given frame (especially during heavy jank periods), but it is
                // only necessary/helpful to log one of those items
                if (item.state !in frameStates) {
                    frameStates.add(item.state)
                }
                // Single-frame states should only be logged once
                if (activeStates == singleFrameStates) {
                    activeStates.removeAt(i)
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
     * @see JankStats.removeState
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
     * @see JankStats.addState
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

    private fun markSingleFrameStateForRemoval(stateName: String) {
        markStateForRemoval(stateName, singleFrameStates, System.nanoTime())
    }

    /**
     * Removes all states (including singleFrame states).
     */
    fun clearStates() {
        for (stateData in states) {
            markStateForRemoval(stateData.state.stateName)
        }
        for (stateData in singleFrameStates) {
            markStateForRemoval(stateData.state.stateName)
        }
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
     * @see JankStats.addState
     */
    fun removeState(stateName: String) {
        markStateForRemoval(stateName)
    }

    internal fun logFrameData(startTime: Long, actualDuration: Long, expectedDuration: Long) {
        val endTime = startTime + actualDuration
        var frameStates: MutableList<StateInfo>
        synchronized(singleFrameStates) {
            frameStates = ArrayList<StateInfo>(
                states.size +
                    singleFrameStates.size
            )
            addFrameState(
                startTime, endTime, frameStates,
                states
            )
            addFrameState(
                startTime, endTime,
                frameStates, singleFrameStates
            )
        }
        val isJank = actualDuration > expectedDuration
        executor.execute(Runnable {
            frameListener.onFrame(FrameData(startTime, actualDuration, isJank, frameStates))
        })

        // Remove any states intended for just one frame
        if (singleFrameStates.size > 0) {
            synchronized(singleFrameStates) {
                for (state in singleFrameStates) {
                    markSingleFrameStateForRemoval(state.state.stateName)
                }
            }
        }
    }

    companion object {

        /**
         * Gets a JankStats object for the given View, creating one if necessary.
         *
         * There is only ever one JankStats object per View hierarchy; those singleton objects are
         * created by calling this method. The View used can be any view currently attached in
         * the hierarchy; the JankStats object is cached at the root level of the hierarchy.
         * If no such object exists, one will be created.
         *
         * Because this method takes a View instance, it should only be called on the UI
         * thread, to ensure that that View is usable at this time, since it will be used
         * internally to set up hierarchy-specific JankStats logging.
         *
         * @param view Any view in the hierarchy which this JankStats object will track. A JankStats
         * instance is specific to each window in an application, since the timing metrics are
         * tracked on a per-window basis internally, and the view hierarchy can be used as a proxy
         * for that window.
         * @param executor The executor that will be used to call the frameListener.
         * @param frameListener This listener will be called on any frame in which jank is detected.
         * @return A new JankStatus object for the given View's hierarchy.
         * @throws IllegalStateException This function will throw an exception if there is already
         * an existing JankStats object for this view hierarchy.
         */
        @JvmStatic
        @UiThread
        fun create(view: View, executor: Executor, frameListener: OnFrameListener): JankStats {
            if (get(view) != null) {
                throw IllegalStateException(
                    "JankStats object already exists in this view hierarchy")
            }
            return JankStats(view, executor, frameListener)
        }

        /**
         * Gets a JankStats object for the given View if it exists.
         *
         * There is only ever one JankStats object per View hierarchy; those singleton objects are
         * retrieved by calling this method. The View used can be any view currently attached in
         * the hierarchy; the JankStats object is cached at the root level of the hierarchy.
         * If no such object exists, null will be returned.
         *
         * This accessor is provided as a utility to simplify accessing a shared JankStats object
         * from different places in an application. It is used by other AndroidX library code
         * which can set information on an existing JankStats object if it exists.
         *
         * Because this method takes a View instance, it should only be called on the UI
         * thread, to ensure that that View is usable at this time, since it will be used
         * internally to set up hierarchy-specific JankStats logging.
         *
         * @param view The View for which the JankStats object is requested.
         * @return A JankStatus object for the given View's hierarchy, or null if no such object
         * exists.
         */
        @JvmStatic
        @UiThread
        @JvmName("getInstance")
        fun get(view: View): JankStats? {
            val activity: Activity? = generateSequence(view.context) {
                if (it is ContextWrapper) {
                    it.baseContext
                } else null
            }.firstOrNull { it is Activity } as Activity?

            return activity?.window?.decorView?.getTag(R.id.jankstats) as JankStats?
        }
    }

    init {
        val activity: Activity? = generateSequence(view.context) {
            if (it is ContextWrapper) {
                it.baseContext
            } else null
        }.firstOrNull { it is Activity } as Activity?

        activity?.window?.decorView?.setTag(R.id.jankstats, this)

        isTrackingEnabled = true
    }

    /**
     * This listener is called on every frame to supply ongoing jank data to apps.
     *
     * [JankStats] requires an implementation of this listener at construction time.
     * On every frame, the listener is called and receivers can aggregate, store, and upload
     * the data as appropriate.
     */
    fun interface OnFrameListener {

        /**
         * The implementation of this method will be called on every frame when an
         * OnFrameListener is set on this JankStats object.
         *
         * @param frameData The data for the frame which just occurred.
         */
        fun onFrame(
            frameData: FrameData
        )
    }
}