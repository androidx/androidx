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
 */
open class FrameData(
    /**
     * The time at which this frame began (in nanoseconds)
     */
    val frameStartNanos: Long,

    /**
     * The time spent in the UI portion of this frame (in nanoseconds).
     *
     * This is essentially the time spent on the UI thread to draw this frame, but does
     * not include any time spent on the RenderThread.
     */
    val frameDurationUiNanos: Long,

    /**
     * Whether this frame was determined to be janky, meaning that its
     * duration exceeds the duration determined by the system to indicate jank (@see
     * [JankStats.jankHeuristicMultiplier])
     */
    val isJank: Boolean,

    /**
     * The UI/app state during this frame.
     *
     * This is the information set by the app, or by other library code, that can be analyzed
     * later to determine the UI state that was current when jank occurred.
     *
     * @see PerformanceMetricsState.addState
     */
    val states: List<StateInfo>
) {

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
 * @property stateName An arbitrary name used for this state, used as a key for storing
 * the state value.
 * @property state The value of this state.
 */
class StateInfo(val stateName: String, val state: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StateInfo

        if (stateName != other.stateName) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stateName.hashCode()
        result = 31 * result + state.hashCode()
        return result
    }

    override fun toString(): String {
        return "$stateName: $state"
    }
}