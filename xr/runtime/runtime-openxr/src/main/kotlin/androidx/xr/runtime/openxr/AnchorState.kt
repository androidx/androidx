/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.openxr

import androidx.xr.runtime.internal.TrackingState
import androidx.xr.runtime.math.Pose

/**
 * Represents the current state of an [Anchor] instance's mutable fields.
 *
 * @property trackingState the [TrackingState] value describing if the anchor is being updated.
 * @property pose the pose of the center of the detected anchor. Can be null iff the tracking state
 *   is [TrackingState.Stopped].
 */
public data class AnchorState(
    val trackingState: TrackingState = TrackingState.Paused,
    val pose: Pose? = Pose(),
) {
    init {
        require(pose != null || trackingState == TrackingState.Stopped) {
            "Pose cannot be null if tracking state is not STOPPED."
        }
    }
}

/**
 * Create a [TrackingState] inferred from the [XrSpaceLocationFlags] returned with the anchor
 * location data. The following rules are used to determine the [TrackingState]:
 * * If both valid and tracking bits are flipped, return [TrackingState.Tracking]
 * * If both valid bits are flipped, but not both tracking bits, return [TrackingState.Paused]
 * * Any other combination of flipped bits (i.e. both valid bits are not flipped), return
 *   [TrackingState.Stopped]
 */
internal fun TrackingState.Companion.fromOpenXrLocationFlags(flags: Int): TrackingState {
    val VALID_MASK = 0x00000001 or 0x00000002
    val TRACKING_MASK = VALID_MASK or 0x00000004 or 0x00000008

    require(flags or TRACKING_MASK == TRACKING_MASK) { "Invalid location flag bits." }

    return when {
        (flags and TRACKING_MASK) == TRACKING_MASK -> TrackingState.Tracking
        (flags and VALID_MASK) == VALID_MASK -> TrackingState.Paused
        else -> TrackingState.Stopped
    }
}
