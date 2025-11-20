/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import androidx.annotation.RestrictTo
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose

/**
 * Represents the current state of an [Anchor] instance's mutable fields.
 *
 * @property trackingState the [androidx.xr.runtime.TrackingState] value describing if the anchor is
 *   being updated.
 * @property pose the pose of the center of the detected anchor. Can be null iff the tracking state
 *   is [androidx.xr.runtime.TrackingState.Companion.STOPPED].
 */
@Suppress("DataClassDefinition")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public data class AnchorState(
    val trackingState: TrackingState = TrackingState.PAUSED,
    val pose: Pose? = Pose(),
) {
    init {
        require(pose != null || trackingState == TrackingState.STOPPED) {
            "Pose cannot be null if tracking state is not STOPPED."
        }
    }
}

/**
 * Create a [androidx.xr.runtime.TrackingState] inferred from the [XrSpaceLocationFlags] returned
 * with the anchor location data. The following rules are used to determine the
 * [androidx.xr.runtime.TrackingState]:
 * * If both valid and tracking bits are flipped, return
 *   [androidx.xr.runtime.TrackingState.Companion.TRACKING]
 * * If both valid bits are flipped, but not both tracking bits, return
 *   [androidx.xr.runtime.TrackingState.Companion.PAUSED]
 * * Any other combination of flipped bits (i.e. both valid bits are not flipped), return
 *   [androidx.xr.runtime.TrackingState.Companion.STOPPED]
 */
internal fun TrackingState.Companion.fromOpenXrLocationFlags(flags: Int): TrackingState {
    val VALID_MASK = 0x00000001 or 0x00000002
    val TRACKING_MASK = VALID_MASK or 0x00000004 or 0x00000008

    require(flags or TRACKING_MASK == TRACKING_MASK) { "Invalid location flag bits." }

    return when {
        (flags and TRACKING_MASK) == TRACKING_MASK -> TrackingState.TRACKING
        (flags and VALID_MASK) == VALID_MASK -> TrackingState.PAUSED
        else -> TrackingState.STOPPED
    }
}
