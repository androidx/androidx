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

import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose

/**
 * Represents the current state of an [OpenXrAnchor] instance's mutable fields.
 *
 * @property trackingState the [TrackingState] value describing if the anchor is being updated
 * @property pose the [Pose] of the center of the detected anchor
 */
@Suppress("DataClassDefinition")
internal data class AnchorState(
    val trackingState: TrackingState = TrackingState.PAUSED,
    val pose: Pose? = Pose(),
) {
    init {
        require(pose != null || trackingState == TrackingState.STOPPED) {
            "Pose cannot be null if tracking state is not STOPPED."
        }
    }
}
