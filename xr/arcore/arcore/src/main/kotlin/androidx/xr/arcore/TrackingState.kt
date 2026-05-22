/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore

import androidx.xr.arcore.runtime.TrackingState as RTTrackingState

/** Describes the state of the tracking performed. */
public class TrackingState private constructor(private val value: Int) {
    public companion object {
        /** The trackable is currently tracked and its pose is current. */
        @JvmField public val TRACKING: TrackingState = TrackingState(0)

        /** Tracking has been paused for this instance but may be resumed in the future. */
        @JvmField public val PAUSED: TrackingState = TrackingState(1)

        /** Tracking has stopped for this instance and will never be resumed in the future. */
        @JvmField public val STOPPED: TrackingState = TrackingState(2)

        /** Tracking is valid but the quality is degraded. */
        @JvmField public val TRACKING_DEGRADED: TrackingState = TrackingState(3)
    }

    /** Returns a string representation of [TrackingState] useful for debugging. */
    override fun toString(): String {
        val repr =
            when (this) {
                TRACKING -> "TRACKING"
                PAUSED -> "PAUSED"
                STOPPED -> "STOPPED"
                TRACKING_DEGRADED -> "TRACKING_DEGRADED"
                else -> throw SOMEONE_FORGOT_TO_UPDATE_TRACKING_STATE
            }
        return "TrackingState($repr)"
    }

    internal fun toRuntimeTrackingState(): RTTrackingState =
        when (this) {
            TRACKING -> RTTrackingState.TRACKING
            PAUSED -> RTTrackingState.PAUSED
            STOPPED -> RTTrackingState.STOPPED
            TRACKING_DEGRADED -> RTTrackingState.TRACKING_DEGRADED
            else -> throw SOMEONE_FORGOT_TO_UPDATE_TRACKING_STATE
        }
}

internal fun RTTrackingState.toTrackingState(): TrackingState =
    when (this) {
        RTTrackingState.TRACKING -> TrackingState.TRACKING
        RTTrackingState.PAUSED -> TrackingState.PAUSED
        RTTrackingState.STOPPED -> TrackingState.STOPPED
        RTTrackingState.TRACKING_DEGRADED -> TrackingState.TRACKING_DEGRADED
        else -> throw SOMEONE_FORGOT_TO_UPDATE_TRACKING_STATE
    }

private val SOMEONE_FORGOT_TO_UPDATE_TRACKING_STATE =
    IllegalStateException(
        "Unexpected TrackingState value. This usually means a new TrackingState value was added but not reflected in the conversion implementations."
    )
