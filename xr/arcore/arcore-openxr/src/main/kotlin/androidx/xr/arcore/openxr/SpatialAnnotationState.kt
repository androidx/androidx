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

package androidx.xr.arcore.openxr

import androidx.xr.arcore.runtime.SpatialAnnotation
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2

/**
 * Represents the current state of a [SpatialAnnotation] instance's mutable fields.
 *
 * @property trackingState the [TrackingState] value describing if the spatial annotation is being
 *   updated
 * @property pose the pose of the detected spatial annotation
 * @property upperLeft the upper left physical boundary of the tracked spatial quad
 * @property upperRight the upper right physical boundary of the tracked spatial quad
 * @property lowerRight the lower right physical boundary of the tracked spatial quad
 * @property lowerLeft the lower left physical boundary of the tracked spatial quad
 */
internal data class SpatialAnnotationState(
    @JvmField val trackingState: TrackingState = TrackingState.PAUSED,
    @JvmField val pose: Pose? = null,
    @JvmField val upperLeft: Vector2? = null,
    @JvmField val upperRight: Vector2? = null,
    @JvmField val lowerRight: Vector2? = null,
    @JvmField val lowerLeft: Vector2? = null,
)
