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

import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Plane
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2

/**
 * Represents the current state of a [Plane] instance's mutable fields.
 *
 * @property trackingState the [TrackingState] value describing if the plane is being updated.
 * @property label the [Plane.Label] associated with the plane.
 * @property centerPose the pose of the center of the detected plane. The pose's transformed +Y axis
 *   will be point normal out of the plane, with the +X and +Z axes orienting the extents of the
 *   bounding rectangle.
 * @property extents the dimensions of the detected plane.
 * @property vertices the 2D vertices of a convex polygon approximating the detected plane,
 * @property subsumedByPlaneId the OpenXR handle of the plane that subsumed this plane.
 */
internal data class PlaneState(
    val trackingState: TrackingState = TrackingState.PAUSED,
    val label: Plane.Label = Plane.Label.UNKNOWN,
    val centerPose: Pose = Pose(),
    val extents: Vector2 = Vector2.Zero,
    val vertices: Array<Vector2> = emptyArray(),
    val subsumedByPlaneId: Long = 0,
) {}

internal fun TrackingState.Companion.fromOpenXrTrackingState(trackingState: Int): TrackingState =
    when (trackingState) {
        0 -> TrackingState.PAUSED // XR_TRACKING_STATE_PAUSED_ANDROID
        1 -> TrackingState.STOPPED // XR_TRACKING_STATE_STOPPED_ANDROID
        2 -> TrackingState.TRACKING // XR_TRACKING_STATE_TRACKING_ANDROID
        else -> {
            throw IllegalArgumentException("Invalid tracking state.")
        }
    }
