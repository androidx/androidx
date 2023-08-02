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

/**
 * This class stores duration data for a single frame.
 *
 * @property frameStartNanos The time at which this frame began (in nanoseconds)
 * @property frameDurationUiNanos The time spent in the UI portion of this frame (in nanoseconds).
 * This is essentially the time spent on the UI thread to draw this frame, but does
 * not include any time spent on the RenderThread.
 * @property isJank Whether this frame was determined to be janky, meaning that its
 * duration exceeds the duration determined by the system to indicate jank (@see
 * [JankStats.jankHeuristicMultiplier]).
 * @property states The UI/app state during this frame.
 * This is the information set by the app, or by other library code, that can be analyzed
 * later to determine the UI state that was current when jank occurred.
 *
 * @see JankStats.jankHeuristicMultiplier
 * @see PerformanceMetricsState.putState
 */
open class FrameData(
    frameStartNanos: Long,
    frameDurationUiNanos: Long,
    isJank: Boolean,
    val states: List<StateInfo>
) {
    /**
     * These backing fields are used to enable mutation of an existing FrameData object, to
     * avoid allocating a new object on every frame for sending out to listeners.
     */
    var frameStartNanos = frameStartNanos
        private set
    var frameDurationUiNanos = frameDurationUiNanos
        private set
    var isJank = isJank
        private set

    /**
     * Utility method which makes a copy of the items in this object (including copying the items
     * in `states` into a new List). This is used internally to create a copy to pass along to
     * listeners to avoid having a reference to the internally-mutable FrameData object.
     */
    open fun copy(): FrameData {
        return FrameData(frameStartNanos, frameDurationUiNanos, isJank, ArrayList(states))
    }

    /**
     * Utility method for updating the internal values in this object. Externally, this object is
     * immutable. Internally, we need the ability to update the values so that we can reuse
     * it for a non-allocating listener model, to avoid having to re-allocate a new FrameData
     * (and its states List). Note that the states object is not being updated here; internal
     * can already use a Mutable list to update the contents of that list; they do not need to
     * update this object with a new List, since any usage of FrameData to avoid allocations
     * should not be creating a new state List anyway.
     */
    internal fun update(frameStartNanos: Long, frameDurationUiNanos: Long, isJank: Boolean) {
        this.frameStartNanos = frameStartNanos
        this.frameDurationUiNanos = frameDurationUiNanos
        this.isJank = isJank
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FrameData

        if (frameStartNanos != other.frameStartNanos) return false
        if (frameDurationUiNanos != other.frameDurationUiNanos) return false
        if (isJank != other.isJank) return false
        if (states != other.states) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frameStartNanos.hashCode()
        result = 31 * result + frameDurationUiNanos.hashCode()
        result = 31 * result + isJank.hashCode()
        result = 31 * result + states.hashCode()
        return result
    }

    override fun toString(): String {
        return "FrameData(frameStartNanos=$frameStartNanos, " +
            "frameDurationUiNanos=$frameDurationUiNanos, " +
            "isJank=$isJank, " +
            "states=$states)"
    }
}

/**
 * This class contains information about application state.
 *
 * @property key An arbitrary name used for this state, used as a key for storing
 * the state value.
 * @property value The value of this state.
 */
class StateInfo(val key: String, val value: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StateInfo

        if (key != other.key) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String {
        return "$key: $value"
    }

    /**
     * This internal componion is used to manage a pool of reusable StateInfo objects.
     * Rather than creating a new StateInfo object very time, the library requests an object
     * for the given stateName/state pair. In general, requests will be common using the same
     * pairs, thus reuse will be high and an object from the pool will be returned. When reuse
     * is not necessary, a new StateInfo object will be created, added to the pool, and returned.
     */
    internal companion object {
        val pool = mutableMapOf<String, MutableMap<String, StateInfo>>()

        fun getStateInfo(stateName: String, state: String): StateInfo {
            synchronized(pool) {
                var poolItem = pool.get(stateName)
                var stateInfo = poolItem?.get(state)
                if (stateInfo != null) return stateInfo
                else {
                    stateInfo = StateInfo(stateName, state)
                    if (poolItem != null) {
                        poolItem.put(state, stateInfo)
                    } else {
                        poolItem = mutableMapOf(state to stateInfo)
                        pool.put(stateName, poolItem)
                    }
                    return stateInfo
                }
            }
        }
    }
}
