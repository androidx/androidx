/*
 * Copyright 2022 The Android Open Source Project
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

open class FrameDataApi24(
    /**
     * The time at which this frame began (in nanoseconds)
     */
    frameStartNanos: Long,

    /**
     * The time spent in the UI portion of this frame (in nanoseconds).
     *
     * This is essentially the time spent on the UI thread to draw this frame, but does
     * not include any time spent on the RenderThread.
     */
    frameDurationUiNanos: Long,

    /**
     * The time spent in the non-GPU portions of this frame (in nanoseconds).
     *
     * This includes the time spent on the UI thread [frameDurationUiNanos] plus time
     * spent on the RenderThread.
     */
    val frameDurationCpuNanos: Long,

    /**
     * Whether this frame was determined to be janky, meaning that its
     * duration exceeds the duration determined by the system to indicate jank (@see
     * [JankStats.jankHeuristicMultiplier])
     */
    isJank: Boolean,

    /**
     * The UI/app state during this frame.
     *
     * This is the information set by the app, or by other library code, that can be analyzed
     * later to determine the UI state that was current when jank occurred.
     *
     * @see PerformanceMetricsState.addState
     */
    states: List<StateInfo>

) : FrameData(frameStartNanos, frameDurationUiNanos, isJank, states) {

    override fun equals(other: Any?): Boolean {
        other as FrameDataApi24
        return super.equals(other) && (frameDurationCpuNanos != other.frameDurationCpuNanos)
    }

    override fun hashCode(): Int {
        return 31 * super.hashCode() + frameDurationCpuNanos.hashCode()
    }

    override fun toString(): String {
        return "FrameData(frameStartNanos=$frameStartNanos, " +
            "frameDurationUiNanos=$frameDurationUiNanos, " +
            "frameDurationCpuNanos=$frameDurationCpuNanos, " +
            "isJank=$isJank, " +
            "states=$states)"
    }
}